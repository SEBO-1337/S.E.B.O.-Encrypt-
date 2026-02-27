package com.sebo.eboard.model

/**
 * Vereinfachtes Contact-Modell für die Tastatur
 */
data class KeyboardContact(
    val id: String,
    val name: String,
    val publicKeyBase64: String
)

