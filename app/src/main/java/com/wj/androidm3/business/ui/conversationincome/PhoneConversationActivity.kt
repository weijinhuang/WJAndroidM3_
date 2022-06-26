package com.wj.androidm3.business.ui.conversationincome

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wj.androidm3.R

class PhoneConversationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_conversation)
        window.decorView.systemUiVisibility
    }
}