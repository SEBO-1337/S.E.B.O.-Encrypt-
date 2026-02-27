package com.sebo.eboard.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Verwaltet Einstellungen für die S.E.B.O. E-Board Tastatur
 */
object SettingsManager {

    private const val PREFS_NAME = "sebo_keyboard_settings"

    // Keys
    private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
    private const val KEY_HAPTIC_STRENGTH = "haptic_strength"
    private const val KEY_SOUND_FEEDBACK = "sound_feedback"
    private const val KEY_THEME = "theme"

    // Theme values
    const val THEME_DARK = "dark"
    const val THEME_BLUE = "blue"
    const val THEME_GREEN = "green"

    // Haptic strength
    const val HAPTIC_LIGHT = 0
    const val HAPTIC_MEDIUM = 1
    const val HAPTIC_STRONG = 2

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Haptic Feedback
    fun isHapticFeedbackEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAPTIC_FEEDBACK, true)
    }

    fun setHapticFeedbackEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_HAPTIC_FEEDBACK, enabled) }
    }

    fun getHapticStrength(context: Context): Int {
        return getPrefs(context).getInt(KEY_HAPTIC_STRENGTH, HAPTIC_MEDIUM)
    }

    fun setHapticStrength(context: Context, strength: Int) {
        getPrefs(context).edit { putInt(KEY_HAPTIC_STRENGTH, strength) }
    }

    // Sound Feedback
    fun isSoundFeedbackEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND_FEEDBACK, false)
    }

    fun setSoundFeedbackEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SOUND_FEEDBACK, enabled) }
    }

    // Theme
    fun getTheme(context: Context): String {
        return getPrefs(context).getString(KEY_THEME, THEME_DARK) ?: THEME_DARK
    }

    fun setTheme(context: Context, theme: String) {
        getPrefs(context).edit { putString(KEY_THEME, theme) }
    }
}

