package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.model.ActiveInterception
import com.example.model.BrainStage
import com.example.model.SupportedApp

/**
 * Manages the native Android WindowManager overlays:
 * 1. Compact draggable floating live counter pill over Instagram, YouTube, Facebook, and Snapchat.
 * 2. Native feed blocker / break card when daily limit, break interval, or scheduled Block triggers.
 */
class FloatingOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var pillView: LinearLayout? = null
    private var pillCountText: TextView? = null
    private var pillStatusDot: View? = null
    private var pillAppLabel: TextView? = null
    private var pillParams: WindowManager.LayoutParams? = null

    private var blockerView: LinearLayout? = null

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showOrUpdateCounterPill(
        todayCount: Int,
        dailyLimit: Int,
        sourceApp: SupportedApp
    ) {
        if (!Settings.canDrawOverlays(context)) return

        mainHandler.post {
            val stage = BrainStage.fromCount(todayCount)
            val accentInt = stage.accentColor.toArgb()

            if (pillView == null) {
                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(8), dp(14), dp(8))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(24).toFloat()
                        setColor(Color.parseColor("#E6131720"))
                        setStroke(dp(1), accentInt)
                    }
                    elevation = dp(8).toFloat()
                }

                val dot = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply {
                        marginEnd = dp(8)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(accentInt)
                    }
                }

                val countTv = TextView(context).apply {
                    textSize = 15f
                    typeface = Typeface.MONOSPACE
                    setTextColor(Color.WHITE)
                }

                val appTv = TextView(context).apply {
                    textSize = 11f
                    setTextColor(Color.parseColor("#9AA4B6"))
                    setPadding(dp(6), 0, 0, 0)
                }

                container.addView(dot)
                container.addView(countTv)
                container.addView(appTv)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    overlayWindowType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    x = dp(12)
                    y = dp(96)
                }

                // Allow smooth dragging so the pill never blocks app buttons
                var startX = 0
                var startY = 0
                var touchX = 0f
                var touchY = 0f
                container.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = params.x
                            startY = params.y
                            touchX = event.rawX
                            touchY = event.rawY
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = startX - (event.rawX - touchX).toInt()
                            params.y = startY + (event.rawY - touchY).toInt()
                            runCatching { windowManager.updateViewLayout(container, params) }
                            true
                        }
                        else -> false
                    }
                }

                runCatching {
                    windowManager.addView(container, params)
                    pillView = container
                    pillStatusDot = dot
                    pillCountText = countTv
                    pillAppLabel = appTv
                    pillParams = params
                }
            }

            pillCountText?.text = "$todayCount/$dailyLimit"
            pillAppLabel?.text = "${sourceApp.shortLabel} • ${stage.titleSemarang}"
            (pillStatusDot?.background as? GradientDrawable)?.setColor(accentInt)
            (pillView?.background as? GradientDrawable)?.setStroke(dp(1), accentInt)
        }
    }

    fun hideCounterPill() {
        mainHandler.post {
            pillView?.let { view ->
                runCatching { windowManager.removeView(view) }
            }
            pillView = null
            pillCountText = null
            pillStatusDot = null
            pillAppLabel = null
            pillParams = null
        }
    }

    fun showBlockerOverlay(
        interception: ActiveInterception,
        onExitFeed: () -> Unit,
        onDismissOverlay: () -> Unit
    ) {
        if (!Settings.canDrawOverlays(context)) return
        if (interception is ActiveInterception.None) return

        mainHandler.post {
            hideBlockerOverlayImmediate()

            val (headline, subtitle, detail, accentInt) = when (interception) {
                is ActiveInterception.LimitReached -> Quad(
                    "Uwes To, Ndes!",
                    "Limit harianmu wis entek (${interception.todayCount}/${interception.dailyLimit} reels).",
                    "Feed ${interception.sourceApp.displayName} ${interception.sourceApp.feedName} dikunci sik nganti jam 00:00. Pesan & fitur liyane tetep iso mbok bukak.",
                    BrainStage.fromCount(interception.todayCount).accentColor.toArgb()
                )
                is ActiveInterception.RuleBlocked -> Quad(
                    "${interception.ruleName} Aktif",
                    "Jadwal ${interception.timeWindowLabel} lagi mlaku.",
                    "Akses ${interception.sourceApp.displayName} ${interception.sourceApp.feedName} ditahan sik. Bukak aplikasi UwesTo nek butuh opsi darurat.",
                    Color.parseColor("#FF6B4A")
                )
                is ActiveInterception.BreakPause -> Quad(
                    "Mandek Sik, Tarik Napas!",
                    "Wis ${interception.triggerCount} reels ik — ngaso ${interception.durationSeconds} detik sek.",
                    "Coba delok adoh sek, ojo melotot terus nang layar, Ndes.",
                    Color.parseColor("#FFB547")
                )
                ActiveInterception.None -> return@post
            }

            val root = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(28), dp(36), dp(28), dp(36))
                setBackgroundColor(Color.parseColor("#F50D0F14"))
            }

            val badgeTv = TextView(context).apply {
                text = "UWESTO BLOKIR"
                textSize = 12f
                typeface = Typeface.MONOSPACE
                setTextColor(accentInt)
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(Color.parseColor("#22FFFFFF"))
                    setStroke(dp(1), accentInt)
                }
            }

            val titleTv = TextView(context).apply {
                text = headline
                textSize = 26f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, dp(20), 0, dp(8))
            }

            val subTv = TextView(context).apply {
                text = subtitle
                textSize = 16f
                setTextColor(accentInt)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12))
            }

            val detailTv = TextView(context).apply {
                text = detail
                textSize = 14f
                setTextColor(Color.parseColor("#B0BAC9"))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(28))
            }

            val exitBtn = Button(context).apply {
                text = "Metu Teko Feed (Balik)"
                setTextColor(Color.parseColor("#0D0F14"))
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(accentInt)
                }
                setPadding(dp(24), dp(14), dp(24), dp(14))
                setOnClickListener {
                    hideBlockerOverlayImmediate()
                    onExitFeed()
                }
            }

            val openAppBtn = Button(context).apply {
                text = "Bukak UwesTo"
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(Color.parseColor("#222938"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
                setPadding(dp(24), dp(12), dp(24), dp(12))
                setOnClickListener {
                    hideBlockerOverlayImmediate()
                    onDismissOverlay()
                    val intent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(intent)
                }
            }

            root.addView(badgeTv)
            root.addView(titleTv)
            root.addView(subTv)
            root.addView(detailTv)
            root.addView(
                exitBtn,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            root.addView(openAppBtn)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            runCatching {
                windowManager.addView(root, params)
                blockerView = root
            }
        }
    }

    fun hideBlockerOverlay() {
        mainHandler.post { hideBlockerOverlayImmediate() }
    }

    private fun hideBlockerOverlayImmediate() {
        blockerView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        blockerView = null
    }

    fun releaseAll() {
        hideCounterPill()
        hideBlockerOverlay()
    }

    private data class Quad(
        val first: String,
        val second: String,
        val third: String,
        val fourth: Int
    )
}
