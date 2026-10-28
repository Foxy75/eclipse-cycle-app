package io.github.foxy75.eclipse.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.foxy75.eclipse.MainActivity
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Dates
import io.github.foxy75.eclipse.databinding.FragmentHomeBinding
import io.github.foxy75.eclipse.ui.calendar.CalendarFragment
import io.github.foxy75.eclipse.ui.common.Press
import io.github.foxy75.eclipse.ui.insights.InsightsFragment
import io.github.foxy75.eclipse.ui.log.LogSheet
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
        Press.attach(binding.fab)

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