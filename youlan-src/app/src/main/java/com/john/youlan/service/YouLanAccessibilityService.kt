package com.john.youlan.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.john.youlan.MainActivity
import com.john.youlan.data.YouLanPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class YouLanAccessibilityService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var warningView: TextView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var selectedPackages: Set<String> = emptySet()
    private var guardUnknownPackages: Boolean = false
    private var currentPackage: String? = null
    private var guardJob: Job? = null

    private val hiddenPackages = setOf(
        "com.android.systemui", "com.android.settings",
        "com.google.android.permissioncontroller", "com.android.permissioncontroller",
        "com.coloros.safecenter", "com.oplus.safecenter"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        serviceScope.launch { YouLanPreferences.selectedPackages(applicationContext).distinctUntilChanged().collect { selectedPackages = it } }
        serviceScope.launch { YouLanPreferences.guardUnknownPackages(applicationContext).distinctUntilChanged().collect { guardUnknownPackages = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        currentPackage = packageName
        if (shouldHideOverlay(packageName)) { hideOverlay(); return }
        val whitelisted = selectedPackages.isEmpty() || packageName in selectedPackages
        showOverlay(whitelisted)
        if (!whitelisted && guardUnknownPackages) scheduleUnknownPackageGuard(packageName)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        guardJob?.cancel(); hideOverlay(); serviceScope.cancel(); super.onDestroy()
    }

    private fun shouldHideOverlay(packageName: String) = packageName == applicationContext.packageName || packageName in hiddenPackages

    private fun scheduleUnknownPackageGuard(packageName: String) {
        guardJob?.cancel()
        guardJob = serviceScope.launch {
            delay(650)
            if (currentPackage != packageName || packageName in selectedPackages) return@launch
            performGlobalAction(GLOBAL_ACTION_BACK)
            delay(550)
            if (currentPackage == packageName && packageName !in selectedPackages) openYouLanHome()
        }
    }

    private fun showOverlay(whitelisted: Boolean) {
        if (overlayView == null) {
            overlayView = createOverlayView()
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.BOTTOM }
            runCatching { windowManager.addView(overlayView, params) }
        }
        warningView?.apply {
            visibility = if (whitelisted) View.GONE else View.VISIBLE
            text = "已进入非白名单应用，可按“主页”立即返回"
        }
    }

    private fun hideOverlay() {
        val view = overlayView ?: return
        runCatching { windowManager.removeView(view) }
        overlayView = null; warningView = null
    }

    private fun createOverlayView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.argb(235, 250, 252, 255))
                cornerRadii = floatArrayOf(dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), 0f, 0f, 0f, 0f)
            }
            elevation = dp(12).toFloat()
        }
        val warning = TextView(this).apply {
            visibility = View.GONE; setTextColor(Color.rgb(160, 70, 40)); textSize = 13f; gravity = Gravity.CENTER
            setPadding(dp(6), dp(4), dp(6), dp(6))
        }
        warningView = warning
        root.addView(warning, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val back = createLargeButton("←  返回").apply { setOnClickListener { performGlobalAction(GLOBAL_ACTION_BACK) } }
        val home = createLargeButton("⌂  主页").apply { setOnClickListener { openYouLanHome() } }
        buttons.addView(back, weightedButtonParams())
        buttons.addView(home, weightedButtonParams().apply { marginStart = dp(10) })
        root.addView(buttons)
        return root
    }

    private fun createLargeButton(textValue: String): TextView = TextView(this).apply {
        text = textValue; textSize = 24f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        setPadding(dp(10), dp(16), dp(10), dp(16))
        background = GradientDrawable().apply { setColor(Color.rgb(79, 125, 243)); cornerRadius = dp(18).toFloat() }
    }

    private fun weightedButtonParams() = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

    private fun openYouLanHome() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
