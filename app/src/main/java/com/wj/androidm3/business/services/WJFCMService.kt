package com.wj.androidm3.business.services

import android.content.Intent
import androidx.annotation.WorkerThread
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wj.basecomponent.util.log.WJLog

class WJFCMService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        WJLog.i("${javaClass.simpleName} -> onCreate")
    }

    override fun getStartCommandIntent(originalIntent: Intent?): Intent {
        return super.getStartCommandIntent(originalIntent)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        WJLog.i("FCM new token -> $token")
    }

    @WorkerThread
    override fun onMessageReceived(message: RemoteMessage) {
        WJLog.i("${javaClass.simpleName} -> onMessageReceived : ${message.notification?.body ?: "Unit"}")
    }

    @WorkerThread
    override fun onDeletedMessages() {
        WJLog.i("${javaClass.simpleName} -> onDeletedMessages")
    }

    @WorkerThread
    override fun onMessageSent(msgId: String) {
        WJLog.i("${javaClass.simpleName} -> onMessageSent : $msgId")
    }

    @WorkerThread
    override fun onSendError(msgId: String, exception: Exception) {
        WJLog.i("${javaClass.simpleName} -> onSendError : ${exception.message}")
    }

}