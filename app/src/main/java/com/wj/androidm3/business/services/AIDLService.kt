package com.wj.androidm3.business.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AIDLService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return super.onStartCommand(intent, flags, startId)
    }
}