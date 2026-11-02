package io.github.foxy75.eclipse.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CyclePredictorTest {

    private fun day(iso: String): Long = Dates.parseIso(iso)

    private fun fromStarts(starts: List<Long>, periodLen: Int): List<Long> {
        val out = ArrayList<Long>()
        starts.forEach { s -> (0 until periodLen).forEach { out.add(s + it) } }
        return out
    }

    @Test
    fun builds_cycles_from_flow_days() {
        val days = fromStarts(
            listOf(day("2025-01-01"), day("2025-01-29"), day("2025-02-26")), 3
        )
        val cycles = CycleBuilder.build(days)
        assertEquals(3, cycles.size)
        assertEquals(day("2025-01-01"), cycles[0].start)
        assertEquals(day("2025-01-29"), cycles[1].start)
        assertEquals(day("2025-02-26"), cycles[2].start)
    }

    @Test
    fun gap_below_threshold_stays_same_cycle() {
        val days = listOf("2025-01-01", "2025-01-02", "2025-01-05").map { day(it) }
        val cycles = CycleBuilder.build(days)
        assertEquals(1, cycles.size)
        assertEquals(5, cycles[0].periodLength)
    }

    @Test
    fun gap_above_threshold_starts_new_cycle() {
        val days = fromStarts(listOf(day("2025-01-01"), day("2025-01-20")), 1)
        val cycles = CycleBuilder.build(days)
        assertEquals(2, cycles.size)
    }

    @Test
    fun cold_start_uses_baseline_prior() {
        val start = day("2025-01-01")
        val days = fromStarts(listOf(start), 4)
        val pred = CyclePredictor.predict(days, baselineCycleLength = 30, baselinePeriodLength = 4)

        assertTrue(pred.usedBaseline)
        assertEquals(start + 30, pred.nextStart)
        assertEquals(30, pred.medianLength)
        assertEquals(4, pred.avgPeriodLength)
        assertNotNull(pred.ovulationDay)
        assertNotNull(pred.fertileWindow)
        assertEquals(pred.nextStart!! - 13L, pred.ovulationDay)
        assertEquals(pred.ovulationDay!! - 5, pred.fertileWindow!!.start)
        assertEquals(pred.ovulationDay, pred.fertileWindow!!.end)
    }

    @Test
    fun stable_prediction_with_recency_weighting() {
        val days = fromStarts((0..6).map { day("2025-01-01") + 28L * it }, 5)
        val pred = CyclePredictor.predict(days)

        assertTrue(pred.stable)
        assertEquals(28, pred.medianLength)
        assertEquals(5, pred.avgPeriodLength)
        assertEquals(day("2025-01-01") + 28L * 7, pred.nextStart)
        assertEquals(CycleRegularity.REGULAR, pred.regularity)
        assertTrue(pred.confidencePercent >= 80)
    }

    @Test
    fun dynamic_luteal_scaling() {
        assertEquals(11L, CyclePredictor.estimateLutealPhase(23))
        assertEquals(12L, CyclePredictor.estimateLutealPhase(26))
        assertEquals(13L, CyclePredictor.estimateLutealPhase(28))
        assertEquals(14L, CyclePredictor.estimateLutealPhase(33))
        assertEquals(15L, CyclePredictor.estimateLutealPhase(38))
    }

    @Test
    fun wilcox_fertile_window_and_phase_resolution() {
        val starts = listOf(day("2025-01-01"), day("2025-01-29"))
        val cycles = CycleBuilder.build(fromStarts(starts, 5))
        val pred = CyclePredictor.predict(fromStarts(starts, 5), 28, 5)

        val nextStart = pred.nextStart!!
        val ovulation = pred.ovulationDay!!
        val fertileStart = pred.fertileWindow!!.start

        assertEquals(nextStart - 13L, ovulation)
        assertEquals(ovulation - 5L, fertileStart)

        // Test phase mapping
        assertEquals(Phase.MENSTRUAL, Phases.phaseFor(day("2025-01-01"), cycles, pred))
        assertEquals(Phase.MENSTRUAL, Phases.phaseFor(day("2025-01-29"), cycles, pred))
        assertEquals(Phase.FERTILE, Phases.phaseFor(ovulation, cycles, pred))
        assertEquals(Phase.FERTILE, Phases.phaseFor(fertileStart, cycles, pred))
        assertEquals(Phase.LUTEAL, Phases.phaseFor(ovulation + 2, cycles, pred))
    }

    @Test
    fun irregular_cycles_classified_correctly() {
        val base = day("2025-01-01")
        val starts = listOf(base, base + 21, base + 48, base + 85, base + 110)
        val pred = CyclePredictor.predict(fromStarts(starts, 4), baselineCycleLength = 28)

        assertEquals(CycleRegularity.IRREGULAR, pred.regularity)
        assertNotNull(pred.nextStart)
        assertNotNull(pred.earliestNextStart)
        assertNotNull(pred.latestNextStart)
        assertTrue(pred.latestNextStart!! > pred.earliestNextStart!!)
    }

    @Test
    fun missed_cycle_artifact_filtered_for_statistics_preserving_observed() {
        val base = day("2025-01-01")
        // User logged Jan 1, Jan 29 (28d), then missed Feb and logged Mar 26 (56d ~ 2x28)
        val starts = listOf(base, base + 28, base + 84)
        val pred = CyclePredictor.predict(fromStarts(starts, 4), baselineCycleLength = 28)

        // Raw observed lengths preserved for truthful chart display
        assertEquals(listOf(28, 56), pred.completedLengths)
        // Statistical median handles the ~2x artifact cleanly
        assertEquals(28, pred.medianLength)
    }

    @Test
    fun median_of_odd_and_even_lists() {
        assertEquals(28, CyclePredictor.medianOf(listOf(27, 28, 29)))
        assertEquals(29, CyclePredictor.medianOf(listOf(28, 30)))
        assertEquals(0, CyclePredictor.medianOf(emptyList()))
    }
}
