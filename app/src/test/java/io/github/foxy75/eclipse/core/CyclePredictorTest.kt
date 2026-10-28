package io.github.foxy75.eclipse.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun stable_prediction_uses_median() {
        val days = fromStarts((0..6).map { day("2025-01-01") + 28L * it }, 5)
        val pred = CyclePredictor.predict(days)
        assertTrue(pred.stable)
        assertEquals(28, pred.medianLength)
        assertEquals(5, pred.avgPeriodLength)
        assertEquals(day("2025-01-01") + 28L * 7, pred.nextStart)
    }

    @Test
    fun unstable_when_mad_exceeds_threshold() {
        val base = day("2025-01-01")
        val starts = listOf(base, base + 20, base + 65, base + 85, base + 130)
        val pred = CyclePredictor.predict(fromStarts(starts, 4))
        assertFalse(pred.stable)
        assertNull(pred.nextStart)
        assertTrue(pred.predictedPeriods.isEmpty())
    }

    @Test
    fun unstable_when_less_than_two_completed_cycles() {
        val days = fromStarts(listOf(day("2025-01-01"), day("2025-01-29")), 4)
        val pred = CyclePredictor.predict(days)
        assertFalse(pred.stable)
        assertNull(pred.nextStart)
    }

    @Test
    fun median_of_odd_and_even_lists() {
        assertEquals(28, CyclePredictor.medianOf(listOf(27, 28, 29)))
        assertEquals(29, CyclePredictor.medianOf(listOf(28, 30)))
        assertEquals(0, CyclePredictor.medianOf(emptyList()))
    }
}