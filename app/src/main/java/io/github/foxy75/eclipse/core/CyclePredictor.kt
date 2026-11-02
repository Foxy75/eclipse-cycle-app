package io.github.foxy75.eclipse.core

import kotlin.math.abs
import kotlin.math.roundToInt

/** Inclusive predicted flow or fertility window. */
data class PeriodRange(val start: Long, val end: Long)

enum class CycleRegularity {
    REGULAR,    // MAD <= 3 days (FIGO standard)
    VARIABLE,   // 3 < MAD <= 7 days
    IRREGULAR   // MAD > 7 days
}

data class Prediction(
    val stable: Boolean,
    val completedLengths: List<Int>,      // Raw empirical lengths for UI & charts
    val medianLength: Int,
    val avgLength: Double,
    val lengthRange: IntRange,
    val avgPeriodLength: Int,
    val nextStart: Long?,
    val predictedPeriods: List<PeriodRange>,
    // --- Top-Tier Extensions ---
    val earliestNextStart: Long? = null,
    val latestNextStart: Long? = null,
    val ovulationDay: Long? = null,
    val fertileWindow: PeriodRange? = null,
    val regularity: CycleRegularity = CycleRegularity.REGULAR,
    val confidencePercent: Int = 85,
    val hasSufficientData: Boolean = false,
    val usedBaseline: Boolean = false
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
            predictedPeriods = emptyList(),
            regularity = CycleRegularity.VARIABLE,
            confidencePercent = 0,
            hasSufficientData = false,
            usedBaseline = false
        )
    }
}

/**
 * Top-tier Offline Menstrual Cycle Predictor conforming to clinical reproductive
 * research (FIGO, Wilcox, Bull et al., Lenton, Li & Elhadad).
 */
object CyclePredictor {

    const val DEFAULT_CYCLE_LENGTH = 28
    const val DEFAULT_PERIOD_LENGTH = 5
    const val MIN_PHYSIOLOGICAL_CYCLE = 18
    const val MAX_PHYSIOLOGICAL_CYCLE = 54
    const val RECENCY_DECAY = 0.85

    fun predict(
        flowDays: Collection<Long>,
        baselineCycleLength: Int? = null,
        baselinePeriodLength: Int? = null
    ): Prediction {
        val priorCycle = baselineCycleLength?.coerceIn(21, 45) ?: DEFAULT_CYCLE_LENGTH
        val priorPeriod = baselinePeriodLength?.coerceIn(3, 8) ?: DEFAULT_PERIOD_LENGTH

        val cycles = CycleBuilder.build(flowDays)
        val starts = cycles.map { it.start }

        // Case 0: No periods recorded at all
        if (starts.isEmpty()) {
            return Prediction.empty()
        }

        val lastStart = starts.last()

        // Extract empirical completed lengths between recorded consecutive period starts
        val rawLengths = ArrayList<Int>()
        for (i in 0 until starts.size - 1) {
            rawLengths.add((starts[i + 1] - starts[i]).toInt())
        }

        val periodLengths = cycles.map { it.periodLength }
        val avgPeriod = if (periodLengths.isNotEmpty()) {
            medianOf(periodLengths.takeLast(6)).coerceIn(2, 10)
        } else {
            priorPeriod
        }

        // Case 1: Cold start (0 or 1 period recorded; no completed cycles yet)
        if (rawLengths.isEmpty()) {
            val nextStart = lastStart + priorCycle
            val lutealDays = estimateLutealPhase(priorCycle)
            val ovulation = nextStart - lutealDays
            val fertile = PeriodRange(ovulation - 5, ovulation)
            val predicted = generateHorizons(lastStart, priorCycle, priorPeriod, 3)

            return Prediction(
                stable = true,
                completedLengths = emptyList(),
                medianLength = priorCycle,
                avgLength = priorCycle.toDouble(),
                lengthRange = priorCycle..priorCycle,
                avgPeriodLength = priorPeriod,
                nextStart = nextStart,
                predictedPeriods = predicted,
                earliestNextStart = nextStart - 2,
                latestNextStart = nextStart + 2,
                ovulationDay = ovulation,
                fertileWindow = fertile,
                regularity = CycleRegularity.REGULAR,
                confidencePercent = 55, // Prior confidence baseline
                hasSufficientData = false,
                usedBaseline = true
            )
        }

        // Case 2: One or more completed cycles available
        // Prepare statistical sample (cleansed of tracking artifacts/skipped cycles)
        val statisticalLengths = cleanForStatistics(rawLengths, priorCycle)

        val estimatedCycle = computeBayesianCycle(statisticalLengths, priorCycle)
        val minLen = rawLengths.takeLast(6).minOrNull() ?: estimatedCycle
        val maxLen = rawLengths.takeLast(6).maxOrNull() ?: estimatedCycle

        // Calculate MAD (Median Absolute Deviation) on statistical sample
        val mad = medianOf(statisticalLengths.takeLast(6).map { abs(it - estimatedCycle) }).coerceAtLeast(1)
        val rangeDiff = maxLen - minLen

        // FIGO 2011 guidelines: Normal cycle variation is <= 7-9 days.
        // Variation > 14 days or MAD > 7 indicates clinically irregular cycle.
        val regularity = when {
            rangeDiff > 14 || mad > 7 -> CycleRegularity.IRREGULAR
            rangeDiff > 8 || mad > 3 -> CycleRegularity.VARIABLE
            else -> CycleRegularity.REGULAR
        }

        // Mark stable unless irregularity is high with limited data
        val stable = regularity != CycleRegularity.IRREGULAR || rawLengths.size >= 3

        val nextStart = lastStart + estimatedCycle
        val uncertaintyDays = (1.96 * mad * 0.6745).roundToInt().coerceIn(2, 9)
        val earliest = nextStart - uncertaintyDays
        val latest = nextStart + uncertaintyDays

        val lutealDays = estimateLutealPhase(estimatedCycle)
        val ovulation = nextStart - lutealDays
        val fertile = PeriodRange(ovulation - 5, ovulation)

        val predictedPeriods = generateHorizons(lastStart, estimatedCycle, avgPeriod, 3)

        // Calibrated confidence score
        val dataScore = (rawLengths.size.toDouble() / 5.0).coerceAtMost(1.0)
        val stabilityScore = (10 - mad.coerceAtMost(10)) / 10.0
        val confidence = ((dataScore * 0.65 + stabilityScore * 0.35) * 100).roundToInt().coerceIn(45, 96)

        return Prediction(
            stable = stable,
            completedLengths = rawLengths, // Maintain truthful empirical logs for UI/Charts
            medianLength = estimatedCycle,
            avgLength = rawLengths.average(),
            lengthRange = minLen..maxLen,
            avgPeriodLength = avgPeriod,
            nextStart = nextStart,
            predictedPeriods = predictedPeriods,
            earliestNextStart = earliest,
            latestNextStart = latest,
            ovulationDay = ovulation,
            fertileWindow = fertile,
            regularity = regularity,
            confidencePercent = confidence,
            hasSufficientData = rawLengths.size >= 2,
            usedBaseline = rawLengths.size < 3
        )
    }

    /**
     * Cleans tracking artifacts (missed period logging) strictly for statistical estimation,
     * without mutating raw observed history.
     */
    private fun cleanForStatistics(raw: List<Int>, baseline: Int): List<Int> {
        val out = ArrayList<Int>()
        for (len in raw) {
            val ratio = len.toDouble() / baseline.toDouble()
            when {
                // Probable single missed cycle (~2x baseline)
                ratio in 1.80..2.25 -> {
                    val half = (len / 2).coerceIn(MIN_PHYSIOLOGICAL_CYCLE, MAX_PHYSIOLOGICAL_CYCLE)
                    out.add(half)
                }
                // Within plausible human physiological bounds
                len in MIN_PHYSIOLOGICAL_CYCLE..MAX_PHYSIOLOGICAL_CYCLE -> {
                    out.add(len)
                }
                // Outlier: bound to limits
                len > MAX_PHYSIOLOGICAL_CYCLE -> {
                    out.add(MAX_PHYSIOLOGICAL_CYCLE)
                }
                else -> {
                    out.add(MIN_PHYSIOLOGICAL_CYCLE)
                }
            }
        }
        return if (out.isEmpty()) listOf(baseline) else out
    }

    /**
     * Conjugate normal Bayesian estimate with exponential recency decay.
     */
    private fun computeBayesianCycle(lengths: List<Int>, prior: Int): Int {
        val recent = lengths.takeLast(6)
        if (recent.isEmpty()) return prior

        // Prior weight attenuates as empirical observations grow
        val priorWeight = (2.5 - 0.4 * recent.size).coerceAtLeast(0.3)
        var totalWeight = priorWeight
        var weightedSum = prior * priorWeight

        var decay = 1.0
        for (i in recent.indices.reversed()) {
            val len = recent[i]
            weightedSum += len * decay
            totalWeight += decay
            decay *= RECENCY_DECAY
        }

        return (weightedSum / totalWeight).roundToInt().coerceIn(MIN_PHYSIOLOGICAL_CYCLE, MAX_PHYSIOLOGICAL_CYCLE)
    }

    /**
     * Dynamic Luteal Phase Scaling anchored on Bull et al. 2019 (mean: 12.4 days).
     */
    fun estimateLutealPhase(cycleLength: Int): Long {
        return when {
            cycleLength <= 24 -> 11L
            cycleLength in 25..27 -> 12L
            cycleLength in 28..31 -> 13L
            cycleLength in 32..35 -> 14L
            else -> 15L
        }
    }

    private fun generateHorizons(
        lastStart: Long,
        cycleLen: Int,
        periodLen: Int,
        count: Int
    ): List<PeriodRange> {
        val list = ArrayList<PeriodRange>(count)
        for (i in 1..count) {
            val s = lastStart + cycleLen.toLong() * i
            list.add(PeriodRange(s, s + (periodLen - 1).toLong()))
        }
        return list
    }

    fun medianOf(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val s = values.sorted()
        val n = s.size
        val mid = n / 2
        return if (n % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2
    }
}
