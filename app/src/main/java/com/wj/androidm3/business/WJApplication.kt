package com.wj.androidm3.business

import android.app.Application
import com.wj.basecomponent.BaseApplication

class WJApplication : BaseApplication() {


    companion object {

        lateinit var mInstance: WJApplication

        fun getInstance() = mInstance
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
    }
}