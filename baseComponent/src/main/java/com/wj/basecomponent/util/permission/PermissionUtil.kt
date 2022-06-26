package com.wj.basecomponent.util.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

fun canDrawOverlays(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

fun requestDrawOverlays(context: Context) {
    val requestIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
    context.startActivity(requestIntent)
}

fun requestSystemAlert(context: Context){

}