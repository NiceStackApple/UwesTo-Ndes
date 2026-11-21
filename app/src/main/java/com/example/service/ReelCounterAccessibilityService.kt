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

/**
 * Strictly scoped AccessibilityService monitoring ONLY the 7 supported short-video packages:
 * Instagram (+Lite), YouTube, Facebook (+Lite), and Snapchat (+Lite).
 *
 * Privacy guarantee (per PRD): never reads chat messages, typed text, passwords, OTPs,
 * video content, or banking apps. Only detects short-video feed scroll transitions.
 */
class ReelCounterAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var overlayManager: FloatingOverlayManager

    private var currentSupportedApp: SupportedApp? = null
    private var lastScrollEventMs: Long = 0L
    private var lastScrollSignature: String = ""
    private var lastScrollIndex: Int = -1
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
                overlayManager.hideCounterPill()
                overlayManager.hideBlockerOverlay()
            }
            return
        }

        val repository = (application as UwesToApplication).repository

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                currentSupportedApp = app
                val inShortFeed = isLikelyShortVideoFeed(app, event, rootInActiveWindow)
                serviceScope.launch {
                    val settings = repository.settingsFlow.first()
                    val todayLog = repository.todayLogFlow.first()
                    if (settings.floatingOverlayEnabled) {
                        overlayManager.showOrUpdateCounterPill(
                            todayCount = todayLog.totalCount,
                            dailyLimit = settings.dailyLimit,
                            sourceApp = app
                        )
                    } else {
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
                if (!isLikelyShortVideoFeed(app, event, rootInActiveWindow)) return

                // Deduplicate micro-scrolls and backward/duplicate scroll events
                val now = System.currentTimeMillis()
                val fromIndex = event.fromIndex
                val scrollY = event.scrollY
                val signature = "${app.id}:$fromIndex:$scrollY"
                val isForwardOrNewItem = fromIndex == -1 || fromIndex > lastScrollIndex || (now - lastScrollEventMs) > 2200L
                val passedDebounce = (now - lastScrollEventMs) >= 850L

                if (!passedDebounce || !isForwardOrNewItem || signature == lastScrollSignature) {
                    return
                }

                lastScrollEventMs = now
                lastScrollSignature = signature
                if (fromIndex >= 0) lastScrollIndex = fromIndex

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
     * Heuristic distinguishing short-video feeds (Reels / Shorts / Spotlight) from
     * non-feed areas like DMs, Chat, Profile, or Search so core app features remain usable.
     */
    private fun isLikelyShortVideoFeed(
        app: SupportedApp,
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?
    ): Boolean {
        val className = event.className?.toString()?.lowercase().orEmpty()
        val contentDesc = event.contentDescription?.toString()?.lowercase().orEmpty()
        val combinedMeta = "$className $contentDesc"

        // Explicitly skip messaging / chat / search / profile screens
        if (combinedMeta.contains("direct") ||
            combinedMeta.contains("inbox") ||
            combinedMeta.contains("chat") ||
            combinedMeta.contains("message") ||
            combinedMeta.contains("search")
        ) {
            return false
        }

        // Check known short-video container markers in view IDs or vertical pagers
        val isPagerLike = className.contains("viewpager") ||
            className.contains("recyclerview") ||
            className.contains("reels") ||
            className.contains("shorts")

        if (root == null) return isPagerLike

        val markers = when (app) {
            SupportedApp.INSTAGRAM -> listOf("clips_viewer", "reel", "reels")
            SupportedApp.YOUTUBE -> listOf("reel_watch", "shorts", "reel_recycler")
            SupportedApp.FACEBOOK -> listOf("reels", "fb_shorts", "short_form_video")
            SupportedApp.SNAPCHAT -> listOf("spotlight", "opera_view")
        }

        return hasMatchingViewId(root, markers, depth = 0) || isPagerLike
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
                // Exit short-video feed back to previous screen without force-killing messages
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
}
