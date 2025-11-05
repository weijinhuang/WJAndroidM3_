package com.wj.basecomponent

import android.app.Application
import com.wj.basecomponent.androidx.WJActivityLifeCycle
import com.wj.basecomponent.util.SPUtils

open class BaseApplication : Application() {

    companion object {
        @JvmStatic
        lateinit var INSTANCE: BaseApplication
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        this.registerActivityLifecycleCallbacks(WJActivityLifeCycle())
        SPUtils.init(this)
    }
}