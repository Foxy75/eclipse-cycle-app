package io.github.foxy75.eclipse.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import io.github.foxy75.eclipse.MainActivity
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Dates
import io.github.foxy75.eclipse.data.PersonalizePreferences
import io.github.foxy75.eclipse.databinding.FragmentHomeBinding
import io.github.foxy75.eclipse.ui.calendar.CalendarFragment
import io.github.foxy75.eclipse.ui.common.Press
import io.github.foxy75.eclipse.ui.insights.InsightsFragment
import io.github.foxy75.eclipse.ui.log.LogSheet
import io.github.foxy75.eclipse.ui.personalize.PersonalizeSheet
import io.github.foxy75.eclipse.ui.today.TodayFragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Press.attach(binding.fab, binding.avatar, binding.btnSettings, binding.btnThemeMode)

        applyWindowInsets()
        updateThemeModeIcon()

        binding.btnThemeMode.setOnClickListener {
            val ctx = requireContext()
            val isNight = PersonalizePreferences.isNight(ctx)
            val newMode = if (isNight) PersonalizePreferences.MODE_LIGHT else PersonalizePreferences.MODE_DARK
            PersonalizePreferences.setNightMode(ctx, newMode)
            PersonalizePreferences.applyNightMode(ctx)
            val label = if (newMode == PersonalizePreferences.MODE_LIGHT) "Mode Terang aktif" else "Mode Gelap aktif"
            android.widget.Toast.makeText(ctx, label, android.widget.Toast.LENGTH_SHORT).show()
            requireActivity().recreate()
        }

        binding.avatar.setImageResource(PersonalizePreferences.getAvatarDrawableRes(requireContext()))
        binding.avatar.setOnClickListener {
            PersonalizeSheet.newInstance().show(childFragmentManager, "personalize")
        }

        binding.btnSettings.setOnClickListener { (activity as? MainActivity)?.openSettings() }

        binding.fab.setOnClickListener {
            LogSheet.newInstance(Dates.today())
                .show(requireActivity().supportFragmentManager, "log")
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_today
        }
    }

    private fun applyWindowInsets() {
        val baseTopPadding = resources.getDimensionPixelSize(R.dimen.space_8)
        val baseNavHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
        val fabClearance = (24 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBar = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.homeTopBar.updatePadding(top = baseTopPadding + statusBar.top)
            binding.bottomNav.updatePadding(bottom = navBar.bottom)

            val fabLp = binding.fab.layoutParams as? ViewGroup.MarginLayoutParams
            fabLp?.let {
                it.bottomMargin = baseNavHeight + navBar.bottom + fabClearance
                binding.fab.layoutParams = it
            }

            insets
        }

        binding.bottomNav.post {
            val fabLp = binding.fab.layoutParams as? ViewGroup.MarginLayoutParams
            fabLp?.let {
                val actualNavHeight = maxOf(baseNavHeight, binding.bottomNav.height)
                it.bottomMargin = actualNavHeight + fabClearance
                binding.fab.layoutParams = it
            }
        }

        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun updateThemeModeIcon() {
        val isNight = PersonalizePreferences.isNight(requireContext())
        binding.btnThemeMode.setImageResource(if (isNight) R.drawable.ic_sun else R.drawable.ic_moon)
        binding.btnThemeMode.contentDescription = if (isNight) "Beralih ke Mode Terang" else "Beralih ke Mode Gelap"
    }

    private fun showTab(itemId: Int) {
        val fragment = when (itemId) {
            R.id.nav_calendar -> CalendarFragment()
            R.id.nav_insights -> InsightsFragment()
            else -> TodayFragment()
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.tabContainer, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}