package com.sebo.eboard.util

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import androidx.core.content.ContextCompat
import com.sebo.eboard.R
import com.sebo.eboard.manager.SettingsManager

/**
 * Helper-Klasse für Theme-Farben
 */
object ThemeHelper {

    data class ThemeColors(
        val backgroundColor: Int,
        val keyNormal: Int,
        val keyPressed: Int,
        val keyBorder: Int,
        val keyText: Int,
        val keyTextSecondary: Int
    )

    fun getThemeColors(context: Context): ThemeColors {
        val theme = SettingsManager.getTheme(context)

        return when (theme) {
            SettingsManager.THEME_BLUE -> ThemeColors(
                backgroundColor = ContextCompat.getColor(context, R.color.keyboard_background_blue),
                keyNormal = ContextCompat.getColor(context, R.color.key_normal_blue),
                keyPressed = ContextCompat.getColor(context, R.color.key_pressed_blue),
                keyBorder = ContextCompat.getColor(context, R.color.key_border_blue),
                keyText = ContextCompat.getColor(context, R.color.key_text_blue),
                keyTextSecondary = ContextCompat.getColor(context, R.color.key_text_secondary_blue)
            )
            SettingsManager.THEME_GREEN -> ThemeColors(
                backgroundColor = ContextCompat.getColor(context, R.color.keyboard_background_green),
                keyNormal = ContextCompat.getColor(context, R.color.key_normal_green),
                keyPressed = ContextCompat.getColor(context, R.color.key_pressed_green),
                keyBorder = ContextCompat.getColor(context, R.color.key_border_green),
                keyText = ContextCompat.getColor(context, R.color.key_text_green),
                keyTextSecondary = ContextCompat.getColor(context, R.color.key_text_secondary_green)
            )
            else -> ThemeColors( // THEME_DARK
                backgroundColor = ContextCompat.getColor(context, R.color.keyboard_background),
                keyNormal = ContextCompat.getColor(context, R.color.key_normal),
                keyPressed = ContextCompat.getColor(context, R.color.key_pressed),
                keyBorder = ContextCompat.getColor(context, R.color.key_border),
                keyText = ContextCompat.getColor(context, R.color.key_text),
                keyTextSecondary = ContextCompat.getColor(context, R.color.key_text_secondary)
            )
        }
    }

    /**
     * Erstellt ein futuristisches StateListDrawable für Keys mit Gradient und Schatten
     */
    fun createKeyDrawable(context: Context): StateListDrawable {
        val themeColors = getThemeColors(context)

        // Berechne heller und dunkler Varianten für Gradient
        val normalLight = lightenColor(themeColors.keyNormal, 1.3f)
        val normalDark = darkenColor(themeColors.keyNormal, 0.7f)

        val pressedLight = lightenColor(themeColors.keyPressed, 1.2f)
        val pressedDark = darkenColor(themeColors.keyPressed, 0.8f)

        // Normaler Zustand - mit Gradient und Schatten
        val normalGradient = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(normalLight, themeColors.keyNormal, normalDark)
            cornerRadius = 14f
            setStroke(2, themeColors.keyBorder)
        }

        // Gedrückter Zustand - dunklerer Gradient
        val pressedGradient = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            colors = intArrayOf(pressedLight, themeColors.keyPressed, pressedDark)
            cornerRadius = 14f
            setStroke(2, themeColors.keyBorder)
        }

        // Erstelle StateListDrawable
        val stateListDrawable = StateListDrawable()
        stateListDrawable.addState(intArrayOf(android.R.attr.state_pressed), pressedGradient)
        stateListDrawable.addState(intArrayOf(), normalGradient)

        return stateListDrawable
    }

    /**
     * Hellt eine Farbe auf
     */
    private fun lightenColor(color: Int, factor: Float): Int {
        val r = ((color shr 16) and 0xFF) * factor
        val g = ((color shr 8) and 0xFF) * factor
        val b = (color and 0xFF) * factor

        return (0xFF shl 24) or
            (r.toInt().coerceIn(0, 255) shl 16) or
            (g.toInt().coerceIn(0, 255) shl 8) or
            b.toInt().coerceIn(0, 255)
    }

    /**
     * Verdunkelt eine Farbe
     */
    private fun darkenColor(color: Int, factor: Float): Int {
        val r = ((color shr 16) and 0xFF) * factor
        val g = ((color shr 8) and 0xFF) * factor
        val b = (color and 0xFF) * factor

        return (0xFF shl 24) or
            (r.toInt().coerceIn(0, 255) shl 16) or
            (g.toInt().coerceIn(0, 255) shl 8) or
            b.toInt().coerceIn(0, 255)
    }
}

