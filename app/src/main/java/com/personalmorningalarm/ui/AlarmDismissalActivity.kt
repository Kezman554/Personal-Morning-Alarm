package com.personalmorningalarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.personalmorningalarm.R
import com.personalmorningalarm.challenge.ShakeChallenge
import com.personalmorningalarm.databinding.ActivityAlarmDismissalBinding
import com.personalmorningalarm.service.AlarmService

/**
 * Shown over the lock screen when the alarm fires. Stage 1: shake the phone for
 * a cumulative 15 seconds to dismiss. Turns the screen on, shows over the
 * keyguard, and blocks the back button so the alarm can't be escaped.
 */
class AlarmDismissalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmDismissalBinding
    private lateinit var shakeChallenge: ShakeChallenge
    private var lastLoggedBucket = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()

        binding = ActivityAlarmDismissalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button must not dismiss the alarm.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Intentionally swallowed — only the shake challenge dismisses.
            }
        })

        shakeChallenge = ShakeChallenge(this).apply {
            onProgress = { progress -> updateProgress(progress) }
            onComplete = { onShakeComplete() }
        }
        updateProgress(0f)

        // TODO(temporary): fallback while tuning shake detection; remove later.
        binding.btnStopAlarm.setOnClickListener {
            AlarmService.stop(this)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        shakeChallenge.start()
    }

    override fun onPause() {
        super.onPause()
        shakeChallenge.stop()
    }

    private fun updateProgress(progress: Float) {
        val percent = (progress * 100).toInt()
        binding.shakeProgress.progress = percent
        binding.shakeStatus.text = getString(R.string.shake_progress_format, percent)

        // Log at 25% milestones so the on-device buffer stays readable.
        val bucket = percent / 25
        if (bucket != lastLoggedBucket) {
            lastLoggedBucket = bucket
            Log.d(TAG, "Shake progress: $percent%")
        }
    }

    private fun onShakeComplete() {
        Log.d(TAG, "Shake challenge complete — dismissing alarm")
        binding.shakeProgress.progress = 100
        binding.shakeStatus.text = getString(R.string.shake_progress_format, 100)
        AlarmService.stop(this)
        finish()
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

    companion object {
        private const val TAG = "PMA"
    }
}
