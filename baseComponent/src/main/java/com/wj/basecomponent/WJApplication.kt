package com.wj.basecomponent

import android.app.Application
import com.wj.basecomponent.androidx.WJActivityLifeCycle

class WJApplication : Application() {

    companion object {
        @JvmStatic
        lateinit var INSTANCE: WJApplication
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        this.registerActivityLifecycleCallbacks(WJActivityLifeCycle())
    }
}