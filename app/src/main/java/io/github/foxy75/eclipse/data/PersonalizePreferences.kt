package io.github.foxy75.eclipse.data

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import io.github.foxy75.eclipse.R

object PersonalizePreferences {
    private const val PREF_NAME = "eclipse_personalize"
    private const val KEY_THEME = "theme_key"
    private const val KEY_AVATAR = "avatar_key"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_MOTTO = "motto_index"
    private const val KEY_NIGHT_MODE = "night_mode_key"

    const val MODE_LIGHT = "light"
    const val MODE_DARK = "dark"
    const val MODE_SYSTEM = "system"

    const val THEME_TERRACOTTA = "terracotta"
    const val THEME_FLO_ROSE = "flo_rose"
    const val THEME_LAVENDER = "lavender"
    const val THEME_SAGE = "sage"
    const val THEME_MIDNIGHT = "midnight"

    data class ThemeOption(
        val key: String,
        val name: String,
        val mood: String,
        val primaryColorRes: Int,
        val secondaryColorRes: Int,
        val themeRes: Int
    )

    data class AvatarOption(
        val key: String,
        val name: String,
        val drawableRes: Int
    )

    val THEMES = listOf(
        ThemeOption(
            THEME_TERRACOTTA,
            "Terracotta",
            "Hangat & Berakar",
            R.color.palette_terracotta_primary,
            R.color.palette_terracotta_secondary,
            R.style.Theme_Eclipse
        ),
        ThemeOption(
            THEME_FLO_ROSE,
            "Flo Rose",
            "Lembut Khas Flo",
            R.color.palette_flo_primary,
            R.color.palette_flo_secondary,
            R.style.Theme_Eclipse_FloRose
        ),
        ThemeOption(
            THEME_LAVENDER,
            "Lavender Mist",
            "Menenangkan PMS",
            R.color.palette_lavender_primary,
            R.color.palette_lavender_secondary,
            R.style.Theme_Eclipse_Lavender
        ),
        ThemeOption(
            THEME_SAGE,
            "Sage & Matcha",
            "Segar & Alami",
            R.color.palette_sage_primary,
            R.color.palette_sage_secondary,
            R.style.Theme_Eclipse_Sage
        ),
        ThemeOption(
            THEME_MIDNIGHT,
            "Midnight Velvet",
            "Kemewahan Tenang",
            R.color.palette_midnight_primary,
            R.color.palette_midnight_secondary,
            R.style.Theme_Eclipse_Midnight
        )
    )

    val AVATARS = listOf(
        AvatarOption("moon", "Sabit Eclipse", R.drawable.ic_avatar_moon),
        AvatarOption("flower", "Bunga Flo", R.drawable.ic_avatar_flower),
        AvatarOption("sparkles", "Kilau Bintang", R.drawable.ic_sparkles),
        AvatarOption("leaf", "Daun Sage", R.drawable.ic_avatar_leaf),
        AvatarOption("butterfly", "Kupu-Kupu", R.drawable.ic_avatar_butterfly),
        AvatarOption("zen", "Ketenangan Zen", R.drawable.ic_avatar_zen),
        AvatarOption("coffee", "Cangkir Hangat", R.drawable.ic_avatar_coffee),
        AvatarOption("cat", "Kucing Manja", R.drawable.ic_avatar_cat)
    )

    val MOTTOS = listOf(
        "Tubuhku tahu ritmenya, aku menghargainya.",
        "Hari ini aku melambat dan bernapas lega.",
        "Penuh energi positif dan selaras dengan alam.",
        "Merawat diri dengan kelembutan dan kesabaran."
    )

    fun getNightMode(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NIGHT_MODE, MODE_LIGHT) ?: MODE_LIGHT

    fun setNightMode(context: Context, mode: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NIGHT_MODE, mode)
            .apply()
    }

    fun applyNightMode(context: Context) {
        val mode = getNightMode(context)
        val appCompatMode = when (mode) {
            MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(appCompatMode)
    }

    fun isNight(context: Context): Boolean {
        val mode = getNightMode(context)
        return when (mode) {
            MODE_LIGHT -> false
            MODE_DARK -> true
            else -> {
                val current = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                current == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun getThemeKey(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_TERRACOTTA) ?: THEME_TERRACOTTA

    fun setThemeKey(context: Context, key: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, key)
            .apply()
    }

    fun getThemeRes(context: Context): Int {
        val key = getThemeKey(context)
        return THEMES.find { it.key == key }?.themeRes ?: R.style.Theme_Eclipse
    }

    fun getAvatarKey(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AVATAR, "moon") ?: "moon"

    fun setAvatarKey(context: Context, key: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AVATAR, key)
            .apply()
    }

    fun getAvatarDrawableRes(context: Context): Int {
        val key = getAvatarKey(context)
        return AVATARS.find { it.key == key }?.drawableRes ?: R.drawable.ic_avatar_moon
    }

    fun getNickname(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NICKNAME, "Luna") ?: "Luna"

    fun setNickname(context: Context, nickname: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NICKNAME, nickname.trim())
            .apply()
    }

    fun getMottoIndex(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MOTTO, 0).coerceIn(0, MOTTOS.lastIndex)

    fun setMottoIndex(context: Context, index: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MOTTO, index.coerceIn(0, MOTTOS.lastIndex))
            .apply()
    }
}
