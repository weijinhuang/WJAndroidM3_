package com.wj.androidm3.business.ui.media

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
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
import androidx.navigation.fragment.findNavController
import com.wj.androidm3.R
import com.wj.androidm3.business.ui.media.audio.AudioEncoder
import com.wj.androidm3.databinding.FragmentAudioBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.BufferConverter
import com.wj.basecomponent.util.fw.basictype.FWUnsignedInt
import com.wj.basecomponent.util.fw.basictype.FWUnsignedShort
import com.wj.basecomponent.util.log.WJLog
import com.wj.nativelib.FFMediaRecorder
import com.wj.nativelib.RECORDER_TYPE_SINGLE_AUDIO
import com.wj.nativelib.WJMediaJNIHepler
import com.wj.nativelib.WJNativeAudioEncoder
import com.wj.nativelib.bean.WaveHeadJava
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Locale


//ffmpeg -i input.mp4 -vf "drawtext=text='Watermark Text':x=10:y=10:fontsize=12:fontcolor=white:shadowy=2" -codec:a copy output.mp4
//ffmpeg -i .\VID_20241214_133448.mp4 -vf "drawtext=fontfile=simhei.ttf:=text='粤AG30750':fontsize=44:fontcolor=white:shadowy=2:x=2580:y=1420" -codec:a copy output.mp4
/**
ffmpeg -i .\VID_20241214_133448.mp4 -vf "drawtext=text='方向：CH1':x=2580:y=1420:fontsize=34:shadowy=2:fontcolor=white:fontfile=.\simhei.ttf, drawtext=text='用户：粤AG30750':x=W-tw-10:y=H-th-10:fontsize=34:shadowy=2:fontcolor=white:fontfile=.\simhei.ttf" -codec:a copy output.mp4
 **/
class AudioFragment : BaseMVVMFragment<MediaViewModel, FragmentAudioBinding>() {


    private var mAudioRecord: AudioRecord? = null

    private var mFFMediaRecorder: FFMediaRecorder? = null

    var mSimpleRate = 44100
    var mChannelConfig = AudioFormat.CHANNEL_IN_STEREO
    val mAudioFormat = AudioFormat.ENCODING_PCM_16BIT

//    var mSimpleRate = 48000
//    var mChannelConfig = AudioFormat.CHANNEL_IN_MONO
//    val mAudioFormat = AudioFormat.ENCODING_PCM_FLOAT

    var mResampleSimpleRate = 48000
    var mResampleChannelConfig = AudioFormat.CHANNEL_OUT_MONO
    val mResampleAudioFormat = AudioFormat.ENCODING_PCM_FLOAT

    val mChannelCount = 1


    private var mRecordingJob: Job? = null

    private var mOutUrl: String = ""

    private var mRecording = false

    private var mWJNativeAudioEncoder: WJNativeAudioEncoder? = null

    private var mAACFileName: String = ""


    init {
        System.loadLibrary("nativelib");
    }

    override fun createViewModel(attachActivity: Boolean): MediaViewModel {
        return super.createViewModel(true)
    }

    override fun firstCreateView() {
        mViewBinding?.run {
            viewModel = mViewModel

            btnFileList.setOnClickListener {
                findNavController().navigate(R.id.mediaListFragment)

            }
            audioResample.setOnClickListener {
                audioResampleByFFmpeg()
            }

            startRecordAAC.setOnClickListener {
                if (!mRecording) {
                    recordAACEncodeByFFMpeg()
                } else {
                    stopRecordingChronometer()
                    startRecordAAC.text = "Start Recording ACC"
                    mRecording = false
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
            playAudio.setOnClickListener {
                checkReadExternalFilePermission {
                    playMP3()
                }

            }
            resampleAudio.setOnClickListener {
                resampleAudio()
            }
            pushAv.setOnClickListener {
                val inputPath = "";
                val outPath = "";
                WJMediaJNIHepler().apply {
                    pushStream(inputPath, outPath);
                }
            }

            aacRecord1.setOnClickListener {
                aacRecord1()
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
                playPCM()
            }
            playResamplePCM.setOnClickListener {
                playResamplePCM()
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

    private var mPlaying = false
    private var audioTrack: AudioTrack? = null
    private fun playPCM() {
        if (mPlaying) {
            return
        }
        mViewModel.mFilePath.let {
            if (it.endsWith("pcm")) {
                // 计算缓冲区大小
                val bufferSize = AudioTrack.getMinBufferSize(mSimpleRate, mChannelConfig, mAudioFormat)

                // 创建AudioTrack实例
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    mSimpleRate,
                    mChannelConfig,
                    mAudioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )


                // 开始播放
                audioTrack?.play()

                mViewModel.launch {
                    mPlaying = true
                    val buffer = ByteArray(bufferSize)
                    FileInputStream(it).use { fos ->
                        var read = fos.read(buffer)
                        while (read != -1 && mPlaying) {
                            audioTrack?.write(buffer, 0, read)
                            read = fos.read(buffer)
                        }
                        mPlaying = false
                    }

                }

            }
        }

    }

    private fun playResamplePCM() {
        if (mPlaying) {
            return
        }
        mViewModel.mFilePath.let {
            if (it.endsWith("pcm")) {
                // 计算缓冲区大小
                val bufferSize = AudioTrack.getMinBufferSize(mResampleSimpleRate, mResampleChannelConfig, mResampleAudioFormat)

                if(bufferSize == AudioTrack.ERROR_BAD_VALUE){
                    WJLog.e("参数组合错误")
                    return
                }
                // 创建AudioTrack实例
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    mResampleSimpleRate,
                    mResampleChannelConfig,
                    mResampleAudioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )


                // 开始播放
                audioTrack?.play()

                mViewModel.launch {
                    mPlaying = true
                    val buffer = ByteArray(bufferSize)
                    FileInputStream(it).use { fos ->
                        var read = fos.read(buffer)
                        while (read != -1 && mPlaying) {
                            audioTrack?.write(buffer, 0, read)
                            read = fos.read(buffer)
                        }
                        mPlaying = false
                    }

                }

            }
        }

    }


    private fun audioResampleByFFmpeg() {
//        mPCMFileName = requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/2025-01-16_230247.pcm"
        mViewModel.mFilePath?.let { srcPcm ->
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA)
            val dstPCM = requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + simpleDateFormat.format(
                System.currentTimeMillis()
            ) + "resample.pcm"
            val srcFile = File(srcPcm)
            if (srcFile.exists()) {
                val dstFile = File(dstPCM)
                if (!dstFile.exists()) {
                    val result = dstFile.createNewFile()
                    WJLog.d("创建文件:$result")
                }
                WJLog.d(" kotlin  源文件：$srcPcm  目标文件：${dstFile.absolutePath}")
                WJMediaJNIHepler().WJAudioResample(
                    srcPcm, mSimpleRate, 2, mAudioFormat,
                    dstPCM, mResampleSimpleRate, 1, mResampleAudioFormat
                )
            } else {
                WJLog.d("源文件不存在")
            }
        }

    }


    private fun resampleAudio() {
        val inPath =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + "if_have_a_date.mp3"
        val outPath =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + "if_have_a_date2.mp3"
        //                        val path = Environment.getExternalStorageDirectory().path + "/Music/if_have_a_date.mp3"
        WJLog.d("重采样：$inPath")
        val mediaPlayer = WJMediaJNIHepler()
        mediaPlayer.audioResample(inPath, outPath, 16000)
    }

    private fun playMP3() {
        mViewModel.launchBackground2 {
            val path =
                requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + "IfThereReallyWereAnotherDate.mp3"
            //                        val path = Environment.getExternalStorageDirectory().path + "/Music/if_have_a_date.mp3"
            val file = File(path)
            if (file.exists()) {
                WJLog.d("播放：$path")
                val mediaPlayer = WJMediaJNIHepler()
                mediaPlayer.playAudio(path)
            } else {
                WJLog.d("文件不存在：$path")
            }
        }
    }

    private fun aacRecord1() {
        checkReadExternalFilePermission {
            checkRecordPermission {
                if (null == mWJNativeAudioEncoder) {
                    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA)
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

    private fun recordAACEncodeByFFMpeg() {
        mViewBinding?.run {
            checkReadExternalFilePermission {
                checkRecordPermission {
                    startRecordingChronometer()
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
                        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA)
                        mAACFileName =
                            requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + simpleDateFormat.format(
                                System.currentTimeMillis()
                            ) + ".aac"
                        WJLog.d("创建aac：$mAACFileName")

                        val aacFile = File(mAACFileName)
                        aacFile.parentFile?.let { parentDir ->
                            if (!parentDir.exists()) {
                                val mkdirs = parentDir.mkdirs()
                                WJLog.d("创建文件夹 ${parentDir.absolutePath}：$mkdirs")
                            }
                        }
                        if (!aacFile.exists()) {
                            val createFileResult = aacFile.createNewFile()
                            WJLog.d("创建文件 :$createFileResult")
                        }


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

                        WJLog.i("停止录制AAC:$mAACFileName 文件存在：${File(mAACFileName).exists()}")

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
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA)
                mPCMFileName =
                    requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path + "/" + simpleDateFormat.format(
                        System.currentTimeMillis()
                    ) + ".pcm"
                WJLog.d("创建pcm：$mPCMFileName")
                startRecordingChronometer()
                audioRecord.startRecording()
                mRecording = true
                mRecordPCMJob = lifecycleScope.launch(Dispatchers.IO) {
                    FileOutputStream(mPCMFileName).use { fos ->
                        val buffer = ByteArray(1024)
                        while (mRecording) {
                            val readCount = audioRecord.read(buffer, 0, 1024)
                            if (readCount > 0) {
                                WJLog.d("data size :$readCount")
                                fos.write(buffer, 0, readCount)
                            }
                        }
                        fos.flush()
                        WJLog.i("循环结束")
                    }
                    WJLog.i("录制完毕")
                }
            }
        }

    }

    private fun stopRecordPCM() {
        mRecording = false
        stopRecordingChronometer()
        mPCMFileName?.let {
            WJLog.d("录制完毕:$mPCMFileName")
            val file = File(mPCMFileName)
            WJLog.d("文件存在：${file.exists()} 文件是否隐藏：${file.isHidden}")
        }
    }

    private var mRecordWaveJob: Job? = null
    private var mWaveFileName: String? = null
    private fun startRecordWav() {
        checkRecordPermission {
            initAudioRecord { audioRecord, bufferSize ->
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA)
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
        mPlaying = false
    }

    @RequiresPermission(value = "android.permission.RECORD_AUDIO")
    private fun initAudioRecord(block: (audioRecord: AudioRecord, bufferSize: Int) -> Unit) {
        val bufferSize = AudioRecord.getMinBufferSize(mSimpleRate, mChannelConfig, mAudioFormat)

        WJLog.d("--initAudioRecord--channelConfig:$mChannelConfig audioFormat:$mAudioFormat bufferSize:$bufferSize")
        if (null != mAudioRecord) {
            block.invoke(mAudioRecord!!, bufferSize)
            return
        }
        mAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            mSimpleRate,
            mChannelConfig,
            mAudioFormat,
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