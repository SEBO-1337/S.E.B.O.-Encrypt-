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
    /** Wird nach dem Laden aus dem Speicher per ECDH+HKDF neu berechnet */
    @Transient val sessionKey: ByteArray? = null
) {
    override fun equals(other: Any?) = other is Contact && id == other.id
    override fun hashCode() = id.hashCode()
}

