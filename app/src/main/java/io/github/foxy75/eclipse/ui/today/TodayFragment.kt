package io.github.foxy75.eclipse.ui.today

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        Press.attach(binding.cta)
        binding.cta.setOnClickListener { openLog(selectedDay) }
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
            binding.heroRoot.visibility = View.GONE
            binding.cta.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            return
        }

        binding.heroRoot.visibility = View.VISIBLE
        binding.cta.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        val phase = Phases.phaseFor(day, cycles, pred)
        binding.heroCycleDay.text = cycleDayOf(cycles, day)?.toString() ?: "–"

        when (phase) {
            Phase.MENSTRUAL -> {
                binding.heroPhaseLabel.setText(phaseLabelRes(phase))
                binding.heroPhaseTitle.setText(phaseLabelRes(phase))
                binding.heroSub.setText(R.string.today_on_period)
            }
            else -> {
                if (pred.stable) {
                    binding.heroPhaseLabel.setText(R.string.today_label_cycle_day)
                    binding.heroPhaseTitle.setText(phaseLabelRes(phase))
                    renderNextPeriod(pred.nextStart, day)
                } else {
                    binding.heroPhaseLabel.setText(R.string.today_unstable)
                    binding.heroPhaseTitle.setText(R.string.phase_none)
                    binding.heroSub.text = getString(
                        R.string.today_unstable_body,
                        pred.avgLength.roundToInt(),
                        pred.lengthRange.first,
                        pred.lengthRange.last
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
        Phase.LUTEAL -> R.string.phase_luteal
        Phase.NONE -> R.string.phase_none
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}