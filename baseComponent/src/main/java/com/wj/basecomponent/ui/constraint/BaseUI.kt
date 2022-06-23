package com.wj.basecomponent.ui.constraint

interface BaseUI {

    fun getLayoutId(): Int

    fun showLoadingProgress(cancelable: Boolean)

    fun dismissLoadingProgressDialog()
}