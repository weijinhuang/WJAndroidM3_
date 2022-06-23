package com.wj.androidm3.business.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.wj.androidm3.R
import com.wj.androidm3.business.ui.conversationincome.PhoneConversationActivity
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.util.notification.sendNotification
import kotlinx.coroutines.*


const val CONVERSATION_NOTIFICATION_ID = 112312312

class BackgroundService : Service() {

    var mJob: Job? = null

    var mCount = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        WJLog.d("${javaClass.simpleName} -> onStartCommand")
        mJob = GlobalScope.launch(Dispatchers.IO) {
            while (mCount < Int.MAX_VALUE) {
                delay(1000)
                WJLog.d("$mCount -> % = ${mCount % 5}")
                if (mCount % 5 == 0) {
                    sendNotification(
                        this@BackgroundService, PhoneConversationActivity::class.java, "WJ:Notification", CONVERSATION_NOTIFICATION_ID,
                        R.mipmap.ic_launcher_round, "TestNotificationTitle", "TestNotificationContent"
                    )
                }
                mCount++
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mJob?.cancel()
        super.onDestroy()
    }
}