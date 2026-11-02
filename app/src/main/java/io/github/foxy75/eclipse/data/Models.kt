package io.github.foxy75.eclipse.data

import org.json.JSONArray
import org.json.JSONObject
import io.github.foxy75.eclipse.core.Dates

object Symptoms {
    const val COUNT = 8
    const val CRAMPS = 0
    const val HEADACHE = 1
    const val BACKACHE = 2
    const val BLOATING = 3
    const val FATIGUE = 4
    const val ACNE = 5
    const val NAUSEA = 6
    const val CRAVING = 7
}

object Moods {
    const val COUNT = 5
    const val CALM = 0
    const val HAPPY = 1
    const val NEUTRAL = 2
    const val ANXIOUS = 3
    const val IRRITABLE = 4
}

object Flow {
    const val MAX = 4
    const val MEDIUM = 3
}

/** A single day's log. [flow] is 0..4; 0 means a logged day without flow. */
data class DayEntry(
    val flow: Int = 0,
    val symptoms: Set<Int> = emptySet(),
    val mood: Int = -1,
    val note: String = ""
)

/** All persisted cycle data. Keyed by epoch-day. */
data class AppState(
    val days: Map<Long, DayEntry> = emptyMap(),
    val baselineCycleLength: Int? = null,
    val baselinePeriodLength: Int? = null
) {

    fun entry(date: Long): DayEntry? = days[date]

    fun flowDays(): List<Long> =
        days.filterValues { it.flow >= 1 }.keys.sorted()

    companion object {
        const val VERSION = 1
    }
}

/** org.json serialization for a single [data.enc] file. */
object Serialization {

    fun toJson(state: AppState): String {
        val root = JSONObject()
        root.put("v", AppState.VERSION)
        val days = JSONObject()
        state.days.toSortedMap().forEach { (date, entry) ->
            days.put(Dates.formatIso(date), entryToJson(entry))
        }
        root.put("days", days)
        state.baselineCycleLength?.let { root.put("bc", it) }
        state.baselinePeriodLength?.let { root.put("bp", it) }
        return root.toString()
    }

    fun fromJson(raw: String): AppState {
        val root = JSONObject(raw)
        val daysObj = root.optJSONObject("days") ?: JSONObject()
        val days = HashMap<Long, DayEntry>()
        val keys = daysObj.keys()
        while (keys.hasNext()) {
            val iso = keys.next()
            days[Dates.parseIso(iso)] = entryFromJson(daysObj.getJSONObject(iso))
        }
        val bc = if (root.has("bc")) root.optInt("bc") else null
        val bp = if (root.has("bp")) root.optInt("bp") else null
        return AppState(
            days = days,
            baselineCycleLength = bc,
            baselinePeriodLength = bp
        )
    }

    private fun entryToJson(entry: DayEntry): JSONObject {
        val o = JSONObject()
        o.put("f", entry.flow)
        if (entry.symptoms.isNotEmpty()) {
            val arr = JSONArray()
            entry.symptoms.sorted().forEach { arr.put(it) }
            o.put("s", arr)
        }
        if (entry.mood >= 0) o.put("m", entry.mood)
        if (entry.note.isNotEmpty()) o.put("n", entry.note)
        return o
    }

    private fun entryFromJson(o: JSONObject): DayEntry {
        val symptoms = HashSet<Int>()
        val sArr = o.optJSONArray("s")
        if (sArr != null) for (i in 0 until sArr.length()) symptoms.add(sArr.getInt(i))
        return DayEntry(
            flow = o.optInt("f", 0),
            symptoms = symptoms,
            mood = o.optInt("m", -1),
            note = o.optString("n", "")
        )
    }
}