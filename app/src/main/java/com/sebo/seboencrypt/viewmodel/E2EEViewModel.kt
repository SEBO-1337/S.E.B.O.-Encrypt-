package com.sebo.seboencrypt.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.sebo.seboencrypt.ClipboardHelper
import com.sebo.seboencrypt.KeyDerivation
import com.sebo.seboencrypt.QRHelper
import com.sebo.seboencrypt.ShareHelper
import com.sebo.seboencrypt.engine.CryptoEngine
import com.sebo.seboencrypt.manager.KeystoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiStatus(val icon: String, val message: String, val isError: Boolean = false)

class E2EEViewModel : ViewModel() {

    // --- Encrypt Tab ---
    val encryptInput  = MutableStateFlow("")
    val encryptOutput = MutableStateFlow("")

    // --- Decrypt Tab ---
    val decryptInput  = MutableStateFlow("")
    val decryptOutput = MutableStateFlow("")

    // --- Key Tab ---
    val myQRBitmap   = MutableStateFlow<Bitmap?>(null)
    val contactAdded = MutableStateFlow(false)

    // --- Global Status ---
    private val _status = MutableStateFlow(UiStatus("🔑", "Schlüssel wurde generiert"))
    val status = _status.asStateFlow()

    private var sessionKey: ByteArray? = null

    init {
        KeystoreManager.generateKeyPairIfAbsent()
        myQRBitmap.value = QRHelper.publicKeyToQR(KeystoreManager.getPublicKey())
    }

    /** Wird nach dem QR-Scan des Kontakts aufgerufen */
    fun onContactQRScanned(qrContent: String) = runCatching {
        val theirPublicKey = QRHelper.qrStringToPublicKey(qrContent)
        val sharedSecret   = KeystoreManager.computeSharedSecret(theirPublicKey)
        sessionKey         = KeyDerivation.deriveAesKey(sharedSecret)
        contactAdded.value = true
        _status.value = UiStatus("✅", "Kontakt hinzugefügt – Schlüssel bereit")
    }.onFailure {
        _status.value = UiStatus("❌", "Ungültiger QR-Code: ${it.message}", isError = true)
    }

    fun encrypt() {
        val key = sessionKey ?: run {
            _status.value = UiStatus("⚠️", "Kein Kontakt gescannt", isError = true)
            return
        }
        runCatching {
            encryptOutput.value = CryptoEngine.encrypt(encryptInput.value, key)
            _status.value = UiStatus("🔒", "Verschlüsselt – kopieren & senden")
        }.onFailure {
            _status.value = UiStatus("❌", "Fehler: ${it.message}", isError = true)
        }
    }

    fun decrypt() {
        val key = sessionKey ?: run {
            _status.value = UiStatus("⚠️", "Kein Kontakt gescannt", isError = true)
            return
        }
        runCatching {
            decryptOutput.value = CryptoEngine.decrypt(decryptInput.value, key)
            _status.value = UiStatus("🔓", "Entschlüsselt")
        }.onFailure {
            _status.value = UiStatus("❌", "Entschlüsselung fehlgeschlagen – falscher Kontakt?", isError = true)
        }
    }

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
            _status.value = UiStatus("📋", "Aus Zwischenablage eingefügt")
        } else {
            _status.value = UiStatus("⚠️", "Zwischenablage ist leer", isError = true)
        }
    }

    fun shareViaWhatsApp(context: Context) {
        if (encryptOutput.value.isNotEmpty()) {
            ShareHelper.shareViaWhatsApp(context, encryptOutput.value)
        } else {
            _status.value = UiStatus("⚠️", "Nichts zum Teilen – zuerst verschlüsseln", isError = true)
        }
    }
}