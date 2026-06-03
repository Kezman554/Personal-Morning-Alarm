package com.personalmorningalarm.ui

import android.animation.ArgbEvaluator
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.personalmorningalarm.R
import com.personalmorningalarm.challenge.ShakeChallenge
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.data.entity.AlarmEvent
import com.personalmorningalarm.data.model.MorningGoal
import com.personalmorningalarm.databinding.ActivityAlarmDismissalBinding
import com.personalmorningalarm.service.AlarmService
import com.personalmorningalarm.service.CountdownService
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil

/**
 * Stage 1 (shake to wake) and the handoff to Stage 2 (countdown). Shows over the
 * lock screen, blocks the back button. Shaking fills a progress bar with a
 * red->green background and haptic pulses; on completion it stops the alarm,
 * logs success, then switches into Stage 2 mode with the countdown running.
 */
class AlarmDismissalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmDismissalBinding
    private lateinit var shakeChallenge: ShakeChallenge
    private lateinit var repository: AlarmRepository

    private val argbEvaluator = ArgbEvaluator()
    private var vibrator: Vibrator? = null

    private var stage = STAGE_1
    private var lastLoggedBucket = -1
    private var lastProgress = 0f
    private var lastPulseTime = 0L

    private var stage2DurationMinutes = DEFAULT_STAGE2_MINUTES
    private var morningGoal = MorningGoal.EXERCISE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()

        binding = ActivityAlarmDismissalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AlarmRepository(AppDatabase.getInstance(this))
        vibrator = resolveVibrator()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Swallowed — the alarm can't be escaped with the back button.
            }
        })

        shakeChallenge = ShakeChallenge(this).apply {
            onProgress = { progress -> onShakeProgress(progress) }
            onComplete = { onShakeComplete() }
        }

        binding.tvCountdown.visibility = View.GONE
        updateBackground(0f)
        binding.tvSeconds.text = getString(R.string.seconds_format, TARGET_SECONDS)

        // TODO(temporary): abort control for testing Stage 1/2; remove later.
        binding.btnStopAlarm.setOnClickListener {
            AlarmService.stop(this)
            CountdownService.stop(this)
            finish()
        }

        // Load config for Stage 2 duration + the day's goal.
        lifecycleScope.launch {
            repository.getCurrentConfig()?.let { config ->
                stage2DurationMinutes = config.stage2DurationMinutes
                morningGoal = config.morningGoal
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (stage == STAGE_1) shakeChallenge.start()
    }

    override fun onPause() {
        super.onPause()
        shakeChallenge.stop()
    }

    // --- Stage 1 ---

    private fun onShakeProgress(progress: Float) {
        val percent = (progress * 100).toInt()
        binding.shakeProgress.progress = percent

        val remaining = ceil((1f - progress) * TARGET_SECONDS).toInt().coerceAtLeast(0)
        binding.tvSeconds.text = getString(R.string.seconds_format, remaining)

        updateBackground(progress)
        pulseIfShaking(progress)

        val bucket = percent / 25
        if (bucket != lastLoggedBucket) {
            lastLoggedBucket = bucket
            Log.d(TAG, "Shake progress: $percent%")
        }
    }

    private fun onShakeComplete() {
        Log.d(TAG, "Shake challenge complete — Stage 1 done")
        stage = STAGE_TRANSITION
        shakeChallenge.stop()

        binding.shakeProgress.progress = 100
        updateBackground(1f)
        AlarmService.stop(this)
        logStage1Success()

        // Brief success message, then move to Stage 2.
        binding.tvTitle.text = getString(R.string.stage1_success)
        binding.tvSeconds.visibility = View.GONE
        binding.root.postDelayed({ startStage2() }, SUCCESS_DELAY_MS)
    }

    private fun pulseIfShaking(progress: Float) {
        // Progress only advances while actively shaking, so a rise = shaking now.
        if (progress > lastProgress) {
            val now = SystemClock.uptimeMillis()
            if (now - lastPulseTime >= PULSE_INTERVAL_MS) {
                lastPulseTime = now
                vibrator?.vibrate(VibrationEffect.createOneShot(PULSE_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
        lastProgress = progress
    }

    private fun updateBackground(progress: Float) {
        val color = argbEvaluator.evaluate(progress, COLOR_RED, COLOR_GREEN) as Int
        binding.root.setBackgroundColor(color)
    }

    private fun logStage1Success() {
        lifecycleScope.launch {
            val today = LocalDate.now().toString()
            val existing = repository.getEventByDate(today)
            if (existing != null) {
                repository.updateEvent(existing.copy(stage1Success = true))
            } else {
                repository.recordEvent(
                    AlarmEvent(date = today, stage1Success = true, morningGoal = morningGoal)
                )
            }
            Log.d(TAG, "Stage 1 success logged for $today")
        }
    }

    // --- Stage 2 ---

    private fun startStage2() {
        stage = STAGE_2
        binding.shakeProgress.visibility = View.GONE
        binding.btnStopAlarm.visibility = View.VISIBLE
        binding.tvTitle.text = getString(R.string.stage2_title)
        binding.tvCountdown.visibility = View.VISIBLE
        binding.root.setBackgroundColor(COLOR_STAGE2)

        CountdownService.start(this, stage2DurationMinutes * 60)
        observeCountdown()
    }

    private fun observeCountdown() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CountdownService.remainingSeconds.collect { seconds ->
                    binding.tvCountdown.text = CountdownService.formatTime(seconds)
                }
            }
        }
    }

    // --- Window flags / helpers ---

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
        // Don't let the screen time out mid-challenge (would pause the activity).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun resolveVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    companion object {
        private const val TAG = "PMA"

        private const val STAGE_1 = 1
        private const val STAGE_TRANSITION = 2
        private const val STAGE_2 = 3

        private const val TARGET_SECONDS =
            (ShakeChallenge.DEFAULT_TARGET_DURATION_MS / 1000).toInt()
        private const val DEFAULT_STAGE2_MINUTES = 10
        private const val SUCCESS_DELAY_MS = 1200L
        private const val PULSE_INTERVAL_MS = 200L
        private const val PULSE_MS = 30L

        private const val COLOR_RED = 0xFFD32F2F.toInt()
        private const val COLOR_GREEN = 0xFF388E3C.toInt()
        private const val COLOR_STAGE2 = 0xFF1565C0.toInt()
    }
}
