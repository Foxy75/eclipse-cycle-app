package io.github.foxy75.eclipse.ui.log

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Dates
import io.github.foxy75.eclipse.data.DayEntry
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.SheetLogBinding
import io.github.foxy75.eclipse.ui.common.Press
import java.util.Locale

class LogSheet : BottomSheetDialogFragment() {

    private var _binding: SheetLogBinding? = null
    private val binding get() = _binding!!

    private var date: Long = 0L

    private var selectedFlow: Int = 0
    private val selectedSymptoms = mutableSetOf<Int>()
    private var selectedMood: Int = -1

    private lateinit var flowButtons: List<TextView>
    private lateinit var symptomItems: List<LinearLayout>
    private lateinit var symptomIcons: List<ImageView>
    private lateinit var symptomTexts: List<TextView>
    private lateinit var moodButtons: List<TextView>

    companion object {
        private const val ARG_DATE = "date"

        fun newInstance(date: Long): LogSheet = LogSheet().apply {
            arguments = Bundle().apply {
                putLong(ARG_DATE, date)
            }
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isFitToContents = true

            val dm = resources.displayMetrics
            val insets = ViewCompat.getRootWindowInsets(bottomSheet)
            val statusBarTop = insets?.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )?.top ?: (dm.density * 48).toInt()

            behavior.maxHeight = dm.heightPixels - statusBarTop - (dm.density * 16).toInt()
        }
        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        date = requireArguments().getLong(ARG_DATE, Dates.today())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews()
        applyWindowInsets()
        renderDateHeader()
        populateFromRepository()
        setupListeners()
    }

    private fun applyWindowInsets() {
        val basePaddingBottom = resources.getDimensionPixelSize(R.dimen.space_24)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(navBars.bottom, ime.bottom)
            binding.sheetContentContainer.updatePadding(bottom = basePaddingBottom + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun initViews() {
        flowButtons = listOf(
            binding.btnFlowSpotting,
            binding.btnFlowLight,
            binding.btnFlowMedium,
            binding.btnFlowHeavy
        )

        symptomItems = listOf(
            binding.itemSymptom0,
            binding.itemSymptom1,
            binding.itemSymptom2,
            binding.itemSymptom3,
            binding.itemSymptom4,
            binding.itemSymptom5,
            binding.itemSymptom6,
            binding.itemSymptom7
        )

        symptomIcons = listOf(
            binding.iconSymptom0,
            binding.iconSymptom1,
            binding.iconSymptom2,
            binding.iconSymptom3,
            binding.iconSymptom4,
            binding.iconSymptom5,
            binding.iconSymptom6,
            binding.iconSymptom7
        )

        symptomTexts = listOf(
            binding.textSymptom0,
            binding.textSymptom1,
            binding.textSymptom2,
            binding.textSymptom3,
            binding.textSymptom4,
            binding.textSymptom5,
            binding.textSymptom6,
            binding.textSymptom7
        )

        moodButtons = listOf(
            binding.btnMood0,
            binding.btnMood1,
            binding.btnMood2,
            binding.btnMood3,
            binding.btnMood4
        )

        Press.attach(
            binding.btnClose,
            binding.btnSave,
            binding.btnDelete,
            *flowButtons.toTypedArray(),
            *symptomItems.toTypedArray(),
            *moodButtons.toTypedArray()
        )
    }

    private fun renderDateHeader() {
        val weekdays = resources.getStringArray(R.array.weekdays_full)
        val months = resources.getStringArray(R.array.months)

        val weekdayIdx = (Dates.weekday(date) - 1).coerceIn(0, weekdays.lastIndex)
        val monthIdx = (Dates.month(date) - 1).coerceIn(0, months.lastIndex)

        val dayName = weekdays[weekdayIdx]
        val rawMonth = months[monthIdx]
        val shortMonth = if (rawMonth.length > 3) rawMonth.substring(0, 3) else rawMonth
        val dayNum = String.format(Locale.getDefault(), "%02d", Dates.dayOfMonth(date))
        val yearNum = Dates.year(date)

        binding.logDateTitle.text = "$dayName, $dayNum $shortMonth $yearNum"
    }

    private fun populateFromRepository() {
        val entry = Repository.get(requireContext()).snapshot().entry(date)
        if (entry != null) {
            selectedFlow = entry.flow.coerceIn(0, 4)
            selectedSymptoms.clear()
            selectedSymptoms.addAll(entry.symptoms.filter { it in symptomItems.indices })
            selectedMood = if (entry.mood in moodButtons.indices) entry.mood else -1
            binding.noteInput.setText(entry.note)
            binding.btnDelete.visibility = View.VISIBLE
        } else {
            selectedFlow = 0
            selectedSymptoms.clear()
            selectedMood = -1
            binding.noteInput.setText("")
            binding.btnDelete.visibility = View.GONE
        }

        updateFlowUI()
        updateSymptomsUI()
        updateMoodUI()
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        flowButtons.forEachIndexed { index, button ->
            val flowLevel = index + 1
            button.setOnClickListener {
                selectedFlow = if (selectedFlow == flowLevel) 0 else flowLevel
                updateFlowUI()
            }
        }

        symptomItems.forEachIndexed { index, item ->
            item.setOnClickListener {
                if (selectedSymptoms.contains(index)) {
                    selectedSymptoms.remove(index)
                } else {
                    selectedSymptoms.add(index)
                }
                updateSymptomsUI()
            }
        }

        moodButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedMood = if (selectedMood == index) -1 else index
                updateMoodUI()
            }
        }

        binding.btnSave.setOnClickListener {
            save()
        }

        binding.btnDelete.setOnClickListener {
            confirmDelete()
        }
    }

    private fun updateFlowUI() {
        flowButtons.forEachIndexed { index, button ->
            button.isSelected = (selectedFlow == index + 1)
        }
    }

    private fun updateSymptomsUI() {
        symptomItems.forEachIndexed { index, item ->
            val isSel = selectedSymptoms.contains(index)
            item.isSelected = isSel
            symptomIcons[index].isSelected = isSel
            symptomTexts[index].isSelected = isSel
        }
        binding.tvSymptomsCount.text = getString(R.string.log_multi, selectedSymptoms.size)
    }

    private fun updateMoodUI() {
        moodButtons.forEachIndexed { index, button ->
            button.isSelected = (selectedMood == index)
        }
    }

    private fun save() {
        val note = binding.noteInput.text?.toString()?.trim().orEmpty()
        val entry = DayEntry(
            flow = selectedFlow,
            symptoms = selectedSymptoms.toSet(),
            mood = selectedMood,
            note = note
        )
        Repository.get(requireContext()).saveDay(date, entry)
        dismiss()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
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
