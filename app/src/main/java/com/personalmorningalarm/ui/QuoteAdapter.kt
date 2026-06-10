package com.personalmorningalarm.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personalmorningalarm.R
import com.personalmorningalarm.data.entity.BundledQuote
import com.personalmorningalarm.databinding.ItemQuoteBinding

/** Shows each quote (text + optional author); tapping a row edits it. */
class QuoteAdapter(
    private val onEdit: (BundledQuote) -> Unit
) : ListAdapter<BundledQuote, QuoteAdapter.QuoteViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val binding = ItemQuoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QuoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuoteViewHolder(
        private val binding: ItemQuoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(quote: BundledQuote) {
            binding.tvQuoteText.text = quote.quoteText
            val author = quote.author
            if (author.isNullOrBlank()) {
                binding.tvQuoteAuthor.visibility = View.GONE
            } else {
                binding.tvQuoteAuthor.visibility = View.VISIBLE
                binding.tvQuoteAuthor.text =
                    binding.root.context.getString(R.string.quote_author_format, author)
            }
            binding.root.setOnClickListener { onEdit(quote) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BundledQuote>() {
            override fun areItemsTheSame(oldItem: BundledQuote, newItem: BundledQuote) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BundledQuote, newItem: BundledQuote) =
                oldItem == newItem
        }
    }
}
