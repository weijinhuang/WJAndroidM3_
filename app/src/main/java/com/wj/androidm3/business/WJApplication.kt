package com.wj.androidm3.business

import android.app.Application
import com.tencent.bugly.crashreport.CrashReport
import com.wj.basecomponent.BaseApplication

class WJApplication : BaseApplication() {


    companion object {

        lateinit var mInstance: WJApplication

        fun getInstance() = mInstance
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        CrashReport.initCrashReport(this, "eff92f0cf3", false);
    }
}