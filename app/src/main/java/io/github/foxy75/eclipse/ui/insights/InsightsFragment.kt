package io.github.foxy75.eclipse.ui.insights

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.github.foxy75.eclipse.MainActivity
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.CyclePredictor
import io.github.foxy75.eclipse.core.Dates
import io.github.foxy75.eclipse.data.AppState
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.data.Symptoms
import io.github.foxy75.eclipse.databinding.FragmentInsightsBinding
import io.github.foxy75.eclipse.ui.common.Press
import kotlin.math.abs

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
        Press.attach(binding.btnExportBackup)
        binding.btnExportBackup.setOnClickListener {
            (activity as? MainActivity)?.openSettings()
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

    private fun render() {
        val repo = Repository.get(requireContext())
        val snapshot = repo.snapshot()
        val pred = repo.prediction()
        val cycles = repo.cycles()

        if (cycles.isEmpty() && repo.flowDays().isEmpty()) {
            binding.content.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            return
        }

        binding.emptyState.visibility = View.GONE
        binding.content.visibility = View.VISIBLE

        val months = resources.getStringArray(R.array.months)
        val target = snapshot.baselineCycleLength ?: 28
        binding.tvTargetCycle.text = "— Target ${target}h"

        // 1. Render Bar Chart
        val recentLengths = pred.completedLengths.takeLast(6)
        if (recentLengths.isNotEmpty()) {
            val starts = cycles.map { it.start }.takeLast(recentLengths.size + 1)
            val barItems = recentLengths.mapIndexed { idx, len ->
                val cStart = starts.getOrNull(idx) ?: Dates.today()
                val monthIdx = (Dates.month(cStart) - 1).coerceIn(0, months.lastIndex)
                val mName = months[monthIdx]
                val shortMonth = if (mName.length > 3) mName.substring(0, 3) else mName
                BarChartView.BarItem(shortMonth, len)
            }
            binding.chart.submitData(barItems, target)

            val minLen = recentLengths.minOrNull() ?: target
            val maxLen = recentLengths.maxOrNull() ?: target
            binding.tvCycleRange.text = getString(R.string.ins_range_n, minLen, maxLen)
        } else {
            binding.chart.submitData(emptyList(), target)
            binding.tvCycleRange.text = getString(R.string.ins_range_n, target, target)
        }

        // 2. Render Metric Cards
        val avgCycle = if (pred.medianLength > 0) pred.medianLength else target
        binding.avgCycleValue.text = avgCycle.toString()

        val cycleCount = if (recentLengths.isNotEmpty()) recentLengths.size else cycles.size
        binding.tvBasedOnCycles.text = getString(R.string.ins_based_n, cycleCount)

        val mad = if (pred.completedLengths.isNotEmpty()) {
            val m = pred.medianLength
            CyclePredictor.medianOf(pred.completedLengths.takeLast(6).map { abs(it - m) })
        } else {
            2
        }
        binding.tvVariationValue.text = "±$mad"

        if (pred.stable || pred.completedLengths.isEmpty()) {
            binding.dotStability.visibility = View.VISIBLE
            binding.stabilityTag.text = getString(R.string.ins_rhythm_stable)
        } else {
            binding.dotStability.visibility = View.GONE
            binding.stabilityTag.text = getString(R.string.rhythm_unstable)
        }

        // 3. Render Top Symptoms
        renderTopSymptoms(snapshot)
    }

    private fun renderTopSymptoms(snapshot: AppState) {
        val symptomCounts = IntArray(Symptoms.COUNT)
        for (entry in snapshot.days.values) {
            for (s in entry.symptoms) {
                if (s in 0 until Symptoms.COUNT) symptomCounts[s]++
            }
        }

        val ranked = (0 until Symptoms.COUNT)
            .filter { symptomCounts[it] > 0 }
            .sortedByDescending { symptomCounts[it] }

        val labels = resources.getStringArray(R.array.symptom_labels)

        if (ranked.isEmpty()) {
            binding.rowSymptom1.visibility = View.GONE
            binding.rowSymptom2.visibility = View.GONE
            binding.rowSymptom3.visibility = View.GONE
            binding.tvEmptySymptoms.visibility = View.VISIBLE
            return
        }

        binding.tvEmptySymptoms.visibility = View.GONE

        // Row 1
        if (ranked.isNotEmpty()) {
            val s0 = ranked[0]
            binding.rowSymptom1.visibility = View.VISIBLE
            binding.iconSymptom1.setImageResource(iconForSymptom(s0))
            binding.tvSymptom1Name.text = labels[s0]
            binding.tvSymptom1Count.text = getString(R.string.unit_days, symptomCounts[s0])
        } else {
            binding.rowSymptom1.visibility = View.GONE
        }

        // Row 2
        if (ranked.size > 1) {
            val s1 = ranked[1]
            binding.rowSymptom2.visibility = View.VISIBLE
            binding.iconSymptom2.setImageResource(iconForSymptom(s1))
            binding.tvSymptom2Name.text = labels[s1]
            binding.tvSymptom2Count.text = getString(R.string.unit_days, symptomCounts[s1])
        } else {
            binding.rowSymptom2.visibility = View.GONE
        }

        // Row 3
        if (ranked.size > 2) {
            val s2 = ranked[2]
            binding.rowSymptom3.visibility = View.VISIBLE
            binding.iconSymptom3.setImageResource(iconForSymptom(s2))
            binding.tvSymptom3Name.text = labels[s2]
            binding.tvSymptom3Count.text = getString(R.string.unit_days, symptomCounts[s2])
        } else {
            binding.rowSymptom3.visibility = View.GONE
        }
    }

    private fun iconForSymptom(symptom: Int): Int = when (symptom) {
        Symptoms.CRAMPS -> R.drawable.ic_bolt
        Symptoms.HEADACHE -> R.drawable.ic_target
        Symptoms.BACKACHE -> R.drawable.ic_spine
        Symptoms.BLOATING -> R.drawable.ic_bloating
        Symptoms.FATIGUE -> R.drawable.ic_battery_low
        Symptoms.ACNE -> R.drawable.ic_sparkles
        Symptoms.NAUSEA -> R.drawable.ic_nausea
        Symptoms.CRAVING -> R.drawable.ic_smile
        else -> R.drawable.ic_sparkles
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
