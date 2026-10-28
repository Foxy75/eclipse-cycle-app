package io.github.foxy75.eclipse.ui.lock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.foxy75.eclipse.MainActivity
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.FragmentLockBinding
import io.github.foxy75.eclipse.ui.common.PinDots

class LockFragment : Fragment() {

    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val lock = Repository.get(requireContext()).pin
            val remaining = lock.remainingLockedMillis()
            if (remaining > 0L) {
                binding.msg.text = getString(
                    R.string.lock_locked_body, formatDuration(remaining)
                )
                handler.postDelayed(this, 1000L)
            } else {
                showEntry()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PinDots.build(binding.pinDots)

        binding.pinInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                renderDots(text.length)
                if (text.length == 6) submit(text)
            }
        })

        if (Repository.get(requireContext()).pin.remainingLockedMillis() > 0L) {
            showLocked()
        } else {
            showEntry()
        }
    }

    private fun renderDots(filled: Int) {
        val dots = binding.pinDots
        val children = (0 until dots.childCount).map { dots.getChildAt(it) }
        PinDots.render(children, filled)
    }

    private fun submit(pin: String) {
        val check = Repository.get(requireContext()).pin.check(pin)
        when {
            check.unlocked -> {
                handler.removeCallbacks(tick)
                (activity as? MainActivity)?.openHome()
            }
            check.locked -> showLocked()
            else -> {
                binding.msg.visibility = View.VISIBLE
                binding.msg.text = getString(R.string.lock_wrong, check.attemptsLeft)
                binding.pinInput.text?.clear()
            }
        }
    }

    private fun showEntry() {
        handler.removeCallbacks(tick)
        binding.tvTitle.setText(R.string.lock_title)
        binding.tvSubtitle.visibility = View.VISIBLE
        binding.tvSubtitle.setText(R.string.lock_subtitle)
        binding.pinDots.visibility = View.VISIBLE
        binding.pinInput.visibility = View.INVISIBLE
        binding.msg.visibility = View.GONE
        binding.pinInput.requestFocus()
    }

    private fun showLocked() {
        handler.removeCallbacks(tick)
        binding.tvTitle.setText(R.string.lock_locked_title)
        binding.tvSubtitle.visibility = View.GONE
        binding.pinDots.visibility = View.GONE
        binding.pinInput.visibility = View.GONE
        binding.msg.visibility = View.VISIBLE
        tick.run()
    }

    private fun formatDuration(ms: Long): String {
        val days = ms / 86_400_000L
        val hours = (ms % 86_400_000L) / 3_600_000L
        val mins = (ms % 3_600_000L) / 60_000L
        return if (days > 0) getString(R.string.time_days_hours, days, hours)
        else getString(R.string.time_hours_mins, hours, mins)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(tick)
        _binding = null
    }
}