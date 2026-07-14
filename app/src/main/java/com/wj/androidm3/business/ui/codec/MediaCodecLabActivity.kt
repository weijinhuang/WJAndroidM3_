package com.wj.androidm3.business.ui.codec

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.wj.androidm3.R
import com.wj.androidm3.business.codec.MediaCodecAudioLoopback
import com.wj.androidm3.business.codec.MediaCodecVideoLoopback
import com.wj.androidm3.databinding.ActivityMediaCodecLabBinding
import com.wj.basecomponent.ui.BaseMVVMActivity
import com.wj.basecomponent.vm.BaseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * MediaCodec 编解码教学页面。
 *
 * 和 LanVideoCallActivity 的 WebRTC 路线不同，这个页面故意把编解码拆出来给你看：
 * - 左侧 CameraX PreviewView 是“未编码的本地预览”。
 * - 右侧 SurfaceView 是“CameraX YUV -> H.264 编码 -> 模拟传输 -> H.264 解码”后的画面。
 * - Audio 按钮启动“麦克风 PCM -> AAC 编码 -> 模拟传输 -> AAC 解码 -> 扬声器播放”。
 *
 * 注意：这里没有做真正局域网传输，使用内存队列/直接投递模拟网络已送达。
 * 等你理解编解码以后，可以把 feedDecoder 的位置替换为 UDP/RTP/TCP 收发。
 */
class MediaCodecLabActivity : BaseMVVMActivity<BaseViewModel, ActivityMediaCodecLabBinding>() {
    private val binding: ActivityMediaCodecLabBinding
        get() = requireNotNull(mViewBinding)
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null
    private var videoLoopback: MediaCodecVideoLoopback? = null
    private var audioLoopback: MediaCodecAudioLoopback? = null
    private var videoRunning = false
    private var audioRunning = false
    private val logLines = ArrayDeque<String>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updateButtons()
        appendLog("permission result: camera=${hasPermission(Manifest.permission.CAMERA)}, audio=${hasPermission(Manifest.permission.RECORD_AUDIO)}")
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_media_codec_lab
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.startVideoButton.setOnClickListener { startVideoCodecLab() }
        binding.stopVideoButton.setOnClickListener { stopVideoCodecLab() }
        binding.startAudioButton.setOnClickListener { startAudioCodecLab() }
        binding.stopAudioButton.setOnClickListener { stopAudioCodecLab() }
        binding.decodedSurfaceView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                appendLog("decoded surface created")
            }

            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) = Unit

            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                stopVideoCodecLab()
            }
        })

        if (!hasRequiredPermissions()) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
        updateButtons()
        setStatus("Ready. Start Video to see H.264 encode/decode loop.")
    }

    override fun onDestroy() {
        stopVideoCodecLab()
        stopAudioCodecLab()
        super.onDestroy()
    }

    private fun startVideoCodecLab() {
        if (!hasPermission(Manifest.permission.CAMERA)) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
            return
        }
        if (videoRunning) {
            return
        }
        val surface: Surface = binding.decodedSurfaceView.holder.surface
        if (!surface.isValid) {
            setStatus("Decoded Surface is not ready")
            return
        }
        videoRunning = true
        updateButtons()
        setStatus("Starting CameraX + MediaCodec H.264 loop...")

        cameraExecutor = Executors.newSingleThreadExecutor()
        videoLoopback = MediaCodecVideoLoopback(surface) { message ->
            runOnUiThread {
                appendLog(message)
            }
        }.apply {
            start(MediaCodecVideoLoopback.VIDEO_WIDTH, MediaCodecVideoLoopback.VIDEO_HEIGHT)
        }

        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            bindCameraForCodec(provider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraForCodec(provider: ProcessCameraProvider) {
        val analyzerExecutor = cameraExecutor ?: return
        val preview = Preview.Builder()
            .setTargetResolution(Size(MediaCodecVideoLoopback.VIDEO_WIDTH, MediaCodecVideoLoopback.VIDEO_HEIGHT))
            .build()
            .apply {
                setSurfaceProvider(binding.rawPreviewView.surfaceProvider)
            }

        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(MediaCodecVideoLoopback.VIDEO_WIDTH, MediaCodecVideoLoopback.VIDEO_HEIGHT))
            .setTargetRotation(Surface.ROTATION_0)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                // ImageAnalysis 的 analyzer 就是“采集帧进入编码器”的入口。
                // 每来一帧 ImageProxy，就复制 YUV 并送入 H.264 encoder。
                setAnalyzer(analyzerExecutor) { image ->
                    videoLoopback?.queueCameraFrame(image) ?: image.close()
                }
            }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis
            )
            appendLog("CameraX bound: ${MediaCodecVideoLoopback.VIDEO_WIDTH}x${MediaCodecVideoLoopback.VIDEO_HEIGHT}")
        } catch (t: Throwable) {
            appendLog("Bind front camera failed: ${t.message ?: t.javaClass.simpleName}")
            provider.unbindAll()
            provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
            appendLog("Fallback to back camera")
        }
    }

    private fun stopVideoCodecLab() {
        if (!videoRunning && videoLoopback == null && cameraProvider == null) {
            return
        }
        videoRunning = false
        cameraProvider?.unbindAll()
        cameraProvider = null
        videoLoopback?.stop()
        videoLoopback = null
        cameraExecutor?.shutdownNow()
        cameraExecutor = null
        updateButtons()
        setStatus(if (audioRunning) "Audio loop is running" else "Idle")
    }

    private fun startAudioCodecLab() {
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
            return
        }
        if (audioRunning) {
            return
        }
        audioRunning = true
        updateButtons()
        setStatus("Starting AudioRecord + AAC encode/decode loop. Watch out for echo.")
        audioLoopback = MediaCodecAudioLoopback(this) { message ->
            runOnUiThread {
                appendLog(message)
            }
        }.apply {
            start()
        }
    }

    private fun stopAudioCodecLab() {
        if (!audioRunning && audioLoopback == null) {
            return
        }
        audioRunning = false
        audioLoopback?.stop()
        audioLoopback = null
        updateButtons()
        setStatus(if (videoRunning) "Video loop is running" else "Idle")
    }

    private fun setStatus(message: String) {
        binding.statusText.text = message
    }

    private fun appendLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date())
        logLines.addLast("$time  $message")
        while (logLines.size > MAX_LOG_LINES) {
            logLines.removeFirst()
        }
        binding.logText.text = logLines.joinToString("\n")
    }

    private fun updateButtons() {
        binding.startVideoButton.isEnabled = hasPermission(Manifest.permission.CAMERA) && !videoRunning
        binding.stopVideoButton.isEnabled = videoRunning
        binding.startAudioButton.isEnabled = hasPermission(Manifest.permission.RECORD_AUDIO) && !audioRunning
        binding.stopAudioButton.isEnabled = audioRunning
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all(::hasPermission)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val MAX_LOG_LINES = 5
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}
