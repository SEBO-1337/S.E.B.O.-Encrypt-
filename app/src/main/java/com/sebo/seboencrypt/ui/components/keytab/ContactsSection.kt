package com.sebo.seboencrypt.ui.components.keytab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebo.seboencrypt.model.Contact


@Composable
fun ContactsSection(
    contacts: List<Contact>,
    activeContact: Contact?,
    onScanQR: () -> Unit,
    onManualAdd: () -> Unit,
    onSelect: (Contact) -> Unit,
    onRename: (Contact) -> Unit,
    onDelete: (Contact) -> Unit,
    onDetail: (Contact) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Kontakt hinzufügen", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onScanQR, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("QR scannen")
            }
            OutlinedButton(onClick = onManualAdd, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Manuell")
            }
        }

        if (contacts.isNotEmpty()) {
            HorizontalDivider()
            Text("Kontakte (${contacts.size})", style = MaterialTheme.typography.titleMedium)
            contacts.forEach { contact ->
                ContactListItem(
                    contact  = contact,
                    isActive = contact.id == activeContact?.id,
                    onSelect = { onSelect(contact) },
                    onRename = { onRename(contact) },
                    onDelete = { onDelete(contact) },
                    onDetail = { onDetail(contact) }
                )
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = MaterialTheme.colorScheme.surfaceVariant,
                shape    = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Noch keine Kontakte", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Scanne den QR-Code oder füge manuell mit Public Key hinzu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
