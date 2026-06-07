package com.personalmorningalarm.ui

import android.animation.ArgbEvaluator
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.personalmorningalarm.R
import com.personalmorningalarm.challenge.CheckpointResult
import com.personalmorningalarm.challenge.ContentScreenManager
import com.personalmorningalarm.challenge.NfcCheckpointManager
import com.personalmorningalarm.challenge.ShakeChallenge
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.data.entity.AlarmEvent
import com.personalmorningalarm.data.model.ContentType
import com.personalmorningalarm.data.model.MorningGoal
import com.personalmorningalarm.databinding.ActivityAlarmDismissalBinding
import com.personalmorningalarm.service.AlarmService
import com.personalmorningalarm.service.CountdownService
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil

/**
 * Stage 1 (shake) and Stage 2 (NFC checkpoint sequence). Shows over the lock
 * screen, blocks back. Stage 1: shake to fill a bar. Stage 2: tap the registered
 * NFC tags in a randomised order while the countdown runs; if none are
 * registered, a single "tap to confirm" stands in.
 */
class AlarmDismissalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmDismissalBinding
    private lateinit var shakeChallenge: ShakeChallenge
    private lateinit var repository: AlarmRepository

    private val argbEvaluator = ArgbEvaluator()
    private var vibrator: Vibrator? = null
    private val dismissRunnable = Runnable { dismissToHome() }

    // When Stage 2's countdown expires, CountdownService starts the nuclear alarm
    // and broadcasts this; finish so we don't linger behind the nuclear screen.
    private val countdownExpiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Countdown expired — nuclear took over, finishing Stage 2")
            finish()
        }
    }

    private var nfcAdapter: NfcAdapter? = null
    private var checkpointManager: NfcCheckpointManager? = null
    private var contentScreenManager: ContentScreenManager? = null
    private var gapsShown = 0
    private var showingContent = false
    private var stretchTimer: CountDownTimer? = null
    private var showingUnlockHint = false

    private val keyguardManager: KeyguardManager by lazy {
        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    // Reader mode (not foreground dispatch): reliable for consecutive reads on
    // this Samsung device, where the system NFC UI stole focus after the first
    // tag and killed dispatch. Callback runs on a binder thread.
    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        val tagId = tag.id.toHex()
        runOnUiThread { if (stage == STAGE_2) onCheckpointTag(tagId) }
    }

    private var stage = STAGE_1
    private var lastLoggedBucket = -1
    private var stage2StartMs = 0L

    private var stage2DurationMinutes = DEFAULT_STAGE2_MINUTES
    private var stretchDurationMinutes = DEFAULT_STRETCH_MINUTES
    private var morningGoal = MorningGoal.EXERCISE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()

        isSessionActive = true
        binding = ActivityAlarmDismissalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AlarmRepository(AppDatabase.getInstance(this))
        vibrator = resolveVibrator()
        setupNfc()

        ContextCompat.registerReceiver(
            this,
            countdownExpiredReceiver,
            IntentFilter(CountdownService.ACTION_COUNTDOWN_EXPIRED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Swallowed — the alarm can't be escaped with the back button.
            }
        })

        shakeChallenge = ShakeChallenge(this).apply {
            onProgress = { progress -> onShakeProgress(progress) }
            onComplete = { onShakeComplete() }
        }
        updateBackground(0f)
        binding.tvSeconds.text = getString(R.string.seconds_format, TARGET_SECONDS)

        binding.btnStopAlarm.setOnClickListener {
            AlarmService.stop(this)
            CountdownService.stop(this)
            finish()
        }

        lifecycleScope.launch {
            repository.getCurrentConfig()?.let { config ->
                stage2DurationMinutes = config.stage2DurationMinutes
                morningGoal = config.morningGoal
            }
        }
    }

    override fun onResume() {
        super.onResume()
        when (stage) {
            STAGE_1 -> shakeChallenge.start()
            STAGE_2 -> {
                if (checkpointManager != null) enableNfcDispatch()
                if (showingUnlockHint && !keyguardManager.isKeyguardLocked) {
                    showingUnlockHint = false
                    binding.tvFeedback.visibility = View.GONE
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        shakeChallenge.stop()
        disableNfcDispatch()
    }

    override fun onDestroy() {
        super.onDestroy()
        stretchTimer?.cancel()
        binding.root.removeCallbacks(dismissRunnable)
        runCatching { unregisterReceiver(countdownExpiredReceiver) }
        isSessionActive = false
    }

    // --- Stage 1 ---

    private fun onShakeProgress(progress: Float) {
        val percent = (progress * 100).toInt()
        binding.shakeProgress.progress = percent

        val remaining = ceil((1f - progress) * TARGET_SECONDS).toInt().coerceAtLeast(0)
        binding.tvSeconds.text = getString(R.string.seconds_format, remaining)

        updateBackground(progress)

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

        binding.tvTitle.text = getString(R.string.stage1_success)
        binding.tvSeconds.visibility = View.GONE
        binding.root.postDelayed({ startStage2() }, SUCCESS_DELAY_MS)
    }

    private fun updateBackground(progress: Float) {
        binding.root.setBackgroundColor(argbEvaluator.evaluate(progress, COLOR_RED, COLOR_GREEN) as Int)
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
        stage2StartMs = SystemClock.elapsedRealtime()

        binding.shakeProgress.visibility = View.GONE
        binding.tvSeconds.visibility = View.GONE
        binding.tvCountdown.visibility = View.VISIBLE
        binding.root.setBackgroundColor(COLOR_STAGE2)

        CountdownService.start(this, stage2DurationMinutes * 60)
        observeCountdown()

        lifecycleScope.launch {
            val tags = repository.getActiveNfcTags()
            val enabledToggles = repository.getEnabledContentToggles()
            contentScreenManager = ContentScreenManager(enabledToggles.map { it.contentType })
            stretchDurationMinutes = enabledToggles
                .firstOrNull { it.contentType == ContentType.STRETCH }
                ?.durationMinutes ?: DEFAULT_STRETCH_MINUTES
            if (tags.isEmpty() || nfcAdapter?.isEnabled != true) {
                Log.d(TAG, "Stage 2: no usable NFC tags — falling back to confirm button")
                setupFallbackConfirm()
            } else {
                checkpointManager = NfcCheckpointManager(tags)
                Log.d(TAG, "Stage 2: ${tags.size} checkpoints, tap order randomised; " +
                    "content=${enabledToggles.map { it.contentType }}, stretch=${stretchDurationMinutes}m")
                enableNfcDispatch()
                showCheckpoint()
                promptUnlockIfNeeded()
            }
        }
    }

    private fun showCheckpoint() {
        val manager = checkpointManager ?: return
        binding.tvCheckpoint.visibility = View.VISIBLE
        binding.tvCheckpoint.text =
            getString(R.string.checkpoint_progress, manager.currentNumber, manager.total)
        binding.tvTitle.text = getString(R.string.stage2_go_to, manager.currentTag?.label.orEmpty())
        binding.tvFeedback.visibility = View.GONE
    }

    private fun onCheckpointTag(tagId: String) {
        if (showingContent) return // ignore taps while a content screen is up
        when (val result = checkpointManager?.onTagScanned(tagId)) {
            is CheckpointResult.Correct -> {
                Log.d(TAG, "Checkpoint correct — next: ${result.next.label}")
                val content = contentScreenManager?.contentForGap(gapsShown)
                gapsShown++
                if (content != null) showContentScreen(content) else showCheckpoint()
            }
            is CheckpointResult.Wrong -> {
                Log.d(TAG, "Wrong tag — still need: ${result.expected.label}")
                binding.tvFeedback.visibility = View.VISIBLE
                binding.tvFeedback.text = getString(R.string.wrong_tag, result.expected.label)
                vibrator?.vibrate(VibrationEffect.createOneShot(WRONG_PULSE_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            CheckpointResult.SequenceComplete -> {
                Log.d(TAG, "All checkpoints tapped")
                onStage2Complete()
            }
            else -> { /* AlreadyComplete / null — ignore */ }
        }
    }

    private fun setupFallbackConfirm() {
        binding.tvCheckpoint.visibility = View.GONE
        binding.tvFeedback.visibility = View.GONE
        binding.tvTitle.text = getString(R.string.stage2_title)
        binding.btnConfirm.visibility = View.VISIBLE
        binding.btnConfirm.setOnClickListener { onStage2Complete() }
    }

    // --- Content screens (between checkpoints) ---

    private fun showContentScreen(type: ContentType) {
        showingContent = true
        // Freeze the Stage 2 deadline while content shows — content time shouldn't
        // count against the nuclear timer (a 10-min stretch must not fail you).
        CountdownService.pause(this)
        binding.content.visibility = View.GONE
        binding.contentPanel.visibility = View.VISIBLE
        binding.contentAuthor.visibility = View.GONE
        binding.contentTimer.visibility = View.GONE
        stretchTimer?.cancel()
        binding.btnContinue.setOnClickListener { onContentContinue() }

        when (type) {
            ContentType.QUOTE -> showQuoteContent()
            ContentType.STRETCH -> showStretchContent()
            ContentType.PLACEHOLDER -> showGoalContent()
        }
        Log.d(TAG, "Content screen shown: $type")
    }

    private fun showQuoteContent() {
        binding.contentHeading.text = getString(R.string.content_quote_heading)
        lifecycleScope.launch {
            val quote = repository.getRandomQuote()
            binding.contentBody.text = quote?.quoteText.orEmpty()
            val author = quote?.author
            if (!author.isNullOrBlank()) {
                binding.contentAuthor.visibility = View.VISIBLE
                binding.contentAuthor.text = getString(R.string.quote_author_format, author)
            }
        }
    }

    private fun showStretchContent() {
        binding.contentHeading.text = getString(R.string.content_stretch_heading)
        binding.contentBody.text = getString(R.string.stretch_suggestions)
        binding.contentTimer.visibility = View.VISIBLE
        val stretchMs = stretchDurationMinutes * 60 * 1000L
        stretchTimer = object : CountDownTimer(stretchMs, 1000L) {
            override fun onTick(msLeft: Long) {
                binding.contentTimer.text = CountdownService.formatTime((msLeft / 1000L).toInt())
            }
            override fun onFinish() = onContentContinue() // auto-advance when timer ends
        }.start()
    }

    private fun showGoalContent() {
        binding.contentHeading.text = getString(R.string.content_goal_heading)
        binding.contentBody.text = when (morningGoal) {
            MorningGoal.EXERCISE -> getString(R.string.goal_exercise)
            MorningGoal.PROJECT -> getString(R.string.goal_project)
        }
    }

    private fun onContentContinue() {
        stretchTimer?.cancel()
        stretchTimer = null
        showingContent = false
        binding.contentPanel.visibility = View.GONE
        binding.content.visibility = View.VISIBLE
        CountdownService.resume(this) // restart the Stage 2 deadline
        showCheckpoint()
    }

    private fun onStage2Complete() {
        val seconds = ((SystemClock.elapsedRealtime() - stage2StartMs) / 1000L).toInt()
        stage = STAGE_DONE
        // Keep reader mode engaged (taps now ignored, since stage != STAGE_2) until
        // the activity pauses — otherwise a lingering chip gets grabbed by the
        // system's own NFC popup. onPause/onDestroy disables it.
        CountdownService.stop(this)
        clearAlarmNotifications()

        // Base success screen; the streak line fills in once it's computed.
        binding.tvCountdown.visibility = View.GONE
        binding.tvFeedback.visibility = View.GONE
        binding.btnConfirm.visibility = View.GONE
        binding.tvCheckpoint.visibility = View.GONE
        binding.tvTitle.text = getString(R.string.success_title)
        binding.root.setBackgroundColor(COLOR_GREEN)

        lifecycleScope.launch {
            recordStage2Success(seconds)
            val streak = repository.getCurrentStreak()
            binding.tvSeconds.visibility = View.VISIBLE
            binding.tvSeconds.text = getString(R.string.streak_format, streak)
            binding.tvFeedback.visibility = View.VISIBLE
            binding.tvFeedback.text = goalReminder()
            Log.d(TAG, "Stage 2 success — streak now $streak")
        }

        // Auto-dismiss after 5s. Tap-to-dismiss only activates after a short grace
        // period so a stray touch right after the final tag doesn't skip the screen.
        binding.root.postDelayed(dismissRunnable, SUCCESS_SCREEN_MS)
        binding.root.postDelayed({
            binding.root.setOnClickListener {
                binding.root.removeCallbacks(dismissRunnable)
                dismissToHome()
            }
        }, TAP_GRACE_MS)
    }

    private suspend fun recordStage2Success(seconds: Int) {
        val today = LocalDate.now().toString()
        val existing = repository.getEventByDate(today)
        if (existing != null) {
            repository.updateEvent(
                existing.copy(stage2Success = true, stage2TimeSeconds = seconds)
            )
        } else {
            repository.recordEvent(
                AlarmEvent(
                    date = today,
                    stage1Success = true,
                    stage2Success = true,
                    stage2TimeSeconds = seconds,
                    morningGoal = morningGoal
                )
            )
        }
        Log.d(TAG, "Stage 2 success logged ($seconds s)")
    }

    private fun goalReminder(): String = when (morningGoal) {
        MorningGoal.EXERCISE -> getString(R.string.goal_reminder_exercise)
        MorningGoal.PROJECT -> getString(R.string.goal_reminder_project)
    }

    private fun clearAlarmNotifications() {
        getSystemService(NotificationManager::class.java).cancelAll()
    }

    private fun dismissToHome() {
        if (!isFinishing) finish()
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

    // --- NFC ---

    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    /**
     * NFC won't deliver taps to the app while the secure keyguard is up, so if
     * Stage 2 starts locked we prompt the user to unlock and show a hint.
     */
    private fun promptUnlockIfNeeded() {
        if (keyguardManager.isKeyguardLocked) {
            showingUnlockHint = true
            binding.tvFeedback.visibility = View.VISIBLE
            binding.tvFeedback.text = getString(R.string.nfc_unlock_hint)
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

    private fun enableNfcDispatch() {
        runCatching { nfcAdapter?.enableReaderMode(this, readerCallback, READER_FLAGS, null) }
    }

    private fun disableNfcDispatch() {
        runCatching { nfcAdapter?.disableReaderMode(this) }
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun resolveVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }

    companion object {
        private const val TAG = "PMA"

        /** True while a wake/dismissal session is on screen, so a re-fired alarm is ignored. */
        @Volatile
        var isSessionActive = false
            private set

        private const val STAGE_1 = 1
        private const val STAGE_TRANSITION = 2
        private const val STAGE_2 = 3
        private const val STAGE_DONE = 4

        private const val TARGET_SECONDS =
            (ShakeChallenge.DEFAULT_TARGET_DURATION_MS / 1000).toInt()
        private const val DEFAULT_STAGE2_MINUTES = 10
        private const val DEFAULT_STRETCH_MINUTES = 5
        private const val SUCCESS_DELAY_MS = 1200L
        private const val SUCCESS_SCREEN_MS = 5000L
        private const val TAP_GRACE_MS = 1500L
        private const val WRONG_PULSE_MS = 150L

        private const val COLOR_RED = 0xFFD32F2F.toInt()
        private const val COLOR_GREEN = 0xFF388E3C.toInt()
        private const val COLOR_STAGE2 = 0xFF1565C0.toInt()

        private const val READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
    }
}
