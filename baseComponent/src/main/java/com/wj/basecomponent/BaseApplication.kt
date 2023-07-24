package com.wj.basecomponent

import android.app.Application
import com.wj.basecomponent.androidx.WJActivityLifeCycle

open class BaseApplication : Application() {

    companion object {
        @JvmStatic
        lateinit var INSTANCE: BaseApplication
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        this.registerActivityLifecycleCallbacks(WJActivityLifeCycle())
    }
}