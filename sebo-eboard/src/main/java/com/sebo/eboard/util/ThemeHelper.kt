package com.sebo.eboard.util

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.shapes.RoundRectShape
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
            SettingsManager.THEME_LIGHT -> ThemeColors(
                backgroundColor = ContextCompat.getColor(context, R.color.keyboard_background_light),
                keyNormal = ContextCompat.getColor(context, R.color.key_normal_light),
                keyPressed = ContextCompat.getColor(context, R.color.key_pressed_light),
                keyBorder = ContextCompat.getColor(context, R.color.key_border_light),
                keyText = ContextCompat.getColor(context, R.color.key_text_light),
                keyTextSecondary = ContextCompat.getColor(context, R.color.key_text_secondary_light)
            )
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
     * Erstellt ein dynamisches StateListDrawable für Keys basierend auf den Theme-Farben
     */
    fun createKeyDrawable(context: Context): StateListDrawable {
        val colors = getThemeColors(context)
        
        // Erstelle Drawable für gedrückten Zustand
        val pressedShape = RoundRectShape(
            floatArrayOf(6f, 6f, 6f, 6f, 6f, 6f, 6f, 6f),
            null,
            null
        )
        val pressedDrawable = ShapeDrawable(pressedShape).apply {
            paint.color = colors.keyPressed
            paint.strokeWidth = 2f
            paint.style = android.graphics.Paint.Style.FILL_AND_STROKE
        }

        // Erstelle Drawable für normalen Zustand
        val normalShape = RoundRectShape(
            floatArrayOf(6f, 6f, 6f, 6f, 6f, 6f, 6f, 6f),
            null,
            null
        )
        val normalDrawable = ShapeDrawable(normalShape).apply {
            paint.color = colors.keyNormal
            paint.strokeWidth = 2f
            paint.style = android.graphics.Paint.Style.FILL_AND_STROKE
        }

        // Erstelle StateListDrawable
        val stateListDrawable = StateListDrawable()
        stateListDrawable.addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
        stateListDrawable.addState(intArrayOf(), normalDrawable)

        return stateListDrawable
    }
}

