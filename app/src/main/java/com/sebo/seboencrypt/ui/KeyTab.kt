package com.sebo.seboencrypt.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.sebo.seboencrypt.viewmodel.E2EEViewModel

@Composable
fun KeyTab(
    vm: E2EEViewModel,
    onScanQR: () -> Unit
) {
    val qrBitmap     by vm.myQRBitmap.collectAsState()
    val contactAdded by vm.contactAdded.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mein öffentlicher Schlüssel",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Zeige diesen QR-Code deinem Kontakt, damit er/sie den Schlüssel einscannen kann.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        qrBitmap?.let { bmp: Bitmap ->
            Card(
                modifier = Modifier.size(280.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Mein QR-Code",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "Kontakt-Schlüssel einscannen",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Scanne den QR-Code deines Kontakts, um den gemeinsamen Sitzungsschlüssel zu berechnen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onScanQR,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📷 Kontakt-QR scannen")
        }

        if (contactAdded) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✅ Kontakt verbunden – Verschlüsselung aktiv",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}


