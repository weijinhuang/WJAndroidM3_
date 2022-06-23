package com.wj.basecomponent.androidx

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.wj.basecomponent.util.log.WJLog

class WJActivityLifeCycle : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, p1: Bundle?) {
        WJLog.d("${activity.javaClass.simpleName} : onCreate")
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        WJLog.d("${activity.javaClass.simpleName} : onDestroyed")
    }
}