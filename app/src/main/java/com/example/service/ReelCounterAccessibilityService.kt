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
 * Strictly scoped AccessibilityService monitoring ONLY the supported short-video packages:
 * Instagram (+Lite), TikTok (Global/Asia/+Lite), YouTube Shorts, Facebook (+Lite), and Snapchat (+Lite).
 * All counts merge seamlessly into one unified daily total in ReelRepository.
 *
 * Key guarantees:
 * 1. Works across modern YouTube Litho Shorts and obfuscated TikTok ViewPagers as well as IG/FB/SC.
 * 2. Floating counter pill ONLY appears while inside a short-video scroll feed and automatically
 *    vanishes when exiting the scroll feed, switching apps, or locking the screen.
 * 3. Scrolling up & down on the same video NEVER double-counts (directional filter + session
 *    high-water marks + in-memory video content fingerprint LRU deduplication).
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
                detail = "Deteksi live UwesTo aktif kanggo IG, TikTok, YouTube Shorts, FB, lan Snapchat."
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
                    hideOverlayAndResetSession()
                    return
                }

                if (currentFeedApp != app) {
                    resetScrollTracking()
                    currentFeedApp = app
                }

                // Seed the initial video fingerprint so swiping away & back to video #0 won't re-count
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

            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                val root = rootInActiveWindow
                if (!isStrictlyShortVideoFeed(app, event, root)) {
                    if (isPillVisible) {
                        hideOverlayAndResetSession()
                    }
                    return
                }

                // Reject horizontal carousels (Stories, photo slides, tabs)
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
                    event.scrollDeltaX != 0 &&
                    abs(event.scrollDeltaX) >= abs(event.scrollDeltaY)
                ) {
                    return
                }

                if (currentFeedApp != app) {
                    resetScrollTracking()
                    currentFeedApp = app
                }

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
                                body = "Gabungan scroll-mu wis ${result.newTotal}/${result.dailyLimit} dino iki. Siap-siap mandek sik yo."
                            )
                        }
                        if (result.interception !is ActiveInterception.None) {
                            maybeShowBlocker(result.interception)
                        }
                    } else if (settings.floatingOverlayEnabled && !isPillVisible) {
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
     * Evaluates whether a scroll/page-selection event moved forward to a genuinely NEW video.
     * Supports IG Reels, TikTok VerticalViewPager, YouTube Litho Shorts, FB Reels, and Snapchat
     * while blocking up-down scrolling on the same video.
     */
    private fun evaluateNewVideoScroll(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?,
        nowMs: Long
    ): Boolean {
        val fromIndex = event.fromIndex
        val scrollY = event.scrollY
        val deltaY = event.scrollDeltaY
        val isPageSelected = event.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED

        // 1. Reject backward / upward scroll immediately (NEVER lower high-water marks!)
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

        // 2. Debounce multi-frame events from a single swipe gesture
        if (nowMs - lastScrollEventMs < MIN_REEL_INTERVAL_MS) {
            if (fromIndex >= 0) lastSeenIndex = fromIndex
            if (scrollY >= 0) lastSeenScrollY = scrollY
            return false
        }

        // 3. Compute in-memory structural fingerprint of the currently visible video
        val fingerprint = computeActiveVideoFingerprint(root)
        val hasBrandNewFingerprint = fingerprint != 0 &&
            fingerprint != lastFingerprintHash &&
            fingerprint !in seenVideoFingerprints

        // If we got a valid fingerprint and it's already seen, ALWAYS block re-counting!
        if (fingerprint != 0 && !hasBrandNewFingerprint) {
            if (fromIndex >= 0) lastSeenIndex = fromIndex
            if (scrollY >= 0) lastSeenScrollY = scrollY
            return false
        }

        // 4. Evaluate forward progression across all Android pager/recycler implementations
        val progressedForward: Boolean = when {
            // Case A: Pager / RecyclerView provides fromIndex
            fromIndex >= 0 -> {
                lastSeenIndex = fromIndex
                when {
                    maxReachedIndex == -1 -> {
                        maxReachedIndex = fromIndex
                        fromIndex > 0 || hasBrandNewFingerprint
                    }
                    fromIndex > maxReachedIndex -> {
                        maxReachedIndex = fromIndex
                        true
                    }
                    // Windowed / recycled RecyclerView (common in YouTube Shorts & TikTok)
                    // where fromIndex stays constant while video content truly changed:
                    fromIndex == maxReachedIndex && hasBrandNewFingerprint && (deltaY > 0 || isPageSelected) -> {
                        true
                    }
                    else -> false
                }
            }

            // Case B: Vertical scrollY reported without fromIndex
            scrollY >= 0 -> {
                lastSeenScrollY = scrollY
                when {
                    maxReachedScrollY == -1 -> {
                        maxReachedScrollY = scrollY
                        scrollY >= MIN_VERTICAL_SWIPE_PX || hasBrandNewFingerprint
                    }
                    scrollY >= maxReachedScrollY + MIN_VERTICAL_SWIPE_PX -> {
                        maxReachedScrollY = scrollY
                        true
                    }
                    hasBrandNewFingerprint && deltaY >= MIN_VERTICAL_SWIPE_PX -> {
                        maxReachedScrollY = maxOf(maxReachedScrollY, scrollY)
                        true
                    }
                    else -> false
                }
            }

            // Case C: Custom ViewPager (TikTok VerticalViewPager) reporting only deltaY or TYPE_VIEW_SELECTED
            else -> {
                deltaY >= MIN_VERTICAL_SWIPE_PX || (isPageSelected && hasBrandNewFingerprint)
            }
        }

        if (!progressedForward) return false

        if (fingerprint != 0) {
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
     * Computes a lightweight, in-memory-only hash of non-sensitive video labels (e.g. creator handle,
     * caption snippet, audio title) purely to recognize when the same video is scrolled back & forth.
     * Never stored to disk or transmitted anywhere.
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
        if (node == null || depth > 9 || out.size >= 5) return
        val viewId = node.viewIdResourceName?.lowercase().orEmpty()
        val rawText = node.text?.toString()?.trim().orEmpty()
        val rawDesc = node.contentDescription?.toString()?.trim().orEmpty()
        val candidate = if (rawText.isNotEmpty()) rawText else rawDesc

        val isIdentityById = viewId.contains("author") ||
            viewId.contains("username") ||
            viewId.contains("channel") ||
            viewId.contains("title") ||
            viewId.contains("caption") ||
            viewId.contains("audio") ||
            viewId.contains("reel_main") ||
            viewId.contains("desc")

        // Also capture @handles or distinctive multi-word captions (works on obfuscated TikTok & YouTube Litho!)
        val isHandleOrCaption = candidate.startsWith("@") ||
            (candidate.length in 10..70 && candidate.contains(" ") && !isGenericUiChromeLabel(candidate))

        if ((isIdentityById || isHandleOrCaption) && candidate.length in 2..80 && !isDynamicNumber(candidate)) {
            if (candidate !in out) {
                out.add(candidate)
            }
        }

        val count = node.childCount.coerceAtMost(14)
        for (i in 0 until count) {
            collectShortVideoIdentityTokens(node.getChild(i), out, depth + 1)
            if (out.size >= 5) return
        }
    }

    private fun isDynamicNumber(text: String): Boolean {
        val cleaned = text.replace(",", "").replace(".", "").replace(":", "").trim()
        return cleaned.all { it.isDigit() || it == 'K' || it == 'M' || it == 'k' || it == 'm' }
    }

    private fun isGenericUiChromeLabel(text: String): Boolean {
        val lower = text.lowercase()
        return lower == "for you" || lower == "untuk anda" || lower == "following" ||
            lower == "mengikuti" || lower == "home" || lower == "beranda" ||
            lower == "shorts" || lower == "reels" || lower == "subscriptions" ||
            lower == "subscribe" || lower == "like this video" || lower == "dislike this video"
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
     * Hides the pill immediately when the user exits Reels/TikTok/Shorts, switches to another app,
     * or turns off the screen.
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
     * Platform-specific verification so short-video feeds on Instagram Reels, TikTok FYP/Following,
     * YouTube Shorts (including Litho UI), Facebook Reels, and Snapchat Spotlight are all reliably
     * detected while excluding comments, DMs, search, profiles, and regular home feeds.
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

        if (root != null && hasFocusedTextInput(root, depth = 0)) {
            return false
        }

        val scan = scanHierarchySignals(root)

        return when (app) {
            SupportedApp.INSTAGRAM -> {
                val igIds = listOf(
                    "clips_viewer", "clips_video", "clips_swipe", "clips_ufi",
                    "clips_author", "clips_media", "reels_viewer", "reel_viewer"
                )
                igIds.any { sourceId.contains(it) } ||
                    scan.hasAnyViewId(igIds) ||
                    scan.hasSelectedTab("reels")
            }

            SupportedApp.YOUTUBE -> {
                val ytIds = listOf(
                    "reel_watch", "reel_recycler", "shorts_player", "reel_player",
                    "reel_progress", "reel_dyn", "reel_like", "reel_comment",
                    "reel_share", "reel_pivot", "shorts_pivot", "reel_main"
                )
                val hasShortsActionOverlay = (scan.hasLabel("remix") || scan.hasLabel("shorts")) &&
                    (scan.hasLabel("like") || scan.hasLabel("suka") || scan.hasLabel("dislike") || scan.hasLabel("tidak suka"))

                ytIds.any { sourceId.contains(it) } ||
                    scan.hasAnyViewId(ytIds) ||
                    scan.hasSelectedTab("shorts") ||
                    hasShortsActionOverlay
            }

            SupportedApp.TIKTOK -> {
                // Reject non-feed tabs/screens in TikTok (Profile, Inbox, Shop, Search, Comments)
                val onExcludedTikTokTab = scan.hasSelectedTab("profile") ||
                    scan.hasSelectedTab("profil") ||
                    scan.hasSelectedTab("inbox") ||
                    scan.hasSelectedTab("kotak masuk") ||
                    scan.hasSelectedTab("shop") ||
                    scan.hasSelectedTab("toko")

                if (onExcludedTikTokTab || scan.hasCommentDrawerVisible) {
                    return false
                }

                val hasFeedHeader = scan.hasLabel("for you") ||
                    scan.hasLabel("untuk anda") ||
                    scan.hasLabel("following") ||
                    scan.hasLabel("mengikuti") ||
                    scan.hasSelectedTab("home") ||
                    scan.hasSelectedTab("beranda") ||
                    scan.hasSelectedTab("friends") ||
                    scan.hasSelectedTab("teman")

                val hasTikTokActions = (scan.hasLabel("like") || scan.hasLabel("suka")) &&
                    (scan.hasLabel("share") || scan.hasLabel("bagikan") || scan.hasLabel("comment") || scan.hasLabel("komentar"))

                val isVerticalPagerEvent = className.contains("viewpager") || className.contains("recyclerview")

                hasFeedHeader || hasTikTokActions || isVerticalPagerEvent
            }

            SupportedApp.FACEBOOK -> {
                val fbIds = listOf("reels_viewer", "fb_shorts", "short_form_video", "reels_overlay", "reels_tab")
                fbIds.any { sourceId.contains(it) } ||
                    scan.hasAnyViewId(fbIds) ||
                    scan.hasSelectedTab("reels")
            }

            SupportedApp.SNAPCHAT -> {
                val scIds = listOf("spotlight", "opera_view", "spotlight_viewer")
                scIds.any { sourceId.contains(it) } ||
                    scan.hasAnyViewId(scIds) ||
                    scan.hasSelectedTab("spotlight")
            }
        }
    }

    private class HierarchySignals {
        val viewIds = ArrayList<String>(32)
        val labels = ArrayList<String>(32)
        val selectedTabs = ArrayList<String>(8)
        var hasCommentDrawerVisible: Boolean = false

        fun hasAnyViewId(markers: List<String>): Boolean {
            return viewIds.any { id -> markers.any { marker -> id.contains(marker) } }
        }

        fun hasLabel(keyword: String): Boolean {
            return labels.any { it.contains(keyword) }
        }

        fun hasSelectedTab(keyword: String): Boolean {
            return selectedTabs.any { it == keyword || it.contains(keyword) }
        }
    }

    private fun scanHierarchySignals(root: AccessibilityNodeInfo?): HierarchySignals {
        val signals = HierarchySignals()
        if (root == null) return signals
        traverseSignals(root, signals, depth = 0)
        return signals
    }

    private fun traverseSignals(
        node: AccessibilityNodeInfo?,
        signals: HierarchySignals,
        depth: Int
    ) {
        if (node == null || depth > 10 || signals.viewIds.size > 120) return

        val viewId = node.viewIdResourceName?.lowercase().orEmpty()
        if (viewId.isNotEmpty()) {
            signals.viewIds.add(viewId)
            if (viewId.contains("comment_list") || viewId.contains("comment_composer") || viewId.contains("comment_input")) {
                signals.hasCommentDrawerVisible = true
            }
        }

        val text = node.text?.toString()?.trim()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.trim()?.lowercase().orEmpty()

        if (text.isNotEmpty() && text.length <= 40) {
            signals.labels.add(text)
            if (node.isSelected) signals.selectedTabs.add(text)
            if (text.contains("add comment") || text.contains("tambahkan komentar") || text.contains("tambah komentar")) {
                signals.hasCommentDrawerVisible = true
            }
        }
        if (desc.isNotEmpty() && desc.length <= 60) {
            signals.labels.add(desc)
            if (node.isSelected) signals.selectedTabs.add(desc)
        }

        val count = node.childCount.coerceAtMost(18)
        for (i in 0 until count) {
            traverseSignals(node.getChild(i), signals, depth + 1)
        }
    }

    private fun hasFocusedTextInput(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 7) return false
        val cls = node.className?.toString().orEmpty()
        if (node.isFocused && (node.isEditable || cls.contains("EditText"))) {
            return true
        }
        val count = node.childCount.coerceAtMost(12)
        for (i in 0 until count) {
            if (hasFocusedTextInput(node.getChild(i), depth + 1)) return true
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
        private const val MIN_REEL_INTERVAL_MS = 1000L
        private const val MIN_VERTICAL_SWIPE_PX = 160
        private const val WATCHDOG_INTERVAL_MS = 400L
        private const val MAX_SEEN_FINGERPRINTS = 40
    }
}
