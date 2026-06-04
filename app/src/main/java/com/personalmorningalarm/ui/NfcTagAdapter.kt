package com.personalmorningalarm.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personalmorningalarm.data.entity.NfcTag
import com.personalmorningalarm.databinding.ItemNfcTagBinding

/** Shows registered NFC tags (label + location) with a per-row delete action. */
class NfcTagAdapter(
    private val onDelete: (NfcTag) -> Unit
) : ListAdapter<NfcTag, NfcTagAdapter.TagViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemNfcTagBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TagViewHolder(
        private val binding: ItemNfcTagBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tag: NfcTag) {
            binding.tvLabel.text = tag.label
            binding.tvLocation.text = tag.location
            binding.btnDelete.setOnClickListener { onDelete(tag) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NfcTag>() {
            override fun areItemsTheSame(oldItem: NfcTag, newItem: NfcTag) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NfcTag, newItem: NfcTag) =
                oldItem == newItem
        }
    }
}
