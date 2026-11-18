package com.example.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = ReminderPreferences(context)
        val config = prefs.getConfig()
        if (!config.enabled) {
            DailyReminderScheduler.cancelDailyReminder(context)
            return
        }

        DailyReminderNotificationHelper.showDailyReminderNotification(
            context = context,
            userName = prefs.getDisplayName()
        )

        // Schedule the next day's reminder occurrence
        DailyReminderScheduler.scheduleDailyReminder(
            context = context,
            hour = config.hour,
            minute = config.minute
        )
    }
}
