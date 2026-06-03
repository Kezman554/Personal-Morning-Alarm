package com.personalmorningalarm.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.personalmorningalarm.receiver.AlarmReceiver

/**
 * Schedules and cancels exact alarms via [AlarmManager].
 *
 * Uses setExactAndAllowWhileIdle so alarms still fire when the device is in
 * Doze. On API 31+ exact alarms require the SCHEDULE_EXACT_ALARM permission;
 * callers should check [canScheduleExactAlarms] first and, if false, send the
 * user to [exactAlarmSettingsIntent]. On API 30 and below it is auto-granted.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    /**
     * Schedules an exact alarm to fire at [triggerAtMillis] (wall-clock epoch ms).
     * Returns false if exact alarms aren't permitted (API 31+ without permission).
     */
    fun scheduleAlarm(triggerAtMillis: Long, requestCode: Int = DEFAULT_REQUEST_CODE): Boolean {
        if (!canScheduleExactAlarms()) {
            Log.w(TAG, "Cannot schedule exact alarm: permission not granted")
            return false
        }
        // Non-null: FLAG_NO_CREATE is absent, so getBroadcast always returns an instance.
        val pendingIntent = buildPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT)!!
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        Log.d(TAG, "Alarm scheduled for $triggerAtMillis (requestCode=$requestCode)")
        return true
    }

    fun cancelAlarm(requestCode: Int = DEFAULT_REQUEST_CODE) {
        val pendingIntent = buildPendingIntent(requestCode, PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Alarm cancelled (requestCode=$requestCode)")
        }
    }

    /**
     * Whether a pending alarm with [requestCode] currently exists. Note a
     * one-shot alarm's PendingIntent lingers after it fires until cancelled.
     */
    fun isAlarmScheduled(requestCode: Int = DEFAULT_REQUEST_CODE): Boolean =
        buildPendingIntent(requestCode, PendingIntent.FLAG_NO_CREATE) != null

    /** Intent to the system "Alarms & reminders" permission screen (API 31+), else null. */
    fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            null
        }

    private fun buildPendingIntent(requestCode: Int, extraFlags: Int): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_FIRED
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or extraFlags
        )
    }

    companion object {
        private const val TAG = "PMA"
        const val DEFAULT_REQUEST_CODE = 1001
    }
}
