package com.sebo.eboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sebo.eboard.crypto.CryptoEngine
import com.sebo.eboard.manager.ContactManager
import com.sebo.eboard.ui.ContactAdapter

/**
 * S.E.B.O. E-Board - Custom Keyboard Service
 *
 * Diese Tastatur ermöglicht systemweites Verschlüsseln und Entschlüsseln von Texten.
 */
class CustomKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var keyboard: Keyboard
    private lateinit var activeContactLabel: TextView
    private lateinit var btnSelectContact: Button

    private var isShifted = false
    private var isCapsLock = false

    // Aktueller Session-Key für Ver-/Entschlüsselung
    private var currentSessionKey: ByteArray? = null
    private var activeContactName: String? = null

    override fun onCreateInputView(): View {
        val rootView = layoutInflater.inflate(R.layout.keyboard_view, null)

        keyboardView = rootView.findViewById(R.id.keyboard)
        activeContactLabel = rootView.findViewById(R.id.active_contact_label)
        btnSelectContact = rootView.findViewById(R.id.btn_select_contact)

        keyboard = Keyboard(this, R.xml.qwerty)
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = false

        // Lade aktiven Session-Key
        loadActiveSessionKey()

        // Kontakt-Auswahl-Button
        btnSelectContact.setOnClickListener {
            showContactSelectorDialog()
        }

        return rootView
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

        updateContactLabel()
    }

    /**
     * Aktualisiert das Label mit dem aktiven Kontakt
     */
    private fun updateContactLabel() {
        if (::activeContactLabel.isInitialized) {
            activeContactLabel.text = if (activeContactName != null && currentSessionKey != null) {
                "🔑 $activeContactName"
            } else if (activeContactName != null) {
                "⚠️ $activeContactName (kein Key)"
            } else {
                "⚠️ Kein Kontakt"
            }
        }
    }

    /**
     * Zeigt den Kontakt-Auswahl-Dialog als PopupWindow
     */
    private fun showContactSelectorDialog() {
        val contacts = ContactManager.loadContacts(this)

        // Erstelle die Popup-View
        val popupView = layoutInflater.inflate(R.layout.dialog_contact_selector, null)
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.contacts_recycler)
        val noContactsMsg = popupView.findViewById<TextView>(R.id.no_contacts_message)

        if (contacts.isEmpty()) {
            noContactsMsg.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noContactsMsg.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        val activeContactId = ContactManager.getActiveContactId(this)

        // Erstelle PopupWindow
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Setze Hintergrund für außerhalb-Klick-Erkennung
        popupWindow.setBackgroundDrawable(resources.getDrawable(android.R.drawable.dialog_holo_light_frame, null))
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.elevation = 10f

        val adapter = ContactAdapter(contacts, activeContactId) { contact ->
            // Kontakt wurde ausgewählt
            ContactManager.setActiveContactId(this, contact.id)
            activeContactName = contact.name
            currentSessionKey = ContactManager.getSessionKey(this, contact.id)
            updateContactLabel()
            popupWindow.dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Schließen-Button (optional, falls vorhanden)
        popupView.findViewById<Button>(R.id.btn_close_dialog)?.setOnClickListener {
            popupWindow.dismiss()
        }

        // Zeige Popup oberhalb der Tastatur
        val rootView = window?.window?.decorView ?: return
        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0)
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

