package com.wj.androidm3.business.ui.camera

import androidx.databinding.Bindable
import com.wj.androidm3.BR
import com.wj.basecomponent.vm.BaseViewModel

class Camera2ViewModel : BaseViewModel() {


    @get:Bindable
    var mRecording:Boolean = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.mRecording)
        }

}