package com.sebo.seboencrypt.model

import java.util.UUID

/**
 * Repräsentiert einen gespeicherten Kontakt.
 * Der AES-Sitzungsschlüssel wird nur im RAM gehalten (nie persistiert).
 * Der öffentliche Schlüssel wird als Base64-String gespeichert.
 */
data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val publicKeyBase64: String,
    /** Fix 2 (TOFU): SHA-256-Fingerprint des öffentlichen Schlüssels (lesbar mit Doppelpunkten) */
    val fingerprint: String = "",
    /** Wird nach dem Laden aus dem Speicher per ECDH+HKDF neu berechnet – Fix 3: var für wipe() */
    @Transient var sessionKey: ByteArray? = null
) {
    override fun equals(other: Any?) = other is Contact && id == other.id
    override fun hashCode() = id.hashCode()

    /**
     * Fix 3: Session Key aus dem Heap sicher löschen (mit Nullen überschreiben).
     * Sollte in ViewModel.onCleared() aufgerufen werden.
     */
    fun wipe() {
        sessionKey?.fill(0)
        sessionKey = null
    }
}

