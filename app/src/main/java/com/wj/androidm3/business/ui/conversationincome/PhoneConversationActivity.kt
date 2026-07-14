package com.wj.androidm3.business.ui.conversationincome

import android.os.Bundle
import com.wj.androidm3.R
import com.wj.androidm3.databinding.ActivityPhoneConversationBinding
import com.wj.basecomponent.ui.BaseMVVMActivity
import com.wj.basecomponent.vm.BaseViewModel

class PhoneConversationActivity : BaseMVVMActivity<BaseViewModel, ActivityPhoneConversationBinding>() {

    override fun getLayoutId(): Int {
        return R.layout.activity_phone_conversation
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility
    }
}
