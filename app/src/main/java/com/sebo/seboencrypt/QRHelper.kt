package com.sebo.seboencrypt

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import android.util.Base64

object QRHelper {

    fun publicKeyToQR(publicKey: PublicKey, size: Int = 512): Bitmap {
        val encoded = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        val bits = QRCodeWriter().encode(encoded, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size)
            bmp.setPixel(x, y, if (bits[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        return bmp
    }

    fun qrStringToPublicKey(base64: String): PublicKey {
        val decoded = Base64.decode(base64, Base64.NO_WRAP)
        return KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(decoded))
    }
}