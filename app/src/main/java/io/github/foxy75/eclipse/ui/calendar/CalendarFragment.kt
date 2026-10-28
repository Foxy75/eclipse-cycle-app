package io.github.foxy75.eclipse.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Dates
import io.github.foxy75.eclipse.data.AppState
import io.github.foxy75.eclipse.data.DayEntry
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.FragmentCalendarBinding
import io.github.foxy75.eclipse.ui.common.Press
import io.github.foxy75.eclipse.ui.log.LogSheet

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

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
        Press.attach(binding.btnPrev, binding.btnNext)
        binding.btnPrev.setOnClickListener {
            viewMonth = Dates.addMonths(viewMonth, -1)
            render()
        }
        binding.btnNext.setOnClickListener {
            viewMonth = Dates.addMonths(viewMonth, 1)
            render()
        }
        binding.monthCalendar.setDayTapListener { day -> openLog(day) }
        binding.monthCalendar.setDayLongPressListener { day -> markFlowStart(day) }
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

    private fun markFlowStart(day: Long) {
        val repo = Repository.get(requireContext())
        val existing = repo.snapshot().entry(day) ?: DayEntry()
        repo.saveDay(day, existing.copy(flow = if (existing.flow >= 1) existing.flow else 1))
    }

    private fun render() {
        val repo = Repository.get(requireContext())
        val flowDays = repo.flowDays().toSet()
        val pred = repo.prediction()
        val predicted = pred.predictedPeriods
            .flatMap { r -> (r.start..r.end).toList() }
            .toSet()

        binding.monthCalendar.setData(
            CalendarData(
                year = Dates.year(viewMonth),
                month = Dates.month(viewMonth),
                factDays = flowDays,
                predictedDays = predicted,
                estimateDay = pred.nextStart,
                today = Dates.today()
            )
        )

        val months = resources.getStringArray(R.array.months)
        binding.monthTitle.text =
            months[Dates.month(viewMonth) - 1] + " " + Dates.year(viewMonth)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}