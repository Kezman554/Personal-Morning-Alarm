package com.personalmorningalarm.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personalmorningalarm.R
import com.personalmorningalarm.data.entity.AlarmEvent
import com.personalmorningalarm.data.model.MorningGoal
import com.personalmorningalarm.databinding.ItemEventHistoryBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Event history list: date, success/failure glyph, time taken, morning goal.
 * One row per recorded [AlarmEvent], newest first (ordering set by the caller).
 */
class EventHistoryAdapter : ListAdapter<AlarmEvent, EventHistoryAdapter.EventViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: ItemEventHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: AlarmEvent) {
            val context = binding.root.context
            binding.tvDate.text = formatDate(event.date)

            val goalText = context.getString(
                if (event.morningGoal == MorningGoal.PROJECT) R.string.goal_project
                else R.string.goal_exercise
            )

            if (event.stage2Success) {
                binding.tvStatus.text = context.getString(R.string.stats_history_success)
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.stat_success))
                binding.tvDetail.text = context.getString(
                    R.string.stats_history_detail, goalText, formatDuration(event.stage2TimeSeconds)
                )
            } else {
                binding.tvStatus.text = context.getString(R.string.stats_history_failure)
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.stat_failure))
                binding.tvDetail.text = context.getString(
                    R.string.stats_history_detail, goalText,
                    context.getString(R.string.stats_history_missed)
                )
            }
        }

        private fun formatDate(isoDate: String): String =
            try {
                LocalDate.parse(isoDate).format(DATE_FORMAT)
            } catch (e: Exception) {
                isoDate // fall back to the raw string if somehow unparseable
            }

        /** "1:25" for ≥1 min, "45s" otherwise. */
        private fun formatDuration(seconds: Int): String =
            if (seconds >= 60) String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
            else "${seconds}s"
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

        private val DIFF = object : DiffUtil.ItemCallback<AlarmEvent>() {
            override fun areItemsTheSame(oldItem: AlarmEvent, newItem: AlarmEvent) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: AlarmEvent, newItem: AlarmEvent) =
                oldItem == newItem
        }
    }
}
