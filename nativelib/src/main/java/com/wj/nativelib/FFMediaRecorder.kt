package com.wj.nativelib

import com.wj.basecomponent.util.log.WJLog

class FFMediaRecorder : MediaRecorderContext() {

    fun init() {
        WJLog.d("FFMediaRecorder.init()")
        CreateContext()
        Init()
    }
}