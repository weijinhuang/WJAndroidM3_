package com.wj.basecomponent.ui

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.wj.basecomponent.ui.constraint.BaseUI

abstract class BaseActivity : AppCompatActivity(), BaseUI {

    private val mFirebaseAnalytics: FirebaseAnalytics by lazy { Firebase.analytics }

    private var mProgressDialog: ProgressDialog? = null

    override
    fun showLoadingProgress(cancelable: Boolean) {
        mProgressDialog?.dismiss()
        mProgressDialog = ProgressDialog(this).apply {
            setCancelable(cancelable)
        }
        mProgressDialog?.show()
        mFirebaseAnalytics.logEvent("ActivityStart") {
            param("Name", javaClass.simpleName)
        }
    }

    override
    fun dismissLoadingProgressDialog() {
        mProgressDialog?.dismiss()
    }
}