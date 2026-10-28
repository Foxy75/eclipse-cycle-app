package io.github.foxy75.eclipse.ui.log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Dates
import io.github.foxy75.eclipse.data.DayEntry
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.SheetLogBinding
import io.github.foxy75.eclipse.ui.common.Press

class LogSheet : BottomSheetDialogFragment() {

    private var _binding: SheetLogBinding? = null
    private val binding get() = _binding!!

    private var date: Long = 0L
    private lateinit var flowChips: List<Chip>
    private lateinit var symptomChips: List<Chip>
    private lateinit var moodChips: List<Chip>

    companion object {
        private const val ARG_DATE = "date"

        fun newInstance(date: Long): LogSheet = LogSheet().apply {
            arguments = bundleOf(ARG_DATE to date)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        date = requireArguments().getLong(ARG_DATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Press.attach(binding.btnSave, binding.btnDelete)

        flowChips = listOf(
            binding.chipFlow0, binding.chipFlow1, binding.chipFlow2,
            binding.chipFlow3, binding.chipFlow4
        )
        symptomChips = listOf(
            binding.chipSymptom0, binding.chipSymptom1, binding.chipSymptom2,
            binding.chipSymptom3, binding.chipSymptom4, binding.chipSymptom5,
            binding.chipSymptom6, binding.chipSymptom7
        )
        moodChips = listOf(
            binding.chipMood0, binding.chipMood1, binding.chipMood2,
            binding.chipMood3, binding.chipMood4
        )

        val months = resources.getStringArray(R.array.months)
        binding.logTitle.text = getString(
            R.string.log_title_date,
            "${Dates.dayOfMonth(date)} ${months[Dates.month(date) - 1]} ${Dates.year(date)}"
        )

        populate()

        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun populate() {
        val entry = Repository.get(requireContext()).snapshot().entry(date)
        if (entry == null) {
            flowChips[0].isChecked = true
            binding.btnDelete.visibility = View.GONE
            return
        }
        flowChips[entry.flow.coerceIn(0, 4)].isChecked = true
        entry.symptoms.forEach { if (it in symptomChips.indices) symptomChips[it].isChecked = true }
        if (entry.mood in moodChips.indices) moodChips[entry.mood].isChecked = true
        binding.noteInput.setText(entry.note)
        binding.btnDelete.visibility = View.VISIBLE
    }

    private fun save() {
        val flow = flowChips.indexOfFirst { it.isChecked }.coerceAtLeast(0)
        val symptoms = symptomChips.indices.filter { symptomChips[it].isChecked }.toSet()
        val mood = moodChips.indexOfFirst { it.isChecked }
        val note = binding.noteInput.text.toString().trim()
        Repository.get(requireContext()).saveDay(date, DayEntry(flow, symptoms, mood, note))
        dismiss()
    }

    private fun confirmDelete() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_body)
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                Repository.get(requireContext()).saveDay(date, DayEntry())
                dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}