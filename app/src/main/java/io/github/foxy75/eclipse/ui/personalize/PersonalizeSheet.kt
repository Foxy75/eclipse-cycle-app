package io.github.foxy75.eclipse.ui.personalize

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.data.PersonalizePreferences
import io.github.foxy75.eclipse.databinding.SheetPersonalizeBinding
import io.github.foxy75.eclipse.ui.common.Press

class PersonalizeSheet : BottomSheetDialogFragment() {

    private var _binding: SheetPersonalizeBinding? = null
    private val binding get() = _binding!!

    private var selectedNightMode = PersonalizePreferences.MODE_LIGHT
    private var selectedAvatarIndex = 0
    private var selectedThemeIndex = 0
    private var selectedMottoIndex = 0

    private lateinit var avatarButtons: List<FrameLayout>
    private lateinit var avatarIcons: List<ImageView>
    private lateinit var themeCards: List<LinearLayout>
    private lateinit var themeChecks: List<ImageView>
    private lateinit var mottoButtons: List<TextView>

    companion object {
        fun newInstance(): PersonalizeSheet = PersonalizeSheet()
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetPersonalizeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews()
        applyWindowInsets()
        loadPreferences()
        setupListeners()
    }

    private fun applyWindowInsets() {
        val basePaddingBottom = resources.getDimensionPixelSize(R.dimen.space_28)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(navBars.bottom, ime.bottom)
            binding.personalizeContainer.updatePadding(bottom = basePaddingBottom + bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun initViews() {
        avatarButtons = listOf(
            binding.btnAvatar0, binding.btnAvatar1, binding.btnAvatar2, binding.btnAvatar3,
            binding.btnAvatar4, binding.btnAvatar5, binding.btnAvatar6, binding.btnAvatar7
        )

        avatarIcons = listOf(
            binding.iconAvatar0, binding.iconAvatar1, binding.iconAvatar2, binding.iconAvatar3,
            binding.iconAvatar4, binding.iconAvatar5, binding.iconAvatar6, binding.iconAvatar7
        )

        themeCards = listOf(
            binding.cardTheme0, binding.cardTheme1, binding.cardTheme2,
            binding.cardTheme3, binding.cardTheme4
        )

        themeChecks = listOf(
            binding.checkTheme0, binding.checkTheme1, binding.checkTheme2,
            binding.checkTheme3, binding.checkTheme4
        )

        mottoButtons = listOf(
            binding.btnMotto0, binding.btnMotto1, binding.btnMotto2, binding.btnMotto3
        )

        Press.attach(
            binding.btnClose, binding.btnSave,
            binding.cardModeLight, binding.cardModeDark, binding.cardModeSystem,
            *avatarButtons.toTypedArray(),
            *themeCards.toTypedArray(),
            *mottoButtons.toTypedArray()
        )
    }

    private fun loadPreferences() {
        val ctx = requireContext()
        selectedNightMode = PersonalizePreferences.getNightMode(ctx)

        val currentAvatar = PersonalizePreferences.getAvatarKey(ctx)
        selectedAvatarIndex = PersonalizePreferences.AVATARS.indexOfFirst { it.key == currentAvatar }
            .takeIf { it >= 0 } ?: 0

        val currentTheme = PersonalizePreferences.getThemeKey(ctx)
        selectedThemeIndex = PersonalizePreferences.THEMES.indexOfFirst { it.key == currentTheme }
            .takeIf { it >= 0 } ?: 0

        selectedMottoIndex = PersonalizePreferences.getMottoIndex(ctx)
        binding.inputNickname.setText(PersonalizePreferences.getNickname(ctx))

        updateNightModeUI()
        updateAvatarUI()
        updateThemeUI()
        updateMottoUI()
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { dismiss() }

        binding.cardModeLight.setOnClickListener {
            selectedNightMode = PersonalizePreferences.MODE_LIGHT
            updateNightModeUI()
        }

        binding.cardModeDark.setOnClickListener {
            selectedNightMode = PersonalizePreferences.MODE_DARK
            updateNightModeUI()
        }

        binding.cardModeSystem.setOnClickListener {
            selectedNightMode = PersonalizePreferences.MODE_SYSTEM
            updateNightModeUI()
        }

        avatarButtons.forEachIndexed { idx, btn ->
            btn.setOnClickListener {
                selectedAvatarIndex = idx
                updateAvatarUI()
            }
        }

        themeCards.forEachIndexed { idx, card ->
            card.setOnClickListener {
                selectedThemeIndex = idx
                updateThemeUI()
            }
        }

        mottoButtons.forEachIndexed { idx, btn ->
            btn.setOnClickListener {
                selectedMottoIndex = idx
                updateMottoUI()
            }
        }

        binding.btnSave.setOnClickListener {
            saveAndApply()
        }
    }

    private fun updateNightModeUI() {
        val ctx = requireContext()
        val activeThemeColor = ContextCompat.getColor(
            ctx, PersonalizePreferences.THEMES[selectedThemeIndex].primaryColorRes
        )
        val inkColor = ContextCompat.getColor(ctx, R.color.ink)
        val inkSecColor = ContextCompat.getColor(ctx, R.color.ink_secondary)

        val isLight = selectedNightMode == PersonalizePreferences.MODE_LIGHT
        val isDark = selectedNightMode == PersonalizePreferences.MODE_DARK
        val isSystem = selectedNightMode == PersonalizePreferences.MODE_SYSTEM

        binding.cardModeLight.setBackgroundResource(
            if (isLight) R.drawable.bg_theme_card_selected else R.drawable.bg_theme_card
        )
        binding.iconModeLight.setColorFilter(if (isLight) activeThemeColor else inkSecColor)
        binding.labelModeLight.setTextColor(if (isLight) inkColor else inkSecColor)

        binding.cardModeDark.setBackgroundResource(
            if (isDark) R.drawable.bg_theme_card_selected else R.drawable.bg_theme_card
        )
        binding.iconModeDark.setColorFilter(if (isDark) activeThemeColor else inkSecColor)
        binding.labelModeDark.setTextColor(if (isDark) inkColor else inkSecColor)

        binding.cardModeSystem.setBackgroundResource(
            if (isSystem) R.drawable.bg_theme_card_selected else R.drawable.bg_theme_card
        )
        binding.iconModeSystem.setColorFilter(if (isSystem) activeThemeColor else inkSecColor)
        binding.labelModeSystem.setTextColor(if (isSystem) inkColor else inkSecColor)
    }

    private fun updateAvatarUI() {
        val ctx = requireContext()
        val activeThemeColor = ContextCompat.getColor(
            ctx, PersonalizePreferences.THEMES[selectedThemeIndex].primaryColorRes
        )

        avatarButtons.forEachIndexed { idx, btn ->
            val isSelected = idx == selectedAvatarIndex
            btn.setBackgroundResource(
                if (isSelected) R.drawable.bg_theme_card_selected else R.drawable.bg_theme_card
            )
            avatarIcons[idx].setColorFilter(
                if (isSelected) activeThemeColor else ContextCompat.getColor(ctx, R.color.ink_secondary)
            )
        }
        val avatarRes = PersonalizePreferences.AVATARS[selectedAvatarIndex].drawableRes
        binding.avatarPreview.setImageResource(avatarRes)
        binding.avatarPreview.backgroundTintList = ColorStateList.valueOf(activeThemeColor)
    }

    private fun updateThemeUI() {
        themeCards.forEachIndexed { idx, card ->
            val isSelected = idx == selectedThemeIndex
            card.setBackgroundResource(
                if (isSelected) R.drawable.bg_theme_card_selected else R.drawable.bg_theme_card
            )
            themeChecks[idx].visibility = if (isSelected) View.VISIBLE else View.GONE
        }
        updateNightModeUI()
        updateAvatarUI()
    }

    private fun updateMottoUI() {
        val ctx = requireContext()
        mottoButtons.forEachIndexed { idx, btn ->
            val isSelected = idx == selectedMottoIndex
            btn.setBackgroundResource(
                if (isSelected) R.drawable.bg_theme_card_selected else R.drawable.bg_theme_card
            )
            btn.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (isSelected) R.color.ink else R.color.ink_secondary
                )
            )
        }
    }

    private fun saveAndApply() {
        val ctx = requireContext()
        val oldTheme = PersonalizePreferences.getThemeKey(ctx)
        val newTheme = PersonalizePreferences.THEMES[selectedThemeIndex].key
        val oldNightMode = PersonalizePreferences.getNightMode(ctx)
        val newNightMode = selectedNightMode
        val newAvatar = PersonalizePreferences.AVATARS[selectedAvatarIndex].key
        val nickname = binding.inputNickname.text?.toString()?.trim().orEmpty()

        PersonalizePreferences.setAvatarKey(ctx, newAvatar)
        PersonalizePreferences.setThemeKey(ctx, newTheme)
        PersonalizePreferences.setNightMode(ctx, newNightMode)
        PersonalizePreferences.setMottoIndex(ctx, selectedMottoIndex)
        if (nickname.isNotEmpty()) {
            PersonalizePreferences.setNickname(ctx, nickname)
        }

        PersonalizePreferences.applyNightMode(ctx)
        dismiss()

        val activity = requireActivity()
        if (oldTheme != newTheme || oldNightMode != newNightMode) {
            activity.recreate()
        } else {
            val hostAvatar = activity.findViewById<ImageView>(R.id.avatar)
            hostAvatar?.setImageResource(PersonalizePreferences.getAvatarDrawableRes(ctx))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
