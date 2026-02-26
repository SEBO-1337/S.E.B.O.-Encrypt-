package com.sebo.seboencrypt.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SEBOEncrypt", text)
        clipboard.setPrimaryClip(clip)
    }

    fun pasteFromClipboard(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0) ?: return null
        return item.coerceToText(context)?.toString()
    }
}

