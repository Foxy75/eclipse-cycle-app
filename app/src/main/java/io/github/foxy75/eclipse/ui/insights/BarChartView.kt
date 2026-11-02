package io.github.foxy75.eclipse.ui.insights

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import io.github.foxy75.eclipse.R

/**
 * Custom 6-cycle bar chart drawn on a single Canvas.
 * Displays Y-axis labels (20, 25, 30, 35), subtle gridlines, a dashed 28-day target line,
 * and up to 6 terracotta bars with top-rounded corners, cycle lengths on top, and 3-letter month labels below.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarItem(val monthLabel: String, val days: Int)

    private val items = ArrayList<BarItem>()
    private var targetCycle: Int = 28

    private val density = resources.displayMetrics.density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val targetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val yAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val monthTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val barRect = RectF()

    private var colorTerracotta = 0
    private var colorInk = 0
    private var colorInkSecondary = 0
    private var colorBorder = 0

    init {
        colorInk = ContextCompat.getColor(context, R.color.ink)
        colorInkSecondary = ContextCompat.getColor(context, R.color.ink_secondary)
        colorBorder = ContextCompat.getColor(context, R.color.border)

        val tv = TypedValue()
        context.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)
        colorTerracotta = if (tv.data != 0) tv.data else ContextCompat.getColor(context, R.color.terracotta)

        barPaint.style = Paint.Style.FILL
        barPaint.color = colorTerracotta

        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 1f * density
        gridPaint.color = Color.parseColor("#F4F4F0")

        targetLinePaint.style = Paint.Style.STROKE
        targetLinePaint.strokeWidth = 1f * density
        targetLinePaint.color = ContextCompat.getColor(context, R.color.border_muted)
        targetLinePaint.pathEffect = DashPathEffect(floatArrayOf(4f * density, 3f * density), 0f)

        val monospace = Typeface.create("monospace", Typeface.NORMAL)
        val monospaceBold = Typeface.create("monospace", Typeface.BOLD)

        yAxisPaint.color = colorInkSecondary
        yAxisPaint.textAlign = Paint.Align.RIGHT
        yAxisPaint.typeface = monospace
        yAxisPaint.textSize = 10f * density

        valueTextPaint.color = colorInk
        valueTextPaint.textAlign = Paint.Align.CENTER
        valueTextPaint.typeface = monospaceBold
        valueTextPaint.textSize = 11f * density

        monthTextPaint.color = colorInkSecondary
        monthTextPaint.textAlign = Paint.Align.CENTER
        monthTextPaint.typeface = monospace
        monthTextPaint.textSize = 11f * density
    }

    /** Set up to 6 cycle bar items with a target cycle line. */
    fun submitData(barItems: List<BarItem>, target: Int = 28) {
        items.clear()
        items.addAll(barItems.takeLast(6))
        targetCycle = target
        invalidate()
    }

    /** Overload for raw cycle lengths. */
    fun setData(values: List<Int>, median: Int) {
        items.clear()
        values.takeLast(6).forEachIndexed { index, days ->
            items.add(BarItem("C${index + 1}", days))
        }
        targetCycle = if (median > 0) median else 28
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bottomPadding = 24f * density
        val topPadding = 20f * density
        val yAxisWidth = 28f * density
        val leftGrid = paddingLeft + yAxisWidth
        val rightGrid = width - paddingRight.toFloat()
        val baseline = height - paddingBottom - bottomPadding
        val chartHeight = baseline - (paddingTop + topPadding)

        val yMin = 15f
        val yMax = 38f
        val yRange = yMax - yMin

        // Draw horizontal grid lines and Y-axis labels at 20, 25, 30, 35
        val yTicks = intArrayOf(20, 25, 30, 35)
        for (tick in yTicks) {
            val yPos = baseline - ((tick - yMin) / yRange) * chartHeight
            canvas.drawLine(leftGrid, yPos, rightGrid, yPos, gridPaint)
            canvas.drawText(tick.toString(), leftGrid - 6f * density, yPos + 3.5f * density, yAxisPaint)
        }

        // Draw dashed target line (e.g. 28)
        val targetY = baseline - ((targetCycle - yMin) / yRange) * chartHeight
        canvas.drawLine(leftGrid, targetY, rightGrid, targetY, targetLinePaint)

        // Draw baseline axis
        canvas.drawLine(leftGrid, baseline, rightGrid, baseline, gridPaint)

        if (items.isEmpty()) return

        val n = items.size
        val availableWidth = rightGrid - leftGrid
        val slotWidth = availableWidth / n.toFloat()
        val barWidth = minOf(slotWidth * 0.45f, 26f * density)
        val cornerRadius = 6f * density

        for (i in 0 until n) {
            val item = items[i]
            val days = item.days.coerceIn(yMin.toInt(), yMax.toInt())
            val barTop = baseline - ((days - yMin) / yRange) * chartHeight
            val barCenterX = leftGrid + slotWidth * i + slotWidth / 2f

            barRect.set(
                barCenterX - barWidth / 2f,
                barTop,
                barCenterX + barWidth / 2f,
                baseline
            )

            // Draw bar with top rounded corners
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)

            // Value text above bar
            canvas.drawText(item.days.toString(), barCenterX, barTop - 5f * density, valueTextPaint)

            // Month label below bar
            canvas.drawText(item.monthLabel, barCenterX, baseline + 16f * density, monthTextPaint)
        }
    }
}
