package com.personalmorningalarm.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personalmorningalarm.R
import com.personalmorningalarm.data.model.ShoppingListSummaryDto
import com.personalmorningalarm.databinding.ItemShoppingListBinding

/** One row of the shopping list-picker menu: title + item counts, tap to open. */
class ShoppingListSummaryAdapter(
    private val onClick: (ShoppingListSummaryDto) -> Unit
) : ListAdapter<ShoppingListSummaryDto, ShoppingListSummaryAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemShoppingListBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(private val binding: ItemShoppingListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: ShoppingListSummaryDto) {
            binding.tvShoppingListTitle.text = summary.title ?: summary.id.orEmpty()
            val total = summary.total ?: 0
            val unticked = summary.unticked ?: total
            binding.tvShoppingListCount.text =
                binding.root.resources.getString(R.string.shopping_list_item_count, unticked, total)
            binding.root.setOnClickListener { onClick(summary) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ShoppingListSummaryDto>() {
            override fun areItemsTheSame(old: ShoppingListSummaryDto, new: ShoppingListSummaryDto): Boolean =
                old.id == new.id

            override fun areContentsTheSame(old: ShoppingListSummaryDto, new: ShoppingListSummaryDto): Boolean =
                old == new
        }
    }
}
