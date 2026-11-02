package io.github.foxy75.eclipse.core

enum class Phase { MENSTRUAL, FOLLICULAR, FERTILE, LUTEAL, NONE }

object Phases {
    /**
     * Resolves the clinical phase for [day] integrating Wilcox 6-day fertile window.
     */
    fun phaseFor(day: Long, cycles: List<Cycle>, prediction: Prediction): Phase {
        if (cycles.isEmpty()) {
            val nextStart = prediction.nextStart ?: return Phase.NONE
            val ovulation = prediction.ovulationDay ?: (nextStart - 13L)
            val fertileStart = ovulation - 5
            return when {
                day in fertileStart..ovulation -> Phase.FERTILE
                day > ovulation && day < nextStart -> Phase.LUTEAL
                else -> Phase.NONE
            }
        }

        val starts = cycles.map { it.start }
        if (day < starts.first()) return Phase.NONE

        var i = starts.indexOfLast { it <= day }
        if (i < 0) i = 0

        val cycle = cycles[i]
        if (isFlowDay(day, cycle)) return Phase.MENSTRUAL

        val nextStart = starts.getOrNull(i + 1) ?: prediction.nextStart ?: return Phase.NONE
        val cycleLen = (nextStart - cycle.start).toInt().coerceIn(20, 45)
        val lutealDays = CyclePredictor.estimateLutealPhase(cycleLen)
        val ovulationDay = nextStart - lutealDays
        val fertileStart = ovulationDay - 5

        return when {
            day in fertileStart..ovulationDay -> Phase.FERTILE
            day > ovulationDay -> Phase.LUTEAL
            else -> Phase.FOLLICULAR
        }
    }

    private fun isFlowDay(day: Long, cycle: Cycle): Boolean =
        day >= cycle.start && day <= cycle.end
}