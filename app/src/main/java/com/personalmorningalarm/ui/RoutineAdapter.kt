package com.personalmorningalarm.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personalmorningalarm.R
import com.personalmorningalarm.data.dao.RoutineWithCount
import com.personalmorningalarm.databinding.ItemRoutineBinding

/**
 * Lists stretch routines. Tapping a row opens the editor; the radio marks the
 * active routine (hidden when routines are matched to the morning goal instead).
 */
class RoutineAdapter(
    private val onOpen: (RoutineWithCount) -> Unit,
    private val onSetActive: (RoutineWithCount) -> Unit
) : ListAdapter<RoutineWithCount, RoutineAdapter.RoutineViewHolder>(DIFF) {

    /** When true, routines follow the morning goal, so the active radios are hidden. */
    var matchToGoal: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding = ItemRoutineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RoutineViewHolder(
        private val binding: ItemRoutineBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RoutineWithCount) {
            val context = binding.root.context
            binding.tvRoutineName.text = item.routine.name
            binding.tvRoutineSubtitle.text =
                if (item.exerciseCount == 1) context.getString(R.string.routine_stretch_count_one)
                else context.getString(R.string.routine_stretch_count, item.exerciseCount)

            binding.rbActive.visibility = if (matchToGoal) View.GONE else View.VISIBLE
            binding.rbActive.isChecked = item.routine.isActive

            binding.root.setOnClickListener { onOpen(item) }
            binding.rbActive.setOnClickListener { onSetActive(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RoutineWithCount>() {
            override fun areItemsTheSame(oldItem: RoutineWithCount, newItem: RoutineWithCount) =
                oldItem.routine.id == newItem.routine.id

            override fun areContentsTheSame(oldItem: RoutineWithCount, newItem: RoutineWithCount) =
                oldItem == newItem
        }
    }
}
