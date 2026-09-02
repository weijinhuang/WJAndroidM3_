package com.wj.androidm3.business.ui.videoeditor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.wj.androidm3.R
import com.wj.androidm3.databinding.ActivityVideoEditorBinding
import com.wj.nativelib.FFmpegVideoFrameExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * ExoPlayer 负责预览（硬件解码、Surface 渲染和精确 seek），FFmpeg 只负责截图帧解码。
 * 两者职责分离的原因是：预览追求低延迟/低功耗，截图则必须绕过屏幕尺寸，直接得到
 * 源视频编码分辨率的像素。最终由 FFmpeg PNG 编码器无损输出，绝不截取 PlayerView。
 */
class VideoEditorActivity : AppCompatActivity(), VideoTimelineView.Listener {

    private lateinit var binding: ActivityVideoEditorBinding
    private lateinit var player: ExoPlayer
    private lateinit var sourceUri: Uri
    private var localSourceFile: File? = null
    private var metadataRetriever: MediaMetadataRetriever? = null
    private var thumbnailJob: Job? = null
    private var currentTimelinePositionMs = 0L
    private var timelineConfigured = false
    private var wasPlayingBeforeScrub = false
    private var lastPreviewSeekAt = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val playbackTicker = object : Runnable {
        override fun run() {
            if (::player.isInitialized && player.isPlaying) {
                currentTimelinePositionMs = player.currentPosition.coerceAtLeast(0L)
                binding.timelineView.setPlaybackPosition(currentTimelinePositionMs)
            }
            mainHandler.postDelayed(this, 33L)
        }
    }

    private val thumbnailCache = object : android.util.LruCache<Long, android.graphics.Bitmap>(12 * 1024) {
        override fun sizeOf(key: Long, value: android.graphics.Bitmap): Int = value.byteCount / 1024
    }

    private var pendingCaptureTimeMs: Long? = null
    private val legacyStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val time = pendingCaptureTimeMs
        pendingCaptureTimeMs = null
        if (granted && time != null) captureOriginalFrame(time)
        else toast(R.string.video_editor_storage_denied)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriText = intent.getStringExtra(EXTRA_VIDEO_URI)
        if (uriText.isNullOrBlank()) {
            toast(R.string.video_editor_invalid_video)
            finish()
            return
        }
        sourceUri = Uri.parse(uriText)
        binding.timelineView.listener = this
        binding.backButton.setOnClickListener { finish() }
        binding.captureButton.setOnClickListener { requestCapture() }
        binding.captureButton.isEnabled = false

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            // EXACT 会从上一个关键帧解码至目标时间，不把 timeline 的帧吸附退化成关键帧 seek。
            exoPlayer.setSeekParameters(SeekParameters.EXACT)
            exoPlayer.setMediaItem(MediaItem.fromUri(sourceUri))
            exoPlayer.playWhenReady = false
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && !timelineConfigured) {
                        val duration = exoPlayer.duration.takeIf { it > 0L } ?: return
                        configureTimeline(duration, DEFAULT_FPS)
                    }
                }
            })
            exoPlayer.prepare()
        }
        mainHandler.post(playbackTicker)
        prepareRandomAccessSource()
    }

    /**
     * FFmpeg 的 avformat_open_input 读取普通文件最可靠。把 content:// 一次性复制到 cache
     * 还能避免相册 provider 在高频随机 seek 时反复跨进程读取；Activity 销毁时立即删除。
     */
    private fun prepareRandomAccessSource() {
        setLoading(true)
        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val input = contentResolver.openInputStream(sourceUri)
                        ?: error("Unable to open selected video")
                    val file = File(cacheDir, "video_editor_${UUID.randomUUID()}.source")
                    input.use { source ->
                        file.outputStream().buffered().use { output -> source.copyTo(output) }
                    }
                    val info = FFmpegVideoFrameExtractor.probeVideo(file.absolutePath)
                    require(info.size >= 5 && info[0] > 0L && info[1] > 0L) { "FFmpeg cannot probe video" }
                    val retriever = MediaMetadataRetriever().apply { setDataSource(file.absolutePath) }
                    PreparedSource(file, retriever, info)
                }
            }
            result.onSuccess { prepared ->
                localSourceFile = prepared.file
                metadataRetriever = prepared.retriever
                val fps = if (prepared.info[3] > 0L && prepared.info[4] > 0L) {
                    prepared.info[3].toDouble() / prepared.info[4].toDouble()
                } else DEFAULT_FPS
                val duration = prepared.info[2].takeIf { it > 0L }
                    ?: player.duration.takeIf { it > 0L }
                    ?: 0L
                configureTimeline(duration, fps)
                binding.captureButton.isEnabled = true
            }.onFailure {
                toast(R.string.video_editor_invalid_video)
            }
            setLoading(false)
        }
    }

    private fun configureTimeline(durationMs: Long, fps: Double) {
        if (durationMs <= 0L) return
        timelineConfigured = true
        binding.timelineView.configure(durationMs, fps)
    }

    override fun onScrubStart() {
        wasPlayingBeforeScrub = player.isPlaying
        player.pause()
    }

    override fun onPositionChanged(positionMs: Long, fromUser: Boolean) {
        currentTimelinePositionMs = positionMs
        if (!fromUser) return
        // 手指移动事件可能远高于屏幕刷新率，限制预览 seek 频率但不降低最终帧精度。
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastPreviewSeekAt >= 32L) {
            player.seekTo(positionMs)
            lastPreviewSeekAt = now
        }
    }

    override fun onScrubStop(positionMs: Long) {
        currentTimelinePositionMs = positionMs
        player.seekTo(positionMs)
        if (wasPlayingBeforeScrub) player.play()
    }

    override fun onThumbnailWindowRequested(timesMs: List<Long>, widthPx: Int, heightPx: Int) {
        val retriever = metadataRetriever ?: return
        thumbnailJob?.cancel()
        thumbnailJob = lifecycleScope.launch {
            for (timeMs in timesMs) {
                coroutineContext.ensureActive()
                val cached = thumbnailCache.get(timeMs)
                if (cached != null) {
                    binding.timelineView.submitThumbnail(timeMs, cached)
                    continue
                }
                val bitmap = withContext(Dispatchers.IO) {
                    // MediaMetadataRetriever 非线程安全；被取消的旧请求可能仍在 native 调用中。
                    synchronized(retriever) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            retriever.getScaledFrameAtTime(
                                timeMs * 1_000L,
                                // CLOSEST 返回该时间附近的真实帧；SYNC 只会重复关键帧，
                                // 在逐帧缩放时无法形成可信的密集预览。
                                MediaMetadataRetriever.OPTION_CLOSEST,
                                widthPx,
                                heightPx
                            )
                        } else {
                            retriever.getFrameAtTime(timeMs * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST)
                                ?.let { source ->
                                    android.graphics.Bitmap.createScaledBitmap(source, widthPx, heightPx, true)
                                        .also { if (it !== source) source.recycle() }
                                }
                        }
                    }
                }
                if (bitmap != null) {
                    thumbnailCache.put(timeMs, bitmap)
                    binding.timelineView.submitThumbnail(timeMs, bitmap)
                }
            }
        }
    }

    private fun requestCapture() {
        val source = localSourceFile
        if (source == null) {
            toast(R.string.video_editor_invalid_video)
            return
        }
        val time = currentTimelinePositionMs.coerceAtLeast(0L)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingCaptureTimeMs = time
            legacyStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            captureOriginalFrame(time)
        }
    }

    private fun captureOriginalFrame(timeMs: Long) {
        val source = localSourceFile ?: return
        binding.captureButton.isEnabled = false
        setLoading(true)
        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val output = File(cacheDir, "frame_${System.currentTimeMillis()}.png")
                    try {
                        val code = FFmpegVideoFrameExtractor.extractFrameToPng(
                            source.absolutePath,
                            output.absolutePath,
                            timeMs * 1_000L
                        )
                        check(code == 0 && output.length() > 0L) { "FFmpeg frame export failed: $code" }
                        VideoFrameMediaStore.savePng(this@VideoEditorActivity, output)
                    } finally {
                        output.delete()
                    }
                }
            }
            setLoading(false)
            binding.captureButton.isEnabled = localSourceFile != null
            if (result.isSuccess) toast(R.string.video_editor_capture_success)
            else toast("${getString(R.string.video_editor_capture_failed)}: ${result.exceptionOrNull()?.message.orEmpty()}")
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        mainHandler.removeCallbacks(playbackTicker)
        thumbnailJob?.cancel()
        binding.timelineView.listener = null
        binding.playerView.player = null
        if (::player.isInitialized) player.release()
        synchronized(metadataRetriever ?: this) {
            metadataRetriever?.release()
            metadataRetriever = null
        }
        thumbnailCache.evictAll()
        localSourceFile?.delete()
        localSourceFile = null
        super.onDestroy()
    }

    private data class PreparedSource(
        val file: File,
        val retriever: MediaMetadataRetriever,
        val info: LongArray
    )

    companion object {
        private const val EXTRA_VIDEO_URI = "video_uri"
        private const val DEFAULT_FPS = 30.0

        fun createIntent(context: Context, uri: Uri): Intent =
            Intent(context, VideoEditorActivity::class.java)
                .putExtra(EXTRA_VIDEO_URI, uri.toString())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
