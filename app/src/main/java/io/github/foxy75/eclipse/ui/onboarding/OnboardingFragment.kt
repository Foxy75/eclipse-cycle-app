package io.github.foxy75.eclipse.ui.onboarding

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import io.github.foxy75.eclipse.MainActivity
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Dates
import io.github.foxy75.eclipse.data.DayEntry
import io.github.foxy75.eclipse.data.Flow
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.FragmentOnboardingBinding
import io.github.foxy75.eclipse.ui.common.Press
import java.util.Calendar

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private enum class Stage { PIN_ENTER, PIN_CONFIRM }

    private var step = 1
    private var stage = Stage.PIN_ENTER
    private var firstPin: String? = null

    private var selectedStartDay: Long? = null
    private var cycleLength = 28
    private var periodDuration = 5

    private var today = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        today = Dates.today()
        selectedStartDay = today - 2L

        applyWindowInsets()

        Press.attach(
            binding.btnCta, binding.btnSkip, binding.btnBack,
            binding.dateField, binding.chipQuickToday, binding.chipQuickYesterday,
            binding.chipQuickForgot, binding.btnCycleLengthMinus, binding.btnCycleLengthPlus,
            binding.btnPeriodDurationMinus, binding.btnPeriodDurationPlus
        )

        binding.btnCta.setOnClickListener { onCta() }
        binding.btnSkip.setOnClickListener { (activity as? MainActivity)?.openHome() }
        binding.btnBack.setOnClickListener {
            when (step) {
                3 -> showStep(2)
                2 -> showStep(1)
            }
        }

        binding.dateField.setOnClickListener { showDatePicker() }
        binding.chipQuickToday.setOnClickListener { selectedStartDay = today; updateDateDisplay() }
        binding.chipQuickYesterday.setOnClickListener { selectedStartDay = today - 1L; updateDateDisplay() }
        binding.chipQuickForgot.setOnClickListener { selectedStartDay = null; updateDateDisplay() }

        binding.btnCycleLengthMinus.setOnClickListener { if (cycleLength > 21) { cycleLength--; renderSteppers() } }
        binding.btnCycleLengthPlus.setOnClickListener { if (cycleLength < 45) { cycleLength++; renderSteppers() } }
        binding.btnPeriodDurationMinus.setOnClickListener { if (periodDuration > 2) { periodDuration--; renderSteppers() } }
        binding.btnPeriodDurationPlus.setOnClickListener { if (periodDuration < 10) { periodDuration++; renderSteppers() } }

        binding.pinInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                if (text.length == 6) submitPin(text) else hideError()
            }
        })

        updateDateDisplay()
        renderSteppers()
        showStep(1)
    }

    private fun applyWindowInsets() {
        val baseTopPadding = resources.getDimensionPixelSize(R.dimen.space_8)
        val baseBottomMargin = resources.getDimensionPixelSize(R.dimen.space_16)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBar = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(navBar.bottom, ime.bottom)

            binding.onboardingContent.updatePadding(top = baseTopPadding + statusBar.top)

            val lp = binding.btnCta.layoutParams as? ViewGroup.MarginLayoutParams
            lp?.let {
                it.bottomMargin = baseBottomMargin + bottomInset
                binding.btnCta.layoutParams = it
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun onCta() {
        when (step) {
            1 -> showStep(2)
            2 -> showStep(3)
            3 -> submitPin(binding.pinInput.text.toString())
        }
    }

    private fun showStep(n: Int) {
        step = n
        binding.step1.visibility = if (n == 1) View.VISIBLE else View.GONE
        binding.step2.visibility = if (n == 2) View.VISIBLE else View.GONE
        binding.step3.visibility = if (n == 3) View.VISIBLE else View.GONE
        binding.btnBack.visibility = if (n > 1) View.VISIBLE else View.GONE
        binding.btnSkip.visibility = if (n < 3) View.VISIBLE else View.GONE
        binding.btnCta.setText(if (n == 3) R.string.onb_finish else R.string.onb_continue)
        binding.progress.setProgressCompat(n * 100 / 3, true)
        if (n == 3) focusPin()
    }

    private fun updateDateDisplay() {
        val chosen = selectedStartDay
        if (chosen == null) {
            binding.tvSelectedStartDate.setText(R.string.onb_chip_not_sure)
            binding.tvRelativeDateBadge.visibility = View.GONE
            return
        }
        val months = resources.getStringArray(R.array.months)
        binding.tvSelectedStartDate.text = getString(
            R.string.onb_date_format,
            Dates.dayOfMonth(chosen).toString(),
            months[Dates.month(chosen) - 1],
            Dates.year(chosen).toString()
        )
        binding.tvRelativeDateBadge.visibility = View.VISIBLE
        binding.tvRelativeDateBadge.text = when (val diff = (today - chosen).toInt()) {
            0 -> getString(R.string.onb_chip_today)
            1 -> getString(R.string.onb_chip_yesterday)
            else -> getString(R.string.onb_ago_days, diff)
        }
    }

    private fun showDatePicker() {
        val c = Dates.calendar(selectedStartDay ?: today)
        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedStartDay = Dates.of(year, month + 1, dayOfMonth)
                updateDateDisplay()
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = today * 86_400_000L
        dialog.show()
    }

    private fun renderSteppers() {
        binding.tvCycleLengthValue.text = cycleLength.toString()
        binding.tvPeriodDurationValue.text = periodDuration.toString()
        binding.tvCycleBadge.text = getString(R.string.onb_common_badge, cycleLength)
        binding.tvPeriodBadge.text = getString(R.string.onb_common_badge, periodDuration)
    }

    private fun focusPin() {
        binding.pinInput.requestFocus()
        binding.pinInput.post {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
            imm.showSoftInput(binding.pinInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun submitPin(pin: String) {
        if (pin.length != 6) return
        when (stage) {
            Stage.PIN_ENTER -> {
                firstPin = pin
                stage = Stage.PIN_CONFIRM
                binding.pinInput.text?.clear()
                binding.pinHintLabel.setText(R.string.lock_confirm_title)
                hideError()
            }
            Stage.PIN_CONFIRM -> {
                if (pin == firstPin) finishSetup(pin)
                else {
                    stage = Stage.PIN_ENTER
                    firstPin = null
                    binding.pinInput.text?.clear()
                    binding.pinHintLabel.setText(R.string.lock_new_title)
                    showError(getString(R.string.lock_mismatch))
                }
            }
        }
    }

    private fun finishSetup(pin: String) {
        val repo = Repository.get(requireContext())
        selectedStartDay?.let { repo.saveDay(it, DayEntry(flow = Flow.MEDIUM)) }
        repo.setBaseline(cycleLength, periodDuration)
        repo.pin.set(pin)
        (activity as? MainActivity)?.openHome()
    }

    private fun showError(msg: String) {
        binding.pinError.text = msg
        binding.pinError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.pinError.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}