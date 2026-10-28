package io.github.foxy75.eclipse.ui.onboarding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.github.foxy75.eclipse.MainActivity
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.FragmentOnboardingBinding
import io.github.foxy75.eclipse.ui.common.PinDots
import io.github.foxy75.eclipse.ui.common.Press

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private enum class Stage { FIRST, CONFIRM }

    private var stage = Stage.FIRST
    private var firstPin: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Press.attach(binding.btnContinue, binding.btnFinish)
        PinDots.build(binding.pinDots)

        binding.btnContinue.setOnClickListener {
            binding.step1.visibility = View.GONE
            binding.step2.visibility = View.VISIBLE
            focusPin()
        }

        binding.btnFinish.setOnClickListener { finalizeStep2() }

        binding.pinInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                val dots = binding.pinDots
                if (dots != null) {
                    val children = (0 until dots.childCount).map { dots.getChildAt(it) }
                    PinDots.render(children, text.length)
                }
                if (text.length == 6) finalizeStep2() else hideError()
            }
        })
    }

    private fun focusPin() {
        binding.pinInput.requestFocus()
        binding.pinInput.post {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
            imm.showSoftInput(binding.pinInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun finalizeStep2() {
        val pin = binding.pinInput.text.toString()
        if (pin.length != 6) return

        when (stage) {
            Stage.FIRST -> {
                firstPin = pin
                stage = Stage.CONFIRM
                binding.pinInput.text?.clear()
                binding.pinHintLabel.text = getString(R.string.onb_pin_confirm)
                hideError()
            }
            Stage.CONFIRM -> {
                if (pin == firstPin) {
                    Repository.get(requireContext()).pin.set(pin)
                    (activity as? MainActivity)?.openHome()
                } else {
                    stage = Stage.FIRST
                    firstPin = null
                    binding.pinInput.text?.clear()
                    binding.pinHintLabel.text = getString(R.string.onb_pin_enter)
                    showError(getString(R.string.onb_pin_mismatch))
                }
            }
        }
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