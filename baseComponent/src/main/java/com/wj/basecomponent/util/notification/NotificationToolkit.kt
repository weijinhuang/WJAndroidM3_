package com.wj.basecomponent.util.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wj.basecomponent.util.log.WJLog

fun sendNotification(
    context: Context,
    destinationClass: Class<*>,
    channelId: String,
    notificationId: Int,
    iconId: Int,
    title: String,
    content: String
) {

    WJLog.i()
    val fullScreenIntent = Intent(context, destinationClass)
    val fullScreenPendingIntent = PendingIntent.getActivity(context, 0, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE)
    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(iconId)
        .setContentText(content)
        .setContentTitle(title)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setFullScreenIntent(fullScreenPendingIntent, true)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setAutoCancel(true)
        .setOngoing(true)
        .setShowWhen(true)
    val notification = notificationBuilder.build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "ChannelName"
        val descriptionText = "channel_description"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance).apply {
            WJLog.i()
            description = descriptionText
        }
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    with(NotificationManagerCompat.from(context)) {
        WJLog.i()
        notify(notificationId, notification)

        WJLog.d("send notification -> $title : $content")
    }

}

fun cancelNotification(context: Context, notificationId: Int) {
    with(NotificationManagerCompat.from(context)) {
        cancel(notificationId)
    }
}

