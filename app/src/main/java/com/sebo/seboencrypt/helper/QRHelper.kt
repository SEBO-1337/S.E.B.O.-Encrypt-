package com.sebo.seboencrypt

import android.graphics.Bitmap
import android.util.Base64
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

object QRHelper {

    fun publicKeyToQR(publicKey: PublicKey, size: Int = 512): Bitmap {
        val encoded = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        val bits = QRCodeWriter().encode(encoded, BarcodeFormat.QR_CODE, size, size)
        val bmp = createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size)
            bmp[x, y] = if (bits[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        return bmp
    }

    fun qrStringToPublicKey(base64: String): PublicKey {
        val decoded = Base64.decode(base64, Base64.NO_WRAP)
        return KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(decoded))
    }

    /**
     * Fix 2 (TOFU): SHA-256-Fingerprint des Public Keys als lesbaren Hex-String zurückgeben.
     * Format: "AB:CD:EF:..." (32 Byte = 64 Hex-Zeichen, gruppiert mit Doppelpunkten)
     */
    fun publicKeyFingerprint(publicKey: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKey.encoded)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}