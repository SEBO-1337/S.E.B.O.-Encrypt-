package com.sebo.seboencrypt.helper

import android.content.Context
import android.content.Intent

object ShareHelper {

    /**
     * Versucht, Text direkt über WhatsApp zu teilen.
     * Falls WhatsApp nicht installiert ist, öffnet sich der allgemeine Android-Teilen-Dialog.
     */
    fun shareViaWhatsApp(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }

        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        if (resolveInfo != null) {
            context.startActivity(intent)
        } else {
            // WhatsApp nicht installiert → allgemeiner Teilen-Dialog
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "Verschlüsselte Nachricht teilen via…"
            )
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}

