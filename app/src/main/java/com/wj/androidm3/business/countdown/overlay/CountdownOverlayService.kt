package com.wj.androidm3.business.countdown.overlay

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wj.androidm3.R
import com.wj.androidm3.business.countdown.data.CountdownEntity
import com.wj.androidm3.business.countdown.data.CountdownPreferences
import com.wj.androidm3.business.countdown.data.CountdownRepository
import com.wj.androidm3.business.countdown.data.CountdownTime
import com.wj.androidm3.business.countdown.ui.AddCountdownActivity
import com.wj.androidm3.business.countdown.ui.CountdownListActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CountdownOverlayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: CountdownRepository
    private lateinit var preferences: CountdownPreferences
    private var overlayController: CountdownOverlayController? = null
    private var tickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = CountdownRepository.getInstance(this)
        preferences = CountdownPreferences(this)
        createNotificationChannels()
        startForeground(ONGOING_NOTIFICATION_ID, buildOngoingNotification(null, System.currentTimeMillis()))
        createOverlayIfPermitted()
        startTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            preferences.assistantEnabled = false
            stopSelf()
            return START_NOT_STICKY
        }
        if (overlayController == null) createOverlayIfPermitted()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickerJob?.cancel()
        overlayController?.remove()
        overlayController = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createOverlayIfPermitted() {
        if (!Settings.canDrawOverlays(this)) return
        val controller = CountdownOverlayController(
            context = this,
            preferences = preferences,
            onAddCountdown = {
                startActivity(
                    Intent(this, AddCountdownActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            onOpenList = {
                startActivity(
                    Intent(this, CountdownListActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            onPermissionLost = { overlayController = null }
        )
        overlayController = controller
        controller.show()
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val result = withContext(Dispatchers.IO) {
                    val preEndAlerts = repository.collectPreEndAlerts(now)
                    val completed = repository.reconcileExpired(now)
                    val tasks = repository.getAll()
                    TickResult(
                        preEndAlerts = preEndAlerts,
                        completed = completed,
                        nearest = CountdownTime.nearestRunning(tasks, now)
                    )
                }
                result.preEndAlerts.forEach { showPreEndNotification(it, now) }
                result.completed.forEach(::showCompletionNotification)
                overlayController?.update(result.nearest, now)
                updateOngoingNotification(result.nearest.firstOrNull(), now)
                delay((1_000L - System.currentTimeMillis() % 1_000L).coerceAtLeast(100L))
            }
        }
    }

    private fun buildOngoingNotification(shortest: CountdownEntity?, nowEpochMs: Long) =
        NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("悬浮倒计时助手")
            .setContentText(
                shortest?.let { "${it.displayName}  ${CountdownTime.format(it, nowEpochMs)}" }
                    ?: "助手已开启"
            )
            .setContentIntent(listPendingIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun updateOngoingNotification(shortest: CountdownEntity?, nowEpochMs: Long) {
        try {
            NotificationManagerCompat.from(this).notify(
                ONGOING_NOTIFICATION_ID,
                buildOngoingNotification(shortest, nowEpochMs)
            )
        } catch (_: SecurityException) {
            // Android 13+ may hide notifications when the user denies POST_NOTIFICATIONS.
        }
    }

    private fun showCompletionNotification(task: CountdownEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(this, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("${task.displayName} 已结束")
            .setContentText("点击查看全部倒计时")
            .setContentIntent(listPendingIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0L, 150L, 100L, 150L))
            .build()
        NotificationManagerCompat.from(this).notify(completionNotificationId(task.id), notification)
    }

    private fun showPreEndNotification(task: CountdownEntity, nowEpochMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val remaining = CountdownTime.format(task, nowEpochMs)
        val notification = NotificationCompat.Builder(this, PRE_END_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("${task.displayName} 即将结束")
            .setContentText("剩余 $remaining，点击查看全部倒计时")
            .setContentIntent(listPendingIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setVibrate(longArrayOf(0L, 250L, 120L, 250L))
            .build()
        NotificationManagerCompat.from(this).notify(preEndNotificationId(task.id), notification)
    }

    private fun listPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, CountdownListActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                ONGOING_CHANNEL_ID,
                "倒计时助手运行状态",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮倒计时助手的常驻运行通知"
                setSound(null, null)
                enableVibration(false)
            }
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                PRE_END_CHANNEL_ID,
                "倒计时即将结束提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "倒计时结束前 50 秒的弹出提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 250L, 120L, 250L)
            }
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                COMPLETION_CHANNEL_ID,
                "倒计时完成提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "倒计时结束提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 150L, 100L, 150L)
            }
        )
    }

    companion object {
        private const val ACTION_START = "com.wj.androidm3.countdown.START"
        private const val ACTION_STOP = "com.wj.androidm3.countdown.STOP"
        private const val ONGOING_CHANNEL_ID = "countdown_assistant_ongoing"
        private const val PRE_END_CHANNEL_ID = "countdown_assistant_pre_end_50s"
        private const val COMPLETION_CHANNEL_ID = "countdown_assistant_completion"
        private const val ONGOING_NOTIFICATION_ID = 48_001

        fun start(context: Context) {
            val intent = Intent(context, CountdownOverlayService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CountdownOverlayService::class.java))
        }

        private fun completionNotificationId(taskId: Long): Int =
            49_000 + (taskId % 10_000).toInt()

        private fun preEndNotificationId(taskId: Long): Int =
            59_000 + (taskId % 10_000).toInt()
    }

    private data class TickResult(
        val preEndAlerts: List<CountdownEntity>,
        val completed: List<CountdownEntity>,
        val nearest: List<CountdownEntity>
    )
}
