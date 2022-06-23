package com.wj.basecomponent.util.log

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.viewbinding.BuildConfig
import timber.log.Timber

class TimberInitializer : Initializer<Unit> {
    override fun create(context: Context): Unit {
            Timber.plant(Timber.DebugTree())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}