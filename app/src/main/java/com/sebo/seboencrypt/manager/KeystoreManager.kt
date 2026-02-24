package com.sebo.seboencrypt.manager

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.*
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement

object KeystoreManager {

    private const val ALIAS = "e2ee_identity_key"
    private const val PROVIDER = "AndroidKeyStore"

    /**
     * Generiert ein Keypair im Android Keystore (bleibt dort, nie exportierbar).
     * Wird nur einmal beim ersten Start aufgerufen.
     */
    fun generateKeyPairIfAbsent() {
        val keystore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (keystore.containsAlias(ALIAS)) return

        // PURPOSE_AGREE_KEY erfordert API 31+; auf älteren Geräten PURPOSE_SIGN als Fallback
        val purpose = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            KeyProperties.PURPOSE_AGREE_KEY
        } else {
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        }

        fun buildSpec(strongBox: Boolean): KeyGenParameterSpec {
            val builder = KeyGenParameterSpec.Builder(ALIAS, purpose)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setUserAuthenticationRequired(false)
            if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setIsStrongBoxBacked(true)
            }
            return builder.build()
        }

        fun generate(spec: KeyGenParameterSpec) =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, PROVIDER)
                .apply { initialize(spec) }
                .generateKeyPair()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                generate(buildSpec(strongBox = true))
            } catch (_: Exception) {
                generate(buildSpec(strongBox = false))
            }
        } else {
            generate(buildSpec(strongBox = false))
        }
    }

    fun getPublicKey(): PublicKey {
        val keystore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return keystore.getCertificate(ALIAS).publicKey
    }

    fun getPrivateKey(): PrivateKey {
        val keystore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return keystore.getKey(ALIAS, null) as PrivateKey
    }

    /**
     * ECDH: Leitet aus eigenem Private Key + fremdem Public Key
     * ein gemeinsames Secret ab. Dieses Secret ist auf beiden
     * Seiten identisch, ohne dass es übertragen wurde.
     */
    fun computeSharedSecret(theirPublicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH", PROVIDER)
        keyAgreement.init(getPrivateKey())
        keyAgreement.doPhase(theirPublicKey, true)
        return keyAgreement.generateSecret()
    }
}