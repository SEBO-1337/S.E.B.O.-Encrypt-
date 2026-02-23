package com.sebo.seboencrypt.repository

import android.content.Context
import androidx.core.content.edit
import com.sebo.seboencrypt.KeyDerivation
import com.sebo.seboencrypt.QRHelper
import com.sebo.seboencrypt.manager.KeystoreManager
import com.sebo.seboencrypt.model.Contact
import org.json.JSONArray
import org.json.JSONObject

/**
 * Speichert Kontakte (Name + PublicKey) persistent in SharedPreferences.
 * Der SessionKey wird beim Laden live per ECDH+HKDF neu berechnet.
 */
object ContactRepository {

    private const val PREFS_NAME = "sebo_contacts"
    private const val KEY_LIST   = "contacts_json"

    /** Alle Kontakte laden und SessionKey live berechnen */
    fun loadContacts(context: Context): List<Contact> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_LIST, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val id            = obj.getString("id")
                val name          = obj.getString("name")
                val pubKeyBase64  = obj.getString("publicKeyBase64")
                val sessionKey    = deriveSessionKey(pubKeyBase64)
                Contact(id = id, name = name, publicKeyBase64 = pubKeyBase64, sessionKey = sessionKey)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Kontakt hinzufügen oder aktualisieren (anhand der ID) */
    fun saveContact(context: Context, contact: Contact, existingContacts: List<Contact>) {
        val updated = existingContacts
            .filter { it.id != contact.id }
            .plus(contact)
        persist(context, updated)
    }

    /** Kontakt löschen */
    fun deleteContact(context: Context, contactId: String, existingContacts: List<Contact>) {
        persist(context, existingContacts.filter { it.id != contactId })
    }

    /** Kontaktnamen aktualisieren */
    fun renameContact(context: Context, contactId: String, newName: String, existingContacts: List<Contact>): List<Contact> {
        val updated = existingContacts.map {
            if (it.id == contactId) it.copy(name = newName) else it
        }
        persist(context, updated)
        return updated
    }

    private fun persist(context: Context, contacts: List<Contact>) {
        val arr = JSONArray()
        contacts.forEach { c ->
            arr.put(JSONObject().apply {
                put("id",             c.id)
                put("name",           c.name)
                put("publicKeyBase64",c.publicKeyBase64)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_LIST, arr.toString()) }
    }

    private fun deriveSessionKey(publicKeyBase64: String): ByteArray? = try {
        val theirPublicKey = QRHelper.qrStringToPublicKey(publicKeyBase64)
        val sharedSecret   = KeystoreManager.computeSharedSecret(theirPublicKey)
        KeyDerivation.deriveAesKey(sharedSecret)
    } catch (_: Exception) {
        null
    }
}



