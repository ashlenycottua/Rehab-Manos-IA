package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.core.notifications.BootReminderReceiver
import com.example.core.notifications.DailyReminderNotificationHelper
import com.example.core.notifications.DailyReminderReceiver
import com.example.core.notifications.DailyReminderScheduler
import com.example.core.notifications.ReminderPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyReminderTest {

    private lateinit var app: Application
    private lateinit var prefs: ReminderPreferences

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        prefs = ReminderPreferences(app)
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun `computeNextTriggerMillis schedules same day when target is in future`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 8, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val trigger = DailyReminderScheduler.computeNextTriggerMillis(hour = 19, minute = 30, nowMillis = now)
        val resultCal = Calendar.getInstance().apply { timeInMillis = trigger }

        assertEquals(2026, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, resultCal.get(Calendar.MONTH))
        assertEquals(18, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(19, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, resultCal.get(Calendar.MINUTE))
    }

    @Test
    fun `computeNextTriggerMillis rolls over to next day when target already passed`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 20, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val trigger = DailyReminderScheduler.computeNextTriggerMillis(hour = 9, minute = 0, nowMillis = now)
        val resultCal = Calendar.getInstance().apply { timeInMillis = trigger }

        assertEquals(19, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
    }

    @Test
    fun `preferences persist and format time cleanly`() {
        prefs.saveConfig(enabled = true, hour = 8, minute = 5)
        prefs.saveDisplayName("Sofía")

        val loaded = prefs.getConfig()
        assertTrue(loaded.enabled)
        assertEquals(8, loaded.hour)
        assertEquals(5, loaded.minute)
        assertEquals("08:05", ReminderPreferences.formatTime(loaded.hour, loaded.minute))
        assertEquals("Sofía", prefs.getDisplayName())
    }

    @Test
    fun `receiver posts local notification and reschedules next alarm`() {
        prefs.saveConfig(enabled = true, hour = 10, minute = 30)
        prefs.saveDisplayName("Carlos")

        val receiver = DailyReminderReceiver()
        receiver.onReceive(app, Intent(app, DailyReminderReceiver::class.java))

        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = Shadows.shadowOf(notificationManager)
        val posted = shadowNotificationManager.getNotification(DailyReminderNotificationHelper.NOTIFICATION_ID)
        assertNotNull(posted)

        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assertNotNull(shadowAlarmManager.nextScheduledAlarm)

        // Turning off and canceling clears scheduled alarm
        prefs.saveConfig(enabled = false, hour = 10, minute = 30)
        DailyReminderScheduler.cancelDailyReminder(app)
        assertNull(shadowAlarmManager.nextScheduledAlarm)
        assertFalse(prefs.getConfig().enabled)
    }

    @Test
    fun `boot receiver restores scheduled alarm when enabled`() {
        prefs.saveConfig(enabled = true, hour = 14, minute = 15)
        DailyReminderScheduler.cancelDailyReminder(app)

        val bootReceiver = BootReminderReceiver()
        bootReceiver.onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assertNotNull(shadowAlarmManager.nextScheduledAlarm)
    }
}
