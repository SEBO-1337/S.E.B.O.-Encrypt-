package com.sebo.eboard.util

import android.content.Context
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

/**
 * Custom KeyboardView mit Longpress-Popup für Umlaute
 */
@Suppress("DEPRECATION")
class LongPressKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : KeyboardView(context, attrs, defStyleAttr) {

    companion object {
        private const val LONG_PRESS_TIMEOUT = 500L

        // Mapping von Codes zu Umlaut-Alternativen
        private val UMLAUT_MAP = mapOf(
            117 to 'ü',  // u -> ü
            111 to 'ö',  // o -> ö
            97 to 'ä',   // a -> ä
            115 to 'ß'   // s -> ß
        )
    }

    private var popupWindow: PopupWindow? = null
    private var longPressHandler = Handler(Looper.getMainLooper())
    private var lastPressedKey: Keyboard.Key? = null
    private var lastX = 0
    private var lastY = 0
    private var isLongPressed = false

    private val longPressRunnable = Runnable {
        if (lastPressedKey != null && UMLAUT_MAP.containsKey(lastPressedKey!!.codes[0])) {
            isLongPressed = true
            showUmlautPopup(lastPressedKey!!, lastX, lastY)
        }
    }

    @Suppress("DEPRECATION")
    override fun onTouchEvent(me: MotionEvent): Boolean {
        val action = me.action
        val x = me.x.toInt()
        val y = me.y.toInt()

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = x
                lastY = y
                isLongPressed = false
                lastPressedKey = findKeyAt(x, y)

                // Starte Longpress-Timer wenn es eine Umlaut-Taste ist
                if (lastPressedKey != null && UMLAUT_MAP.containsKey(lastPressedKey!!.codes[0])) {
                    longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT)
                }
                // Weitergabe an super für normale Keyboard-Verarbeitung
                return super.onTouchEvent(me)
            }

            MotionEvent.ACTION_MOVE -> {
                // Wenn Finger zu weit weg, cancel longpress
                val currentKey = findKeyAt(x, y)
                if (currentKey != lastPressedKey) {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    dismissPopup()
                    isLongPressed = false
                }
                return super.onTouchEvent(me)
            }

            MotionEvent.ACTION_UP -> {
                longPressHandler.removeCallbacks(longPressRunnable)

                // Wenn Longpress aktiv ist, popup schließen aber keine normale key-verarbeitung
                if (isLongPressed) {
                    dismissPopup()
                    isLongPressed = false
                    // Vermeide doppelte Eingabe
                    return true
                }

                // Normale Verarbeitung wenn kein Longpress
                isLongPressed = false
                return super.onTouchEvent(me)
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                dismissPopup()
                isLongPressed = false
                return super.onTouchEvent(me)
            }
        }

        return super.onTouchEvent(me)
    }

    private fun findKeyAt(x: Int, y: Int): Keyboard.Key? {
        val keys = keyboard?.keys ?: return null
        for (key in keys) {
            if (key.isInside(x, y)) {
                return key
            }
        }
        return null
    }

    private fun showUmlautPopup(key: Keyboard.Key, x: Int, y: Int) {
        val umlaut = UMLAUT_MAP[key.codes[0]] ?: return

        dismissPopup()

        // Erstelle ein einfaches Popup mit 2 Buttons
        val popupView = LinearLayout(context)
        popupView.orientation = LinearLayout.HORIZONTAL
        popupView.setBackgroundColor(0xFF333333.toInt())

        // Normale Taste
        val normalButton = createUmlautButton(key.label.toString(), key.codes[0])
        popupView.addView(normalButton)

        // Umlaut-Taste
        val umlautButton = createUmlautButton(umlaut.toString(), umlaut.code)
        popupView.addView(umlautButton)

        popupWindow = PopupWindow(popupView, 180, 80, true)
        popupWindow?.elevation = 10f
        popupWindow?.isOutsideTouchable = true

        val screenLocation = IntArray(2)
        getLocationOnScreen(screenLocation)
        popupWindow?.showAtLocation(
            this,
            Gravity.NO_GRAVITY,
            screenLocation[0] + x - 90,
            screenLocation[1] + y - 150
        )
    }

    private fun createUmlautButton(text: String, code: Int): TextView {
        val button = TextView(context)
        button.text = text
        button.textSize = 24f
        button.gravity = Gravity.CENTER
        button.layoutParams = LinearLayout.LayoutParams(90, 80)
        button.setTextColor(0xFFFFFFFF.toInt())
        button.setBackgroundColor(0xFF666666.toInt())

        button.setOnClickListener {
            // Sende den Code an den KeyboardActionListener
            onKeyboardActionListener?.let {
                it.onKey(code, intArrayOf(code))
                it.onRelease(code)
            }
            dismissPopup()
        }

        return button
    }

    private fun dismissPopup() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}




