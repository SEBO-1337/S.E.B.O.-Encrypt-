@file:Suppress("DEPRECATION")

package com.sebo.eboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sebo.eboard.crypto.CryptoEngine
import com.sebo.eboard.manager.ContactManager
import com.sebo.eboard.manager.SettingsManager
import com.sebo.eboard.ui.ContactAdapter
import com.sebo.eboard.util.ThemeHelper

/**
 * S.E.B.O. E-Board - Custom Keyboard Service
 *
 * Diese Tastatur ermöglicht systemweites Verschlüsseln und Entschlüsseln von Texten.
 */
@Suppress(
    "DEPRECATION",
    "unused",
    "DiscouragedPrivateApi"
)
class CustomKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var numbersKeyboard: Keyboard
    private lateinit var symbolsKeyboard: Keyboard
    private lateinit var keyboard: Keyboard

    private var isShifted = false
    private var isCapsLock = false

    // Keyboard mode tracking
    private enum class KeyboardMode {
        QWERTY, NUMBERS, SYMBOLS
    }
    private var currentMode = KeyboardMode.QWERTY

    // Aktueller Session-Key für Ver-/Entschlüsselung
    private var currentSessionKey: ByteArray? = null
    private var activeContactName: String? = null

    // Feedback services
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null

    // Longpress-Handling
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var currentPressedCode: Int = -1
    private var popupWindow: PopupWindow? = null
    private val longPressTimeout = 500L
    private var isLongPressPopupActive = false

    private lateinit var longPressRunnable: Runnable

    companion object {
        // Mapping von Codes zu Umlaut-Alternativen
        private val UMLAUT_MAP = mapOf(
            117 to 'ü',  // u -> ü
            111 to 'ö',  // o -> ö
            97 to 'ä',   // a -> ä
            115 to 'ß'   // s -> ß
        )

        private const val KEYCODE_SPACE = 32
        private const val KEYCODE_ENCRYPT = -100
        private const val KEYCODE_DECRYPT = -101
        private const val KEYCODE_MODE_SYMBOLS = -102      // =\# Taste
        private const val KEYCODE_MODE_ABC = -103          // ABC Taste
        private const val KEYCODE_MODE_NUMBERS = -104      // 123 Taste
        private const val KEYCODE_SHOW_CONTACTS = -105     // Kontakt-Auswahl Taste
    }

    @Suppress("InflateParams")
    override fun onCreateInputView(): View {
        val rootView = layoutInflater.inflate(R.layout.keyboard_view, null, false)

        keyboardView = rootView.findViewById(R.id.keyboard)

        // Initialize longPressRunnable
        longPressRunnable = Runnable {
            if (UMLAUT_MAP.containsKey(currentPressedCode)) {
                showUmlautPopup(currentPressedCode)
            }
        }

        // Initialize all keyboards
        qwertyKeyboard = Keyboard(this, R.xml.qwertz)
        numbersKeyboard = Keyboard(this, R.xml.numbers)
        symbolsKeyboard = Keyboard(this, R.xml.symbols)

        // Start with QWERTY keyboard
        keyboard = qwertyKeyboard
        currentMode = KeyboardMode.QWERTY

        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = false

        // Initialize feedback services
        vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager

        // Apply settings (must be called after view initialization)
        applySettings()

        // Lade aktiven Session-Key
        loadActiveSessionKey()


        return rootView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Reload settings when keyboard is shown
        applySettings()
        loadActiveSessionKey()

        // Reset to QWERTY keyboard when opening
        if (currentMode != KeyboardMode.QWERTY) {
            switchToQwerty()
        }
    }

    /**
     * Wendet die Einstellungen auf die Tastatur an
     */
    @Suppress("DiscouragedPrivateApi")
    @android.annotation.SuppressLint("DiscouragedPrivateApi", "PrivateApi")
    private fun applySettings() {
        // Apply theme colors
        val themeColors = ThemeHelper.getThemeColors(this)

        // Set keyboard view colors and properties
        if (::keyboardView.isInitialized) {
            keyboardView.setBackgroundColor(themeColors.backgroundColor)

            // Setze das futuristische Key-Drawable basierend auf dem Theme durch Reflection
            @Suppress("DiscouragedPrivateApi", "SoonBlockedPrivateApi")
            try {
                val keyDrawable = ThemeHelper.createKeyDrawable(this)
                val field = keyboardView.javaClass.getDeclaredField("mKeyBackground")
                field.isAccessible = true
                field.set(keyboardView, keyDrawable)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Update the root view background color
            val rootView = keyboardView.parent as? ViewGroup
            rootView?.setBackgroundColor(themeColors.backgroundColor)

            // Find the top-level LinearLayout
            rootView?.parent?.let { grandParent ->
                if (grandParent is ViewGroup) {
                    grandParent.setBackgroundColor(themeColors.backgroundColor)
                    grandParent.invalidate()
                }
            }

            // Update button and label colors
            // (buttons removed from keyboard view)

            // Force keyboard view to redraw
            keyboardView.invalidateAllKeys()
            keyboardView.invalidate()
            rootView?.invalidate()
        }
    }

    /**
     * Lädt den Session-Key des aktiven Kontakts
     */
    private fun loadActiveSessionKey() {
        val activeContactId = ContactManager.getActiveContactId(this)
        currentSessionKey = if (activeContactId != null) {
            // Lade Kontakt-Namen
            val contacts = ContactManager.loadContacts(this)
            val activeContact = contacts.find { it.id == activeContactId }
            activeContactName = activeContact?.name

            ContactManager.getSessionKey(this, activeContactId)
        } else {
            // Fallback: Ersten Kontakt verwenden
            val contacts = ContactManager.loadContacts(this)
            if (contacts.isNotEmpty()) {
                val firstContact = contacts.first()
                activeContactName = firstContact.name
                ContactManager.setActiveContactId(this, firstContact.id)
                ContactManager.getSessionKey(this, firstContact.id)
            } else {
                activeContactName = null
                null
            }
        }
    }

    // Label entfernt - wird jetzt im Dropdown-Menü angezeigt

    /**
     * Zeigt ein Dropdown-Menü mit Kontakt-Auswahl und schnellen Aktionen
     */
    private fun showContactSelectorDialog() {
        // Erstelle ein einfaches PopupMenu-ähnliches Dropdown
        val contacts = ContactManager.loadContacts(this)
        val activeContactId = ContactManager.getActiveContactId(this)

        if (contacts.isEmpty()) {
            // Keine Kontakte vorhanden - zeige Popup
            val ic = currentInputConnection ?: return
            ic.commitText("[⚠️ Keine Kontakte definiert]", 1)
            return
        }

        // Erstelle die Popup-View mit RecyclerView
        @Suppress("InflateParams")
        val popupView = layoutInflater.inflate(R.layout.dialog_contact_selector, null, false)
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.contacts_recycler)
        val noContactsMsg = popupView.findViewById<TextView>(R.id.no_contacts_message)

        noContactsMsg.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        // Erstelle PopupWindow
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Setze Hintergrund für außerhalb-Klick-Erkennung
        popupWindow.setBackgroundDrawable(ResourcesCompat.getDrawable(resources, android.R.drawable.dialog_holo_light_frame, null))
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = false
        popupWindow.elevation = 10f

        val adapter = ContactAdapter(contacts, activeContactId) { contact ->
            // Kontakt wurde ausgewählt
            ContactManager.setActiveContactId(this, contact.id)
            activeContactName = contact.name
            currentSessionKey = ContactManager.getSessionKey(this, contact.id)
            popupWindow.dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Clipboard-Entschlüsselung Button
        popupView.findViewById<Button>(R.id.btn_decrypt_clipboard)?.setOnClickListener {
            decryptFromClipboard()
            popupWindow.dismiss()
        }

        // Schließen-Button (optional, falls vorhanden)
        popupView.findViewById<Button>(R.id.btn_close_dialog)?.setOnClickListener {
            popupWindow.dismiss()
        }

        // Zeige Popup oberhalb der Tastatur
        val rootView = window?.window?.decorView ?: return
        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0)
    }

    @Deprecated("Using deprecated KeyboardView API")
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        // Ignoriere onKey wenn ein Longpress-Popup aktiv ist
        if (isLongPressPopupActive) {
            return
        }

        val ic = currentInputConnection ?: return

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                handleDelete(ic)
                resetShiftIfNeeded()
            }
            Keyboard.KEYCODE_SHIFT -> {
                handleShift()
            }
            Keyboard.KEYCODE_DONE -> {
                ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
                resetShiftIfNeeded()
            }
            KEYCODE_SPACE -> {
                ic.commitText(" ", 1)
                resetShiftIfNeeded()
            }
            KEYCODE_ENCRYPT -> {
                encryptText()
            }
            KEYCODE_DECRYPT -> {
                decryptText()
            }
            KEYCODE_SHOW_CONTACTS -> {
                showContactSelectorDialog()
            }
            KEYCODE_MODE_NUMBERS -> {
                switchToNumbers()
            }
            KEYCODE_MODE_SYMBOLS -> {
                switchToSymbols()
            }
            KEYCODE_MODE_ABC -> {
                switchToQwerty()
            }
            else -> {
                var char = primaryCode.toChar()

                // Groß-/Kleinschreibung
                if (isShifted || isCapsLock) {
                    char = char.uppercaseChar()
                }

                ic.commitText(char.toString(), 1)
                resetShiftIfNeeded()
            }
        }
    }

    private fun resetShiftIfNeeded() {
        // Shift zurücksetzen (außer bei Caps Lock)
        if (isShifted && !isCapsLock) {
            isShifted = false
            keyboard.isShifted = false
            keyboardView.invalidateAllKeys()
        }
    }

    private fun handleShift() {
        if (isCapsLock) {
            // Caps Lock deaktivieren
            isCapsLock = false
            isShifted = false
        } else if (isShifted) {
            // Shift ist aktiv -> aktiviere Caps Lock
            isCapsLock = true
        } else {
            // Shift aktivieren
            isShifted = true
        }

        keyboard.isShifted = (isShifted || isCapsLock)
        keyboardView.invalidateAllKeys()
    }

    /**
     * Wechselt zur Zahlen-Tastatur
     */
    private fun switchToNumbers() {
        currentMode = KeyboardMode.NUMBERS
        keyboard = numbersKeyboard
        keyboardView.keyboard = keyboard
        keyboardView.invalidateAllKeys()
        // Reset shift state
        isShifted = false
        isCapsLock = false
    }

    /**
     * Wechselt zur Sonderzeichen-Tastatur
     */
    private fun switchToSymbols() {
        currentMode = KeyboardMode.SYMBOLS
        keyboard = symbolsKeyboard
        keyboardView.keyboard = keyboard
        keyboardView.invalidateAllKeys()
        // Reset shift state
        isShifted = false
        isCapsLock = false
    }

    /**
     * Wechselt zur QWERTY-Buchstaben-Tastatur
     */
    private fun switchToQwerty() {
        currentMode = KeyboardMode.QWERTY
        keyboard = qwertyKeyboard
        keyboardView.keyboard = keyboard
        keyboardView.invalidateAllKeys()
        // Reset shift state
        isShifted = false
        isCapsLock = false
    }

    private fun encryptText() {
        val ic = currentInputConnection ?: return

        // Prüfe, ob ein Session-Key verfügbar ist
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
        } catch (_: Exception) {
            ic.commitText("[❌ Verschlüsselung fehlgeschlagen]", 1)
        }
    }

    private fun decryptText() {
        val ic = currentInputConnection ?: return

        // Prüfe, ob ein Session-Key verfügbar ist
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

    @Deprecated("Using deprecated KeyboardView API")
    override fun onPress(primaryCode: Int) {
        // Speichere den Code und starte Longpress-Timer
        currentPressedCode = primaryCode

        // Nur für Umlaut-Tasten Longpress-Timer starten
        if (UMLAUT_MAP.containsKey(primaryCode)) {
            longPressHandler.postDelayed(longPressRunnable, longPressTimeout)
        }

        // Haptic Feedback
        if (SettingsManager.isHapticFeedbackEnabled(this)) {
            vibrator?.let { vib ->
                val strength = SettingsManager.getHapticStrength(this)
                val duration = when (strength) {
                    SettingsManager.HAPTIC_LIGHT -> 10L
                    SettingsManager.HAPTIC_MEDIUM -> 25L
                    SettingsManager.HAPTIC_STRONG -> 50L
                    else -> 25L
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val amplitude = when (strength) {
                        SettingsManager.HAPTIC_LIGHT -> 50
                        SettingsManager.HAPTIC_MEDIUM -> 128
                        SettingsManager.HAPTIC_STRONG -> 255
                        else -> 128
                    }
                    vib.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(duration)
                }
            }
        }

        // Sound Feedback
        if (SettingsManager.isSoundFeedbackEnabled(this)) {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.5f)
        }
    }

    @Deprecated("Using deprecated KeyboardView API")
    override fun onRelease(primaryCode: Int) {
        // Longpress-Timer stoppen
        longPressHandler.removeCallbacks(longPressRunnable)
        // Popup wird nur geschlossen, wenn es der Button-Klick macht
        currentPressedCode = -1
    }

    @Deprecated("Using deprecated KeyboardView API")
    override fun onText(text: CharSequence?) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    /**
     * Liest verschlüsselten Text aus der Zwischenablage, entschlüsselt ihn und fügt ihn ein
     */
    private fun decryptFromClipboard() {
        val ic = currentInputConnection ?: return

        // Prüfe, ob ein Session-Key verfügbar ist
        val sessionKey = currentSessionKey
        if (sessionKey == null) {
            ic.commitText("[⚠️ Kein Kontakt aktiv]", 1)
            return
        }

        // Hole Text aus Zwischenablage
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clipData = clipboard.primaryClip
        val clipboardText = clipData?.getItemAt(0)?.text?.toString()

        if (clipboardText.isNullOrBlank()) {
            ic.commitText("[⚠️ Zwischenablage ist leer]", 1)
            return
        }

        try {
            // Echte AES-GCM Entschlüsselung
            val decrypted = CryptoEngine.decrypt(clipboardText.trim(), sessionKey)
            ic.commitText(decrypted, 1)
        } catch (e: Exception) {
            ic.commitText("[❌ Entschlüsselung fehlgeschlagen - falscher Kontakt?]", 1)
        }
    }

    /**
     * Löscht markierten Text oder ein Zeichen vor dem Cursor
     */
    private fun handleDelete(ic: android.view.inputmethod.InputConnection) {
        // Versuche, den markierten Text zu löschen
        // dies funktioniert bei Android 6.0+ mit der Cursor-Position API
        try {
            // Setze eine Löschoperation für den markierten Text
            ic.commitText("", 1)

            // Falls das nicht funktioniert, lösche einfach ein Zeichen
            ic.deleteSurroundingText(1, 0)
        } catch (e: Exception) {
            // Fallback: Lösche einfach ein Zeichen
            ic.deleteSurroundingText(1, 0)
        }
    }

    @Deprecated("Using deprecated KeyboardView API")
    override fun swipeLeft() {
        // Optional: Swipe-Gesten
    }

    @Deprecated("Using deprecated KeyboardView API")
    override fun swipeRight() {
        // Optional: Swipe-Gesten
    }

    @Deprecated("Using deprecated KeyboardView API")
    override fun swipeDown() {
        // Tastatur schließen
        requestHideSelf(0)
    }

    @Deprecated("Using deprecated KeyboardView API")
    override fun swipeUp() {
        // Optional: Swipe-Gesten
    }

    /**
     * Zeigt ein Popup mit Umlaut-Alternativen
     */
    private fun showUmlautPopup(primaryCode: Int) {
        val umlaut = UMLAUT_MAP[primaryCode] ?: return

        dismissUmlautPopup()

        // Markiere dass das Longpress-Popup aktiv ist
        isLongPressPopupActive = true

        // Erstelle Popup mit beiden Optionen
        val popupView = LinearLayout(this)
        popupView.orientation = LinearLayout.HORIZONTAL
        popupView.setBackgroundColor(0xFF333333.toInt())

        // Original-Buchstabe Button
        val originalButton = createUmlautButton(primaryCode.toChar().toString(), primaryCode)
        popupView.addView(originalButton)

        // Umlaut Button
        val umlautButton = createUmlautButton(umlaut.toString(), umlaut.code)
        popupView.addView(umlautButton)

        popupWindow = PopupWindow(popupView, 180, 80, false)
        popupWindow?.elevation = 10f
        popupWindow?.isOutsideTouchable = false
        // WICHTIG: Verhindert, dass die Tastatur versteckt wird
        popupWindow?.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED

        // Zeige Popup über der Tastatur
        val rootView = window?.window?.decorView ?: return
        popupWindow?.showAtLocation(rootView, Gravity.CENTER, 0, -300)
    }

    /**
     * Erstellt einen Button für Umlaut-Auswahl
     */
    private fun createUmlautButton(text: String, code: Int): TextView {
        val button = TextView(this)
        button.text = text
        button.textSize = 24f
        button.gravity = Gravity.CENTER
        button.layoutParams = LinearLayout.LayoutParams(90, 80)
        button.setTextColor(0xFFFFFFFF.toInt())
        button.setBackgroundColor(0xFF666666.toInt())

        button.setOnClickListener {
            // Sende den gewählten Character
            val ic = currentInputConnection ?: return@setOnClickListener
            ic.commitText(text, 1)
            dismissUmlautPopup()
        }

        return button
    }

    /**
     * Schließt das Umlaut-Popup
     */
    private fun dismissUmlautPopup() {
        popupWindow?.dismiss()
        popupWindow = null
        isLongPressPopupActive = false
    }
}

