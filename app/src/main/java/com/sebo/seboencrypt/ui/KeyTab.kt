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

    var selectedSubTab by remember { mutableIntStateOf(0) }

    // Dialog-States
    var showNameDialog      by remember { mutableStateOf(false) }
    var pendingName         by remember { mutableStateOf("") }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var manualName          by remember { mutableStateOf("") }
    var manualKey           by remember { mutableStateOf("") }
    var renameTarget        by remember { mutableStateOf<Contact?>(null) }
    var renameText          by remember { mutableStateOf("") }
    var deleteTarget        by remember { mutableStateOf<Contact?>(null) }
    var detailContact       by remember { mutableStateOf<Contact?>(null) }

    // Dialog nach QR-Scan öffnen
    LaunchedEffect(hasPendingQR) {
        if (hasPendingQR) {
            pendingName = ""
            showNameDialog = true
        }
    }

    // ── Dialog: Nach QR-Scan benennen ────────────────────────────────────────
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Kontakt benennen") },
            text = {
                OutlinedTextField(
                    value = pendingName,
                    onValueChange = { pendingName = it },
                    label = { Text("Name (z. B. Alice)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.confirmAddContact(pendingName)
                    showNameDialog = false
                }) { Text("Hinzufuegen") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    // ── Dialog: Manuell per Base64 hinzufügen ────────────────────────────────
    if (showManualAddDialog) {
        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Kontakt manuell hinzufuegen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Gib den Namen und den Base64-kodierten oeffentlichen Schluessel ein.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualKey,
                        onValueChange = { manualKey = it },
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
                        vm.addContactManually(manualName, manualKey)
                        showManualAddDialog = false
                        manualName = ""
                        manualKey = ""
                    },
                    enabled = manualName.isNotBlank() && manualKey.isNotBlank()
                ) { Text("Hinzufuegen") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showManualAddDialog = false
                    manualName = ""
                    manualKey = ""
                }) { Text("Abbrechen") }
            }
        )
    }

    // ── Dialog: Umbenennen ───────────────────────────────────────────────────
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Umbenennen") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Neuer Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.renameContact(target.id, renameText)
                        renameTarget = null
                    },
                    enabled = renameText.isNotBlank()
                ) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Abbrechen") }
            }
        )
    }

    // ── Dialog: Löschen bestätigen ───────────────────────────────────────────
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Kontakt loeschen?") },
            text = { Text("\"${target.name}\" wirklich loeschen? Diese Aktion kann nicht rueckgaengig gemacht werden.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteContact(target.id)
                    deleteTarget = null
                }) { Text("Loeschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Abbrechen") }
            }
        )
    }

    // ── Dialog: Kontaktdetails ───────────────────────────────────────────────
    detailContact?.let { contact ->
        AlertDialog(
            onDismissRequest = { detailContact = null },
            title = { Text(contact.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Oeffentlicher Schluessel:", style = MaterialTheme.typography.labelMedium)
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
                TextButton(onClick = { detailContact = null }) { Text("Schliessen") }
            }
        )
    }

    // ── Haupt-Layout ─────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick  = { selectedSubTab = 0 },
                text     = { Text("Mein Schluessel") }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick  = { selectedSubTab = 1 },
                text     = { Text("Kontakte (${contacts.size})") }
            )
        }

        when (selectedSubTab) {
            0 -> MyKeySection(vm = vm, qrBitmap = qrBitmap, context = context)
            1 -> ContactsSection(
                contacts      = contacts,
                activeContact = activeContact,
                onScanQR      = onScanQR,
                onManualAdd   = { showManualAddDialog = true },
                onSelect      = { vm.selectContact(it) },
                onRename      = { c -> renameText = c.name; renameTarget = c },
                onDelete      = { deleteTarget = it },
                onDetail      = { detailContact = it }
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
        Text("Mein oeffentlicher Schluessel", style = MaterialTheme.typography.titleMedium)
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
        Text("Kontakt hinzufuegen", style = MaterialTheme.typography.titleMedium)

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
                        "Scanne den QR-Code oder fuege manuell mit Public Key hinzu.",
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
                Icon(Icons.Filled.Delete, contentDescription = "Loeschen", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
