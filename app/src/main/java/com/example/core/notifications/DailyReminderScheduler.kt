package com.example.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object DailyReminderScheduler {
    private const val REQUEST_CODE_DAILY_ALARM = 2001

    fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int): Long {
        DailyReminderNotificationHelper.ensureNotificationChannel(context)
        val triggerAtMillis = computeNextTriggerMillis(hour, minute)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context)

        alarmManager?.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        return triggerAtMillis
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val existingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_ALARM,
            Intent(context, DailyReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existingIntent != null) {
            alarmManager?.cancel(existingIntent)
            existingIntent.cancel()
        }
    }

    fun syncScheduleFromPreferences(context: Context) {
        val config = ReminderPreferences(context).getConfig()
        if (config.enabled && DailyReminderNotificationHelper.hasRuntimeNotificationPermission(context)) {
            scheduleDailyReminder(context, config.hour, config.minute)
        } else {
            cancelDailyReminder(context)
        }
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
