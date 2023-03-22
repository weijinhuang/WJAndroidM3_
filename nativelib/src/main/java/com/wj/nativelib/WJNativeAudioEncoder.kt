package com.wj.nativelib

import android.app.Activity
import com.wj.basecomponent.util.log.WJLog

class WJNativeAudioEncoder(val act: Activity, val aacPath: String) {
    companion object {
        // Used to load the 'nativelib' library on application startup.
        init {
            System.loadLibrary("nativelib")
        }
    }

    external fun encodeAudioStart(aacPath: String)

    external fun encodeAudioStop();

    external fun onAudioFrame(pcmData: ByteArray, len: Int)

    private var mAudioRecorder: WJAudioRecordRecorder = WJAudioRecordRecorder(act, aacPath).apply {
        setOnAudioRecordListener { bytes, len ->
            onAudioFrame(bytes, len)
        }
    }

    private var mAACPath: String? = null

    fun initRecorder() {
        //初始化native编码器
        WJLog.d("initRecorder()")
        encodeAudioStart(aacPath)
        mAudioRecorder.initRecorder()
    }

    fun recordStart(): Int {
        WJLog.d("--recordStart--")
        return mAudioRecorder.recordStart()
    }

    fun recordStop() {
        mAudioRecorder.recordStop()
        encodeAudioStop()
    }
}