package com.wj.androidm3.business.countdown.screenshot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ResultReceiver
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wj.androidm3.R
import com.wj.androidm3.business.countdown.data.CountdownScreenshotStore
import com.wj.androidm3.business.countdown.data.CountdownRepository
import com.wj.androidm3.business.countdown.ui.CountdownListActivity
import kotlinx.coroutines.runBlocking
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class CountdownScreenshotService : Service() {
    private val mainHandler = Handler(android.os.Looper.getMainLooper())
    private val completed = AtomicBoolean(false)
    private var receiver: ResultReceiver? = null
    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var outputPath: String? = null
    private var countdownSeconds: Int = 0
    private var countdownName: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        receiver = intent.parcelableExtra(EXTRA_RECEIVER)
        countdownSeconds = intent.getIntExtra(EXTRA_COUNTDOWN_SECONDS, 0)
        countdownName = intent.getStringExtra(EXTRA_COUNTDOWN_NAME)
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val resultData: Intent? = intent.parcelableExtra(EXTRA_RESULT_DATA)
        if (resultCode == Int.MIN_VALUE || resultData == null || receiver == null || countdownSeconds <= 0) {
            completeFailure("缺少系统截图授权信息")
            return START_NOT_STICKY
        }

        startCaptureForeground()
        mainHandler.postDelayed({ beginCapture(resultCode, resultData) }, CAPTURE_SETTLE_DELAY_MS)
        mainHandler.postDelayed({ completeFailure("系统截图超时") }, CAPTURE_TIMEOUT_MS)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (!completed.get()) completeFailure("系统截图服务已停止")
        releaseResources(stopProjection = true)
        super.onDestroy()
    }

    private fun beginCapture(resultCode: Int, resultData: Intent) {
        if (completed.get()) return
        try {
            val projectionManager = getSystemService(MediaProjectionManager::class.java)
            val projection = projectionManager.getMediaProjection(resultCode, resultData)
                ?: run {
                    completeFailure("无法取得系统截图授权")
                    return
                }
            mediaProjection = projection

            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    if (!completed.get()) completeFailure("系统终止了截图")
                }
            }
            projectionCallback = callback
            projection.registerCallback(callback, mainHandler)

            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val densityDpi = metrics.densityDpi

            val thread = HandlerThread("countdown-screenshot").also { it.start() }
            captureThread = thread
            val captureHandler = Handler(thread.looper)
            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            reader.setOnImageAvailableListener({ source ->
                val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val file = CountdownScreenshotStore(this).createOutputFile()
                    outputPath = file.absolutePath
                    val plane = image.planes.first()
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val paddedWidth = width + rowPadding / pixelStride
                    val paddedBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                    paddedBitmap.copyPixelsFromBuffer(plane.buffer)
                    val cropSizePx = (SCREENSHOT_SIZE_DP * metrics.density)
                        .roundToInt()
                        .coerceAtLeast(1)
                    val screenshot = Bitmap.createBitmap(
                        paddedBitmap,
                        0,
                        0,
                        cropSizePx.coerceAtMost(width),
                        cropSizePx.coerceAtMost(height)
                    )
                    FileOutputStream(file).use { output ->
                        check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
                    }
                    if (screenshot !== paddedBitmap) screenshot.recycle()
                    paddedBitmap.recycle()
                    runBlocking {
                        CountdownRepository.getInstance(this@CountdownScreenshotService)
                            .createAndStart(countdownSeconds, countdownName, file.absolutePath)
                    }
                    completeSuccess(file.absolutePath)
                } catch (error: Throwable) {
                    completeFailure(error.message ?: "保存系统截图失败")
                } finally {
                    image.close()
                }
            }, captureHandler)

            virtualDisplay = projection.createVirtualDisplay(
                "CountdownScreenshot",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                captureHandler
            )
        } catch (error: Throwable) {
            completeFailure(error.message ?: "系统截图失败")
        }
    }

    private fun completeSuccess(path: String) {
        if (!completed.compareAndSet(false, true)) return
        mainHandler.removeCallbacksAndMessages(null)
        receiver?.send(RESULT_SUCCESS, Bundle().apply { putString(KEY_SCREENSHOT_PATH, path) })
        releaseResources(stopProjection = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun completeFailure(message: String) {
        if (!completed.compareAndSet(false, true)) return
        mainHandler.removeCallbacksAndMessages(null)
        outputPath?.let { CountdownScreenshotStore(this).deleteSafely(it) }
        receiver?.send(RESULT_FAILURE, Bundle().apply { putString(KEY_ERROR, message) })
        releaseResources(stopProjection = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseResources(stopProjection: Boolean) {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        captureThread?.quitSafely()
        captureThread = null
        val projection = mediaProjection
        val callback = projectionCallback
        mediaProjection = null
        projectionCallback = null
        if (projection != null && callback != null) {
            runCatching { projection.unregisterCallback(callback) }
        }
        if (stopProjection && projection != null) {
            runCatching { projection.stop() }
        }
    }

    private fun startCaptureForeground() {
        val openList = PendingIntent.getActivity(
            this,
            0,
            Intent(this, CountdownListActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("正在保存倒计时截图")
            .setContentText("截图完成后会自动结束")
            .setContentIntent(openList)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "倒计时截图",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "创建倒计时时临时截取当前屏幕"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    @Suppress("DEPRECATION")
    private inline fun <reified T> Intent.parcelableExtra(key: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, T::class.java)
        } else {
            getParcelableExtra(key)
        }

    companion object {
        const val RESULT_SUCCESS = 1
        const val RESULT_FAILURE = 2
        const val KEY_SCREENSHOT_PATH = "screenshot_path"
        const val KEY_ERROR = "error"

        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"
        private const val EXTRA_RECEIVER = "receiver"
        private const val EXTRA_COUNTDOWN_SECONDS = "countdown_seconds"
        private const val EXTRA_COUNTDOWN_NAME = "countdown_name"
        private const val CHANNEL_ID = "countdown_screenshot_capture"
        private const val NOTIFICATION_ID = 48_101
        private const val CAPTURE_SETTLE_DELAY_MS = 350L
        private const val CAPTURE_TIMEOUT_MS = 5_000L
        private const val SCREENSHOT_SIZE_DP = 150

        fun capture(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            receiver: ResultReceiver,
            countdownSeconds: Int,
            countdownName: String?
        ) {
            val serviceIntent = Intent(context, CountdownScreenshotService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, resultData)
                .putExtra(EXTRA_RECEIVER, receiver)
                .putExtra(EXTRA_COUNTDOWN_SECONDS, countdownSeconds)
                .putExtra(EXTRA_COUNTDOWN_NAME, countdownName)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
