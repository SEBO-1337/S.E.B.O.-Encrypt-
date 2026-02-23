package com.sebo.seboencrypt

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters

object KeyDerivation {

    /**
     * HKDF (RFC 5869) – leitet aus dem rohen ECDH-Secret
     * einen sicheren AES-256 Key ab.
     * info-Parameter bindet den Key an diesen spezifischen Use-Case.
     */
    fun deriveAesKey(sharedSecret: ByteArray, info: String = "e2ee-chat-v1"): ByteArray {
        val output = ByteArray(32) // 256 bit
        HKDFBytesGenerator(SHA256Digest()).apply {
            init(HKDFParameters(sharedSecret, null, info.toByteArray()))
            generateBytes(output, 0, output.size)
        }
        return output
    }
}