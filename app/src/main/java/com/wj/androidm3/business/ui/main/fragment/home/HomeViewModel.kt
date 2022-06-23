package com.wj.androidm3.business.ui.main.fragment.home

import android.os.Build
import androidx.databinding.Bindable
import com.wj.androidm3.BR
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel

class HomeViewModel : BaseViewModel() {

    var mSystemInfo = "Power by H.W.J"
        @Bindable
        get


    fun getSystemInfo() {
        launch {
            mSystemInfo = "Model:${Build.MODEL}:SDK_INT:${Build.VERSION.SDK_INT}\n"
            notifyPropertyChanged(BR.mSystemInfo)
            WJLog.d("-------$mSystemInfo")
        }


    }
}