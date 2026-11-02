package io.github.foxy75.eclipse

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.foxy75.eclipse.data.PersonalizePreferences
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.ActivityMainBinding
import io.github.foxy75.eclipse.ui.home.HomeFragment
import io.github.foxy75.eclipse.ui.lock.LockFragment
import io.github.foxy75.eclipse.ui.onboarding.OnboardingFragment
import io.github.foxy75.eclipse.ui.settings.SettingsFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        PersonalizePreferences.applyNightMode(this)
        val themeRes = PersonalizePreferences.getThemeRes(this)
        setTheme(themeRes)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val isNight = PersonalizePreferences.isNight(this)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isNight
            isAppearanceLightNavigationBars = !isNight
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = Repository.get(this)
        applySecureFlag()
        lifecycleScope.launch {
            repo.init()
            showGate()
        }
    }

    fun applySecureFlag() {
        val flags = WindowManager.LayoutParams.FLAG_SECURE
        if (repo.secureFlag()) window.addFlags(flags) else window.clearFlags(flags)
    }

    private fun showGate() {
        when {
            !repo.pin.isSet() -> showFragment(OnboardingFragment())
            repo.pin.remainingLockedMillis() > 0L -> showFragment(LockFragment())
            else -> showFragment(HomeFragment())
        }
    }

    fun showFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val tx = supportFragmentManager.beginTransaction().replace(R.id.container, fragment)
        if (addToBackStack) tx.addToBackStack(null)
        tx.commit()
    }

    fun openHome() {
        showFragment(HomeFragment())
    }

    fun openSettings() {
        showFragment(SettingsFragment(), addToBackStack = true)
    }

    fun lockNow() {
        showFragment(LockFragment())
    }

    override fun onStop() {
        super.onStop()
        repo.flush()
    }
}