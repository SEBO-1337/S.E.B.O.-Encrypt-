package com.sebo.seboencrypt.ui.components.keytab

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.sebo.seboencrypt.helper.ClipboardHelper
import com.sebo.seboencrypt.helper.ShareHelper
import com.sebo.seboencrypt.viewmodel.E2EEViewModel

@Composable
fun MyKeySection(
    vm: E2EEViewModel,
    qrBitmap: Bitmap?,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Mein öffentlicher Schlüssel", style = MaterialTheme.typography.titleMedium)
        Text(
            "Zeige diesen QR-Code deinem Kontakt zum Einscannen oder teile den Key als Text.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        qrBitmap?.let { bmp: Bitmap ->
            Card(
                modifier  = Modifier.size(260.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Image(
                    bitmap             = bmp.asImageBitmap(),
                    contentDescription = "Mein QR-Code",
                    modifier           = Modifier.fillMaxSize().padding(8.dp)
                )
            }
        }

        HorizontalDivider()
        Text("Key als Text teilen", style = MaterialTheme.typography.titleSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick  = { ClipboardHelper.copyToClipboard(context, vm.getMyPublicKeyBase64()) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Kopieren")
            }
            Button(
                onClick  = { ShareHelper.shareViaWhatsApp(context, vm.getMyPublicKeyBase64()) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Teilen")
            }
        }
    }
}
