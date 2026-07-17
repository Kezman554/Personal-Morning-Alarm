package com.personalmorningalarm.ui

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import com.personalmorningalarm.R
import com.personalmorningalarm.data.model.ScheduleGroup
import com.personalmorningalarm.data.model.SchedulePeriod
import com.personalmorningalarm.util.VaultText

/**
 * Renders Alfred's daily schedule as a bold period heading followed by that
 * period's tasks.
 *
 * Shared by every screen that shows the schedule — the Stage 2 content screen and
 * the Today screen — so the schedule can't drift into looking like two different
 * things depending on where you meet it.
 */
object ScheduleRenderer {

    fun format(context: Context, groups: List<ScheduleGroup>): CharSequence {
        val out = SpannableStringBuilder()
        groups.forEach { group ->
            if (out.isNotEmpty()) out.append("\n\n")
            val start = out.length
            out.append(context.getString(periodHeading(group.period)))
            out.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                out.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            group.tasks.forEach { task ->
                out.append("\n")
                // Build the bullet around the rendered task so its emphasis spans
                // survive — getString would flatten them to plain text.
                out.append("• ").append(VaultText.render(task))
            }
        }
        return out
    }

    @StringRes
    fun periodHeading(period: SchedulePeriod): Int = when (period) {
        SchedulePeriod.MORNING -> R.string.schedule_period_morning
        SchedulePeriod.AFTERNOON -> R.string.schedule_period_afternoon
        SchedulePeriod.EVENING -> R.string.schedule_period_evening
        SchedulePeriod.UNGROUPED -> R.string.schedule_period_ungrouped
    }
}
