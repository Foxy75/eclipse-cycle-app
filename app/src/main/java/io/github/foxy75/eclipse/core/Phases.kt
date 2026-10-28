package io.github.foxy75.eclipse.core

enum class Phase { MENSTRUAL, FOLLICULAR, LUTEAL, NONE }

object Phases {
    /**
     * Resolves the phase for [day]. Completed cycles use their real next-start
     * (deterministic); the latest, still-open cycle falls back to [prediction].
     */
    fun phaseFor(day: Long, cycles: List<Cycle>, prediction: Prediction): Phase {
        if (cycles.isEmpty()) return Phase.NONE
        val starts = cycles.map { it.start }

        // Before the first recorded period there is nothing to anchor on.
        if (day < starts.first()) return Phase.NONE

        var i = starts.indexOfLast { it <= day }
        if (i < 0) i = 0

        val cycle = cycles[i]
        if (isFlowDay(day, cycle)) return Phase.MENSTRUAL

        val nextStart = starts.getOrNull(i + 1) ?: prediction.nextStart
            ?: return Phase.NONE

        val lutealStart = nextStart - CyclePredictor.LUTEAL_DAYS
        return if (day >= lutealStart) Phase.LUTEAL else Phase.FOLLICULAR
    }

    private fun isFlowDay(day: Long, cycle: Cycle): Boolean =
        day >= cycle.start && day <= cycle.end
}