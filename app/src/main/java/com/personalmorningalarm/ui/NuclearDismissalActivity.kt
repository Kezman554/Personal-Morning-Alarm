package com.personalmorningalarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.personalmorningalarm.databinding.ActivityNuclearDismissalBinding
import com.personalmorningalarm.service.NuclearAlarmService

/**
 * The nuclear-alarm screen (Stage 2 failure). Shows over the lock screen and is
 * deliberately hard to escape:
 *  - Back button is swallowed.
 *  - HOME/recents can't be blocked outright on stock Android, so we relaunch
 *    ourselves when the user leaves ([onUserLeaveHint]); the nuclear screen
 *    snaps back to the front.
 *  - The foreground-service notification is ongoing, so it can't be swiped away.
 *
 * The full dismissal gauntlet (30s shake + math + NFC + typed phrase) is the
 * next task; for now a temporary Stop button ends a test run.
 */
class NuclearDismissalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNuclearDismissalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()

        binding = ActivityNuclearDismissalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Swallowed — nuclear can't be escaped with the back button.
            }
        })

        // TODO(temporary): until the dismissal challenges are built, this ends a
        // test run so the alarm can be silenced without a force-stop.
        binding.btnStopNuclear.setOnClickListener {
            Log.d(TAG, "Nuclear stopped via temp button")
            NuclearAlarmService.stop(this)
            finish()
        }
    }

    /**
     * Belt-and-suspenders back blocking: the OnBackPressedCallback above handles
     * the modern path, this catches the legacy hardware/key path. Also swallow
     * the recent-apps key. HOME can't be intercepted at all on stock Android.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true // consumed — don't let it leave the nuclear screen
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * Pressing HOME (or otherwise leaving) sends us to the background. While the
     * nuclear service is still running, bring ourselves back to the front so the
     * alarm can't be dodged by going to the launcher.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (NuclearAlarmService.isRunning) {
            Log.d(TAG, "User left nuclear screen — relaunching")
            startActivity(
                Intent(this, NuclearDismissalActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
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

    companion object {
        private const val TAG = "PMA"
    }
}
