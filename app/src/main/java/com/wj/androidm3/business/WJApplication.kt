package com.wj.androidm3.business

import android.app.Application

class WJApplication : Application() {


    companion object {

        lateinit var mInstance: WJApplication

        fun getInstance() = mInstance
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
    }
}