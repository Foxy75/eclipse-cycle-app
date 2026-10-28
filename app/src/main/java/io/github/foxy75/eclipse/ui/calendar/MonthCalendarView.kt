package io.github.foxy75.eclipse.ui.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Dates

/** Month data the calendar needs to render one month. */
data class CalendarData(
    val year: Int,
    val month: Int,
    val factDays: Set<Long>,
    val predictedDays: Set<Long>,
    val estimateDay: Long?,
    val today: Long
)

/**
 * Custom 7-column month grid (weekday header + day cells) drawn on a single
 * Canvas with reused Paint/RectF objects. Handles tap (detail) and long-press
 * (mark flow start) by hit-testing cell bounds.
 */
class MonthCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: CalendarData? = null
    private var onDayTap: ((Long) -> Unit)? = null
    private var onDayLongPress: ((Long) -> Unit)? = null

    private val density = resources.displayMetrics.density

    private val fillFact = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circleToday = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circlePrediction = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotEstimate = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()

    private val weekdays: Array<String>
    private var cellHeightPx: Float = 0f
    private var headerHeightPx: Float = 0f
    private var rows = 0
    private var leadingEmpty = 0
    private var daysInMonth = 0
    private var firstOfMonth = 0L

    private var colorInk = 0
    private var colorInkSecondary = 0
    private var colorTint = 0
    private var colorTerracotta = 0
    private var colorTeal = 0

    init {
        val t = resources.obtainTypedArray(R.array.weekdays_short)
        weekdays = Array(t.length()) { t.getString(it) ?: "" }
        t.recycle()

        val monospace = Typeface.create("monospace", Typeface.BOLD)
        colorInk = color(R.color.ink)
        colorInkSecondary = color(R.color.ink_secondary)
        colorTint = color(R.color.teal_tint)
        colorTerracotta = color(R.color.terracotta)
        colorTeal = color(R.color.teal)

        fillFact.style = Paint.Style.FILL
        fillFact.color = colorTerracotta

        circleToday.style = Paint.Style.STROKE
        circleToday.strokeWidth = 1.5f * density
        circleToday.color = colorInk

        circlePrediction.style = Paint.Style.STROKE
        circlePrediction.strokeWidth = 1.5f * density
        circlePrediction.color = colorTeal
        circlePrediction.pathEffect = DashPathEffect(floatArrayOf(6f * density, 5f * density), 0f)

        dotEstimate.style = Paint.Style.FILL
        dotEstimate.color = colorTeal

        textPaint.color = colorInk
        textPaint.typeface = monospace
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 14f * density

        headerPaint.color = colorInkSecondary
        headerPaint.textAlign = Paint.Align.CENTER
        headerPaint.textSize = 12f * density
        headerPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        cellHeightPx = 46f * density
        headerHeightPx = 30f * density
    }

    private fun color(id: Int): Int = resources.getColor(id, null)

    fun setData(data: CalendarData) {
        this.data = data
        computeLayout()
        invalidate()
    }

    fun setDayTapListener(l: (Long) -> Unit) { onDayTap = l }

    fun setDayLongPressListener(l: (Long) -> Unit) { onDayLongPress = l }

    private fun computeLayout() {
        val d = data ?: return
        firstOfMonth = Dates.of(d.year, d.month, 1)
        leadingEmpty = Dates.weekday(firstOfMonth) - 1
        daysInMonth = Dates.daysInMonth(d.year, d.month)
        rows = ((leadingEmpty + daysInMonth + 6) / 7).coerceAtLeast(1)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val desired = (headerHeightPx + rows * cellHeightPx + paddingTop + paddingBottom).toInt()
        val height = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY)
            MeasureSpec.getSize(heightMeasureSpec) else desired
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = data ?: return
        if (d.year == 0) return
        if (rows <= 0 || daysInMonth <= 0) return

        val width = width.toFloat()
        val cellW = width / 7f
        val top = paddingTop + headerHeightPx

        for (c in 0 until 7) {
            val x = c * cellW + cellW / 2f
            canvas.drawText(weekdays[c], x, headerHeightPx - 6f * density, headerPaint)
        }

        for (r in 0 until rows) {
            for (c in 0 until 7) {
                val dayIndex = r * 7 + c - leadingEmpty
                if (dayIndex < 0 || dayIndex >= daysInMonth) continue
                val day = firstOfMonth + dayIndex
                val cellCX = c * cellW + cellW / 2f
                val cellCY = top + r * cellHeightPx + cellHeightPx / 2f
                val radius = cellW * 0.33f

                val isFact = d.factDays.contains(day)
                val isPredicted = d.predictedDays.contains(day)
                val isToday = day == d.today
                val isEstimate = d.estimateDay == day

                if (isPredicted) {
                    bounds.set(cellCX - radius, cellCY - radius, cellCX + radius, cellCY + radius)
                    canvas.drawRoundRect(bounds, radius, radius, circlePrediction)
                }

                if (isFact) {
                    canvas.drawCircle(cellCX, cellCY, radius, fillFact)
                } else if (isToday) {
                    canvas.drawCircle(cellCX, cellCY, radius, circleToday)
                }

                textPaint.color = if (isFact) Color.WHITE else colorInk
                val baseline = cellCY - (textPaint.ascent() + textPaint.descent()) / 2f
                canvas.drawText((dayIndex + 1).toString(), cellCX, baseline, textPaint)

                if (isEstimate && !isFact) {
                    canvas.drawCircle(cellCX, cellCY + radius + 5f * density, 2.5f * density, dotEstimate)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (data == null) return super.onTouchEvent(event)
        if (width == 0) return super.onTouchEvent(event)

        val cellW = width / 7f
        val top = paddingTop + headerHeightPx

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val c = (event.x / cellW).toInt().coerceIn(0, 6)
                val r = ((event.y - top) / cellHeightPx).toInt()
                val dayIndex = r * 7 + c - leadingEmpty
                if (dayIndex >= 0 && dayIndex < daysInMonth) {
                    tapDay = firstOfMonth + dayIndex
                    tapX = event.x
                    tapY = event.y
                    longPressFired = false
                    handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS)
                } else {
                    tapDay = null
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val day = tapDay ?: return true
                val dx = event.x - tapX
                val dy = event.y - tapY
                if (dx * dx + dy * dy > 24f * 24f * density * density) cancelPress()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val day = tapDay
                if (day != null && !longPressFired) {
                    val dx = event.x - tapX
                    val dy = event.y - tapY
                    if (dx * dx + dy * dy < 24f * 24f * density * density) onDayTap?.invoke(day)
                }
                cancelPress()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelPress()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun cancelPress() {
        handler.removeCallbacks(longPressRunnable)
        tapDay = null
        longPressFired = false
    }

    private fun triggerLongPress() {
        val day = tapDay ?: return
        longPressFired = true
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        cancelPress()
        onDayLongPress?.invoke(day)
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val longPressRunnable = Runnable { triggerLongPress() }
    private var tapDay: Long? = null
    private var tapX = 0f
    private var tapY = 0f
    private var longPressFired = false

    companion object {
        private const val LONG_PRESS_TIMEOUT_MS = 500L
    }
}