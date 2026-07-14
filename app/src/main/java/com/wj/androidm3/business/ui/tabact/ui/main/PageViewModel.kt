package com.wj.androidm3.business.ui.tabact.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.wj.basecomponent.vm.BaseViewModel

class PageViewModel : BaseViewModel() {

    private val _index = MutableLiveData<Int>()
    val text: LiveData<String> = _index.map {
        "Hello world from section: $it"
    }

    fun setIndex(index: Int) {
        _index.value = index
    }
}
