package com.sebo.eboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import android.view.inputmethod.EditorInfo
import com.sebo.eboard.crypto.CryptoEngine
import com.sebo.eboard.manager.ContactManager

/**
 * S.E.B.O. E-Board - Custom Keyboard Service
 *
 * Diese Tastatur ermöglicht systemweites Verschlüsseln und Entschlüsseln von Texten.
 */
class CustomKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var keyboard: Keyboard
    private var isShifted = false
    private var isCapsLock = false

    // Aktueller Session-Key für Ver-/Entschlüsselung
    private var currentSessionKey: ByteArray? = null

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        keyboard = Keyboard(this, R.xml.qwerty)
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)

        // Lade aktiven Session-Key
        loadActiveSessionKey()

        return keyboardView
    }

    /**
     * Lädt den Session-Key des aktiven Kontakts
     */
    private fun loadActiveSessionKey() {
        val activeContactId = ContactManager.getActiveContactId(this)
        currentSessionKey = if (activeContactId != null) {
            ContactManager.getSessionKey(this, activeContactId)
        } else {
            // Fallback: Ersten Kontakt verwenden
            val contacts = ContactManager.loadContacts(this)
            if (contacts.isNotEmpty()) {
                ContactManager.getSessionKey(this, contacts.first().id)
            } else {
                null
            }
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                ic.deleteSurroundingText(1, 0)
            }
            Keyboard.KEYCODE_SHIFT -> {
                handleShift()
            }
            Keyboard.KEYCODE_DONE -> {
                ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
            }
            KEYCODE_SPACE -> {
                ic.commitText(" ", 1)
            }
            KEYCODE_ENCRYPT -> {
                encryptText()
            }
            KEYCODE_DECRYPT -> {
                decryptText()
            }
            else -> {
                var char = primaryCode.toChar()

                // Groß-/Kleinschreibung
                if (isShifted || isCapsLock) {
                    char = char.uppercaseChar()
                }

                ic.commitText(char.toString(), 1)

                // Shift zurücksetzen (außer bei Caps Lock)
                if (isShifted && !isCapsLock) {
                    isShifted = false
                    keyboard.isShifted = false
                    keyboardView.invalidateAllKeys()
                }
            }
        }
    }

    private fun handleShift() {
        if (isShifted) {
            // War bereits Shift -> jetzt Caps Lock
            isCapsLock = true
            isShifted = true
        } else {
            // Aktiviere Shift
            isShifted = true
            isCapsLock = false
        }

        keyboard.isShifted = isShifted
        keyboardView.invalidateAllKeys()
    }

    private fun encryptText() {
        val ic = currentInputConnection ?: return

        // Prüfe ob ein Session-Key verfügbar ist
        val sessionKey = currentSessionKey
        if (sessionKey == null) {
            ic.commitText("[⚠️ Kein Kontakt aktiv - öffne App]", 1)
            return
        }

        // Hole den Text vor dem Cursor
        val textBeforeCursor = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""

        if (textBeforeCursor.isEmpty()) {
            ic.commitText("[⚠️ Kein Text zum Verschlüsseln]", 1)
            return
        }

        // Finde die letzte Zeile oder den gesamten Text
        val lastLineStart = textBeforeCursor.lastIndexOf('\n') + 1
        val textToEncrypt = textBeforeCursor.substring(lastLineStart)

        if (textToEncrypt.isEmpty()) {
            ic.commitText("[⚠️ Kein Text zum Verschlüsseln]", 1)
            return
        }

        try {
            // Lösche den ursprünglichen Text
            ic.deleteSurroundingText(textToEncrypt.length, 0)

            // Echte AES-GCM Verschlüsselung
            val encrypted = CryptoEngine.encrypt(textToEncrypt, sessionKey)
            ic.commitText(encrypted, 1)
        } catch (e: Exception) {
            ic.commitText("[❌ Verschlüsselung fehlgeschlagen: ${e.message}]", 1)
        }
    }

    private fun decryptText() {
        val ic = currentInputConnection ?: return

        // Prüfe ob ein Session-Key verfügbar ist
        val sessionKey = currentSessionKey
        if (sessionKey == null) {
            ic.commitText("[⚠️ Kein Kontakt aktiv - öffne App]", 1)
            return
        }

        // Hole den Text vor dem Cursor
        val textBeforeCursor = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""

        if (textBeforeCursor.isEmpty()) {
            ic.commitText("[⚠️ Kein Text zum Entschlüsseln]", 1)
            return
        }

        // Finde die letzte Zeile oder den gesamten Text
        val lastLineStart = textBeforeCursor.lastIndexOf('\n') + 1
        val textToDecrypt = textBeforeCursor.substring(lastLineStart).trim()

        if (textToDecrypt.isEmpty()) {
            ic.commitText("[⚠️ Kein Text zum Entschlüsseln]", 1)
            return
        }

        try {
            // Lösche den ursprünglichen Text
            ic.deleteSurroundingText(textToDecrypt.length, 0)

            // Echte AES-GCM Entschlüsselung
            val decrypted = CryptoEngine.decrypt(textToDecrypt, sessionKey)
            ic.commitText(decrypted, 1)
        } catch (e: Exception) {
            ic.commitText("[❌ Entschlüsselung fehlgeschlagen - falscher Kontakt?]", 1)
        }
    }

    override fun onPress(primaryCode: Int) {
        // Optional: Feedback bei Tastendruck (Vibration, Sound)
    }

    override fun onRelease(primaryCode: Int) {
        // Optional: Feedback beim Loslassen
    }

    override fun onText(text: CharSequence?) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    override fun swipeLeft() {
        // Optional: Swipe-Gesten
    }

    override fun swipeRight() {
        // Optional: Swipe-Gesten
    }

    override fun swipeDown() {
        // Tastatur schließen
        requestHideSelf(0)
    }

    override fun swipeUp() {
        // Optional: Swipe-Gesten
    }

    companion object {
        private const val KEYCODE_SPACE = 32
        private const val KEYCODE_ENCRYPT = -100
        private const val KEYCODE_DECRYPT = -101
    }
}

