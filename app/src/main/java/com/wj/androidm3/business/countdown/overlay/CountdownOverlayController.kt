package com.wj.androidm3.business.countdown.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.wj.androidm3.business.countdown.data.CountdownEntity
import com.wj.androidm3.business.countdown.data.CountdownPreferences
import com.wj.androidm3.business.countdown.data.CountdownTime
import kotlin.math.abs

class CountdownOverlayController(
    private val context: Context,
    private val preferences: CountdownPreferences,
    private val onAddCountdown: () -> Unit,
    private val onOpenList: () -> Unit,
    private val onPermissionLost: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val root = FrameLayout(context)
    private val button = object : AppCompatTextView(context) {
        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }
    private val menu = LinearLayout(context)
    private val windowParams = WindowManager.LayoutParams(
        dp(24),
        dp(56),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    private var isAttached = false
    private var overlayState = CountdownOverlayState.HIDDEN_HANDLE
    private val isExpanded: Boolean
        get() = overlayState == CountdownOverlayState.EXPANDED_BUTTON ||
            overlayState == CountdownOverlayState.MENU
    private var isDragging = false
    private var hasRunningCountdown = false
    private var nearestTasks: List<CountdownEntity> = emptyList()
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private var buttonX = 0
    private var buttonY = 0
    private var lastScreenHeight = 0
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val collapseRunnable = Runnable { collapse() }
    private val longPressRunnable = Runnable {
        isDragging = true
        ensureFullScreen()
    }

    init {
        root.setBackgroundColor(Color.TRANSPARENT)
        root.isClickable = true
        root.setOnClickListener {
            if (isExpanded) collapse()
        }

        button.gravity = Gravity.CENTER
        button.setTextColor(Color.WHITE)
        button.textSize = 14f
        button.elevation = dp(8).toFloat()
        button.contentDescription = "悬浮倒计时助手"
        button.setOnTouchListener(::handleButtonTouch)
        root.addView(button)

        menu.orientation = LinearLayout.VERTICAL
        menu.elevation = dp(10).toFloat()
        menu.background = roundedDrawable(Color.WHITE, 12f)
        menu.setPadding(0, dp(4), 0, dp(4))
        menu.visibility = View.GONE
        menu.isClickable = true
        menu.addView(createMenuItem("添加倒计时") {
            collapse()
            onAddCountdown()
        })
        menu.addView(createMenuItem("查看倒计时列表") {
            collapse()
            onOpenList()
        })
        root.addView(menu)
    }

    fun show() {
        if (isAttached) return
        val (_, screenHeight) = screenSize()
        lastScreenHeight = screenHeight
        buttonY = ((screenHeight - dp(56)) * preferences.verticalFraction).toInt()
        applyCollapsedStyle(System.currentTimeMillis())
        applyCollapsedLayout()
        try {
            windowManager.addView(root, windowParams)
            isAttached = true
        } catch (_: SecurityException) {
            onPermissionLost()
        }
    }

    fun update(nearestRunningTasks: List<CountdownEntity>, nowEpochMs: Long) {
        nearestTasks = nearestRunningTasks.take(2)
        hasRunningCountdown = nearestTasks.isNotEmpty()
        val (_, screenHeight) = screenSize()
        if (screenHeight != lastScreenHeight) {
            lastScreenHeight = screenHeight
            buttonY = ((screenHeight - dp(56)) * preferences.verticalFraction).toInt()
            if (isExpanded) {
                ensureFullScreen()
                positionExpandedButton()
                return
            }
        }
        if (isExpanded) return
        overlayState = CountdownOverlayStateReducer.collapsed(hasRunningCountdown)
        applyCollapsedStyle(nowEpochMs)
        applyCollapsedLayout()
    }

    fun remove() {
        handler.removeCallbacksAndMessages(null)
        if (!isAttached) return
        try {
            windowManager.removeView(root)
        } catch (_: SecurityException) {
            // Permission may have been revoked while the view was attached.
        } catch (_: IllegalArgumentException) {
            // The window was already removed by the system.
        } finally {
            isAttached = false
        }
    }

    private fun handleButtonTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                root.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                downRawX = event.rawX
                downRawY = event.rawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                isDragging = false
                handler.postDelayed(longPressRunnable, LONG_PRESS_MS)
                scheduleCollapse()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging &&
                    (abs(event.rawX - downRawX) > touchSlop || abs(event.rawY - downRawY) > touchSlop)
                ) {
                    handler.removeCallbacks(longPressRunnable)
                }
                if (isDragging) {
                    val (screenWidth, screenHeight) = screenSize()
                    buttonX = (buttonX + (event.rawX - lastRawX).toInt())
                        .coerceIn(0, screenWidth - dp(56))
                    buttonY = (buttonY + (event.rawY - lastRawY).toInt())
                        .coerceIn(dp(24), screenHeight - dp(104))
                    positionExpandedButton()
                }
                lastRawX = event.rawX
                lastRawY = event.rawY
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (isDragging) {
                    finishDrag()
                } else if (event.actionMasked == MotionEvent.ACTION_UP &&
                    abs(event.rawX - downRawX) <= touchSlop &&
                    abs(event.rawY - downRawY) <= touchSlop
                ) {
                    view.performClick()
                    if (isExpanded) showMenu() else expand()
                }
                isDragging = false
                return true
            }
        }
        return false
    }

    private fun expand() {
        overlayState = CountdownOverlayStateReducer.onPrimaryTap(overlayState)
        ensureFullScreen()
        button.text = "⏱"
        button.textSize = 24f
        button.background = roundedDrawable(Color.rgb(54, 103, 214), 28f)
        button.contentDescription = "打开倒计时菜单"
        positionExpandedButton()
        button.bringToFront()
        scheduleCollapse()
    }

    private fun showMenu() {
        overlayState = CountdownOverlayStateReducer.onPrimaryTap(overlayState)
        menu.visibility = View.VISIBLE
        val (screenWidth, screenHeight) = screenSize()
        val menuWidth = dp(184)
        val menuHeight = dp(104)
        val x = if (preferences.dockOnLeft) {
            dp(8)
        } else {
            screenWidth - menuWidth - dp(8)
        }
        val placeBelow = buttonY + dp(56) + menuHeight <= screenHeight - dp(48)
        val y = if (placeBelow) buttonY + dp(64) else buttonY - menuHeight - dp(8)
        menu.layoutParams = FrameLayout.LayoutParams(menuWidth, menuHeight).apply {
            leftMargin = x
            topMargin = y.coerceAtLeast(dp(24))
        }
        menu.bringToFront()
        scheduleCollapse()
    }

    private fun collapse() {
        if (!isAttached) return
        handler.removeCallbacks(collapseRunnable)
        handler.removeCallbacks(longPressRunnable)
        menu.visibility = View.GONE
        overlayState = CountdownOverlayStateReducer.onDismiss(hasRunningCountdown)
        val (screenWidth, screenHeight) = screenSize()
        preferences.dockOnLeft = buttonX + dp(28) < screenWidth / 2
        buttonX = if (preferences.dockOnLeft) 0 else screenWidth - collapsedWindowWidth()
        buttonY = buttonY.coerceIn(dp(24), screenHeight - dp(104))
        preferences.verticalFraction = buttonY.toFloat() / (screenHeight - dp(56)).coerceAtLeast(1)
        applyCollapsedStyle(System.currentTimeMillis())
        applyCollapsedLayout()
    }

    private fun finishDrag() {
        val (screenWidth, screenHeight) = screenSize()
        preferences.dockOnLeft = buttonX + dp(28) < screenWidth / 2
        buttonY = buttonY.coerceIn(dp(24), screenHeight - dp(104))
        preferences.verticalFraction = buttonY.toFloat() / (screenHeight - dp(56)).coerceAtLeast(1)
        overlayState = CountdownOverlayStateReducer.onDismiss(hasRunningCountdown)
        menu.visibility = View.GONE
        applyCollapsedStyle(System.currentTimeMillis())
        applyCollapsedLayout()
    }

    private fun ensureFullScreen() {
        val (screenWidth, screenHeight) = screenSize()
        if (windowParams.width != WindowManager.LayoutParams.MATCH_PARENT) {
            buttonX = if (preferences.dockOnLeft) 0 else screenWidth - dp(56)
            buttonY = windowParams.y.coerceIn(dp(24), screenHeight - dp(104))
        }
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.height = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.x = 0
        windowParams.y = 0
        safelyUpdateWindow()
        positionExpandedButton()
    }

    private fun positionExpandedButton() {
        button.layoutParams = FrameLayout.LayoutParams(dp(56), dp(56)).apply {
            leftMargin = buttonX
            topMargin = buttonY
        }
        button.bringToFront()
    }

    private fun applyCollapsedStyle(nowEpochMs: Long) {
        if (hasRunningCountdown) {
            val formatted = nearestTasks.joinToString("\n") { CountdownTime.format(it, nowEpochMs) }
            button.text = formatted
            button.textSize = if (nearestTasks.size > 1) 12f else 14f
            button.setLineSpacing(0f, 0.9f)
            button.background = roundedDrawable(Color.argb(225, 40, 78, 160), 18f)
            button.contentDescription = "最近结束的${nearestTasks.size}个倒计时 $formatted"
        } else {
            button.text = ""
            button.background = roundedDrawable(Color.argb(175, 54, 103, 214), 4f)
            button.contentDescription = "展开悬浮倒计时助手"
        }
    }

    private fun applyCollapsedLayout() {
        if (!isAttached && root.parent != null) return
        val (screenWidth, screenHeight) = screenSize()
        val windowWidth = collapsedWindowWidth()
        val visualWidth = when {
            nearestTasks.size > 1 -> dp(88)
            hasRunningCountdown -> dp(72)
            else -> dp(8)
        }
        val visualHeight = when {
            nearestTasks.size > 1 -> dp(52)
            hasRunningCountdown -> dp(36)
            else -> dp(40)
        }
        button.layoutParams = FrameLayout.LayoutParams(visualWidth, visualHeight).apply {
            gravity = (if (preferences.dockOnLeft) Gravity.START else Gravity.END) or Gravity.CENTER_VERTICAL
        }
        windowParams.width = windowWidth
        windowParams.height = dp(56)
        windowParams.x = if (preferences.dockOnLeft) 0 else screenWidth - windowWidth
        windowParams.y = buttonY.coerceIn(dp(24), screenHeight - dp(104))
        if (isAttached) safelyUpdateWindow()
    }

    private fun safelyUpdateWindow() {
        try {
            windowManager.updateViewLayout(root, windowParams)
        } catch (_: SecurityException) {
            remove()
            onPermissionLost()
        } catch (_: IllegalArgumentException) {
            isAttached = false
        }
    }

    @Suppress("DEPRECATION")
    private fun screenSize(): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun collapsedWindowWidth(): Int = when {
        nearestTasks.size > 1 -> dp(88)
        hasRunningCountdown -> dp(72)
        else -> dp(24)
    }

    private fun createMenuItem(label: String, action: () -> Unit): TextView = TextView(context).apply {
        text = label
        textSize = 16f
        setTextColor(Color.rgb(35, 35, 35))
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(20), 0, dp(12), 0)
        isClickable = true
        setBackgroundColor(Color.TRANSPARENT)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        setOnClickListener { action() }
    }

    private fun roundedDrawable(color: Int, radiusDp: Float): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(radiusDp.toInt()).toFloat()
    }

    private fun scheduleCollapse() {
        handler.removeCallbacks(collapseRunnable)
        handler.postDelayed(collapseRunnable, COLLAPSE_DELAY_MS)
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    companion object {
        private const val COLLAPSE_DELAY_MS = 3_000L
        private const val LONG_PRESS_MS = 350L
    }
}
