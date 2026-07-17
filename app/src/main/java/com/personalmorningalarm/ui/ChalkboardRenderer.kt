package com.personalmorningalarm.ui

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.annotation.ColorInt
import com.personalmorningalarm.data.model.RollingTodoItem
import com.personalmorningalarm.util.VaultText

/**
 * Renders the rolling to-do as a read-only checklist: an unticked box per item —
 * Alfred sends only unchecked ones, and nothing ticks them off — with the item's
 * date on a faint second line when it has one.
 *
 * Shared by the Stage 2 content screen and the Today screen. [dateColor] is the
 * one thing that differs between them: the alarm panel is white-on-blue, the
 * Today screen follows the app theme.
 */
object ChalkboardRenderer {

    private const val BOX = "☐  "

    /** Indents the date under its task rather than under the box. */
    private const val DATE_INDENT = "     "

    fun format(items: List<RollingTodoItem>, @ColorInt dateColor: Int): CharSequence {
        val out = SpannableStringBuilder()
        items.forEach { item ->
            if (out.isNotEmpty()) out.append("\n")
            // Build the line around the rendered task so its emphasis spans survive.
            out.append(BOX).append(VaultText.render(item.task))
            item.date?.let { date ->
                out.append("\n")
                val start = out.length
                out.append(DATE_INDENT).append(date)
                // Secondary — present, but not competing with the task itself.
                out.setSpan(RelativeSizeSpan(0.8f), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.setSpan(ForegroundColorSpan(dateColor), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return out
    }
}
