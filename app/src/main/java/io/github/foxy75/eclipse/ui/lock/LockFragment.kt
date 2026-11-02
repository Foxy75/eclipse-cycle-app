package io.github.foxy75.eclipse.ui.lock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import io.github.foxy75.eclipse.MainActivity
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.FragmentLockBinding
import io.github.foxy75.eclipse.ui.common.PinDots
import io.github.foxy75.eclipse.ui.common.Press

class LockFragment : Fragment() {

    private var _binding: FragmentLockBinding? = null
    private val binding get() = _binding!!

    private val pin = StringBuilder()
    private var dots: List<View> = emptyList()

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val lock = Repository.get(requireContext()).pin
            val remaining = lock.remainingLockedMillis()
            if (remaining > 0L) {
                binding.tvLockoutCountdown.text = getString(
                    R.string.lock_locked_value, formatDuration(remaining)
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
        applyWindowInsets()
        dots = PinDots.build(binding.pinDots)

        val keys: List<View> = listOf(
            binding.btnPin0, binding.btnPin1, binding.btnPin2,
            binding.btnPin3, binding.btnPin4, binding.btnPin5,
            binding.btnPin6, binding.btnPin7, binding.btnPin8, binding.btnPin9
        )
        Press.attach(*(keys + binding.btnPinBackspace).toTypedArray())

        keys.forEachIndexed { digit, key ->
            key.setOnClickListener { onDigit(digit) }
        }
        binding.btnPinBackspace.setOnClickListener { onBackspace() }

        if (Repository.get(requireContext()).pin.remainingLockedMillis() > 0L) {
            showLocked()
        } else {
            showEntry()
        }
    }

    private fun applyWindowInsets() {
        val basePaddingTop = resources.getDimensionPixelSize(R.dimen.space_24)
        val basePaddingBottom = resources.getDimensionPixelSize(R.dimen.space_24)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBar = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.lockRoot.updatePadding(
                top = basePaddingTop + statusBar.top,
                bottom = basePaddingBottom + navBar.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun onDigit(digit: Int) {
        if (pin.length >= 6) return
        pin.append(digit)
        PinDots.render(dots, pin.length)
        binding.msg.visibility = View.GONE
        if (pin.length == 6) submit(pin.toString())
    }

    private fun onBackspace() {
        if (pin.isEmpty()) return
        pin.deleteCharAt(pin.length - 1)
        PinDots.render(dots, pin.length)
        binding.msg.visibility = View.GONE
    }

    private fun submit(entered: String) {
        val check = Repository.get(requireContext()).pin.check(entered)
        when {
            check.unlocked -> {
                handler.removeCallbacks(tick)
                (activity as? MainActivity)?.openHome()
            }
            check.locked -> showLocked()
            else -> {
                binding.msg.visibility = View.VISIBLE
                binding.msg.text = getString(R.string.lock_wrong, check.attemptsLeft)
                pin.clear()
                PinDots.render(dots, 0)
            }
        }
    }

    private fun showEntry() {
        handler.removeCallbacks(tick)
        binding.containerPinLocked.visibility = View.GONE
        binding.containerPinNormal.visibility = View.VISIBLE
        pin.clear()
        PinDots.render(dots, 0)
        binding.msg.visibility = View.GONE
    }

    private fun showLocked() {
        handler.removeCallbacks(tick)
        binding.containerPinNormal.visibility = View.GONE
        binding.containerPinLocked.visibility = View.VISIBLE
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