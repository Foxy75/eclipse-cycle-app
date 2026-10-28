package io.github.foxy75.eclipse.ui.insights

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import io.github.foxy75.eclipse.R

/** Minimal bar chart of cycle lengths, drawn on a single Canvas with reused Paint/RectF. */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val lengths = ArrayList<Int>()
    private val density = resources.displayMetrics.density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val medPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var colorTerracotta = 0
    private var colorTeal = 0
    private var colorInkSecondary = 0

    init {
        colorTerracotta = resources.getColor(R.color.terracotta, null)
        colorTeal = resources.getColor(R.color.teal, null)
        colorInkSecondary = resources.getColor(R.color.ink_secondary, null)

        barPaint.style = Paint.Style.FILL
        barPaint.color = colorTerracotta

        medPaint.style = Paint.Style.FILL
        medPaint.color = colorTeal

        labelPaint.color = colorInkSecondary
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.typeface = Typeface.create("monospace", Typeface.NORMAL)
        labelPaint.textSize = 11f * density

        axisPaint.style = Paint.Style.STROKE
        axisPaint.strokeWidth = 1f * density
        axisPaint.color = colorInkSecondary
    }

    fun setData(values: List<Int>, median: Int) {
        lengths.clear()
        lengths.addAll(values)
        this.median = median
        invalidate()
    }

    private var median = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (lengths.isEmpty()) return

        val labelSpace = 20f * density
        val baseline = height - paddingBottom - labelSpace
        val topPad = paddingTop + 6f * density
        val maxValue = (lengths.maxOrNull() ?: 1).coerceAtLeast(1)
        val n = lengths.size
        val slot = width / n.toFloat()
        val barW = slot * 0.5f

        canvas.drawLine(
            paddingLeft.toFloat(), baseline,
            width - paddingRight.toFloat(), baseline,
            axisPaint
        )

        for (i in 0 until n) {
            val v = lengths[i]
            val h = (v.toFloat() / maxValue) * (baseline - topPad)
            val cx = paddingLeft + slot * i + slot / 2f
            val bx = cx - barW / 2f
            val by = baseline - h
            rect.set(bx, by, cx + barW / 2f, baseline)
            val useMedian = v == median
            canvas.drawRoundRect(rect, 6f * density, 6f * density, if (useMedian) medPaint else barPaint)
            canvas.drawText(v.toString(), cx, by - 6f * density, labelPaint)
        }
    }
}