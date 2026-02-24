package com.sebo.seboencrypt.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.sebo.seboencrypt.ClipboardHelper
import com.sebo.seboencrypt.KeyDerivation
import com.sebo.seboencrypt.QRHelper
import com.sebo.seboencrypt.ShareHelper
import com.sebo.seboencrypt.engine.CryptoEngine
import com.sebo.seboencrypt.manager.KeystoreManager
import com.sebo.seboencrypt.model.Contact
import com.sebo.seboencrypt.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiStatus(val icon: String, val message: String, val isError: Boolean = false)

class E2EEViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Context get() = getApplication()

    // --- Encrypt Tab ---
    val encryptInput   = MutableStateFlow("")
    val encryptOutput  = MutableStateFlow("")

    // --- Decrypt Tab ---
    val decryptInput   = MutableStateFlow("")
    val decryptOutput  = MutableStateFlow("")

    // --- Geteilter Text (Share-Intent) ---
    private val _sharedTextPending = MutableStateFlow<String?>(null)
    val sharedTextPending = _sharedTextPending.asStateFlow()

    // --- Key Tab ---
    val myQRBitmap     = MutableStateFlow<Bitmap?>(null)

    // --- Kontakte ---
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _activeContact = MutableStateFlow<Contact?>(null)
    val activeContact = _activeContact.asStateFlow()

    // --- Global Status ---
    private val _status = MutableStateFlow(UiStatus("🔑", "Schlüssel wurde generiert"))
    val status = _status.asStateFlow()

    // Zwischenspeicher für gescannten QR (wartet auf Namenseingabe)
    private var pendingPublicKeyBase64: String? = null
    private val _hasPendingQR = MutableStateFlow(false)
    val hasPendingQR = _hasPendingQR.asStateFlow()

    init {
        KeystoreManager.generateKeyPairIfAbsent()
        myQRBitmap.value = QRHelper.publicKeyToQR(KeystoreManager.getPublicKey())
        _contacts.value = ContactRepository.loadContacts(ctx)
        // Letzten aktiven Kontakt wiederherstellen (ersten nehmen falls vorhanden)
        _activeContact.value = _contacts.value.firstOrNull()
        if (_activeContact.value != null) {
            _status.value = UiStatus("✅", "Kontakt \"${_activeContact.value!!.name}\" aktiv")
        }
    }

    // ── QR-Scan ──────────────────────────────────────────────────────────────

    /** Wird nach dem QR-Scan aufgerufen – speichert den Key temporär bis ein Name vergeben wird */
    fun onQRScanned(qrContent: String) {
        pendingPublicKeyBase64 = qrContent
        _hasPendingQR.value = true
        _status.value = UiStatus("📷", "QR gescannt - bitte Namen vergeben")
    }

    /** Kontakt mit Namen speichern (nach QR-Scan) */
    fun confirmAddContact(name: String) {
        val base64 = pendingPublicKeyBase64 ?: run {
            _status.value = UiStatus("❌", "Kein QR-Code gescannt", isError = true)
            return
        }
        runCatching {
            val theirPublicKey = QRHelper.qrStringToPublicKey(base64)
            val sharedSecret   = KeystoreManager.computeSharedSecret(theirPublicKey)
            val sessionKey     = KeyDerivation.deriveAesKey(sharedSecret)
            val contact = Contact(
                name            = name.trim().ifEmpty { "Kontakt ${_contacts.value.size + 1}" },
                publicKeyBase64 = base64,
                sessionKey      = sessionKey
            )
            ContactRepository.saveContact(ctx, contact, _contacts.value)
            _contacts.value += contact
            _activeContact.value = contact
            pendingPublicKeyBase64 = null
            _hasPendingQR.value  = false
            _status.value = UiStatus("✅", "\"${contact.name}\" hinzugefügt & aktiv")
        }.onFailure {
            _status.value = UiStatus("❌", "Ungültiger QR-Code: ${it.message}", isError = true)
        }
    }

    /** Aktiven Kontakt wechseln */
    fun selectContact(contact: Contact) {
        _activeContact.value = contact
        _status.value = UiStatus("✅", "Kontakt \"${contact.name}\" aktiv")
    }

    /** Kontakt manuell per Base64-PublicKey hinzufügen */
    fun addContactManually(name: String, base64PublicKey: String) {
        runCatching {
            val theirPublicKey = QRHelper.qrStringToPublicKey(base64PublicKey.trim())
            val sharedSecret   = KeystoreManager.computeSharedSecret(theirPublicKey)
            val sessionKey     = KeyDerivation.deriveAesKey(sharedSecret)
            val contact = Contact(
                name            = name.trim().ifEmpty { "Kontakt ${_contacts.value.size + 1}" },
                publicKeyBase64 = base64PublicKey.trim(),
                sessionKey      = sessionKey
            )
            ContactRepository.saveContact(ctx, contact, _contacts.value)
            _contacts.value += contact
            _activeContact.value = contact
            _status.value = UiStatus("✅", "\"${contact.name}\" hinzugefügt & aktiv")
        }.onFailure {
            _status.value = UiStatus("❌", "Ungültiger Public Key: ${it.message}", isError = true)
        }
    }

    /** Gibt den eigenen Public Key als Base64-String zurück */
    fun getMyPublicKeyBase64(): String =
        android.util.Base64.encodeToString(
            KeystoreManager.getPublicKey().encoded,
            android.util.Base64.NO_WRAP
        )


    /** Kontakt umbenennen */
    fun renameContact(contactId: String, newName: String) {
        val updated = ContactRepository.renameContact(ctx, contactId, newName, _contacts.value)
        _contacts.value = updated
        if (_activeContact.value?.id == contactId) {
            _activeContact.value = updated.find { it.id == contactId }
        }
        _status.value = UiStatus("✏️", "Kontakt umbenannt")
    }

    /** Kontakt löschen */
    fun deleteContact(contactId: String) {
        ContactRepository.deleteContact(ctx, contactId, _contacts.value)
        val updated = _contacts.value.filter { it.id != contactId }
        _contacts.value = updated
        if (_activeContact.value?.id == contactId) {
            _activeContact.value = updated.firstOrNull()
            if (_activeContact.value != null) {
                _status.value = UiStatus("✅", "Kontakt \"${_activeContact.value!!.name}\" aktiv")
            } else {
                _status.value = UiStatus("⚠️", "Kein aktiver Kontakt - bitte QR scannen")
            }
        } else {
            _status.value = UiStatus("🗑️", "Kontakt gelöscht")
        }
    }

    // ── Crypto ───────────────────────────────────────────────────────────────

    fun encrypt() {
        val key = _activeContact.value?.sessionKey ?: run {
            _status.value = UiStatus("⚠️", "Kein Kontakt ausgewählt", isError = true)
            return
        }
        runCatching {
            encryptOutput.value = CryptoEngine.encrypt(encryptInput.value, key)
            _status.value = UiStatus("🔒", "Verschluesselt - kopieren & senden")
        }.onFailure {
            _status.value = UiStatus("❌", "Fehler: ${it.message}", isError = true)
        }
    }

    fun decrypt() {
        val key = _activeContact.value?.sessionKey ?: run {
            _status.value = UiStatus("⚠️", "Kein Kontakt ausgewählt", isError = true)
            return
        }
        runCatching {
            decryptOutput.value = CryptoEngine.decrypt(decryptInput.value, key)
            _status.value = UiStatus("🔓", "Entschluesselt")
        }.onFailure {
            _status.value = UiStatus("❌", "Entschluesselung fehlgeschlagen - falscher Kontakt?", isError = true)
        }
    }

    // ── Clipboard / Share ─────────────────────────────────────────────────────

    fun copyEncryptOutput(context: Context) {
        if (encryptOutput.value.isNotEmpty()) {
            ClipboardHelper.copyToClipboard(context, encryptOutput.value)
            _status.value = UiStatus("📋", "In Zwischenablage kopiert")
        }
    }

    fun copyDecryptOutput(context: Context) {
        if (decryptOutput.value.isNotEmpty()) {
            ClipboardHelper.copyToClipboard(context, decryptOutput.value)
            _status.value = UiStatus("📋", "In Zwischenablage kopiert")
        }
    }

    fun pasteToDecryptInput(context: Context) {
        val text = ClipboardHelper.pasteFromClipboard(context)
        if (text != null) {
            decryptInput.value = text
            _status.value = UiStatus("📋", "Aus Zwischenablage eingefuegt")
        } else {
            _status.value = UiStatus("⚠️", "Zwischenablage ist leer", isError = true)
        }
    }

    fun shareViaWhatsApp(context: Context) {
        if (encryptOutput.value.isNotEmpty()) {
            ShareHelper.shareViaWhatsApp(context, encryptOutput.value)
        } else {
            _status.value = UiStatus("⚠️", "Nichts zum Teilen - zuerst verschluesseln", isError = true)
        }
    }

    // ── Share-Intent ─────────────────────────────────────────────────────────

    /** Wird aufgerufen wenn die App einen geteilten Text empfängt (z.B. aus WhatsApp) */
    fun onSharedTextReceived(text: String) {
        decryptInput.value = text
        decryptOutput.value = ""
        _sharedTextPending.value = text
        _status.value = UiStatus("📥", "Text empfangen – Entschlüsseln tippen")
    }

    /** Bestätigt, dass der pending Share-Text verarbeitet wurde (Navigation erfolgt) */
    fun consumeSharedText() {
        _sharedTextPending.value = null
    }
}