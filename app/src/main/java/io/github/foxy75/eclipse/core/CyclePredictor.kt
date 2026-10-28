package io.github.foxy75.eclipse.core

/** Inclusive predicted flow window. */
data class PeriodRange(val start: Long, val end: Long)

data class Prediction(
    val stable: Boolean,
    val completedLengths: List<Int>,
    val medianLength: Int,
    val avgLength: Double,
    val lengthRange: IntRange,
    val avgPeriodLength: Int,
    val nextStart: Long?,
    val predictedPeriods: List<PeriodRange>
) {
    companion object {
        fun empty(): Prediction = Prediction(
            stable = false,
            completedLengths = emptyList(),
            medianLength = 0,
            avgLength = 0.0,
            lengthRange = 0..0,
            avgPeriodLength = 0,
            nextStart = null,
            predictedPeriods = emptyList()
        )
    }
}

/**
 * Locally computes the next cycle using the median of the last (up to) 6 completed
 * cycles, a fixed 14-day luteal phase, and flags instability when MAD > 9 days or
 * there are fewer than 2 completed cycles.
 */
object CyclePredictor {
    const val LUTEAL_DAYS = 14L
    const val MIN_COMPLETED = 2
    const val MAX_CYCLES = 6
    const val MAD_THRESHOLD = 9

    fun predict(flowDays: Collection<Long>): Prediction {
        val cycles = CycleBuilder.build(flowDays)
        val starts = cycles.map { it.start }
        if (starts.size < 2) return Prediction.empty()

        val lengths = ArrayList<Int>()
        for (i in 0 until starts.size - 1) {
            lengths.add((starts[i + 1] - starts[i]).toInt())
        }
        if (lengths.size < MIN_COMPLETED) return Prediction.empty()

        val periodLengths = cycles.map { it.periodLength }
        val recentLengths = lengths.takeLast(MAX_CYCLES)

        val median = medianOf(recentLengths)
        val mad = medianOf(recentLengths.map { Math.abs(it - median) })
        val stable = mad <= MAD_THRESHOLD

        val avgPeriod = medianOf(periodLengths.takeLast(MAX_CYCLES))
        val avg = recentLengths.average()
        val range = recentLengths.minOrNull()!!..recentLengths.maxOrNull()!!

        if (!stable) {
            return Prediction(
                stable = false,
                completedLengths = lengths,
                medianLength = median,
                avgLength = avg,
                lengthRange = range,
                avgPeriodLength = avgPeriod,
                nextStart = null,
                predictedPeriods = emptyList()
            )
        }

        val lastStart = starts.last()
        val nextStart = lastStart + median
        val predicted = ArrayList<PeriodRange>(3)
        for (i in 1..3) {
            val s = lastStart + median.toLong() * i
            predicted.add(PeriodRange(s, s + (avgPeriod - 1).toLong()))
        }

        return Prediction(
            stable = true,
            completedLengths = lengths,
            medianLength = median,
            avgLength = avg,
            lengthRange = range,
            avgPeriodLength = avgPeriod,
            nextStart = nextStart,
            predictedPeriods = predicted
        )
    }

    fun medianOf(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val s = values.sorted()
        val n = s.size
        val mid = n / 2
        return if (n % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2
    }
}