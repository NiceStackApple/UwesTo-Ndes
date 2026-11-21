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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Low-latency, strictly scoped AccessibilityService for:
 * Instagram (+Lite), TikTok (Global/Asia/+Lite), YouTube Shorts, Facebook (+Lite), and Snapchat (+Lite).
 *
 * Responsiveness + Accuracy Design:
 * 1. Zero-IPC hot path on TYPE_VIEW_SCROLLED (feed verification cached per window + refreshed lazily)
 *    and optimistic instant floating-pill update before SQLite flush (~1ms visual feedback).
 * 2. Instant anti-up-down protection without mid-animation text blocking:
 *    - Primary: strict High-Water Mark on pager/list index (fromIndex/toIndex) & scrollY.
 *    - Secondary (for obfuscated/recycled pagers like TikTok & YouTube Litho): backtrackDepth counter
 *      that absorbs downward re-scrolls after upward swipes until advancing to a brand-new video.
 * 3. Floating pill ONLY appears inside active short-video scroll feeds and disappears automatically
 *    when exiting the scroll feed, switching apps, or locking the screen.
 */
class ReelCounterAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var overlayManager: FloatingOverlayManager

    // Live cached repository state so every scroll updates UI immediately with zero blocking .first() calls
    @Volatile private var cachedTodayCount: Int = 0
    @Volatile private var cachedDailyLimit: Int = 50
    @Volatile private var cachedOverlayEnabled: Boolean = true

    // Active feed session cache (eliminates heavy Binder IPC tree walks on every scroll frame)
    private var currentFeedApp: SupportedApp? = null
    private var isConfirmedInShortFeed: Boolean = false
    private var lastFeedVerifyMs: Long = 0L
    private var isPillVisible: Boolean = false
    private var watchdogJob: Job? = null
    private var consecutiveNullRootChecks: Int = 0

    // Per-session anti-up-down & gesture coalescing state
    private var lastCountedEventMs: Long = 0L
    private var lastBackwardEventMs: Long = 0L
    private var lastSeenIndex: Int = -1
    private var maxReachedIndex: Int = -1
    private var lastSeenScrollY: Int = -1
    private var maxReachedScrollY: Int = -1
    private var backtrackDepth: Int = 0
    private var pendingForwardDeltaY: Int = 0
    private var lastBlockerShownMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = FloatingOverlayManager(this)
        val repository = (application as UwesToApplication).repository
        repository.setAccessibilityServiceConnected(true)

        // Keep live counts & settings hot in memory for instant zero-lag pill updates
        serviceScope.launch {
            repository.todayLogFlow.collect { log ->
                cachedTodayCount = log.totalCount
            }
        }
        serviceScope.launch {
            repository.settingsFlow.collect { settings ->
                cachedDailyLimit = settings.dailyLimit
                cachedOverlayEnabled = settings.floatingOverlayEnabled
                if (!settings.floatingOverlayEnabled && isPillVisible) {
                    overlayManager.hideCounterPill()
                    isPillVisible = false
                }
            }
        }
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
        val now = System.currentTimeMillis()

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val root = rootInActiveWindow
                val inShortFeed = isStrictlyShortVideoFeed(app, event, root)
                lastFeedVerifyMs = now
                if (!inShortFeed) {
                    hideOverlayAndResetSession()
                    return
                }

                if (currentFeedApp != app) {
                    resetScrollTracking()
                    currentFeedApp = app
                }
                isConfirmedInShortFeed = true

                serviceScope.launch {
                    val block = repository.evaluateCurrentFeedBlock(app)
                    if (block !is ActiveInterception.None) {
                        maybeShowBlocker(block)
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                // Fast O(1) rejection for horizontal carousels, comments, chat, and search
                if (isExcludedScrollEventFast(event)) {
                    if (isPillVisible && isLikelyNonFeedOverlayEvent(event)) {
                        hideOverlayAndResetSession()
                    }
                    return
                }

                // Use cached feed state on the hot path; only re-verify tree every 1200ms or on app switch
                val needsVerify = currentFeedApp != app ||
                    !isConfirmedInShortFeed ||
                    (now - lastFeedVerifyMs) > FEED_VERIFY_CACHE_MS

                if (needsVerify) {
                    val root = rootInActiveWindow
                    val verified = isStrictlyShortVideoFeed(app, event, root)
                    lastFeedVerifyMs = now
                    if (!verified) {
                        if (isPillVisible) hideOverlayAndResetSession()
                        return
                    }
                    if (currentFeedApp != app) {
                        resetScrollTracking()
                        currentFeedApp = app
                    }
                    isConfirmedInShortFeed = true
                }

                val shouldCount = evaluateFastNewVideoScroll(event, now)

                if (shouldCount) {
                    // Optimistic instant pill update (~1ms latency!)
                    val optimisticTotal = cachedTodayCount + 1
                    cachedTodayCount = optimisticTotal
                    if (cachedOverlayEnabled) {
                        showPillAndStartWatchdog(
                            todayCount = optimisticTotal,
                            dailyLimit = cachedDailyLimit,
                            sourceApp = app
                        )
                    }

                    // Persist asynchronously & reconcile exact Room count + limits/breaks
                    serviceScope.launch {
                        val result = repository.incrementScroll(app = app, delta = 1)
                        cachedTodayCount = result.newTotal
                        cachedDailyLimit = result.dailyLimit
                        if (cachedOverlayEnabled && isPillVisible) {
                            overlayManager.showOrUpdateCounterPill(
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
                    }
                } else if (cachedOverlayEnabled && !isPillVisible) {
                    // Show live pill immediately when user starts scrolling inside a confirmed feed
                    showPillAndStartWatchdog(
                        todayCount = cachedTodayCount,
                        dailyLimit = cachedDailyLimit,
                        sourceApp = app
                    )
                }
            }
        }
    }

    /**
     * Zero-IPC check rejecting horizontal scrolls (Stories/photo carousels) and obvious non-reel lists.
     */
    private fun isExcludedScrollEventFast(event: AccessibilityEvent): Boolean {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            event.scrollDeltaX != 0 &&
            abs(event.scrollDeltaX) >= abs(event.scrollDeltaY)
        ) {
            return true
        }
        return isLikelyNonFeedOverlayEvent(event)
    }

    private fun isLikelyNonFeedOverlayEvent(event: AccessibilityEvent): Boolean {
        val sourceId = event.source?.viewIdResourceName?.lowercase().orEmpty()
        val className = event.className?.toString()?.lowercase().orEmpty()
        val desc = event.contentDescription?.toString()?.lowercase().orEmpty()
        val meta = "$sourceId $className $desc"
        return meta.contains("comment") ||
            meta.contains("direct") ||
            meta.contains("inbox") ||
            meta.contains("chat") ||
            meta.contains("message") ||
            meta.contains("search") ||
            meta.contains("stories_tray") ||
            meta.contains("share_sheet")
    }

    /**
     * Snappy, zero-IPC scroll progression detector that:
     * - Reacts immediately on every genuine forward swipe to a new video (420ms swipe coalescing).
     * - Never double-counts scrolling up & down over the same videos (High-Water Mark + backtrackDepth).
     */
    private fun evaluateFastNewVideoScroll(
        event: AccessibilityEvent,
        nowMs: Long
    ): Boolean {
        val fromIndex = event.fromIndex
        val scrollY = event.scrollY
        val deltaY = event.scrollDeltaY
        val isPageSelected = event.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED

        // 1. Detect backward / upward scroll immediately (scrolling back to previous video)
        val isBackwardByDelta = deltaY < -20
        val isBackwardByIndex = fromIndex >= 0 && lastSeenIndex >= 0 && fromIndex < lastSeenIndex
        val isBackwardByScrollY = scrollY >= 0 && lastSeenScrollY >= 0 && scrollY < lastSeenScrollY - 20

        if (isBackwardByDelta || isBackwardByIndex || isBackwardByScrollY) {
            if (fromIndex >= 0) lastSeenIndex = fromIndex
            if (scrollY >= 0) lastSeenScrollY = scrollY
            pendingForwardDeltaY = 0

            // Count distinct upward swipes into backtrackDepth so scrolling back down doesn't re-count
            if (nowMs - lastBackwardEventMs >= SWIPE_COALESCE_MS) {
                backtrackDepth = (backtrackDepth + 1).coerceAtMost(50)
                lastBackwardEventMs = nowMs
            }
            return false
        }

        // 2. Priority Path A: Pager / RecyclerView reports fromIndex
        if (fromIndex >= 0) {
            val prevIndex = lastSeenIndex
            lastSeenIndex = fromIndex

            if (maxReachedIndex == -1) {
                // First scroll event of the feed session!
                // If user already moved onto index >= 1, count it immediately!
                maxReachedIndex = fromIndex
                if (fromIndex > 0) {
                    lastCountedEventMs = nowMs
                    backtrackDepth = 0
                    return true
                }
                return false
            }

            if (fromIndex > maxReachedIndex) {
                // Strictly beyond session high-water mark -> guaranteed brand-new video!
                maxReachedIndex = fromIndex
                backtrackDepth = 0
                if (nowMs - lastCountedEventMs >= SWIPE_COALESCE_MS) {
                    lastCountedEventMs = nowMs
                    return true
                }
                return false
            }

            // Still below maxReachedIndex -> user is scrolling back down over already-watched videos
            if (fromIndex < maxReachedIndex) {
                if (fromIndex > prevIndex && backtrackDepth > 0) {
                    backtrackDepth = (backtrackDepth - 1).coerceAtLeast(0)
                }
                return false
            }

            // fromIndex == maxReachedIndex:
            // Only happens on recycled/windowed pagers (e.g., YouTube Shorts / TikTok keeping index fixed)
            // Fall through ONLY if there is a strong positive vertical swipe delta or page selection
            if (deltaY <= 0 && !isPageSelected) {
                return false
            }
        }

        // 3. Priority Path B: scrollY reported without advancing fromIndex
        if (fromIndex < 0 && scrollY >= 0) {
            lastSeenScrollY = scrollY
            if (maxReachedScrollY == -1) {
                maxReachedScrollY = scrollY
                if (scrollY >= MIN_SWIPE_DELTA_PX) {
                    lastCountedEventMs = nowMs
                    backtrackDepth = 0
                    return true
                }
                return false
            }

            if (scrollY >= maxReachedScrollY + MIN_SWIPE_DELTA_PX) {
                maxReachedScrollY = scrollY
                backtrackDepth = 0
                if (nowMs - lastCountedEventMs >= SWIPE_COALESCE_MS) {
                    lastCountedEventMs = nowMs
                    return true
                }
                return false
            }

            // Still inside previously reached scrollY -> do not re-count!
            return false
        }

        // 4. Priority Path C: Delta-Y accumulation & TYPE_VIEW_SELECTED (TikTok VerticalViewPager / Litho Shorts)
        if (deltaY > 0) {
            pendingForwardDeltaY += deltaY
        }

        val triggeredForwardSwipe = pendingForwardDeltaY >= MIN_SWIPE_DELTA_PX || isPageSelected
        if (!triggeredForwardSwipe) return false

        // Coalesce multi-frame fling events of a single swipe
        if (nowMs - lastCountedEventMs < SWIPE_COALESCE_MS) {
            pendingForwardDeltaY = 0
            return false
        }

        pendingForwardDeltaY = 0

        // Absorb downward re-scrolls if user previously scrolled UP onto older videos
        if (backtrackDepth > 0) {
            backtrackDepth -= 1
            lastCountedEventMs = nowMs
            return false
        }

        lastCountedEventMs = nowMs
        return true
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
     * Lightweight watchdog running ONLY while the floating pill is visible.
     * Hides the pill immediately when the user exits Reels/TikTok/Shorts, switches apps,
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
                } else {
                    lastFeedVerifyMs = System.currentTimeMillis()
                }
            }
        }
    }

    private fun hideOverlayAndResetSession() {
        isPillVisible = false
        isConfirmedInShortFeed = false
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
        backtrackDepth = 0
        pendingForwardDeltaY = 0
    }

    /**
     * Platform-specific short-video feed verification for Instagram Reels, TikTok FYP/Following,
     * YouTube Shorts (including Litho UI), Facebook Reels, and Snapchat Spotlight.
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
        if (node == null || depth > 9 || signals.viewIds.size > 90) return

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

        val count = node.childCount.coerceAtMost(16)
        for (i in 0 until count) {
            traverseSignals(node.getChild(i), signals, depth + 1)
        }
    }

    private fun hasFocusedTextInput(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 6) return false
        val cls = node.className?.toString().orEmpty()
        if (node.isFocused && (node.isEditable || cls.contains("EditText"))) {
            return true
        }
        val count = node.childCount.coerceAtMost(10)
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
        private const val SWIPE_COALESCE_MS = 420L
        private const val MIN_SWIPE_DELTA_PX = 120
        private const val FEED_VERIFY_CACHE_MS = 1200L
        private const val WATCHDOG_INTERVAL_MS = 400L
    }
}
