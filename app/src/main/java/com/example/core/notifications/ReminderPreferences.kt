package com.example.core.notifications

import android.content.Context

data class ReminderConfig(
    val enabled: Boolean = false,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE
) {
    companion object {
        const val DEFAULT_HOUR = 9
        const val DEFAULT_MINUTE = 0
    }
}

class ReminderPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getConfig(): ReminderConfig {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val hour = prefs.getInt(KEY_HOUR, ReminderConfig.DEFAULT_HOUR).coerceIn(0, 23)
        val minute = prefs.getInt(KEY_MINUTE, ReminderConfig.DEFAULT_MINUTE).coerceIn(0, 59)
        return ReminderConfig(enabled = enabled, hour = hour, minute = minute)
    }

    fun saveConfig(enabled: Boolean, hour: Int, minute: Int) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    fun saveDisplayName(displayName: String?) {
        val cleaned = displayName?.trim()?.take(60).orEmpty()
        prefs.edit().apply {
            if (cleaned.isEmpty()) remove(KEY_DISPLAY_NAME) else putString(KEY_DISPLAY_NAME, cleaned)
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "rehab_reminder_prefs"
        private const val KEY_ENABLED = "daily_reminder_enabled"
        private const val KEY_HOUR = "daily_reminder_hour"
        private const val KEY_MINUTE = "daily_reminder_minute"
        private const val KEY_DISPLAY_NAME = "reminder_display_name"

        fun formatTime(hour: Int, minute: Int): String =
            "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }
}
