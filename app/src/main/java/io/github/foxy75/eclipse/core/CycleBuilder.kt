package io.github.foxy75.eclipse.core

/** An inclusive run [start, end] of flow days belonging to one period. */
data class Cycle(val start: Long, val end: Long) {
    val periodLength: Int get() = (end - start).toInt() + 1
}

/** Groups recorded flow days into cycles. */
object CycleBuilder {
    /** A new cycle starts after at least this many flow-free days. */
    const val GAP_DAYS = 10L

    fun build(flowDays: Collection<Long>): List<Cycle> {
        val sorted = flowDays.distinct().sorted()
        val cycles = ArrayList<Cycle>()
        var current: Cycle? = null
        for (d in sorted) {
            val c = current
            if (c == null) {
                current = Cycle(d, d)
            } else if (d - c.end > GAP_DAYS) {
                cycles.add(c)
                current = Cycle(d, d)
            } else {
                current = c.copy(end = d)
            }
        }
        current?.let { cycles.add(it) }
        return cycles
    }
}