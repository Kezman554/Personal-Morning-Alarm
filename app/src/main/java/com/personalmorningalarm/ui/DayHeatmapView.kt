package com.personalmorningalarm.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.personalmorningalarm.R
import kotlin.math.ceil

/**
 * Calendar-style heatmap: one rounded square per day, coloured by outcome
 * (green = Stage 2 success, red = missed, faint grey = no alarm). Fixed 7-column
 * grid, oldest day top-left. Custom canvas — no charting dependency.
 */
class DayHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var days: List<DayCell> = emptyList()

    private val successColor = ContextCompat.getColor(context, R.color.stat_success)
    private val failureColor = ContextCompat.getColor(context, R.color.stat_failure)
    private val emptyColor = ContextCompat.getColor(context, R.color.stat_empty)

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cellRect = RectF()

    private val gap = dp(4f)

    fun setData(days: List<DayCell>) {
        this.days = days
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(dp(280f).toInt(), widthMeasureSpec)
        // Square cells: derive height from the resolved width and the row count.
        val rows = if (days.isEmpty()) 1 else ceil(days.size / COLUMNS.toFloat()).toInt()
        val cell = (width - paddingLeft - paddingRight - gap * (COLUMNS - 1)) / COLUMNS
        val desiredHeight = (paddingTop + paddingBottom + cell * rows + gap * (rows - 1)).toInt()
        setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        if (days.isEmpty()) return

        val cell = (width - paddingLeft - paddingRight - gap * (COLUMNS - 1)) / COLUMNS
        val corner = dp(4f)

        days.forEachIndexed { i, day ->
            val col = i % COLUMNS
            val row = i / COLUMNS
            val left = paddingLeft + col * (cell + gap)
            val top = paddingTop + row * (cell + gap)
            cellPaint.color = when (day.outcome) {
                DayOutcome.SUCCESS -> successColor
                DayOutcome.FAILURE -> failureColor
                DayOutcome.NONE -> emptyColor
            }
            cellRect.set(left, top, left + cell, top + cell)
            canvas.drawRoundRect(cellRect, corner, corner, cellPaint)
        }
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    companion object {
        private const val COLUMNS = 7
    }
}
