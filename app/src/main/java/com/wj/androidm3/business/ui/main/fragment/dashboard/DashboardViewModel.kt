package com.wj.androidm3.business.ui.main.fragment.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wj.basecomponent.vm.BaseViewModel

class DashboardViewModel : BaseViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text
}