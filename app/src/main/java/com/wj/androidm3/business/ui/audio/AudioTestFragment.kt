package com.wj.androidm3.business.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.AudioManager
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentAudioTestBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioTestFragment : BaseMVVMFragment<AudioTestViewModel, FragmentAudioTestBinding>() {

    private val sampleRates = arrayOf("48000", "44100", "22050", "16000", "8000")
    private val sampleFormats = arrayOf("16-bit", "24-bit", "8-bit")
    private val channelConfigs = arrayOf("Mono", "Stereo")

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private lateinit var pcmFilesDir: File
    private lateinit var aacFilesDir: File
    private var audioTrack: AudioTrack? = null
    private var isPlaying : Boolean = false
        set(value) {
            field = value
            mViewModel.isPlaying.postValue(value)
        }
    private var aacPlayer: MediaPlayer? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(requireContext(), "Permission denied to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getLayoutId(): Int = R.layout.fragment_audio_test

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewBinding?.viewModel = mViewModel
        mViewBinding?.lifecycleOwner = this

        pcmFilesDir = File(requireContext().getExternalFilesDir(null), "pcm_records")
        aacFilesDir = File((requireActivity().getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.path ?: pcmFilesDir.path))
        WJLog.d("PCM DIR: ${pcmFilesDir.absolutePath}  AAC DIR: ${aacFilesDir.absolutePath}")
        if (!pcmFilesDir.exists()) {
            pcmFilesDir.mkdirs()
        }
        mViewBinding?.playButton?.setOnClickListener { handlePlayClick(adapter) }
        setupSpinners()
        setupRecyclerView()

        mViewModel.isRecording.observe(viewLifecycleOwner) { recording ->
            if (recording) {
                checkPermissionAndStartRecording()
            } else {
                stopRecording()
            }
        }

        mViewModel.loadPcmFiles(pcmFilesDir, aacFilesDir)

        mViewBinding?.encodeAacButton?.setOnClickListener { encodeAAc() }
    }

    private fun setupSpinners() {
        mViewBinding?.sampleRateSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sampleRates)
        mViewBinding?.sampleFormatSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sampleFormats)
        mViewBinding?.channelConfigSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, channelConfigs)
    }

    var adapter: PcmFileListAdapter? = null
    private fun setupRecyclerView() {
        adapter = PcmFileListAdapter(
            emptyList(),
            onSelect = { index, file ->
                mViewModel.selectedIndex.postValue(index)
                mViewModel.selectedFilePath.postValue(file.absolutePath)
//                    adapter?.setSelectedPosition(index)
            }
        )
        mViewBinding?.pcmFileList?.adapter = adapter
        mViewModel.pcmFiles.observe(viewLifecycleOwner) { files ->
            adapter?.updateFiles(files)
        }
    }

    private fun checkPermissionAndStartRecording() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            val sampleRate = mViewBinding?.sampleRateSpinner?.selectedItem.toString().toInt()
            val audioFormat =
                if (mViewBinding?.sampleFormatSpinner?.selectedItem.toString() == "16-bit") AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_8BIT
            val channelConfig =
                if (mViewBinding?.channelConfigSpinner?.selectedItem.toString() == "Stereo") AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO

            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Toast.makeText(requireContext(), "Invalid audio parameters", Toast.LENGTH_SHORT).show()
                mViewModel.isRecording.postValue(false)
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize
            )

            val pcmFile = createPcmFile()
            isRecording = true
            audioRecord?.startRecording()

            Thread {
                val data = ByteArray(bufferSize)
                val fos = FileOutputStream(pcmFile)
                while (isRecording) {
                    val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                    if (read > 0) {
                        fos.write(data, 0, read)
                    }
                }
                fos.close()
                mViewModel.loadPcmFiles(pcmFilesDir, aacFilesDir)
            }.start()
        }

    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun createPcmFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sampleRate = mViewBinding?.sampleRateSpinner?.selectedItem
        val format = mViewBinding?.sampleFormatSpinner?.selectedItem
        val channels = mViewBinding?.channelConfigSpinner?.selectedItem
        return File(pcmFilesDir, "PCM_${sampleRate}_${format}_${channels}_${timeStamp}.pcm")
    }

    override fun firstCreateView() {

    }

    private fun encodeAAc() {
        val path = mViewModel.selectedFilePath.value
        if (path.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "未选中PCM文件", Toast.LENGTH_SHORT).show()
            return
        }
        val sr = mViewBinding?.sampleRateSpinner?.selectedItem?.toString()?.toIntOrNull() ?: 0
        val fmtStr = mViewBinding?.sampleFormatSpinner?.selectedItem?.toString() ?: ""
        val chStr = mViewBinding?.channelConfigSpinner?.selectedItem?.toString() ?: ""
        if (sr <= 0) {
            Toast.makeText(requireContext(), "采样率不合法", Toast.LENGTH_SHORT).show()
            return
        }
        val channelCount = if (chStr == "Stereo") 2 else 1
        val sampleFormat = when (fmtStr) {
            "16-bit" -> AudioFormat.ENCODING_PCM_16BIT
            "24-bit" -> AudioFormat.ENCODING_PCM_24BIT_PACKED
            else -> AudioFormat.ENCODING_PCM_8BIT
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())


//        pcmFilesDir = File(requireContext().getExternalFilesDir(null), "pcm_records")
        val aacOut = aacFilesDir.absolutePath + "/AAC_${sr}_${fmtStr}_${chStr}_${timeStamp}.aac"
        try {
            com.wj.nativelib.WJMediaJNIHepler().pcm2aac(path, aacOut, sr, channelCount, sampleFormat)
            Toast.makeText(requireContext(), "编码完成: $aacOut", Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            Toast.makeText(requireContext(), "编码失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePlayClick(adapter: PcmFileListAdapter?) {
        adapter?.let {
            val path = mViewModel.selectedFilePath.value
            val index = mViewModel.selectedIndex.value
            if (path.isNullOrEmpty() || index == null) {
                Toast.makeText(requireContext(), "未选中P文件", Toast.LENGTH_SHORT).show()
                return
            }
            if (isPlaying) {
                stopPlayback()
                isPlaying = false
                adapter.setPlaying(false)
                return
            }

            if(path.endsWith("aac")){
                playAac(File(path))
                return
            }
            val sr = mViewBinding?.sampleRateSpinner?.selectedItem?.toString()?.toIntOrNull() ?: 0
            val fmtStr = mViewBinding?.sampleFormatSpinner?.selectedItem?.toString() ?: ""
            val chStr = mViewBinding?.channelConfigSpinner?.selectedItem?.toString() ?: ""
            val encoding = when (fmtStr) {
                "16-bit" -> AudioFormat.ENCODING_PCM_16BIT
                "24-bit" -> AudioFormat.ENCODING_PCM_24BIT_PACKED
                else -> AudioFormat.ENCODING_PCM_8BIT
            }
            val channelOut = if (chStr == "Stereo") AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
            val minBuf = AudioTrack.getMinBufferSize(sr, channelOut, encoding)
            if (sr <= 0 || minBuf == AudioTrack.ERROR_BAD_VALUE) {
                Toast.makeText(requireContext(), "播放参数不合法", Toast.LENGTH_SHORT).show()
                return
            }
            try {
                audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, sr, channelOut, encoding, minBuf, AudioTrack.MODE_STREAM)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "初始化播放器失败", Toast.LENGTH_SHORT).show()
                return
            }
            isPlaying = true
            adapter.setPlaying(true)
            audioTrack?.play()
            requireActivity().runOnUiThread {
                mViewBinding?.playProgress?.progress = 0
                mViewBinding?.playProgressText?.text = "0%"
            }
            Thread {
                try {
                    val file = File(path)
                    val data = ByteArray(minBuf)
                    val input = file.inputStream()
                    val total = file.length()
                    var sum: Long = 0
                    var read: Int
                    while (isPlaying) {
                        read = input.read(data)
                        if (read <= 0) break
                        audioTrack?.write(data, 0, read)
                        sum += read
                        val percent = if (total > 0) ((sum * 100) / total).toInt() else 0
                        requireActivity().runOnUiThread {
                            mViewBinding?.playProgress?.progress = percent
                            mViewBinding?.playProgressText?.text = "$percent%"
                        }
                    }
                    input.close()
                } catch (t: Throwable) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "播放出错", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    requireActivity().runOnUiThread {
                        stopPlayback()
                        isPlaying = false
                        adapter.setPlaying(false)
                        Toast.makeText(requireContext(), "播放完成", Toast.LENGTH_SHORT).show()
                        mViewBinding?.playProgress?.progress = 0
                        mViewBinding?.playProgressText?.text = "0%"
                    }
                }
            }.start()
        }

    }

    fun playAac(aacFile: File) {
        if (!aacFile.exists()) {
            Toast.makeText(requireContext(), "文件不存在", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            aacPlayer?.release()
            aacPlayer = MediaPlayer()
            aacPlayer?.setDataSource(aacFile.absolutePath)
            aacPlayer?.setOnPreparedListener {
                it.start()
            }
            aacPlayer?.setOnCompletionListener {
                it.release()
                aacPlayer = null
                Toast.makeText(requireContext(), "播放完成", Toast.LENGTH_SHORT).show()
            }
            aacPlayer?.setOnErrorListener { _, _, _ ->
                Toast.makeText(requireContext(), "播放失败", Toast.LENGTH_SHORT).show()
                aacPlayer?.release()
                aacPlayer = null
                true
            }
            aacPlayer?.prepareAsync()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "播放失败", Toast.LENGTH_SHORT).show()
            aacPlayer?.release()
            aacPlayer = null
        }
    }

    private fun stopPlayback() {
        try {
            audioTrack?.stop()
        } catch (_: Exception) {
        }
        audioTrack?.release()
        audioTrack = null
        mViewBinding?.playProgress?.progress = 0
        mViewBinding?.playProgressText?.text = "0%"
    }

    override fun onDestroyView() {
        try {
            aacPlayer?.stop()
        } catch (_: Exception) {}
        aacPlayer?.release()
        aacPlayer = null
        stopPlayback()
        stopRecording()
        super.onDestroyView()
    }
}
