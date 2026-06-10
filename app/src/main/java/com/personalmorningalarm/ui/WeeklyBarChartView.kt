package com.personalmorningalarm.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.personalmorningalarm.R

/**
 * Lightweight bar chart for the weekly success rate — one bar per [WeekBar],
 * height proportional to the success fraction. Custom canvas (no charting
 * dependency) to keep the APK lean; the data set is tiny and fixed-width.
 */
class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var weeks: List<WeekBar> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.stat_empty)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(com.google.android.material.R.attr.colorOnSurface)
        textAlign = Paint.Align.CENTER
        textSize = sp(12f)
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = themeColor(com.google.android.material.R.attr.colorOnSurface)
        textAlign = Paint.Align.CENTER
        textSize = sp(12f)
        typeface = Typeface.DEFAULT_BOLD
    }

    private val barColor = themeColor(com.google.android.material.R.attr.colorPrimary)
    private val barRect = RectF()

    fun setData(weeks: List<WeekBar>) {
        this.weeks = weeks
        requestLayout() // labels/desired height unaffected, but keep invalidate simple
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(dp(280f).toInt(), widthMeasureSpec)
        val height = resolveSize(dp(180f).toInt(), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (weeks.isEmpty()) return

        val valueGap = sp(16f)      // space above bars for the % label
        val labelGap = sp(18f)      // space below bars for the date label
        val chartTop = paddingTop + valueGap
        val chartBottom = height - paddingBottom - labelGap
        val chartHeight = chartBottom - chartTop
        if (chartHeight <= 0) return

        val slot = (width - paddingLeft - paddingRight) / weeks.size.toFloat()
        val barWidth = slot * 0.5f
        val corner = dp(4f)

        weeks.forEachIndexed { i, week ->
            val cx = paddingLeft + slot * i + slot / 2f
            val left = cx - barWidth / 2f
            val right = cx + barWidth / 2f

            // Faint full-height track so empty weeks still occupy the slot.
            barRect.set(left, chartTop, right, chartBottom)
            canvas.drawRoundRect(barRect, corner, corner, trackPaint)

            // Filled portion = success rate. Weeks with no attempts draw nothing.
            if (week.attemptedDays > 0 && week.rate > 0f) {
                val top = chartBottom - chartHeight * week.rate
                barRect.set(left, top, right, chartBottom)
                barPaint.color = barColor
                canvas.drawRoundRect(barRect, corner, corner, barPaint)
            }

            // Value label: "3/5" when attempted, "—" otherwise.
            val valueText = if (week.attemptedDays > 0) {
                context.getString(R.string.stats_week_rate, week.successDays, week.attemptedDays)
            } else {
                "—"
            }
            canvas.drawText(valueText, cx, chartTop - sp(4f), valuePaint)

            // Week label below the chart.
            canvas.drawText(week.label, cx, chartBottom + labelGap - sp(4f), labelPaint)
        }
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun sp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    /** Resolves a theme color attr to a color int (handles both color and resource refs). */
    private fun themeColor(attrRes: Int): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(attrRes, tv, true)
        return if (tv.resourceId != 0) ContextCompat.getColor(context, tv.resourceId) else tv.data
    }
}
