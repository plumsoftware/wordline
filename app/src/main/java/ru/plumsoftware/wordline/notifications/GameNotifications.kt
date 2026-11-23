package ru.plumsoftware.wordline.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.plumsoftware.wordline.MainActivity
import ru.plumsoftware.wordline.R

object GameNotifications {
    const val TASKS_CHANNEL = "daily_tasks"
    const val NEWS_CHANNEL = "game_news"
    const val OPEN_TASKS = "open_tasks"
    const val TASKS_ID = 1001

    fun createChannels(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(
            NotificationChannel(TASKS_CHANNEL, "Новые задания", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Новый набор ежедневных заданий и награды"
            },
            NotificationChannel(NEWS_CHANNEL, "Новости игры", NotificationManager.IMPORTANCE_DEFAULT)
        ))
    }

    fun canPost(context: Context, channel: String): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE

    fun show(context: Context, title: String, body: String, tasks: Boolean, id: Int): Boolean {
        createChannels(context)
        val channel = if(tasks) TASKS_CHANNEL else NEWS_CHANNEL
        if(!canPost(context, channel)) return false
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(OPEN_TASKS, tasks)
        val tap = PendingIntent.getActivity(context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tap).setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).build()
        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        } catch(_: SecurityException) { false }
    }
}
