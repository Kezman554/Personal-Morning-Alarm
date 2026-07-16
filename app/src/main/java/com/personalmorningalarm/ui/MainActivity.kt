package com.personalmorningalarm.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.databinding.ActivityMainBinding
import com.personalmorningalarm.util.AlarmScheduler
import com.personalmorningalarm.util.BatteryOptimisationHelper
import com.personalmorningalarm.util.NotificationPermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Guards against stacking a second dialog when onStart runs again. */
    private var notificationDialogShown = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "POST_NOTIFICATIONS granted=$granted")
            // Denied means the alarm can sound with no way to dismiss it, so say so
            // now rather than letting it surface at 6am.
            if (!granted) showNotificationsBlockedDialog()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navHostFragment.id) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)

        rearmAlarmIfEnabled()

        // Once, on first launch, nudge the user to whitelist the app so alarms fire
        // reliably. savedInstanceState guards against re-showing on recreation
        // (e.g. when a theme change recreates the activity).
        if (savedInstanceState == null && BatteryOptimisationHelper.shouldPromptOnLaunch(this)) {
            BatteryOptimisationHelper.markPrompted(this)
            BatteryOptimisationHelper.showExplanationDialog(this)
        }
    }

    override fun onStart() {
        super.onStart()
        // Re-checked on every launch, not once: the grant can vanish under the app
        // (a reinstall resets it, and the user can revoke it in system settings),
        // and the failure is silent — the alarm sounds with no dismissal screen.
        ensureNotificationsEnabled()
    }

    private fun ensureNotificationsEnabled() {
        if (NotificationPermissionHelper.areNotificationsEnabled(this)) {
            notificationDialogShown = false
            return
        }
        if (NotificationPermissionHelper.shouldRequestPermission(this)) {
            NotificationPermissionHelper.markRequested(this)
            notificationPermissionLauncher.launch(NotificationPermissionHelper.PERMISSION)
        } else {
            // Already asked (or notifications were turned off in settings, where the
            // system dialog would silently no-op) — send them to settings instead.
            showNotificationsBlockedDialog()
        }
    }

    private fun showNotificationsBlockedDialog() {
        if (notificationDialogShown) return
        notificationDialogShown = true
        Log.w(TAG, "Notifications disabled — the alarm cannot show its dismissal screen")
        NotificationPermissionHelper.showBlockedDialog(this)
    }

    /**
     * Self-heals the alarm schedule on every app launch. Exact alarms are one-shot
     * and only re-arm when they fire ([AlarmReceiver]) or after a reboot
     * ([BootReceiver]); a force-stop or a single missed fire silently drops the
     * pending alarm while the saved config still reads "enabled", so the toggle
     * looks on but nothing is scheduled. Re-arming here means simply opening the
     * app guarantees the next occurrence is set. Idempotent: scheduleDailyAlarm
     * uses FLAG_UPDATE_CURRENT, so a re-arm just overwrites any existing alarm.
     */
    private fun rearmAlarmIfEnabled() {
        val appContext = applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val config = AlarmRepository(AppDatabase.getInstance(appContext)).getCurrentConfig()
                if (config == null || !config.isEnabled) return@launch
                val scheduler = AlarmScheduler(appContext)
                if (scheduler.canScheduleExactAlarms()) {
                    val ok = scheduler.scheduleDailyAlarm(config.alarmTime)
                    Log.d(TAG, "Re-armed alarm on launch (alarmTime=${config.alarmTime}, ok=$ok)")
                } else {
                    // Permission revoked; HomeFragment surfaces the prompt when shown.
                    Log.w(TAG, "Alarm enabled but exact-alarm permission missing — not re-armed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-arm alarm on launch", e)
            }
        }
    }

    companion object {
        private const val TAG = "PMA"
    }
}
