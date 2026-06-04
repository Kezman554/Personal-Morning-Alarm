package com.personalmorningalarm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.personalmorningalarm.databinding.FragmentHomeBinding
import com.personalmorningalarm.service.CountdownService
import com.personalmorningalarm.util.AlarmScheduler

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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

        // TODO(temporary): dev button to verify alarm scheduling end-to-end.
        val scheduler = AlarmScheduler(requireContext())
        binding.btnTestAlarm.setOnClickListener {
            if (!scheduler.canScheduleExactAlarms()) {
                Toast.makeText(
                    requireContext(),
                    "Grant 'Alarms & reminders' permission, then tap again",
                    Toast.LENGTH_LONG
                ).show()
                scheduler.exactAlarmSettingsIntent()?.let { startActivity(it) }
                return@setOnClickListener
            }
            val triggerAt = System.currentTimeMillis() + 60_000L
            scheduler.scheduleAlarm(triggerAt)
            Toast.makeText(
                requireContext(),
                "Test alarm scheduled for 1 minute from now",
                Toast.LENGTH_SHORT
            ).show()
        }

        // TODO(temporary): dev button to verify the Stage 2 countdown in isolation.
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
