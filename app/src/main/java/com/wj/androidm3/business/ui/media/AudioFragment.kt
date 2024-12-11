package com.wj.androidm3.business.ui.media

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.os.Environment
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.wj.androidm3.R
import com.wj.androidm3.business.ui.media.audio.AudioEncoder
import com.wj.androidm3.databinding.FragmentAudioBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.BufferConverter
import com.wj.basecomponent.util.fw.basictype.FWUnsignedInt
import com.wj.basecomponent.util.fw.basictype.FWUnsignedShort
import com.wj.basecomponent.util.log.WJLog
import com.wj.nativelib.*
import com.wj.nativelib.bean.WaveHeadJava
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.*

class AudioFragment : BaseMVVMFragment<MediaViewModel, FragmentAudioBinding>() {

    private var mAudioRecord: AudioRecord? = null

    private var mFFMediaRecorder: FFMediaRecorder? = null

    var mSimpleRate = 8000

    var mChannelConfig = AudioFormat.CHANNEL_IN_MONO

    val mChannelCount = 1

    private var mRecordingJob: Job? = null

    private var mOutUrl: String = ""

    private var mRecording = false

    private var mWJNativeAudioEncoder: WJNativeAudioEncoder? = null

    private var mAACFileName: String = ""

    init {
        System.loadLibrary("nativelib");
    }


    override fun firstCreateView() {
        mViewBinding?.run {
            viewModel = mViewModel
            startRecordAAC.setOnClickListener {
                if (!mRecording) {
                    checkReadExternalFilePermission {
                        checkRecordPermission {
                            initAudioRecord { audioRecord, buffSize ->
                                startRecordAAC.text = "Stop Recording ACC"
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
                            mAACFileName =
                                requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + simpleDateFormat.format(
                                    System.currentTimeMillis()
                                ) + ".aac"
                            WJLog.d("创建aac：$mAACFileName")
                            mWJNativeAudioEncoder = WJNativeAudioEncoder(requireActivity(), mAACFileName)
                        }
                        mWJNativeAudioEncoder?.run {
                            initRecorder()
                            recordStart()
                        }
                    }
                }


            }

            stopAAcRecord1.setOnClickListener {
                WJLog.d("停止录制AAC: $mAACFileName")
                mWJNativeAudioEncoder?.recordStop()
                mWJNativeAudioEncoder = null
            }

            startRecordWav.setOnClickListener {
                startRecordWav()
            }

            stopRecordWav.setOnClickListener {
                stopRecordWav()
            }
            startRecordPCM.setOnClickListener {
                startRecordPCM()
            }
            stopRecordPCM.setOnClickListener {
                stopRecordPCM()
            }
            playPCM.setOnClickListener {
            }
            startRecordAACByMediaCodec.setOnClickListener {
                if (!mViewModel.recordingAACByMediaCodec) {
                    mViewModel.recordingAACByMediaCodec = true
                    startRecordAACByMediaCodec()
                } else {
                    mViewModel.recordingAACByMediaCodec = false
                    mRecordingJob?.cancel()
                }
            }

        }

    }

    private var mAudioEncoder: AudioEncoder? = null
    private fun startRecordAACByAudioRecord2() {
        mAudioEncoder = AudioEncoder()
        mAudioEncoder?.startEncoding(requireActivity())
    }

    var mMediaMuxer: MediaMuxer? = null
    var mTrackIndex = -1
    private fun startRecordAACByMediaCodec() {
        checkRecordPermission {
            initAudioRecord { audioRecord, bufferSize ->
                initMediaCodec(bufferSize) { mediaCodec ->
                    mRecordingJob?.cancel()
                    startRecordingChronometer()
                    mViewModel.recordingAACByMediaCodec = true
                    mRecordingJob = lifecycleScope.launch(Dispatchers.IO) {
                        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                        mAACFileName =
                            requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + simpleDateFormat.format(
                                System.currentTimeMillis()
                            ) + ".aac"
                        WJLog.d("创建aac：$mAACFileName")

                        mMediaMuxer = MediaMuxer(mAACFileName, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                        audioRecord.startRecording()
                        val mediaCodecBufferInfo = MediaCodec.BufferInfo()

                        WJLog.d("开始录音")
//                        startRecordTimer()
                        while (mViewModel.recordingAACByMediaCodec) {
                            val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
                            if (inputBufferIndex >= 0) {
                                mediaCodec.getInputBuffer(inputBufferIndex)?.let { inputBuffer ->
                                    inputBuffer.clear()
                                    val readSize = audioRecord.read(inputBuffer, inputBuffer.capacity())
                                    if (readSize > 0) {
                                        WJLog.d(" Read PCM $readSize")
                                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, readSize, System.nanoTime() / 1000, 0)
                                    }
                                }

                            }
                            var outputBufferIndex = mediaCodec.dequeueOutputBuffer(mediaCodecBufferInfo, -1)
                            while (outputBufferIndex >= 0) {
                                mediaCodec.getOutputBuffer(outputBufferIndex)?.let { outputBuffer ->
                                    val outData = ByteArray(mediaCodecBufferInfo.size)
                                    outputBuffer.get(outData)
                                    if ((mediaCodecBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                        if (mTrackIndex == -1) {
                                            val outputFormat = mediaCodec.outputFormat
                                            mTrackIndex = mMediaMuxer?.addTrack(outputFormat) ?: -1
                                            mMediaMuxer?.start()
                                        }
                                        WJLog.d("encode aac size :${mediaCodecBufferInfo.size}")
                                        mMediaMuxer?.writeSampleData(mTrackIndex, outputBuffer, mediaCodecBufferInfo)
                                    }
                                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(mediaCodecBufferInfo, 0)
                                }
                            }


                        }

                        WJLog.i("停止录制AAC:$mAACFileName")

                        stopRecordingChronometer()
                        if (mTrackIndex != -1) {
                            WJLog.d("释放")
                            mTrackIndex = -1
                            mMediaMuxer?.stop()
                            mMediaMuxer?.release()
                            mMediaMuxer = null

                        }


                    }

                }
            }
        }
    }

    private var mPCMFileName: String? = null
    private var mRecordPCMJob: Job? = null
    private fun startRecordPCM() {
        checkRecordPermission {
            initAudioRecord { audioRecord, bufferSize ->
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                mPCMFileName =
                    requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + simpleDateFormat.format(
                        System.currentTimeMillis()
                    ) + ".pcm"
                WJLog.d("创建pcm：$mPCMFileName")
                audioRecord.startRecording()
                mRecordPCMJob = lifecycleScope.launch(Dispatchers.IO) {
                    FileOutputStream(mPCMFileName).use { fos ->
                        val buffer = ByteArray(1024)
                        while (isActive) {
                            val readCount = audioRecord.read(buffer, 0, 1024)
                            if (readCount > 0) {
                                WJLog.d("data size :$readCount")
                                fos.write(buffer, 0, readCount)
                            } else {
                                fos.flush()
                                cancel()
                            }
                        }
                        WJLog.i("循环结束")
                    }
                }
            }
        }

    }

    private fun stopRecordPCM() {
        mRecordPCMJob?.cancel()
    }

    private var mRecordWaveJob: Job? = null
    private var mWaveFileName: String? = null
    private fun startRecordWav() {
        checkRecordPermission {
            initAudioRecord { audioRecord, bufferSize ->
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                mWaveFileName =
                    requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + simpleDateFormat.format(
                        System.currentTimeMillis()
                    ) + ".wav"
                WJLog.d("创建wave：$mWaveFileName")
                audioRecord.startRecording()
                mRecordWaveJob = lifecycleScope.launch(Dispatchers.IO) {
                    FileOutputStream(mWaveFileName).use { fos ->
                        fos.write(ByteArray(44), 0, 44)
                        val buffer = ByteArray(1024)
                        while (isActive) {
                            val readCount = audioRecord.read(buffer, 0, 1024)
                            if (readCount > 0) {
                                WJLog.d("data size :$readCount")
                                fos.write(buffer, 0, readCount)
                            } else {
                                fos.flush()
                                cancel()
                            }
                        }
                        WJLog.i("循环结束")
                    }
                }

            }
        }

    }

    private fun stopRecordWav() {
        mRecordWaveJob?.let {
            it.cancel()
            lifecycleScope.launch(Dispatchers.IO) {
                delay(100L)

                val file = File(mWaveFileName)

                val wavHead = WaveHeadJava().apply {
                    numChannels = FWUnsignedShort(2)
                    simpleRate = FWUnsignedInt(44100)
                    bitsPerSample = FWUnsignedShort(16)
                    blockAlign = FWUnsignedShort(bitsPerSample.value * numChannels.value / 8)
                    byteRate = FWUnsignedInt(simpleRate.value * blockAlign.value)
                    dataChunkDataSize = FWUnsignedInt(file.length() - 44)
                    riffChunkDataSize = FWUnsignedInt(dataChunkDataSize.value + 44 - 8)

                }

                RandomAccessFile(mWaveFileName, "rw").use {
                    it.seek(0)
                    val buffer = BufferConverter.getBuffer(wavHead)
                    WJLog.d(buffer.contentToString())
                    it.write(buffer)
                }

                WJLog.i("录制结束：$mWaveFileName")
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

    @RequiresPermission(value = "android.permission.RECORD_AUDIO")
    private fun initAudioRecord(block: (audioRecord: AudioRecord, bufferSize: Int) -> Unit) {
        val channelConfig = mChannelConfig
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(mSimpleRate, channelConfig, audioFormat)
        WJLog.d("--initAudioRecord--channelConfig:$channelConfig audioFormat:$audioFormat bufferSize:$bufferSize")
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

    private var mediaCodec: MediaCodec? = null

    @RequiresPermission(value = "android.permission.RECORD_AUDIO")
    private fun initMediaCodec(bufferSize: Int, block: (mediaCodec: MediaCodec) -> Unit) {
        val audioFormat = MediaFormat.createAudioFormat(MediaFormat.KEY_MIME, mSimpleRate, mChannelCount)
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 48000)
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        mediaCodec?.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec?.start()
        block.invoke(mediaCodec!!)
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
        block.invoke()
//        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//            block.invoke()
//        } else {
//            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 99)
//        }
    }

    private fun startRecordingChronometer() {
        mViewBinding?.recordingChronometer?.let { chronometer: Chronometer ->
            chronometer.visibility = View.VISIBLE
            chronometer.base = SystemClock.elapsedRealtime()
            chronometer.start()
        }
    }

    private fun stopRecordingChronometer() {
        mViewBinding?.recordingChronometer?.let { chronometer: Chronometer ->
            requireActivity().runOnUiThread {
                chronometer.stop()
                chronometer.visibility = View.GONE
            }

        }
    }
}