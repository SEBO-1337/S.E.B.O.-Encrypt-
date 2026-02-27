package com.sebo.seboencrypt.helper

import android.content.Context
import android.util.Base64
import androidx.core.content.edit

/**
 * Helper zum Synchronisieren von Session-Keys zwischen App und Tastatur
 */
object KeyboardSyncHelper {

    private const val PREFS_NAME = "sebo_session_keys"
    private const val ACTIVE_CONTACT_PREFS = "sebo_keyboard_prefs"
    private const val ACTIVE_CONTACT_ID = "active_contact_id"

    /**
     * Speichert den Session-Key eines Kontakts für die Tastatur
     */
    fun saveSessionKey(context: Context, contactId: String, sessionKey: ByteArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val base64 = Base64.encodeToString(sessionKey, Base64.NO_WRAP)
        prefs.edit { putString("key_$contactId", base64) }
    }

    /**
     * Speichert den aktiven Kontakt für die Tastatur
     */
    fun setActiveContact(context: Context, contactId: String) {
        val prefs = context.getSharedPreferences(ACTIVE_CONTACT_PREFS, Context.MODE_PRIVATE)
        prefs.edit { putString(ACTIVE_CONTACT_ID, contactId) }
    }

    /**
     * Entfernt den Session-Key eines gelöschten Kontakts
     */
    fun removeSessionKey(context: Context, contactId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove("key_$contactId") }
    }
}

