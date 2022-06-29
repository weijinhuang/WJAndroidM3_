package com.wj.androidm3.business.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.wj.basecomponent.util.log.WJLog

class WJFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        WJLog.i("FCM new token -> $token")
    }

}