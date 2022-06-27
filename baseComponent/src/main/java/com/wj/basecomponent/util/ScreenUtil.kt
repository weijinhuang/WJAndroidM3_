package com.wj.basecomponent.util

import android.content.Context

fun dp2Pixel(context: Context, dp: Int) = context.resources.displayMetrics.density * dp