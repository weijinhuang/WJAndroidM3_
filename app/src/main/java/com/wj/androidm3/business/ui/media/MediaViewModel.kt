package com.wj.androidm3.business.ui.media

import android.content.Context
import android.os.Environment
import com.wj.androidm3.business.WJApplication
import com.wj.basecomponent.vm.BaseViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MediaViewModel : BaseViewModel() {

    fun createPrivateAudioFile(fileName: String) =
        File(WJApplication.getInstance().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + fileName)

    fun createAACAudioFile(): File {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA)
        return File(
            "${WJApplication.getInstance().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path}/${simpleDateFormat.format(System.currentTimeMillis())}.aac"
        )
    }


}