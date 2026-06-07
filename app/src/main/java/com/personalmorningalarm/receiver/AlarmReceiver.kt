package com.personalmorningalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.personalmorningalarm.service.AlarmService
import com.personalmorningalarm.service.NuclearAlarmService
import com.personalmorningalarm.ui.AlarmDismissalActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fired by AlarmManager when a scheduled alarm goes off. Starts the Stage 1
 * foreground [AlarmService], which plays the alarm and shows the dismissal UI.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        if (AlarmDismissalActivity.isSessionActive || NuclearAlarmService.isRunning) {
            Log.d(TAG, "Alarm fired at $now but a wake session/nuclear alarm is active — ignoring")
            return
        }
        Log.d(TAG, "Alarm fired at $now (action=${intent?.action}) — starting AlarmService")
        AlarmService.start(context)
    }

    companion object {
        private const val TAG = "PMA"
        const val ACTION_ALARM_FIRED = "com.personalmorningalarm.action.ALARM_FIRED"
    }
}
