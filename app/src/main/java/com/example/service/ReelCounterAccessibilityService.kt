package com.example.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.UwesToApplication
import com.example.model.ActiveInterception
import com.example.model.SupportedApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Strictly scoped AccessibilityService monitoring ONLY the 7 supported short-video packages:
 * Instagram (+Lite), YouTube, Facebook (+Lite), and Snapchat (+Lite).
 *
 * Key guarantees:
 * 1. Floating counter pill ONLY appears while actively in a Reels/Shorts/Spotlight scroll feed,
 *    and automatically disappears as soon as the user exits the scroll feed, switches to another app,
 *    or turns off the screen (via lightweight foreground watchdog while pill is visible).
 * 2. Up-down scrolling on the same video NEVER double-counts:
 *    - Rejects backward/upward scrolls (negative deltaY or decreasing index/scrollY).
 *    - Tracks session high-water marks (maxReachedIndex & maxReachedScrollY).
 *    - Deduplicates active video fingerprints in-memory (hash-only, never stored/sent).
 */
class ReelCounterAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var overlayManager: FloatingOverlayManager

    private var currentFeedApp: SupportedApp? = null
    private var isPillVisible: Boolean = false
    private var watchdogJob: Job? = null
    private var consecutiveNullRootChecks: Int = 0

    // Per-session anti-up-down & deduplication state
    private var lastScrollEventMs: Long = 0L
    private var lastSeenIndex: Int = -1
    private var maxReachedIndex: Int = -1
    private var lastSeenScrollY: Int = -1
    private var maxReachedScrollY: Int = -1
    private var lastFingerprintHash: Int = 0
    private val seenVideoFingerprints = LinkedHashSet<Int>()
    private var lastBlockerShownMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = FloatingOverlayManager(this)
        val repository = (application as UwesToApplication).repository
        repository.setAccessibilityServiceConnected(true)
        serviceScope.launch {
            repository.logPermissionEvent(
                eventType = "ACCESSIBILITY_ON",
                detail = "Deteksi live UwesTo aktif kanggo IG, YT, FB, lan Snapchat."
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        val app = SupportedApp.fromPackageName(pkg)
        if (app == null) {
            hideOverlayAndResetSession()
            return
        }

        val repository = (application as UwesToApplication).repository

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val root = rootInActiveWindow
                val inShortFeed = isStrictlyShortVideoFeed(app, event, root)
                if (!inShortFeed) {
                    // User navigated away from Reels/Shorts (e.g., to Home, DM, Profile, Search)
                    hideOverlayAndResetSession()
                    return
                }

                if (currentFeedApp != app) {
                    resetScrollTracking()
                    currentFeedApp = app
                }

                // Seed the initial video fingerprint so scrolling away & back to video #0 won't re-count
                val initialHash = computeActiveVideoFingerprint(root)
                if (initialHash != 0) {
                    lastFingerprintHash = initialHash
                    recordSeenFingerprint(initialHash)
                }

                serviceScope.launch {
                    val block = repository.evaluateCurrentFeedBlock(app)
                    if (block !is ActiveInterception.None) {
                        maybeShowBlocker(block)
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val root = rootInActiveWindow
                if (!isStrictlyShortVideoFeed(app, event, root)) {
                    // Scrolling in comments, DMs, regular feed, or profile -> hide pill & ignore
                    if (isPillVisible) {
                        hideOverlayAndResetSession()
                    }
                    return
                }

                // Reject horizontal carousels (Stories, photo slides, tabs)
                if (event.scrollDeltaX != 0 && abs(event.scrollDeltaX) >= abs(event.scrollDeltaY)) {
                    return
                }

                if (currentFeedApp != app) {
                    resetScrollTracking()
                    currentFeedApp = app
                }

                // Show/refresh pill because user is actively scrolling inside Reels/Shorts
                val now = System.currentTimeMillis()
                val shouldCount = evaluateNewVideoScroll(event, root, now)

                serviceScope.launch {
                    val settings = repository.settingsFlow.first()
                    if (shouldCount) {
                        val result = repository.incrementScroll(app = app, delta = 1)
                        if (settings.floatingOverlayEnabled) {
                            showPillAndStartWatchdog(
                                todayCount = result.newTotal,
                                dailyLimit = result.dailyLimit,
                                sourceApp = app
                            )
                        }
                        if (result.nearLimitWarning) {
                            postStatusNotification(
                                title = "Awas Ndes, Meh Entek Kuotane!",
                                body = "Wis ${result.newTotal}/${result.dailyLimit} reels dino iki. Siap-siap mandek sik yo."
                            )
                        }
                        if (result.interception !is ActiveInterception.None) {
                            maybeShowBlocker(result.interception)
                        }
                    } else if (settings.floatingOverlayEnabled && !isPillVisible) {
                        // Still show current count pill when actively scrolling in Reels even if same video
                        val todayLog = repository.todayLogFlow.first()
                        showPillAndStartWatchdog(
                            todayCount = todayLog.totalCount,
                            dailyLimit = settings.dailyLimit,
                            sourceApp = app
                        )
                    }
                }
            }
        }
    }

    /**
     * Evaluates whether a scroll event represents moving forward to a genuinely NEW reel/short.
     * Returns false for:
     * - Scrolling UP / backward to a previous video.
     * - Scrolling DOWN again over a video already seen in this session (up-down-up-down loop).
     * - Micro-scrolls or rapid duplicate events inside the debounce window.
     */
    private fun evaluateNewVideoScroll(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?,
        nowMs: Long
    ): Boolean {
        val fromIndex = event.fromIndex
        val scrollY = event.scrollY
        val deltaY = event.scrollDeltaY

        // 1. Detect backward / upward scroll immediately (do NOT count, do NOT lower high-water mark!)
        val isBackwardByDelta = deltaY < 0
        val isBackwardByIndex = fromIndex >= 0 && lastSeenIndex >= 0 && fromIndex < lastSeenIndex
        val isBackwardByScrollY = scrollY >= 0 && lastSeenScrollY >= 0 && scrollY < lastSeenScrollY

        if (isBackwardByDelta || isBackwardByIndex || isBackwardByScrollY) {
            if (fromIndex >= 0) lastSeenIndex = fromIndex
            if (scrollY >= 0) lastSeenScrollY = scrollY
            val currentHash = computeActiveVideoFingerprint(root)
            if (currentHash != 0) lastFingerprintHash = currentHash
            return false
        }

        // 2. Debounce rapid multi-frame scroll callbacks from a single swipe gesture
        if (nowMs - lastScrollEventMs < MIN_REEL_INTERVAL_MS) {
            if (fromIndex >= 0) lastSeenIndex = fromIndex
            if (scrollY >= 0) lastSeenScrollY = scrollY
            return false
        }

        // 3. Enforce high-water mark on pager/list index (prevents 1 -> 2 -> 1 -> 2 re-counting!)
        val advancedBeyondMaxIndex: Boolean? = if (fromIndex >= 0) {
            lastSeenIndex = fromIndex
            if (maxReachedIndex == -1) {
                maxReachedIndex = fromIndex
                fromIndex > 0
            } else if (fromIndex > maxReachedIndex) {
                maxReachedIndex = fromIndex
                true
            } else {
                // Still <= previously reached index (user scrolled back down over already-watched video)
                false
            }
        } else null

        if (advancedBeyondMaxIndex == false) {
            return false
        }

        // 4. Enforce high-water mark on scrollY when index isn't available
        val advancedBeyondMaxScrollY: Boolean? = if (advancedBeyondMaxIndex == null && scrollY >= 0) {
            lastSeenScrollY = scrollY
            if (maxReachedScrollY == -1) {
                maxReachedScrollY = scrollY
                scrollY >= MIN_VERTICAL_SWIPE_PX
            } else if (scrollY >= maxReachedScrollY + MIN_VERTICAL_SWIPE_PX) {
                maxReachedScrollY = scrollY
                true
            } else {
                false
            }
        } else null

        if (advancedBeyondMaxScrollY == false) {
            return false
        }

        // 5. Fallback pixel delta check when neither fromIndex nor scrollY is provided
        if (advancedBeyondMaxIndex == null && advancedBeyondMaxScrollY == null) {
            if (deltaY < MIN_VERTICAL_SWIPE_PX) {
                return false
            }
        }

        // 6. Content fingerprint check: even if ViewPager recycles indices, never count the same video twice!
        val fingerprint = computeActiveVideoFingerprint(root)
        if (fingerprint != 0) {
            if (fingerprint == lastFingerprintHash || fingerprint in seenVideoFingerprints) {
                lastFingerprintHash = fingerprint
                return false
            }
            lastFingerprintHash = fingerprint
            recordSeenFingerprint(fingerprint)
        }

        lastScrollEventMs = nowMs
        return true
    }

    private fun recordSeenFingerprint(hash: Int) {
        if (seenVideoFingerprints.size >= MAX_SEEN_FINGERPRINTS) {
            val oldest = seenVideoFingerprints.firstOrNull()
            if (oldest != null) seenVideoFingerprints.remove(oldest)
        }
        seenVideoFingerprints.add(hash)
    }

    /**
     * Computes a lightweight, in-memory-only hash of structural labels on the current Reel/Short
     * (never stored to disk or transmitted) purely to detect when the user scrolls back and forth
     * onto the exact same video.
     */
    private fun computeActiveVideoFingerprint(root: AccessibilityNodeInfo?): Int {
        if (root == null) return 0
        val tokens = ArrayList<String>(6)
        collectShortVideoIdentityTokens(root, tokens, depth = 0)
        if (tokens.isEmpty()) return 0
        return tokens.joinToString("|").hashCode()
    }

    private fun collectShortVideoIdentityTokens(
        node: AccessibilityNodeInfo?,
        out: MutableList<String>,
        depth: Int
    ) {
        if (node == null || depth > 6 || out.size >= 5) return
        val viewId = node.viewIdResourceName?.lowercase().orEmpty()
        val isIdentityNode = viewId.contains("author") ||
            viewId.contains("username") ||
            viewId.contains("channel") ||
            viewId.contains("title") ||
            viewId.contains("caption") ||
            viewId.contains("audio") ||
            viewId.contains("reel_main")

        if (isIdentityNode) {
            val label = node.text?.toString()?.trim()
                ?: node.contentDescription?.toString()?.trim()
            if (!label.isNullOrBlank() && label.length in 2..80) {
                out.add(label)
            }
        }

        val count = node.childCount.coerceAtMost(12)
        for (i in 0 until count) {
            collectShortVideoIdentityTokens(node.getChild(i), out, depth + 1)
            if (out.size >= 5) return
        }
    }

    private fun showPillAndStartWatchdog(
        todayCount: Int,
        dailyLimit: Int,
        sourceApp: SupportedApp
    ) {
        isPillVisible = true
        consecutiveNullRootChecks = 0
        overlayManager.showOrUpdateCounterPill(
            todayCount = todayCount,
            dailyLimit = dailyLimit,
            sourceApp = sourceApp
        )
        startForegroundWatchdog()
    }

    /**
     * Runs ONLY while the floating pill is visible.
     * Checks every 400ms whether the user left the Reels/Shorts feed, switched to another app
     * (like Chrome, Home, WhatsApp, or UwesTo), or turned off the screen — and hides the pill immediately.
     */
    private fun startForegroundWatchdog() {
        if (watchdogJob?.isActive == true) return
        watchdogJob = serviceScope.launch {
            while (isActive && isPillVisible) {
                delay(WATCHDOG_INTERVAL_MS)

                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (powerManager != null && !powerManager.isInteractive) {
                    hideOverlayAndResetSession()
                    break
                }

                val root = rootInActiveWindow
                if (root == null) {
                    consecutiveNullRootChecks += 1
                    // 2 consecutive null checks (~800ms) confirms user left the supported app
                    if (consecutiveNullRootChecks >= 2) {
                        hideOverlayAndResetSession()
                        break
                    }
                    continue
                }

                consecutiveNullRootChecks = 0
                val activePkg = root.packageName?.toString().orEmpty()
                val activeApp = SupportedApp.fromPackageName(activePkg)

                if (activeApp == null || !isStrictlyShortVideoFeed(activeApp, null, root)) {
                    hideOverlayAndResetSession()
                    break
                }
            }
        }
    }

    private fun hideOverlayAndResetSession() {
        isPillVisible = false
        currentFeedApp = null
        consecutiveNullRootChecks = 0
        watchdogJob?.cancel()
        watchdogJob = null
        resetScrollTracking()
        if (::overlayManager.isInitialized) {
            overlayManager.hideCounterPill()
            overlayManager.hideBlockerOverlay()
        }
    }

    private fun resetScrollTracking() {
        lastSeenIndex = -1
        maxReachedIndex = -1
        lastSeenScrollY = -1
        maxReachedScrollY = -1
        lastFingerprintHash = 0
        seenVideoFingerprints.clear()
    }

    /**
     * Strict short-video feed verification so the pill and counter ONLY activate inside
     * Reels / Shorts / Spotlight (never on Home Feed, Stories, DMs, Comments, Profile, or other apps).
     */
    private fun isStrictlyShortVideoFeed(
        app: SupportedApp,
        event: AccessibilityEvent?,
        root: AccessibilityNodeInfo?
    ): Boolean {
        val className = event?.className?.toString()?.lowercase().orEmpty()
        val contentDesc = event?.contentDescription?.toString()?.lowercase().orEmpty()
        val sourceId = event?.source?.viewIdResourceName?.lowercase().orEmpty()
        val combinedMeta = "$className $contentDesc $sourceId"

        val excludedTokens = listOf(
            "direct", "inbox", "chat", "message", "comment",
            "search", "notification", "profile", "setting", "share_sheet", "stories_tray"
        )
        if (excludedTokens.any { combinedMeta.contains(it) }) {
            return false
        }

        val markers = when (app) {
            SupportedApp.INSTAGRAM -> listOf(
                "clips_viewer", "clips_video", "clips_swipe", "clips_ufi",
                "clips_author", "clips_media", "reels_viewer", "reel_viewer"
            )
            SupportedApp.YOUTUBE -> listOf(
                "reel_watch", "reel_recycler", "shorts_player",
                "reel_player", "reel_progress", "reel_dyn"
            )
            SupportedApp.FACEBOOK -> listOf(
                "reels_viewer", "fb_shorts", "short_form_video", "reels_overlay"
            )
            SupportedApp.SNAPCHAT -> listOf(
                "spotlight", "opera_view", "spotlight_viewer"
            )
        }

        if (markers.any { sourceId.contains(it) }) return true
        if (root != null && hasMatchingViewId(root, markers, depth = 0)) return true

        return false
    }

    private fun hasMatchingViewId(
        node: AccessibilityNodeInfo?,
        markers: List<String>,
        depth: Int
    ): Boolean {
        if (node == null || depth > 7) return false
        val viewId = node.viewIdResourceName?.lowercase().orEmpty()
        if (markers.any { viewId.contains(it) }) return true

        val count = node.childCount.coerceAtMost(14)
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            if (hasMatchingViewId(child, markers, depth + 1)) return true
        }
        return false
    }

    private fun maybeShowBlocker(interception: ActiveInterception) {
        val now = System.currentTimeMillis()
        if (now - lastBlockerShownMs < 1500L) return
        lastBlockerShownMs = now
        overlayManager.showBlockerOverlay(
            interception = interception,
            onExitFeed = {
                performGlobalAction(GLOBAL_ACTION_BACK)
            },
            onDismissOverlay = {
                overlayManager.hideBlockerOverlay()
            }
        )
    }

    override fun onInterrupt() {
        handleServiceStopped("Layanan deteksi UwesTo mandek dadakan. Bukak aplikasi kanggo nguripke maneh.")
    }

    override fun onDestroy() {
        hideOverlayAndResetSession()
        handleServiceStopped("Deteksi otomatis UwesTo dipateni sistem utawa pengguna.")
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleServiceStopped(reason: String) {
        val repository = (application as? UwesToApplication)?.repository ?: return
        repository.setAccessibilityServiceConnected(false)
        serviceScope.launch {
            repository.logPermissionEvent(
                eventType = "SERVICE_STOPPED",
                detail = reason,
                markDayInterrupted = true
            )
        }
        postStatusNotification(
            title = "UwesTo Mandek Ngitung, Ndes!",
            body = "Izin Accessibility lagi mati. Tap kanggo ngaktifke maneh ben hitungan ora bolong."
        )
    }

    private fun postStatusNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "uwesto_status_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Status Deteksi UwesTo",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        manager.notify(4201, notification)
    }

    companion object {
        private const val MIN_REEL_INTERVAL_MS = 1250L
        private const val MIN_VERTICAL_SWIPE_PX = 200
        private const val WATCHDOG_INTERVAL_MS = 400L
        private const val MAX_SEEN_FINGERPRINTS = 40
    }
}
