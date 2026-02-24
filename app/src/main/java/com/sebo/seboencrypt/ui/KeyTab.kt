package com.sebo.seboencrypt.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sebo.seboencrypt.ClipboardHelper
import com.sebo.seboencrypt.ShareHelper
import com.sebo.seboencrypt.model.Contact
import com.sebo.seboencrypt.viewmodel.E2EEViewModel

@Composable
fun KeyTab(
    vm: E2EEViewModel,
    onScanQR: () -> Unit
) {
    val context        = LocalContext.current
    val qrBitmap       by vm.myQRBitmap.collectAsState()
    val contacts       by vm.contacts.collectAsState()
    val activeContact  by vm.activeContact.collectAsState()
    val hasPendingQR   by vm.hasPendingQR.collectAsState()

    val selectedSubTab = remember { mutableIntStateOf(0) }

    // Dialog-States
    val showNameDialog      = remember { mutableStateOf(false) }
    val pendingName         = remember { mutableStateOf("") }
    val showManualAddDialog = remember { mutableStateOf(false) }
    val manualName          = remember { mutableStateOf("") }
    val manualKey           = remember { mutableStateOf("") }
    val renameTarget        = remember { mutableStateOf<Contact?>(null) }
    val renameText          = remember { mutableStateOf("") }
    val deleteTarget        = remember { mutableStateOf<Contact?>(null) }
    val detailContact       = remember { mutableStateOf<Contact?>(null) }

    // Dialog nach QR-Scan öffnen
    LaunchedEffect(hasPendingQR) {
        if (hasPendingQR) {
            pendingName.value = ""
            showNameDialog.value = true
        }
    }

    // ── Dialog: Nach QR-Scan benennen ────────────────────────────────────────
    if (showNameDialog.value) {
        AlertDialog(
            onDismissRequest = { showNameDialog.value = false },
            title = { Text("Kontakt benennen") },
            text = {
                OutlinedTextField(
                    value = pendingName.value,
                    onValueChange = { pendingName.value = it },
                    label = { Text("Name (z. B. Alice)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.confirmAddContact(pendingName.value)
                    showNameDialog.value = false
                }) { Text("Hinzufügen") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog.value = false }) { Text("Abbrechen") }
            }
        )
    }

    // ── Dialog: Manuell per Base64 hinzufügen ────────────────────────────────
    if (showManualAddDialog.value) {
        AlertDialog(
            onDismissRequest = { showManualAddDialog.value = false },
            title = { Text("Kontakt manüll hinzufügen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Gib den Namen und den Base64-kodierten öffentlichen Schlüssel ein.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = manualName.value,
                        onValueChange = { manualName.value = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualKey.value,
                        onValueChange = { manualKey.value = it },
                        label = { Text("Public Key (Base64)") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.addContactManually(manualName.value, manualKey.value)
                        showManualAddDialog.value = false
                        manualName.value = ""
                        manualKey.value = ""
                    },
                    enabled = manualName.value.isNotBlank() && manualKey.value.isNotBlank()
                ) { Text("Hinzufügen") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showManualAddDialog.value = false
                    manualName.value = ""
                    manualKey.value = ""
                }) { Text("Abbrechen") }
            }
        )
    }

    // ── Dialog: Umbenennen ───────────────────────────────────────────────────
    renameTarget.value?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget.value = null },
            title = { Text("Umbenennen") },
            text = {
                OutlinedTextField(
                    value = renameText.value,
                    onValueChange = { renameText.value = it },
                    label = { Text("Neuer Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.renameContact(target.id, renameText.value)
                        renameTarget.value = null
                    },
                    enabled = renameText.value.isNotBlank()
                ) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget.value = null }) { Text("Abbrechen") }
            }
        )
    }

    // ── Dialog: Löschen bestätigen ───────────────────────────────────────────
    deleteTarget.value?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget.value = null },
            title = { Text("Kontakt löschen?") },
            text = { Text("\"${target.name}\" wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteContact(target.id)
                    deleteTarget.value = null
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget.value = null }) { Text("Abbrechen") }
            }
        )
    }

    // ── Dialog: Kontaktdetails ───────────────────────────────────────────────
    detailContact.value?.let { contact ->
        AlertDialog(
            onDismissRequest = { detailContact.value = null },
            title = { Text(contact.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Öffentlicher Schlüssel:", style = MaterialTheme.typography.labelMedium)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = contact.publicKeyBase64,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = { ClipboardHelper.copyToClipboard(context, contact.publicKeyBase64) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kopieren")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailContact.value = null }) { Text("Schliessen") }
            }
        )
    }

    // ── Haupt-Layout ─────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab.intValue) {
            Tab(
                selected = selectedSubTab.intValue == 0,
                onClick  = { selectedSubTab.intValue = 0 },
                text     = { Text("Mein Schlüssel") }
            )
            Tab(
                selected = selectedSubTab.intValue == 1,
                onClick  = { selectedSubTab.intValue = 1 },
                text     = { Text("Kontakte (${contacts.size})") }
            )
        }

        when (selectedSubTab.intValue) {
            0 -> MyKeySection(vm = vm, qrBitmap = qrBitmap, context = context)
            1 -> ContactsSection(
                contacts      = contacts,
                activeContact = activeContact,
                onScanQR      = onScanQR,
                onManualAdd   = { showManualAddDialog.value = true },
                onSelect      = { vm.selectContact(it) },
                onRename      = { c -> renameText.value = c.name; renameTarget.value = c },
                onDelete      = { deleteTarget.value = it },
                onDetail      = { detailContact.value = it }
            )
        }
    }
}

@Composable
private fun MyKeySection(
    vm: E2EEViewModel,
    qrBitmap: Bitmap?,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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

@Composable
private fun ContactsSection(
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

@Composable
fun ContactListItem(
    contact: Contact,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDetail: () -> Unit = {}
) {
    Surface(
        color    = if (isActive) MaterialTheme.colorScheme.primaryContainer
                   else MaterialTheme.colorScheme.surfaceVariant,
        shape    = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = contact.name,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = contact.publicKeyBase64.take(24) + "...",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isActive) {
                    Text(
                        text  = "Aktiv",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDetail) {
                Icon(Icons.Filled.Info, contentDescription = "Details")
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Filled.Edit, contentDescription = "Umbenennen")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
