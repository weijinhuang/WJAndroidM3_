package com.wj.androidm3.business.ui.media

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import androidx.core.app.ActivityCompat
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentAudioBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import java.io.FileOutputStream

class AudioFragment : BaseMVVMFragment<MediaViewModel, FragmentAudioBinding>() {

    private var mAudioRecord: AudioRecord? = null

    var mSimpleRate = 44100

    private var mRecordingJob: Job? = null

    override fun firstCreateView() {
        mViewBinding?.run {
            startRecordAudio.setOnClickListener { btn ->
                checkPermission {
                    initAudioRecord { ar, bufSize ->
                        if (ar.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            ar.stop()
                            mRecordingJob?.cancel()
                            mRecordingJob = null
                            startRecordAudio.text = "Start Record"
                        } else {
                            startRecordAudio.text = "Stop Record"
                            mRecordingJob = mViewModel.launchBackground2 {
                                ar.startRecording()
                                val buffer = ByteArray(bufSize)
                                try {
                                    val audioFileName =
                                        requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + System.currentTimeMillis() + ".pcm"
                                    WJLog.i("Start record -> $audioFileName")
                                    FileOutputStream(audioFileName).use { fos ->
                                        while (isActive) {
                                            val ret = ar.read(buffer, 0, bufSize)
                                            WJLog.d("recording : $ret")
                                            if (ret > 0) {
                                                fos.write(buffer, 0, ret)
                                            }
                                        }
                                    }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    WJLog.e(e.message ?: "")
                                }
                                WJLog.i("Recording end")
                            }
                        }
                    }
                }
            }
        }

    }

    override fun onStop() {
        super.onStop()
        mRecordingJob?.cancel()
        mRecordingJob = null
    }

    private fun initAudioRecord(block: (AudioRecord, Int) -> Unit) {
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(mSimpleRate, channelConfig, audioFormat)
        if (null != mAudioRecord) {
            block.invoke(mAudioRecord!!, bufferSize)
            return
        }
        mAudioRecord =
            AudioRecord(MediaRecorder.AudioSource.MIC, mSimpleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        block.invoke(mAudioRecord!!, bufferSize)
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_audio
    }

    private fun checkPermission(block: () -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            block.invoke()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 99)
        }
    }
}