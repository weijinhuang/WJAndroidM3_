package com.wj.androidm3.business.ui.main.fragment.home

import android.content.Context
import android.os.Build
import androidx.databinding.Bindable
import com.wj.androidm3.BR
import com.wj.androidm3.business.WJApplication
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel

class HomeViewModel : BaseViewModel() {

    var mSystemInfo = "Power by H.W.J"
        @Bindable
        get

    var mCurrentDensity = ""
        @Bindable
        get
        set(value) {
            field = value
            notifyPropertyChanged(BR.mCurrentDensity)
        }


    fun getSystemInfo() {
        launch {
            mSystemInfo = "Model:${Build.MODEL}:SDK_INT:${Build.VERSION.SDK_INT}\n"
            notifyPropertyChanged(BR.mSystemInfo)
            WJLog.d("-------$mSystemInfo")
        }
    }

    fun getDensityInfo(context: Context) {
        val displayMetrics = context.resources.displayMetrics
        mCurrentDensity = "Density:${displayMetrics.density}\n" +
                " densityDpi:${displayMetrics.densityDpi}\n" +
                " scaledDensity:${displayMetrics.scaledDensity}\n" +
                " xdpi:${displayMetrics.xdpi}\n" +
                " ydpi:${displayMetrics.ydpi}\n" +
                " heightPixels:${displayMetrics.heightPixels}\n" +
                " widthPixels:${displayMetrics.widthPixels}\n" +
                " x:${displayMetrics.density * displayMetrics.xdpi}\n"
    }
}