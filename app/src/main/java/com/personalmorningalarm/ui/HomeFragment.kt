package com.personalmorningalarm.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.personalmorningalarm.R
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.data.entity.AlarmConfig
import com.personalmorningalarm.data.model.MorningGoal
import com.personalmorningalarm.databinding.FragmentHomeBinding
import com.personalmorningalarm.service.CountdownService
import com.personalmorningalarm.util.AlarmScheduler
import com.personalmorningalarm.util.PinManager
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        ViewModelFactory(AlarmRepository(AppDatabase.getInstance(requireContext())))
    }

    private lateinit var scheduler: AlarmScheduler
    private lateinit var pinManager: PinManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scheduler = AlarmScheduler(requireContext())
        pinManager = PinManager(requireContext())

        binding.tvAlarmTime.setOnClickListener {
            PinPrompts.guard(requireContext(), pinManager) { showTimePicker() }
        }

        // setOnClickListener fires only on user taps (after the checked state
        // flips), so programmatic state updates below don't loop back.
        binding.switchEnabled.setOnClickListener { onToggleEnabledTapped() }
        binding.rbExercise.setOnClickListener { viewModel.setMorningGoal(MorningGoal.EXERCISE) }
        binding.rbProject.setOnClickListener { viewModel.setMorningGoal(MorningGoal.PROJECT) }

        setupDevTools()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.config.collect(::renderConfig) }
                launch { viewModel.stats.collect(::renderStats) }
            }
        }
    }

    // --- Rendering ---

    private fun renderConfig(config: AlarmConfig?) {
        val minutes = config?.alarmTime ?: HomeViewModel.DEFAULT_ALARM_MINUTES
        val enabled = config?.isEnabled == true

        binding.tvAlarmTime.text = formatTime(minutes)
        binding.switchEnabled.isChecked = enabled
        binding.tvNextAlarm.text =
            if (enabled) getString(R.string.home_alarm_set_for, formatTime(minutes))
            else getString(R.string.home_alarm_off)

        binding.rgGoal.check(
            if ((config?.morningGoal ?: MorningGoal.EXERCISE) == MorningGoal.PROJECT) {
                R.id.rb_project
            } else {
                R.id.rb_exercise
            }
        )
    }

    private fun renderStats(stats: HomeStats) {
        binding.tvStreak.text =
            if (stats.currentStreak == 1) getString(R.string.home_current_streak_one)
            else getString(R.string.home_current_streak, stats.currentStreak)

        binding.tvWeekRate.text =
            if (stats.attemptedDays == 0) getString(R.string.home_week_rate_empty)
            else getString(R.string.home_week_rate, stats.successDays, stats.attemptedDays)
    }

    // --- Actions ---

    private fun showTimePicker() {
        val minutes = currentMinutes()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                val newMinutes = hour * 60 + minute
                viewModel.setAlarmTime(newMinutes)
                // Keep a live alarm in sync with the new time.
                if (currentEnabled()) scheduleOrPrompt(newMinutes)
            },
            minutes / 60,
            minutes % 60,
            DateFormat.is24HourFormat(requireContext())
        ).show()
    }

    /**
     * The switch flips visually on tap. When a PIN guards it, revert that flip
     * until the PIN is confirmed, then re-apply on success; a wrong/cancelled PIN
     * leaves the switch in its original state.
     */
    private fun onToggleEnabledTapped() {
        val desired = binding.switchEnabled.isChecked
        if (pinManager.isPinSet()) binding.switchEnabled.isChecked = !desired
        PinPrompts.guard(requireContext(), pinManager) {
            binding.switchEnabled.isChecked = desired
            onToggleEnabled(desired)
        }
    }

    private fun onToggleEnabled(enabled: Boolean) {
        if (enabled) {
            if (!scheduler.canScheduleExactAlarms()) {
                binding.switchEnabled.isChecked = false // can't honour it yet
                promptForExactAlarmPermission()
                return
            }
            viewModel.setEnabled(true)
            scheduler.scheduleDailyAlarm(currentMinutes())
        } else {
            viewModel.setEnabled(false)
            scheduler.cancelAlarm()
        }
    }

    private fun scheduleOrPrompt(minutes: Int) {
        if (scheduler.canScheduleExactAlarms()) {
            scheduler.scheduleDailyAlarm(minutes)
        } else {
            promptForExactAlarmPermission()
        }
    }

    private fun promptForExactAlarmPermission() {
        Toast.makeText(requireContext(), R.string.home_exact_alarm_needed, Toast.LENGTH_LONG).show()
        scheduler.exactAlarmSettingsIntent()?.let { startActivity(it) }
    }

    private fun currentMinutes(): Int =
        viewModel.config.value?.alarmTime ?: HomeViewModel.DEFAULT_ALARM_MINUTES

    private fun currentEnabled(): Boolean = viewModel.config.value?.isEnabled == true

    private fun formatTime(minutes: Int): String {
        val pattern = if (DateFormat.is24HourFormat(requireContext())) "H:mm" else "h:mm a"
        return LocalTime.of(minutes / 60, minutes % 60)
            .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    // --- Temporary dev tools ---

    private fun setupDevTools() {
        binding.btnTestAlarm.setOnClickListener {
            if (!scheduler.canScheduleExactAlarms()) {
                promptForExactAlarmPermission()
                return@setOnClickListener
            }
            scheduler.scheduleAlarm(System.currentTimeMillis() + 60_000L)
            Toast.makeText(requireContext(), "Test alarm in 1 minute", Toast.LENGTH_SHORT).show()
        }
        binding.btnTestCountdown.setOnClickListener {
            CountdownService.start(requireContext(), 15)
            Toast.makeText(requireContext(), "15s countdown started", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
