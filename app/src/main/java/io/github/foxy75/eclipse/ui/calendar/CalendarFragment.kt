package io.github.foxy75.eclipse.ui.calendar

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Cycle
import io.github.foxy75.eclipse.core.CycleBuilder
import io.github.foxy75.eclipse.core.CyclePredictor
import io.github.foxy75.eclipse.core.Dates
import io.github.foxy75.eclipse.data.Flow
import io.github.foxy75.eclipse.data.AppState
import io.github.foxy75.eclipse.data.DayEntry
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.FragmentCalendarBinding
import io.github.foxy75.eclipse.ui.common.Press
import io.github.foxy75.eclipse.ui.log.LogSheet
import java.util.Locale
import kotlin.math.abs

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private var selectedDay: Long = Dates.today()
    private var viewMonth: Long = Dates.startOfMonth(Dates.today())

    private val listener: (AppState) -> Unit = {
        if (_binding != null && isAdded) render()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Press.attach(binding.btnPrev, binding.btnNext, binding.btnOpenFullLog)

        binding.btnPrev.setOnClickListener {
            viewMonth = Dates.addMonths(viewMonth, -1)
            render()
        }
        binding.btnNext.setOnClickListener {
            viewMonth = Dates.addMonths(viewMonth, 1)
            render()
        }

        binding.monthCalendar.setDayTapListener { day ->
            selectedDay = day
            render()
        }

        binding.monthCalendar.setDayLongPressListener { day ->
            markFlowToggle(day)
        }

        binding.btnOpenFullLog.setOnClickListener {
            openLog(selectedDay)
        }

        render()
    }

    override fun onStart() {
        super.onStart()
        Repository.get(requireContext()).addListener(listener)
        render()
    }

    override fun onStop() {
        super.onStop()
        Repository.get(requireContext()).removeListener(listener)
    }

    private fun openLog(day: Long) {
        LogSheet.newInstance(day).show(requireActivity().supportFragmentManager, "log")
    }

    private fun markFlowToggle(day: Long) {
        val repo = Repository.get(requireContext())
        val existing = repo.snapshot().entry(day) ?: DayEntry()
        val nextFlow = if (existing.flow > 0) 0 else Flow.MEDIUM
        repo.saveDay(day, existing.copy(flow = nextFlow))
    }

    private fun render() {
        val repo = Repository.get(requireContext())
        val snapshot = repo.snapshot()
        val flowDays = repo.flowDays().toSet()
        val pred = repo.prediction()
        val predicted = pred.predictedPeriods
            .flatMap { r -> (r.start..r.end).toList() }
            .toSet()

        val today = Dates.today()

        // 1. Render Calendar View
        binding.monthCalendar.setData(
            CalendarData(
                year = Dates.year(viewMonth),
                month = Dates.month(viewMonth),
                factDays = flowDays,
                predictedDays = predicted,
                estimateDay = pred.ovulationDay ?: pred.nextStart,
                today = today,
                selectedDay = selectedDay
            )
        )

        val months = resources.getStringArray(R.array.months)
        val monthIdx = (Dates.month(viewMonth) - 1).coerceIn(0, months.lastIndex)
        binding.monthTitle.text = "${months[monthIdx]} ${Dates.year(viewMonth)}"

        // 2. Render Selected Day Detail Card
        renderSelectedDayDetail(snapshot, repo.cycles(), today)

        // 3. Render 2 Bottom Metric Cards
        renderMetricCards(pred, snapshot)
    }

    private fun renderSelectedDayDetail(snapshot: AppState, cycles: List<Cycle>, today: Long) {
        val months = resources.getStringArray(R.array.months)
        val monthIdx = (Dates.month(selectedDay) - 1).coerceIn(0, months.lastIndex)
        val rawMonth = months[monthIdx]
        val shortMonth = if (rawMonth.length > 3) rawMonth.substring(0, 3) else rawMonth
        val dayNum = String.format(Locale.getDefault(), "%02d", Dates.dayOfMonth(selectedDay))
        val yearNum = Dates.year(selectedDay)

        val prefix = if (selectedDay == today) "Hari ini · " else ""
        binding.tvDetailDate.text = "$prefix$dayNum $shortMonth $yearNum"

        val cycleDay = cycleDayOf(cycles, selectedDay)
        binding.tvDetailCycleDay.text = if (cycleDay != null) {
            getString(R.string.cal_det_day, cycleDay)
        } else {
            "Belum ada siklus"
        }

        // Populate detail chips
        val container = binding.containerDetailChips
        container.removeAllViews()

        val entry = snapshot.entry(selectedDay)
        val chipLabels = mutableListOf<String>()

        if (entry != null) {
            val flowLabels = resources.getStringArray(R.array.flow_labels)
            if (entry.flow in 1 until flowLabels.size) {
                chipLabels.add("Aliran ${flowLabels[entry.flow].lowercase()}")
            }

            val symptomLabels = resources.getStringArray(R.array.symptom_labels)
            entry.symptoms.forEach { idx ->
                if (idx in symptomLabels.indices) {
                    chipLabels.add(symptomLabels[idx])
                }
            }

            val moodLabels = resources.getStringArray(R.array.mood_labels)
            if (entry.mood in moodLabels.indices) {
                chipLabels.add("Mood ${moodLabels[entry.mood].lowercase()}")
            }
        }

        if (chipLabels.isEmpty()) {
            chipLabels.add("Belum ada catatan")
        }

        val density = resources.displayMetrics.density
        chipLabels.forEachIndexed { idx, text ->
            val tv = TextView(requireContext()).apply {
                this.text = text
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.ink))
                setBackgroundResource(R.drawable.bg_chip_canvas)
                gravity = Gravity.CENTER
                setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (idx > 0) marginStart = (8 * density).toInt()
                }
            }
            container.addView(tv)
        }
    }

    private fun renderMetricCards(pred: io.github.foxy75.eclipse.core.Prediction, snapshot: AppState) {
        val cycleLen = if (pred.medianLength > 0) pred.medianLength else (snapshot.baselineCycleLength ?: 28)
        binding.tvAvgCycleValue.text = cycleLen.toString()

        val mad = if (pred.completedLengths.isNotEmpty()) {
            val m = pred.medianLength
            CyclePredictor.medianOf(pred.completedLengths.takeLast(6).map { abs(it - m) }).toDouble()
        } else {
            1.2
        }
        binding.tvAvgVariationSub.text = String.format(Locale.getDefault(), "Variasi ±%.1f hari", mad)

        val nextDays = pred.nextStart?.let { (it - selectedDay).coerceAtLeast(0) }
            ?: (cycleLen - ((selectedDay - (snapshot.flowDays().lastOrNull() ?: selectedDay)) % cycleLen)).coerceAtLeast(0)

        binding.tvNextPhaseValue.text = nextDays.toString()

        if (pred.stable || pred.completedLengths.isEmpty()) {
            binding.tvProspectStatus.text = getString(R.string.cal_prospect)
            binding.tvProspectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.teal))
        } else {
            binding.tvProspectStatus.text = getString(R.string.rhythm_unstable)
            binding.tvProspectStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_secondary))
        }
    }

    private fun cycleDayOf(cycles: List<Cycle>, day: Long): Int? {
        val starts = cycles.map { it.start }
        val idx = starts.indexOfLast { it <= day }
        return if (idx < 0) null else (day - starts[idx] + 1).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
