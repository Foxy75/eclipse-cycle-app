package io.github.foxy75.eclipse.ui.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Dates
import java.util.Locale

/** Month data the calendar needs to render one month. */
data class CalendarData(
    val year: Int,
    val month: Int,
    val factDays: Set<Long>,
    val predictedDays: Set<Long>,
    val estimateDay: Long?,
    val today: Long,
    val selectedDay: Long = today
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
    private val fillPredicted = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePredicted = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringSelected = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotEstimate = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val cellBox = RectF()
    private val outerRingBox = RectF()

    private val weekdays: Array<String>
    private var cellHeightPx: Float = 0f
    private var headerHeightPx: Float = 0f
    private var rows = 0
    private var leadingEmpty = 0
    private var daysInMonth = 0
    private var firstOfMonth = 0L

    private var colorInk = 0
    private var colorInkSecondary = 0
    private var colorTerracotta = 0
    private var colorTeal = 0
    private var colorPredictedBg = 0

    init {
        val t = resources.obtainTypedArray(R.array.weekdays_short)
        weekdays = Array(t.length()) { t.getString(it) ?: "" }
        t.recycle()

        colorInk = ContextCompat.getColor(context, R.color.ink)
        colorInkSecondary = ContextCompat.getColor(context, R.color.ink_secondary)

        val tv = TypedValue()
        val theme = context.theme

        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)
        colorTerracotta = if (tv.data != 0) tv.data else ContextCompat.getColor(context, R.color.terracotta)

        theme.resolveAttribute(com.google.android.material.R.attr.colorSecondary, tv, true)
        colorTeal = if (tv.data != 0) tv.data else ContextCompat.getColor(context, R.color.teal)

        colorPredictedBg = ContextCompat.getColor(context, R.color.pred_cell_bg)

        fillFact.style = Paint.Style.FILL
        fillFact.color = colorTerracotta

        fillPredicted.style = Paint.Style.FILL
        fillPredicted.color = colorPredictedBg

        strokePredicted.style = Paint.Style.STROKE
        strokePredicted.strokeWidth = 1.2f * density
        strokePredicted.color = colorTerracotta
        strokePredicted.pathEffect = DashPathEffect(floatArrayOf(4f * density, 3f * density), 0f)

        ringSelected.style = Paint.Style.STROKE
        ringSelected.strokeWidth = 1.5f * density
        ringSelected.color = colorInk

        dotEstimate.style = Paint.Style.FILL
        dotEstimate.color = colorTeal

        val monospace = Typeface.create("monospace", Typeface.BOLD)
        textPaint.typeface = monospace
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 13f * density

        headerPaint.color = colorInkSecondary
        headerPaint.textAlign = Paint.Align.CENTER
        headerPaint.textSize = 11f * density
        headerPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        cellHeightPx = 48f * density
        headerHeightPx = 28f * density
    }

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

        // Draw weekday headers
        for (c in 0 until 7) {
            val x = c * cellW + cellW / 2f
            canvas.drawText(weekdays[c], x, headerHeightPx - 6f * density, headerPaint)
        }

        val boxHalf = minOf(cellW * 0.44f, 19f * density)
        val boxRadius = 10f * density
        val outerRadius = 12f * density
        val outerInset = 2f * density

        for (r in 0 until rows) {
            for (c in 0 until 7) {
                val dayIndex = r * 7 + c - leadingEmpty
                if (dayIndex < 0 || dayIndex >= daysInMonth) continue
                val day = firstOfMonth + dayIndex
                val cellCX = c * cellW + cellW / 2f
                val cellCY = top + r * cellHeightPx + cellHeightPx / 2f

                val isFact = d.factDays.contains(day)
                val isPredicted = d.predictedDays.contains(day)
                val isSelected = day == d.selectedDay
                val isEstimate = d.estimateDay == day

                cellBox.set(
                    cellCX - boxHalf,
                    cellCY - boxHalf,
                    cellCX + boxHalf,
                    cellCY + boxHalf
                )

                // 1. Draw cell background / border
                if (isFact) {
                    canvas.drawRoundRect(cellBox, boxRadius, boxRadius, fillFact)
                } else if (isPredicted) {
                    canvas.drawRoundRect(cellBox, boxRadius, boxRadius, fillPredicted)
                    canvas.drawRoundRect(cellBox, boxRadius, boxRadius, strokePredicted)
                }

                // 2. Draw outer ring for selected day
                if (isSelected) {
                    outerRingBox.set(
                        cellBox.left - outerInset,
                        cellBox.top - outerInset,
                        cellBox.right + outerInset,
                        cellBox.bottom + outerInset
                    )
                    canvas.drawRoundRect(outerRingBox, outerRadius, outerRadius, ringSelected)
                }

                // 3. Draw day number text (2 digits: "01".."31")
                textPaint.color = when {
                    isFact -> Color.WHITE
                    isPredicted -> colorTerracotta
                    else -> colorInk
                }
                val numText = String.format(Locale.US, "%02d", dayIndex + 1)
                val baseline = cellCY - (textPaint.ascent() + textPaint.descent()) / 2f - (if (isEstimate && !isFact) 2f * density else 0f)
                canvas.drawText(numText, cellCX, baseline, textPaint)

                // 4. Draw estimate teal dot below number
                if (isEstimate && !isFact) {
                    val dotY = cellCY + boxHalf - 4.5f * density
                    canvas.drawCircle(cellCX, dotY, 2f * density, dotEstimate)
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
                if (dayIndex in 0 until daysInMonth) {
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
