package io.github.foxy75.eclipse.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.data.AppState
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.data.Symptoms
import io.github.foxy75.eclipse.databinding.FragmentInsightsBinding

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private val listener: (AppState) -> Unit = {
        if (_binding != null && isAdded) render()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

    private fun render() {
        val repo = Repository.get(requireContext())
        val pred = repo.prediction()
        val cycles = repo.cycles()

        if (pred.completedLengths.isEmpty()) {
            binding.content.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            return
        }

        binding.emptyState.visibility = View.GONE
        binding.content.visibility = View.VISIBLE

        binding.avgCycleValue.text = getString(R.string.ins_days_unit, pred.medianLength)
        binding.avgCycleRange.text =
            getString(R.string.ins_range_unit, pred.lengthRange.first, pred.lengthRange.last)
        binding.avgPeriodValue.text = getString(R.string.ins_days_unit, pred.avgPeriodLength)
        binding.cyclesLogged.text = getString(R.string.ins_cycles_logged, cycles.size)

        binding.stabilityTag.text =
            getString(if (pred.stable) R.string.ins_stable else R.string.ins_unstable)

        val recent = pred.completedLengths.takeLast(6)
        binding.chartTitle.text = getString(R.string.ins_cycle_length_chart, recent.size)
        binding.chart.setData(recent, pred.medianLength)

        val common = mostCommonSymptom(repo.snapshot())
        binding.commonSymptom.text =
            common ?: getString(R.string.ins_common_none)
    }

    private fun mostCommonSymptom(state: AppState): String? {
        val counts = IntArray(Symptoms.COUNT)
        for (entry in state.days.values) {
            for (s in entry.symptoms) {
                if (s in 0 until Symptoms.COUNT) counts[s]++
            }
        }
        var best = -1
        var bestCount = 0
        for (i in counts.indices) {
            if (counts[i] > bestCount) {
                bestCount = counts[i]
                best = i
            }
        }
        if (best < 0 || bestCount == 0) return null
        val labels = resources.getStringArray(R.array.symptom_labels)
        return getString(R.string.ins_common_count, labels[best], bestCount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}