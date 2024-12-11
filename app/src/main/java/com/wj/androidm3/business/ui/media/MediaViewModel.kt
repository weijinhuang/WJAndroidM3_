package com.wj.androidm3.business.ui.media

import android.content.Context
import android.os.Environment
import androidx.databinding.Bindable
import com.wj.androidm3.business.WJApplication
import com.wj.basecomponent.vm.BaseViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.wj.androidm3.BR
import kotlinx.coroutines.Job

class MediaViewModel : BaseViewModel() {

    @get:Bindable
    var recordingAACByMediaCodec = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.recordingAACByMediaCodec)
        }

    fun createPrivateAudioFile(fileName: String) =
        File(WJApplication.getInstance().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + fileName)

    fun createAACAudioFile(): File {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA)
        return File(
            "${
                WJApplication.getInstance().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path
            }/${simpleDateFormat.format(System.currentTimeMillis())}.aac"
        )
    }


}