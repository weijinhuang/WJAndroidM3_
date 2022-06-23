package com.wj.basecomponent.ui

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import com.wj.basecomponent.ui.constraint.BaseUI

abstract class BaseActivity : AppCompatActivity(), BaseUI {

    private var mProgressDialog: ProgressDialog? = null


    override
    fun showLoadingProgress(cancelable: Boolean) {
        mProgressDialog?.dismiss()
        mProgressDialog = ProgressDialog(this).apply {
            setCancelable(cancelable)
        }
        mProgressDialog?.show()
    }

    override
    fun dismissLoadingProgressDialog() {
        mProgressDialog?.dismiss()
    }
}