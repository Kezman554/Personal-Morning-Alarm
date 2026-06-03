package com.personalmorningalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fired by AlarmManager when a scheduled alarm goes off. For now it only logs;
 * later it will start the Stage 1 foreground alarm service.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        Log.d(TAG, "Alarm fired at $now (action=${intent?.action})")
    }

    companion object {
        private const val TAG = "PMA"
        const val ACTION_ALARM_FIRED = "com.personalmorningalarm.action.ALARM_FIRED"
    }
}
