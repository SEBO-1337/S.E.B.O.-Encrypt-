package com.sebo.seboencrypt.repository

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.sebo.seboencrypt.QRHelper
import com.sebo.seboencrypt.helper.KeyDerivation
import com.sebo.seboencrypt.manager.KeystoreManager
import com.sebo.seboencrypt.model.Contact
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Speichert Kontakte (Name + PublicKey + Fingerprint) persistent in SharedPreferences,
 * deren Werte mit AES-256-GCM über den Android Keystore verschlüsselt werden.
 * Ersetzt die deprecated EncryptedSharedPreferences / MasterKey-API.
 */
object ContactRepository {

    private const val PREFS_NAME        = "sebo_contacts_enc"
    private const val KEY_LIST          = "contacts_json"
    private const val KEYSTORE_ALIAS    = "sebo_contacts_key"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val GCM_TAG_LENGTH    = 128

    // ──────────────────────────────────────────────
    // Android Keystore – AES-256-GCM
    // ──────────────────────────────────────────────

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val keySpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            .also { it.init(keySpec) }
            .generateKey()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv         = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        // Format: Base64(iv) + ":" + Base64(ciphertext)
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
               Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val parts      = encoded.split(":")
        val iv         = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher     = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ──────────────────────────────────────────────
    // Öffentliche API
    // ──────────────────────────────────────────────

    /** Alle Kontakte laden und SessionKey live berechnen */
    fun loadContacts(context: Context): List<Contact> {
        val prefs = getPrefs(context)
        val raw   = prefs.getString(KEY_LIST, null) ?: return emptyList()
        val json  = try { decrypt(raw) } catch (_: Exception) { return emptyList() }
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj           = arr.getJSONObject(i)
                val id            = obj.getString("id")
                val name          = obj.getString("name")
                val pubKeyBase64  = obj.getString("publicKeyBase64")
                val fingerprint   = obj.optString("fingerprint", "")
                val sessionKey    = deriveSessionKey(pubKeyBase64)
                Contact(id = id, name = name, publicKeyBase64 = pubKeyBase64,
                        fingerprint = fingerprint, sessionKey = sessionKey)
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
                put("id",              c.id)
                put("name",            c.name)
                put("publicKeyBase64", c.publicKeyBase64)
                put("fingerprint",     c.fingerprint)
            })
        }
        getPrefs(context).edit { putString(KEY_LIST, encrypt(arr.toString())) }
    }

    private fun deriveSessionKey(publicKeyBase64: String): ByteArray? = try {
        val theirPublicKey = QRHelper.qrStringToPublicKey(publicKeyBase64)
        val sharedSecret   = KeystoreManager.computeSharedSecret(theirPublicKey)
        KeyDerivation.deriveAesKey(sharedSecret)
    } catch (_: Exception) {
        null
    }
}

