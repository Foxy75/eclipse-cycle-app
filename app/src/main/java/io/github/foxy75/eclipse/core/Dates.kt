package io.github.foxy75.eclipse.core

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/**
 * Date arithmetic over epoch-day integers (days since 1970-01-01).
 * minSdk 23 has no java.time without desugaring, so we stay on java.util.
 */
object Dates {
    private const val MILLIS_PER_DAY = 86_400_000L
    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    fun of(year: Int, month: Int, dayOfMonth: Int): Long {
        val c = GregorianCalendar(UTC)
        c.clear()
        c.set(year, month - 1, dayOfMonth)
        return c.timeInMillis / MILLIS_PER_DAY
    }

    fun today(): Long {
        val c = Calendar.getInstance()
        return of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun parseIso(s: String): Long {
        val p = s.split("-")
        return of(p[0].toInt(), p[1].toInt(), p[2].toInt())
    }

    fun formatIso(day: Long): String {
        val c = calendar(day)
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun calendar(day: Long): Calendar {
        val c = GregorianCalendar(UTC)
        c.clear()
        c.timeInMillis = day * MILLIS_PER_DAY
        return c
    }

    fun year(day: Long): Int = calendar(day).get(Calendar.YEAR)

    fun month(day: Long): Int = calendar(day).get(Calendar.MONTH) + 1

    fun dayOfMonth(day: Long): Int = calendar(day).get(Calendar.DAY_OF_MONTH)

    fun weekday(day: Long): Int {
        val dow = calendar(day).get(Calendar.DAY_OF_WEEK)
        return if (dow == Calendar.SUNDAY) 7 else dow - 1
    }

    fun daysInMonth(year: Int, month: Int): Int {
        val c = GregorianCalendar(UTC)
        c.clear()
        c.set(year, month - 1, 1)
        return c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun startOfMonth(day: Long): Long = of(year(day), month(day), 1)

    fun addMonths(day: Long, delta: Int): Long {
        val y = year(day)
        val m = month(day)
        val d = dayOfMonth(day)
        val shifted = m - 1 + delta
        val ny = y + Math.floorDiv(shifted, 12)
        val nm = Math.floorMod(shifted, 12) + 1
        return of(ny, nm, minOf(d, daysInMonth(ny, nm)))
    }

    fun isSameDay(a: Long, b: Long): Boolean = a == b
}