package com.sebo.eboard.manager

import android.content.Context
import com.sebo.eboard.model.KeyboardContact
import org.json.JSONArray

/**
 * Manager zum Laden von Kontakten aus SharedPreferences
 * Synchronisiert mit der Haupt-App
 */
object ContactManager {

    private const val PREFS_NAME = "sebo_contacts"
    private const val KEY_LIST = "contacts_json"
    private const val ACTIVE_CONTACT_PREFS = "sebo_keyboard_prefs"
    private const val ACTIVE_CONTACT_ID = "active_contact_id"

    /**
     * Lädt alle gespeicherten Kontakte aus SharedPreferences
     */
    fun loadContacts(context: Context): List<KeyboardContact> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LIST, "[]") ?: "[]"

        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val pubKeyBase64 = obj.getString("publicKeyBase64")

                // Prüfe ob SessionKey verfügbar ist
                val hasSessionKey = getSessionKey(context, id) != null

                KeyboardContact(
                    id = id,
                    name = name,
                    publicKeyBase64 = pubKeyBase64,
                    hasSessionKey = hasSessionKey
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gibt den zuletzt aktiven Kontakt zurück
     */
    fun getActiveContactId(context: Context): String? {
        val prefs = context.getSharedPreferences(ACTIVE_CONTACT_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(ACTIVE_CONTACT_ID, null)
    }

    /**
     * Speichert den aktiven Kontakt
     */
    fun setActiveContactId(context: Context, contactId: String) {
        val prefs = context.getSharedPreferences(ACTIVE_CONTACT_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(ACTIVE_CONTACT_ID, contactId).apply()
    }

    /**
     * Lädt den SessionKey eines Kontakts aus einem separaten Cache
     * (wird von der Haupt-App geschrieben)
     */
    fun getSessionKey(context: Context, contactId: String): ByteArray? {
        val prefs = context.getSharedPreferences("sebo_session_keys", Context.MODE_PRIVATE)
        val base64 = prefs.getString("key_$contactId", null) ?: return null
        return try {
            android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}

