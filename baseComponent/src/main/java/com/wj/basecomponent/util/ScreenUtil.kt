package com.wj.basecomponent.util

import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.wj.basecomponent.BaseApplication

fun dp2Pixel(context: Context, dp: Int) = context.resources.displayMetrics.density * dp


fun getScreenWidth(): Int {
    val windowManager = BaseApplication.INSTANCE.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT < 30) {
        windowManager.defaultDisplay.width
    } else {
        windowManager.currentWindowMetrics.bounds.width()
    }
}

fun getScreenHeight(): Int {
    val windowManager = BaseApplication.INSTANCE.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT < 30) {
        windowManager.defaultDisplay.height
    } else {
        windowManager.currentWindowMetrics.bounds.height()
    }
}