package com.wj.nativelib

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import com.wj.basecomponent.util.log.WJLog
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.lang.ref.WeakReference

const val SAMPLE_RATE = 44100

const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC

const val CHANNEL = AudioFormat.CHANNEL_IN_STEREO

const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

class WJAudioRecordRecorder(context: Activity, filePath: String) {

    private val mCtxRef = WeakReference(context)

    var mAudioRecord: AudioRecord? = null

    var mRecordThread: Job? = null

    private var mPcmPath = filePath

    private var mBufferSize = 0

    private var mRecording = false

    private var mOnAudioRecordListener: ((ByteArray, Int) -> Unit)? = null

    fun setOnAudioRecordListener(onAudioRecordListener: (ByteArray, Int) -> Unit) {
        mOnAudioRecordListener = onAudioRecordListener
    }

    fun initRecorder() {
        mAudioRecord?.release()
        mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, AUDIO_FORMAT)
        mCtxRef.get()?.let {
            if (ActivityCompat.checkSelfPermission(it, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.RECORD_AUDIO), 99)
                return
            }
            mAudioRecord = AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL, AUDIO_FORMAT, mBufferSize)
        }
    }

    fun recordStart(): Int {
        return if (mRecording) {
            WJLog.d("正在录制")
            RECORD_STATE.STATE_RECORDING.value
        } else {
            mAudioRecord?.let {
                if (it.state == AudioRecord.STATE_INITIALIZED) {
                    WJLog.d("Android层开启录制")
                    it.startRecording()
                    mRecording = true
                    mRecordThread = GlobalScope.launch(Dispatchers.IO) {
//                        val outputStream = FileOutputStream(mPcmPath)
                        val audioBuffer = ByteArray(mBufferSize)
//                        outputStream.use { os ->
                            while (isActive && mRecording) {
                                val audioSampleSize = it.read(audioBuffer, 0, mBufferSize)
                                if (audioSampleSize > 0) {
//                                    os.write(audioBuffer)
                                    mOnAudioRecordListener?.invoke(audioBuffer, audioBuffer.size)
                                }
                            }
//                        }

                    }
                    RECORD_STATE.STATE_SUCCESS.value
                } else {
                    RECORD_STATE.STATE_ERROR.value
                }
            } ?: RECORD_STATE.STATE_ERROR.value
        }
    }

    fun recordStop() {
        mAudioRecord?.let {
            mRecording = false
            mRecordThread?.cancel()
            if (it.state == AudioRecord.STATE_INITIALIZED) {
                it.stop()
            }
            it.release()
        }
    }

    fun clear() {
        mRecordThread?.cancel()
    }

}