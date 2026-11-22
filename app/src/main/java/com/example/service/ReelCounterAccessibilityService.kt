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
 * Instagram Reels, TikTok FYP/Following, YouTube Shorts, Facebook Reels, and Snapchat Spotlight.
 *
 * Non-Intrusive Protection Guarantees:
 * - TikTok Shop (Shop/Toko tab, product pages, cart, checkout, vouchers, live shop) is NEVER counted or blocked.
 * - Instagram Carousels, Home Feed, Stories, Explore, Profile, and DMs are NEVER counted or blocked.
 * - YouTube long-form videos (watch page, fullscreen, related list, Home, Subscriptions) are NEVER counted or blocked.
 * - Floating pill ONLY appears inside confirmed short-video feeds and hides automatically on exit.
 * - Up-down scrolling on the same video never double-counts (High-Water Mark + backtrackDepth).
 */
class ReelCounterAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var overlayManager: FloatingOverlayManager

    @Volatile private var cachedTodayCount: Int = 0
    @Volatile private var cachedDailyLimit: Int = 50
    @Volatile private var cachedOverlayEnabled: Boolean = true

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
                detail = "Deteksi live UwesTo aktif kanggo IG Reels, TikTok FYP, YouTube Shorts, FB Reels, lan Snapchat."
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
                        maybeShowBlockerSafely(app, block)
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                // Fast O(1) rejection for horizontal carousels, TikTok Shop, YouTube long watch, comments, DMs
                if (isExcludedScrollEventFast(event)) {
                    if (isPillVisible && isLikelyNonFeedOverlayEvent(event)) {
                        hideOverlayAndResetSession()
                    }
                    return
                }

                // Use cached feed state on the hot path; re-verify lazily every 700ms or on app switch
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
                    val optimisticTotal = cachedTodayCount + 1
                    cachedTodayCount = optimisticTotal
                    if (cachedOverlayEnabled) {
                        showPillAndStartWatchdog(
                            todayCount = optimisticTotal,
                            dailyLimit = cachedDailyLimit,
                            sourceApp = app
                        )
                    }

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
                            maybeShowBlockerSafely(app, result.interception)
                        }
                    }
                } else if (cachedOverlayEnabled && !isPillVisible) {
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
     * Zero-IPC rejection for horizontal swipes (Instagram Carousels, Stories, Shop banners)
     * and obvious non-short-video containers.
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
            meta.contains("share_sheet") ||
            // Instagram Carousel & Home Feed protection
            meta.contains("carousel") ||
            meta.contains("row_feed_photo") ||
            meta.contains("main_feed") ||
            // TikTok Shop protection
            meta.contains("shop") ||
            meta.contains("toko") ||
            meta.contains("cart") ||
            meta.contains("keranjang") ||
            meta.contains("checkout") ||
            meta.contains("ecommerce") ||
            meta.contains("pdp_") ||
            meta.contains("product") ||
            // YouTube Long-Form Video protection
            meta.contains("watch_player") ||
            meta.contains("watch_list") ||
            meta.contains("next_gen_watch") ||
            meta.contains("related_results") ||
            meta.contains("browse_results")
    }

    private fun evaluateFastNewVideoScroll(
        event: AccessibilityEvent,
        nowMs: Long
    ): Boolean {
        val fromIndex = event.fromIndex
        val scrollY = event.scrollY
        val deltaY = event.scrollDeltaY
        val isPageSelected = event.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED

        // 1. Reject backward / upward scrolls immediately
        val isBackwardByDelta = deltaY < -20
        val isBackwardByIndex = fromIndex >= 0 && lastSeenIndex >= 0 && fromIndex < lastSeenIndex
        val isBackwardByScrollY = scrollY >= 0 && lastSeenScrollY >= 0 && scrollY < lastSeenScrollY - 20

        if (isBackwardByDelta || isBackwardByIndex || isBackwardByScrollY) {
            if (fromIndex >= 0) lastSeenIndex = fromIndex
            if (scrollY >= 0) lastSeenScrollY = scrollY
            pendingForwardDeltaY = 0

            if (nowMs - lastBackwardEventMs >= SWIPE_COALESCE_MS) {
                backtrackDepth = (backtrackDepth + 1).coerceAtMost(50)
                lastBackwardEventMs = nowMs
            }
            return false
        }

        // 2. Priority Path A: fromIndex high-water mark
        if (fromIndex >= 0) {
            val prevIndex = lastSeenIndex
            lastSeenIndex = fromIndex

            if (maxReachedIndex == -1) {
                maxReachedIndex = fromIndex
                if (fromIndex > 0) {
                    lastCountedEventMs = nowMs
                    backtrackDepth = 0
                    return true
                }
                return false
            }

            if (fromIndex > maxReachedIndex) {
                maxReachedIndex = fromIndex
                backtrackDepth = 0
                if (nowMs - lastCountedEventMs >= SWIPE_COALESCE_MS) {
                    lastCountedEventMs = nowMs
                    return true
                }
                return false
            }

            if (fromIndex < maxReachedIndex) {
                if (fromIndex > prevIndex && backtrackDepth > 0) {
                    backtrackDepth = (backtrackDepth - 1).coerceAtLeast(0)
                }
                return false
            }

            if (deltaY <= 0 && !isPageSelected) {
                return false
            }
        }

        // 3. Priority Path B: scrollY high-water mark
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

            return false
        }

        // 4. Priority Path C: Delta-Y / TYPE_VIEW_SELECTED with backtrackDepth absorption
        if (deltaY > 0) {
            pendingForwardDeltaY += deltaY
        }

        val triggeredForwardSwipe = pendingForwardDeltaY >= MIN_SWIPE_DELTA_PX || isPageSelected
        if (!triggeredForwardSwipe) return false

        if (nowMs - lastCountedEventMs < SWIPE_COALESCE_MS) {
            pendingForwardDeltaY = 0
            return false
        }

        pendingForwardDeltaY = 0

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
     * Strict platform-aware verification:
     * - TikTok: ONLY FYP / Following / Friends video feed; NEVER TikTok Shop, Cart, Checkout, Product, Profile, Inbox, Comments.
     * - Instagram: ONLY full-screen Reels viewer; NEVER Carousel posts, Home Feed, Stories, Explore, Profile, DMs.
     * - YouTube: ONLY Shorts vertical player; NEVER long-form YouTube watch pages, Home, Subscriptions, or related lists.
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

        val globalExclusions = listOf(
            "direct", "inbox", "chat", "message", "comment",
            "search", "notification", "profile", "setting", "share_sheet", "stories_tray"
        )
        if (globalExclusions.any { combinedMeta.contains(it) }) {
            return false
        }

        if (root != null && hasFocusedTextInput(root, depth = 0)) {
            return false
        }

        val scan = scanHierarchySignals(root)
        if (scan.hasCommentDrawerVisible) {
            return false
        }

        return when (app) {
            SupportedApp.INSTAGRAM -> {
                // 1. Explicitly protect Instagram Carousels, Home Feed, Explore, and Profile
                val onExcludedIgTab = scan.hasSelectedTab("home") ||
                    scan.hasSelectedTab("beranda") ||
                    scan.hasSelectedTab("search") ||
                    scan.hasSelectedTab("cari") ||
                    scan.hasSelectedTab("explore") ||
                    scan.hasSelectedTab("jelajahi") ||
                    scan.hasSelectedTab("profile") ||
                    scan.hasSelectedTab("profil")

                val carouselViewIds = listOf(
                    "carousel", "feed_carousel", "carousel_media", "carousel_indicator",
                    "carousel_page_indicator", "row_feed_photo", "row_feed_carousel",
                    "stories_tray", "reel_tray", "main_feed"
                )
                val hasCarouselSignal = carouselViewIds.any { sourceId.contains(it) } ||
                    scan.hasAnyViewId(carouselViewIds) ||
                    scan.hasAnyLabel(
                        listOf("carousel", "photo 1 of", "foto 1 dari", "slide 1 of", "gambar 1 dari")
                    )

                if (onExcludedIgTab || hasCarouselSignal) {
                    return false
                }

                // 2. Confirm genuine full-screen Instagram Reels viewer
                val igReelsIds = listOf(
                    "clips_viewer", "clips_video", "clips_swipe", "clips_ufi",
                    "clips_author", "clips_media", "reels_viewer", "reel_viewer"
                )
                igReelsIds.any { sourceId.contains(it) } ||
                    scan.hasAnyViewId(igReelsIds) ||
                    scan.hasSelectedTab("reels")
            }

            SupportedApp.YOUTUBE -> {
                // 1. Explicitly protect YouTube long-form watch pages, Home, Subscriptions, and Library
                val onExcludedYtTab = scan.hasSelectedTab("home") ||
                    scan.hasSelectedTab("beranda") ||
                    scan.hasSelectedTab("subscriptions") ||
                    scan.hasSelectedTab("subscription") ||
                    scan.hasSelectedTab("you") ||
                    scan.hasSelectedTab("anda") ||
                    scan.hasSelectedTab("library") ||
                    scan.hasSelectedTab("koleksi")

                val ytLongFormIds = listOf(
                    "watch_player", "next_gen_watch", "watch_while", "time_bar",
                    "fullscreen_button", "player_control_play_pause", "related_results",
                    "watch_list", "browse_results", "video_description", "miniplayer",
                    "floaty_bar", "watch_metadata"
                )
                val ytLongFormLabels = listOf(
                    "enter fullscreen", "masuk layar penuh", "exit fullscreen", "keluar layar penuh",
                    "minimize", "perkecil", "up next", "berikutnya", "autoplay", "putar otomatis",
                    "save to playlist", "simpan ke playlist", "playback speed", "kecepatan pemutaran",
                    "chapters", "lihat bab"
                )

                val isLongFormVideo = onExcludedYtTab ||
                    ytLongFormIds.any { sourceId.contains(it) } ||
                    scan.hasAnyViewId(ytLongFormIds) ||
                    scan.hasAnyLabel(ytLongFormLabels)

                if (isLongFormVideo) {
                    return false
                }

                // 2. Strictly require genuine YouTube Shorts player or selected Shorts tab
                val ytShortsIds = listOf(
                    "reel_watch", "reel_recycler", "shorts_player", "reel_player",
                    "reel_progress", "reel_dyn", "reel_like", "reel_comment",
                    "reel_share", "reel_pivot", "shorts_pivot", "reel_main"
                )
                ytShortsIds.any { sourceId.contains(it) } ||
                    scan.hasAnyViewId(ytShortsIds) ||
                    scan.hasSelectedTab("shorts")
            }

            SupportedApp.TIKTOK -> {
                // 1. Explicitly protect TikTok Shop (marketplace, product detail, cart, checkout, orders, live shop)
                val onExcludedTikTokTab = scan.hasSelectedTab("shop") ||
                    scan.hasSelectedTab("toko") ||
                    scan.hasSelectedTab("mall") ||
                    scan.hasSelectedTab("profile") ||
                    scan.hasSelectedTab("profil") ||
                    scan.hasSelectedTab("inbox") ||
                    scan.hasSelectedTab("kotak masuk")

                val tikTokShopIds = listOf(
                    "shop", "ecommerce", "pdp_", "cart", "checkout", "buy_now",
                    "add_to_cart", "voucher", "order", "mall", "product_detail", "anchor_shop"
                )
                val tikTokShopLabels = listOf(
                    "tiktok shop", "tokopedia", "beli sekarang", "buy now",
                    "tambah ke keranjang", "add to cart", "keranjang", "shopping cart",
                    "checkout", "gratis ongkir", "free shipping", "klaim voucher",
                    "pesanan saya", "my orders", "flash sale", "cod", "bayar sekarang",
                    "ulasan produk", "spesifikasi produk", "opsi pengiriman", "toko resmi"
                )

                val isTikTokShop = onExcludedTikTokTab ||
                    tikTokShopIds.any { sourceId.contains(it) } ||
                    scan.hasAnyViewId(tikTokShopIds) ||
                    scan.hasAnyLabel(tikTokShopLabels)

                if (isTikTokShop) {
                    return false
                }

                // 2. Confirm genuine TikTok short-video feed (FYP / Following / Friends) — never generic RecyclerView!
                val hasFeedHeader = scan.hasLabel("for you") ||
                    scan.hasLabel("untuk anda") ||
                    scan.hasLabel("following") ||
                    scan.hasLabel("mengikuti") ||
                    scan.hasSelectedTab("home") ||
                    scan.hasSelectedTab("beranda") ||
                    scan.hasSelectedTab("friends") ||
                    scan.hasSelectedTab("teman")

                val hasTikTokVideoActions = (scan.hasLabel("like") || scan.hasLabel("suka")) &&
                    (scan.hasLabel("share") || scan.hasLabel("bagikan") || scan.hasLabel("comment") || scan.hasLabel("komentar"))

                val isVerticalViewPager = className.contains("viewpager")

                hasFeedHeader || hasTikTokVideoActions || isVerticalViewPager
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
        val viewIds = ArrayList<String>(40)
        val labels = ArrayList<String>(48)
        val selectedTabs = ArrayList<String>(8)
        var hasCommentDrawerVisible: Boolean = false

        fun hasAnyViewId(markers: List<String>): Boolean {
            return viewIds.any { id -> markers.any { marker -> id.contains(marker) } }
        }

        fun hasLabel(keyword: String): Boolean {
            return labels.any { it.contains(keyword) }
        }

        fun hasAnyLabel(keywords: List<String>): Boolean {
            return labels.any { label -> keywords.any { kw -> label.contains(kw) } }
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
        if (node == null || depth > 9 || signals.viewIds.size > 100) return

        val viewId = node.viewIdResourceName?.lowercase().orEmpty()
        if (viewId.isNotEmpty()) {
            signals.viewIds.add(viewId)
            if (viewId.contains("comment_list") || viewId.contains("comment_composer") || viewId.contains("comment_input")) {
                signals.hasCommentDrawerVisible = true
            }
        }

        val text = node.text?.toString()?.trim()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.trim()?.lowercase().orEmpty()

        if (text.isNotEmpty() && text.length <= 50) {
            signals.labels.add(text)
            if (node.isSelected) signals.selectedTabs.add(text)
            if (text.contains("add comment") || text.contains("tambahkan komentar") || text.contains("tambah komentar")) {
                signals.hasCommentDrawerVisible = true
            }
        }
        if (desc.isNotEmpty() && desc.length <= 70) {
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

    /**
     * Re-verifies right before showing the Blocker popup that the user is STILL inside
     * a genuine short-video feed (and NOT in TikTok Shop, IG Carousel, or YouTube long-form video).
     */
    private fun maybeShowBlockerSafely(app: SupportedApp, interception: ActiveInterception) {
        val now = System.currentTimeMillis()
        if (now - lastBlockerShownMs < 1500L) return

        val root = rootInActiveWindow
        if (root == null || !isStrictlyShortVideoFeed(app, null, root)) {
            hideOverlayAndResetSession()
            return
        }

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
        private const val FEED_VERIFY_CACHE_MS = 700L
        private const val WATCHDOG_INTERVAL_MS = 400L
    }
}
