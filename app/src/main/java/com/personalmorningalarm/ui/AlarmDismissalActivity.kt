package com.personalmorningalarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.personalmorningalarm.databinding.ActivityAlarmDismissalBinding
import com.personalmorningalarm.service.AlarmService

/**
 * Shown over the lock screen when the alarm fires. Stage 1 (shake-to-dismiss)
 * UI will live here; for now it just turns the screen on, shows over the
 * keyguard, blocks the back button, and offers a temporary stop control.
 */
class AlarmDismissalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmDismissalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()

        binding = ActivityAlarmDismissalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button must not dismiss the alarm.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Intentionally swallowed — the alarm can only be dismissed via the sequence.
            }
        })

        // TODO(temporary): real Stage 1 (shake) dismissal replaces this button.
        binding.btnStopAlarm.setOnClickListener {
            AlarmService.stop(this)
            finish()
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }
}
