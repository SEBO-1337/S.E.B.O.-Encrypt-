package com.sebo.eboard.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Crypto-Engine für AES-GCM Verschlüsselung/Entschlüsselung
 * Identisch zur Implementierung in der Haupt-App
 */
object CryptoEngine {

    private const val GCM_TAG_BITS = 128
    private const val IV_LENGTH    = 12

    fun encrypt(plaintext: String, aesKeyBytes: ByteArray): String {
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = SecretKeySpec(aesKeyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String, aesKeyBytes: ByteArray): String {
        val raw        = Base64.decode(encoded, Base64.NO_WRAP)
        val iv         = raw.sliceArray(0 until IV_LENGTH)
        val ciphertext = raw.sliceArray(IV_LENGTH until raw.size)
        val key        = SecretKeySpec(aesKeyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}

