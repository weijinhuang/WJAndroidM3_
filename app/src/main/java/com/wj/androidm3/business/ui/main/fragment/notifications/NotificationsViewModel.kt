package com.wj.androidm3.business.ui.main.fragment.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wj.basecomponent.vm.BaseViewModel

class NotificationsViewModel : BaseViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text
}
