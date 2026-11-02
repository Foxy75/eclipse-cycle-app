package io.github.foxy75.eclipse.ui.today

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Cycle
import io.github.foxy75.eclipse.core.CycleBuilder
import io.github.foxy75.eclipse.core.CyclePredictor
import io.github.foxy75.eclipse.core.Dates
import io.github.foxy75.eclipse.core.Phase
import io.github.foxy75.eclipse.core.Phases
import io.github.foxy75.eclipse.data.AppState
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.FragmentTodayBinding
import io.github.foxy75.eclipse.ui.common.Press
import io.github.foxy75.eclipse.ui.log.LogSheet
import kotlin.math.roundToInt

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private var selectedDay: Long = Dates.today()

    private val listener: (AppState) -> Unit = {
        if (_binding != null && isAdded) render()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Press.attach(binding.heroCta, binding.heroEmptyCta)
        binding.heroCta.setOnClickListener { openLog(selectedDay) }
        binding.heroEmptyCta.setOnClickListener { openLog(selectedDay) }
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

    private fun render() {
        renderDayStrip()
        renderHero()
        renderMetrics()
        renderSymptoms()
    }

    private fun renderDayStrip() {
        val repo = Repository.get(requireContext())
        val today = Dates.today()
        val days = (0..13).map { today + it }
        val fact = repo.flowDays().toSet()
        val predicted = repo.prediction().predictedPeriods
            .flatMap { r -> (r.start..r.end).toList() }
            .toSet()
        binding.dayStrip.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.dayStrip.adapter = DayStripAdapter(days, fact, predicted, selectedDay) { day ->
            selectedDay = day
            renderDayStrip()
            renderHero()
        }
    }

    private fun renderHero() {
        val repo = Repository.get(requireContext())
        val flowDays = repo.flowDays()
        val cycles = CycleBuilder.build(flowDays)
        val pred = repo.prediction()
        val day = selectedDay

        if (flowDays.isEmpty()) {
            binding.heroActive.visibility = View.GONE
            binding.heroEmpty.visibility = View.VISIBLE
            return
        }

        binding.heroActive.visibility = View.VISIBLE
        binding.heroEmpty.visibility = View.GONE

        val phase = Phases.phaseFor(day, cycles, pred)
        binding.heroCycleDay.text = cycleDayOf(cycles, day)?.toString() ?: "–"

        when (phase) {
            Phase.MENSTRUAL -> {
                binding.heroPhaseLabel.setText(R.string.today_label_cycle_day)
                binding.heroPhaseTitle.setText(phaseLabelRes(phase))
                binding.heroSub.setText(R.string.today_on_period)
            }
            else -> {
                if (pred.stable) {
                    binding.heroPhaseLabel.setText(R.string.today_label_cycle_day)
                    binding.heroPhaseTitle.setText(phaseLabelRes(phase))
                    renderNextPeriod(pred.nextStart, day)
                } else if (pred.nextStart != null) {
                    binding.heroPhaseLabel.setText(R.string.today_unstable)
                    binding.heroPhaseTitle.setText(phaseLabelRes(phase))
                    renderNextPeriod(pred.nextStart, day)
                } else {
                    binding.heroPhaseLabel.setText(R.string.today_unstable)
                    binding.heroPhaseTitle.setText(R.string.phase_none)
                    val avg = if (pred.avgLength > 0) pred.avgLength.roundToInt() else 28
                    val min = if (pred.lengthRange.first > 0) pred.lengthRange.first else 28
                    val max = if (pred.lengthRange.last > 0) pred.lengthRange.last else 28
                    binding.heroSub.text = getString(
                        R.string.today_unstable_body,
                        avg,
                        min,
                        max
                    )
                }
            }
        }
    }

    private fun renderNextPeriod(nextStart: Long?, day: Long) {
        val next = nextStart
        if (next == null) {
            binding.heroSub.text = ""
            return
        }
        val diff = (next - day).toInt()
        binding.heroSub.text = when {
            diff <= 0 -> getString(R.string.today_next_period_today)
            else -> getString(R.string.today_next_period, diff)
        }
    }

    private fun cycleDayOf(cycles: List<Cycle>, day: Long): Int? {
        val starts = cycles.map { it.start }
        val idx = starts.indexOfLast { it <= day }
        return if (idx < 0) null else (day - starts[idx] + 1).toInt()
    }

    private fun phaseLabelRes(phase: Phase): Int = when (phase) {
        Phase.MENSTRUAL -> R.string.phase_menstrual
        Phase.FOLLICULAR -> R.string.phase_follicular
        Phase.FERTILE -> R.string.phase_fertile
        Phase.LUTEAL -> R.string.phase_luteal
        Phase.NONE -> R.string.phase_none
    }

    private fun renderMetrics() {
        val repo = Repository.get(requireContext())
        val pred = repo.prediction()
        val hasCompleted = pred.completedLengths.isNotEmpty()

        if (hasCompleted) {
            val cycleLen = if (pred.medianLength > 0) pred.medianLength else pred.avgLength.roundToInt()
            binding.metricCycleValue.text = "$cycleLen ${getString(R.string.today_label_day)}"
            binding.metricRhythmValue.text = if (pred.stable) {
                getString(R.string.cal_prospect)
            } else {
                getString(R.string.rhythm_unstable)
            }
        } else {
            binding.metricCycleValue.text = "---"
            binding.metricRhythmValue.text = "---"
        }
    }

    private fun renderSymptoms() {
        val repo = Repository.get(requireContext())
        val entry = repo.snapshot().entry(selectedDay)
        val container = binding.symptomChips
        container.removeAllViews()

        val chips = mutableListOf<String>()
        if (entry != null) {
            val flowLabels = resources.getStringArray(R.array.flow_labels)
            if (entry.flow in 1 until flowLabels.size) {
                chips.add("Aliran ${flowLabels[entry.flow].lowercase()}")
            }
            val symptomLabels = resources.getStringArray(R.array.symptom_labels)
            entry.symptoms.forEach { idx ->
                if (idx in symptomLabels.indices) chips.add(symptomLabels[idx])
            }
            val moodLabels = resources.getStringArray(R.array.mood_labels)
            if (entry.mood in moodLabels.indices) {
                chips.add("Mood ${moodLabels[entry.mood].lowercase()}")
            }
        }

        if (chips.isEmpty()) {
            binding.symptomsScroll.visibility = View.GONE
            binding.symptomsEmpty.visibility = View.VISIBLE
            binding.btnEdit.visibility = View.GONE
        } else {
            binding.symptomsEmpty.visibility = View.GONE
            binding.symptomsScroll.visibility = View.VISIBLE
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnEdit.setOnClickListener { openLog(selectedDay) }

            val density = resources.displayMetrics.density
            chips.forEachIndexed { idx, text ->
                val tv = TextView(requireContext()).apply {
                    this.text = text
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.ink))
                    setBackgroundResource(R.drawable.bg_chip_canvas)
                    gravity = Gravity.CENTER
                    setPadding(
                        (10 * density).toInt(), (6 * density).toInt(),
                        (10 * density).toInt(), (6 * density).toInt()
                    )
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}