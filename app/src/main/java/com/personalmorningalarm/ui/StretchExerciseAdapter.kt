package com.personalmorningalarm.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personalmorningalarm.R
import com.personalmorningalarm.data.entity.StretchExercise
import com.personalmorningalarm.databinding.ItemStretchExerciseBinding

/**
 * Lists a routine's exercises with move up/down and delete controls; tapping the
 * row edits the exercise. Move buttons are disabled at the ends of the list.
 */
class StretchExerciseAdapter(
    private val onEdit: (StretchExercise) -> Unit,
    private val onMoveUp: (position: Int) -> Unit,
    private val onMoveDown: (position: Int) -> Unit,
    private val onDelete: (StretchExercise) -> Unit
) : ListAdapter<StretchExercise, StretchExerciseAdapter.ExerciseViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemStretchExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExerciseViewHolder(
        private val binding: ItemStretchExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(exercise: StretchExercise) {
            val context = binding.root.context
            binding.tvExName.text = exercise.name
            binding.tvExDuration.text =
                context.getString(R.string.stretch_seconds_label, exercise.durationSeconds)
            binding.tvExInstructions.text = exercise.instructions

            val position = bindingAdapterPosition
            binding.btnMoveUp.isEnabled = position > 0
            binding.btnMoveUp.alpha = if (position > 0) 1f else 0.3f
            binding.btnMoveDown.isEnabled = position < itemCount - 1
            binding.btnMoveDown.alpha = if (position < itemCount - 1) 1f else 0.3f

            binding.root.setOnClickListener { onEdit(exercise) }
            binding.btnMoveUp.setOnClickListener { onMoveUp(bindingAdapterPosition) }
            binding.btnMoveDown.setOnClickListener { onMoveDown(bindingAdapterPosition) }
            binding.btnDeleteEx.setOnClickListener { onDelete(exercise) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StretchExercise>() {
            override fun areItemsTheSame(oldItem: StretchExercise, newItem: StretchExercise) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: StretchExercise, newItem: StretchExercise) =
                oldItem == newItem
        }
    }
}
