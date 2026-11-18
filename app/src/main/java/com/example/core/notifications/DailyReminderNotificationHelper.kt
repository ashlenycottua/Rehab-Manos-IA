package com.example.core.notifications

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
import com.example.MainActivity
import com.example.R

object DailyReminderNotificationHelper {
    const val CHANNEL_ID = "rehab_daily_reminders"
    const val NOTIFICATION_ID = 1001
    const val EXTRA_OPEN_FROM_REMINDER = "open_from_reminder"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_daily_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_daily_desc)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasRuntimeNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return hasRuntimeNotificationPermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showDailyReminderNotification(
        context: Context,
        userName: String? = ReminderPreferences(context).getDisplayName()
    ): Boolean {
        ensureNotificationChannel(context)
        if (!areNotificationsEnabled(context)) return false

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_FROM_REMINDER, true)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanName = userName?.trim()?.takeIf { it.isNotEmpty() }
        val title = if (cleanName != null) {
            context.getString(R.string.daily_reminder_title_personalized, cleanName)
        } else {
            context.getString(R.string.daily_reminder_title)
        }
        val body = context.getString(R.string.daily_reminder_body)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_hand)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
