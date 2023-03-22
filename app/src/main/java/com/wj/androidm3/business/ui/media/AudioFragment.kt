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
import com.wj.nativelib.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AudioFragment : BaseMVVMFragment<MediaViewModel, FragmentAudioBinding>() {

    private var mAudioRecord: AudioRecord? = null

    private var mFFMediaRecorder: FFMediaRecorder? = null

    var mSimpleRate = 44100

    private var mRecordingJob: Job? = null

    private var mOutUrl: String = ""

    private var mRecording = false

    private var mWJNativeAudioEncoder: WJNativeAudioEncoder? = null

    override fun firstCreateView() {
        mViewBinding?.run {
            startRecordAAC.setOnClickListener {
                if (!mRecording) {
                    startRecordAAC.text = "Stop Recording ACC"
                    checkReadExternalFilePermission {
                        checkRecordPermission {
                            initAudioRecord { audioRecord, buffSize ->
                                if (null == mFFMediaRecorder) {
                                    mFFMediaRecorder = FFMediaRecorder().apply { init() }
                                }
                                mFFMediaRecorder?.run {
                                    mRecordingJob = mViewModel.launchBackground2 {
                                        val mOutUrl = mViewModel.createAACAudioFile().absolutePath
                                        WJLog.i("开始录制ACC->$mOutUrl")
                                        StartRecord(RECORDER_TYPE_SINGLE_AUDIO, mOutUrl, 0, 0, 0, 0)
                                        audioRecord.startRecording()
                                        val simpleBuffer = ByteArray(4096)
                                        while (isActive) {
                                            val result = audioRecord.read(simpleBuffer, 0, 4096)
                                            if (result > 0) {
                                                WJLog.d("Kotlin层读取数据：$result")
                                                mFFMediaRecorder?.OnAudioData(simpleBuffer, result)
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                } else {
                    startRecordAAC.text = "Start Recording ACC"
                    if (mAudioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        mFFMediaRecorder?.StopRecord()
                        mAudioRecord?.stop()
                        mAudioRecord?.release()
                        mAudioRecord = null
                        mRecordingJob?.cancel()
                        mRecordingJob = null
                    }
                }

            }

            startRecordAudio.setOnClickListener { btn ->
                checkReadExternalFilePermission {
                    checkRecordPermission {
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
                                        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                                        val audioFileName =
                                            requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + simpleDateFormat.format(
                                                System.currentTimeMillis()
                                            ) + ".pcm"
                                        WJLog.i("Start record -> $audioFileName")
                                        FileOutputStream(audioFileName).use { fos ->
                                            while (isActive) {
                                                val ret = ar.read(buffer, 0, bufSize)
                                                WJLog.d("recording : $ret")
                                                if (ret > 0) {
                                                    fos.write(buffer, 0, ret)
//                                                    WJLog.i(buffer.contentToString())
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
            playAudio.setOnClickListener {
                checkReadExternalFilePermission {
                    mViewModel.launchBackground2 {
                        val path =
                            requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + "if_have_a_date.mp3"
//                        val path = Environment.getExternalStorageDirectory().path + "/Music/if_have_a_date.mp3"
                        WJLog.d("播放：$path")
                        val mediaPlayer = WJMediaJNIHepler()
                        mediaPlayer.playAudio(path)
                    }
                }

            }
            resampleAudio.setOnClickListener {
                val inPath =
                    requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + "if_have_a_date.mp3"
                val outPath =
                    requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + "if_have_a_date2.mp3"
//                        val path = Environment.getExternalStorageDirectory().path + "/Music/if_have_a_date.mp3"
                WJLog.d("播放：$inPath")
                val mediaPlayer = WJMediaJNIHepler()
                mediaPlayer.audioResample(inPath, outPath, 16000)
            }
            pushAv.setOnClickListener {
                val inputPath = "";
                val outPath = "";
                WJMediaJNIHepler().apply {
                    pushStream(inputPath, outPath);
                }
            }

            aacRecord1.setOnClickListener {
                checkReadExternalFilePermission {
                    checkRecordPermission {
                        if (null == mWJNativeAudioEncoder) {
                            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                            val audioFileName =
                                requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + simpleDateFormat.format(
                                    System.currentTimeMillis()
                                ) + ".aac"
                            WJLog.d("创建aac：$audioFileName")
                            mWJNativeAudioEncoder = WJNativeAudioEncoder(requireActivity(), audioFileName)
                        }
                        mWJNativeAudioEncoder?.run {
                            initRecorder()
                            recordStart()
                        }
                    }
                }


            }

            stopAAcRecord1.setOnClickListener {
                WJLog.d("停止录制AAC")
                mWJNativeAudioEncoder?.recordStop()
                mWJNativeAudioEncoder = null
            }
        }

    }

    override fun onStop() {
        super.onStop()
        mRecordingJob?.cancel()
        mRecordingJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mFFMediaRecorder?.DestroyContext()
    }

    private fun initAudioRecord(block: (AudioRecord, Int) -> Unit) {
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(mSimpleRate, channelConfig, audioFormat)
        if (null != mAudioRecord) {
            block.invoke(mAudioRecord!!, bufferSize)
            return
        }
        mAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            mSimpleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        block.invoke(mAudioRecord!!, bufferSize)
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_audio
    }

    private fun checkRecordPermission(block: () -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            block.invoke()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 99)
        }
    }

    private fun checkReadExternalFilePermission(block: () -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            block.invoke()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 99)
        }
    }
}