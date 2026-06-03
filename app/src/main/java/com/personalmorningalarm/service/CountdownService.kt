package com.personalmorningalarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.personalmorningalarm.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Foreground service that runs the Stage 2 countdown. Ticks once per second,
 * publishing the remaining seconds via [remainingSeconds] for the UI and a
 * notification. When it expires the nuclear alarm will eventually fire (TODO);
 * for now it just logs and stops.
 */
class CountdownService : Service() {

    private var timer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val totalSeconds = intent?.getIntExtra(EXTRA_DURATION_SECONDS, DEFAULT_DURATION_SECONDS)
            ?: DEFAULT_DURATION_SECONDS
        Log.d(TAG, "CountdownService starting ($totalSeconds s)")
        startForeground(NOTIFICATION_ID, buildNotification(totalSeconds))
        isRunning = true
        startTimer(totalSeconds)
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "CountdownService stopping")
        isRunning = false
        timer?.cancel()
        timer = null
        _remainingSeconds.value = 0
        super.onDestroy()
    }

    private fun startTimer(totalSeconds: Int) {
        _remainingSeconds.value = totalSeconds
        timer?.cancel()
        timer = object : CountDownTimer(totalSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000L).toInt()
                _remainingSeconds.value = seconds
                updateNotification(seconds)
            }

            override fun onFinish() {
                _remainingSeconds.value = 0
                Log.d(TAG, "Stage 2 countdown expired — nuclear alarm TODO")
                // TODO: trigger nuclear alarm on expiry.
                stopSelf()
            }
        }.start()
    }

    private fun buildNotification(seconds: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.countdown_notification_title))
            .setContentText(formatTime(seconds))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun updateNotification(seconds: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(seconds))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.countdown_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.countdown_channel_description)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "PMA"
        private const val CHANNEL_ID = "countdown_channel"
        private const val NOTIFICATION_ID = 3001
        private const val DEFAULT_DURATION_SECONDS = 10 * 60

        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val ACTION_STOP = "com.personalmorningalarm.action.STOP_COUNTDOWN"

        private val _remainingSeconds = MutableStateFlow(0)

        /** Seconds left in the Stage 2 countdown; 0 when not running. */
        val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context, durationSeconds: Int) {
            val intent = Intent(context, CountdownService::class.java).apply {
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CountdownService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun formatTime(totalSeconds: Int): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }
}
