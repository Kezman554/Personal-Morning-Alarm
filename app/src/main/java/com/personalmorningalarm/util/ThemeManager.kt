package com.personalmorningalarm.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Light / Dark / Follow-system, mapped to the AppCompat night-mode constants. */
enum class ThemeMode(val key: String, val nightMode: Int) {
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/**
 * Persists the user's theme choice and applies it via [AppCompatDelegate]. Applying
 * while an activity is on screen recreates it, so the change shows immediately;
 * [applySaved] is called from Application.onCreate so the right theme is set from
 * app start.
 */
class ThemeManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode = ThemeMode.fromKey(prefs.getString(KEY_THEME, null))

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.key).apply()
        apply(mode)
    }

    /** Applies the saved theme (defaults to Follow-system). */
    fun applySaved() = apply(getThemeMode())

    private fun apply(mode: ThemeMode) = AppCompatDelegate.setDefaultNightMode(mode.nightMode)

    companion object {
        const val PREFS_NAME = "pma_settings"
        private const val KEY_THEME = "theme_mode"
    }
}
