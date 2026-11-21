package com.example.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Strictly scoped AccessibilityService monitoring ONLY the 7 supported short-video packages:
 * Instagram (+Lite), YouTube, Facebook (+Lite), and Snapchat (+Lite).
 *
 * Calibrated scroll detection:
 * - Ignores micro-scrolls, comment sheets, DMs, search, and non-feed screens.
 * - Uses 1250ms debounce + minimum scroll delta / pager index check to prevent over-counting.
 */
class ReelCounterAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var overlayManager: FloatingOverlayManager

    private var currentSupportedApp: SupportedApp? = null
    private var lastScrollEventMs: Long = 0L
    private var lastScrollSignature: String = ""
    private var lastScrollIndex: Int = -1
    private var lastScrollY: Int = -1
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
            if (currentSupportedApp != null) {
                currentSupportedApp = null
                lastScrollIndex = -1
                lastScrollY = -1
                overlayManager.hideCounterPill()
                overlayManager.hideBlockerOverlay()
            }
            return
        }

        val repository = (application as UwesToApplication).repository

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (currentSupportedApp != app) {
                    lastScrollIndex = -1
                    lastScrollY = -1
                }
                currentSupportedApp = app
                val inShortFeed = isConfirmedShortVideoFeed(app, event, rootInActiveWindow)
                serviceScope.launch {
                    val settings = repository.settingsFlow.first()
                    val todayLog = repository.todayLogFlow.first()
                    if (settings.floatingOverlayEnabled && inShortFeed) {
                        overlayManager.showOrUpdateCounterPill(
                            todayCount = todayLog.totalCount,
                            dailyLimit = settings.dailyLimit,
                            sourceApp = app
                        )
                    } else if (!inShortFeed) {
                        overlayManager.hideCounterPill()
                    }
                    if (inShortFeed) {
                        val block = repository.evaluateCurrentFeedBlock(app)
                        if (block !is ActiveInterception.None) {
                            maybeShowBlocker(block)
                        }
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                currentSupportedApp = app
                if (!isConfirmedShortVideoFeed(app, event, rootInActiveWindow)) return

                // Reject horizontal carousels (e.g., Stories/photo carousels) — Reels/Shorts scroll vertically
                if (event.scrollDeltaX != 0 && abs(event.scrollDeltaX) > abs(event.scrollDeltaY)) {
                    return
                }

                // Calibrated anti-oversensitivity filter
                val now = System.currentTimeMillis()
                val elapsedMs = now - lastScrollEventMs
                if (elapsedMs < MIN_REEL_INTERVAL_MS) return

                val fromIndex = event.fromIndex
                val scrollY = event.scrollY
                val deltaY = abs(event.scrollDeltaY)

                // Ignore tiny micro-scrolls or rubber-band bounce inside the same reel
                val hasMeaningfulIndexJump = fromIndex >= 0 && lastScrollIndex >= 0 && fromIndex > lastScrollIndex
                val hasMeaningfulPixelSwipe = deltaY >= MIN_VERTICAL_SWIPE_PX ||
                    (scrollY >= 0 && lastScrollY >= 0 && abs(scrollY - lastScrollY) >= MIN_VERTICAL_SWIPE_PX)
                val isFirstScrollInSession = lastScrollIndex == -1 && lastScrollY == -1

                val signature = "${app.id}:$fromIndex:$scrollY"
                if (signature == lastScrollSignature && elapsedMs < 3500L) return

                if (!isFirstScrollInSession && !hasMeaningfulIndexJump && !hasMeaningfulPixelSwipe) {
                    return
                }

                lastScrollEventMs = now
                lastScrollSignature = signature
                if (fromIndex >= 0) lastScrollIndex = fromIndex
                if (scrollY >= 0) lastScrollY = scrollY

                serviceScope.launch {
                    val result = repository.incrementScroll(app = app, delta = 1)
                    val settings = repository.settingsFlow.first()
                    if (settings.floatingOverlayEnabled) {
                        overlayManager.showOrUpdateCounterPill(
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
                }
            }
        }
    }

    /**
     * Strict short-video feed check so regular feeds, comments, DMs, profiles, and search
     * are NOT counted accidentally.
     */
    private fun isConfirmedShortVideoFeed(
        app: SupportedApp,
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?
    ): Boolean {
        val className = event.className?.toString()?.lowercase().orEmpty()
        val contentDesc = event.contentDescription?.toString()?.lowercase().orEmpty()
        val sourceId = event.source?.viewIdResourceName?.lowercase().orEmpty()
        val combinedMeta = "$className $contentDesc $sourceId"

        // Explicitly reject non-reel UIs (comments, DMs, chat, search, notifications, settings)
        val excludedTokens = listOf(
            "direct", "inbox", "chat", "message", "comment",
            "search", "notification", "profile", "setting", "share_sheet"
        )
        if (excludedTokens.any { combinedMeta.contains(it) }) {
            return false
        }

        val markers = when (app) {
            SupportedApp.INSTAGRAM -> listOf("clips_viewer", "clips_video", "reels_viewer", "reel_viewer")
            SupportedApp.YOUTUBE -> listOf("reel_watch", "reel_recycler", "shorts_player", "reel_player")
            SupportedApp.FACEBOOK -> listOf("reels_viewer", "fb_shorts", "short_form_video")
            SupportedApp.SNAPCHAT -> listOf("spotlight", "opera_view")
        }

        if (markers.any { sourceId.contains(it) }) return true
        if (root != null && hasMatchingViewId(root, markers, depth = 0)) return true

        // Fallback only for vertical full-screen ViewPager (not generic RecyclerView lists)
        return className.contains("viewpager")
    }

    private fun hasMatchingViewId(
        node: AccessibilityNodeInfo?,
        markers: List<String>,
        depth: Int
    ): Boolean {
        if (node == null || depth > 4) return false
        val viewId = node.viewIdResourceName?.lowercase().orEmpty()
        if (markers.any { viewId.contains(it) }) return true

        val count = node.childCount.coerceAtMost(8)
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            val matched = hasMatchingViewId(child, markers, depth + 1)
            if (matched) return true
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
        handleServiceStopped("Deteksi otomatis UwesTo dipateni sistem utawa pengguna.")
        if (::overlayManager.isInitialized) {
            overlayManager.releaseAll()
        }
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
        private const val MIN_VERTICAL_SWIPE_PX = 180
    }
}
