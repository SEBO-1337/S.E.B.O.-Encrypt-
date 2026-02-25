package com.sebo.seboencrypt.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sebo.seboencrypt.ClipboardHelper
import com.sebo.seboencrypt.model.Contact
import com.sebo.seboencrypt.ui.components.keytab.ContactsSection
import com.sebo.seboencrypt.ui.components.keytab.MyKeySection
import com.sebo.seboencrypt.viewmodel.E2EEViewModel

@Composable
fun KeyTab(
    vm: E2EEViewModel,
    onScanQR: () -> Unit
) {
    val context           = LocalContext.current
    val qrBitmap          by vm.myQRBitmap.collectAsState()
    val contacts          by vm.contacts.collectAsState()
    val activeContact     by vm.activeContact.collectAsState()
    val hasPendingQR      by vm.hasPendingQR.collectAsState()
    // Fix 2 (TOFU): Fingerprint des gescannten Keys für den Verifikations-Dialog
    val pendingFingerprint by vm.pendingFingerprint.collectAsState()

    val selectedSubTab = remember { mutableIntStateOf(0) }

    // Dialog-States
    val showFingerprintDialog = remember { mutableStateOf(false) }
    val showNameDialog        = remember { mutableStateOf(false) }
    val pendingName           = remember { mutableStateOf("") }
    val showManualAddDialog   = remember { mutableStateOf(false) }
    val manualName            = remember { mutableStateOf("") }
    val manualKey             = remember { mutableStateOf("") }
    val renameTarget          = remember { mutableStateOf<Contact?>(null) }
    val renameText            = remember { mutableStateOf("") }
    val deleteTarget          = remember { mutableStateOf<Contact?>(null) }
    val detailContact         = remember { mutableStateOf<Contact?>(null) }

    // Fix 2: Erst Fingerprint-Verifikation, dann Namens-Dialog
    LaunchedEffect(hasPendingQR) {
        if (hasPendingQR) {
            pendingName.value = ""
            showFingerprintDialog.value = true
        }
    }

    // ── Dialog: Fix 2 – TOFU Fingerprint-Verifikation ──────────────────────
    if (showFingerprintDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showFingerprintDialog.value = false
                vm.cancelPendingQR()
            },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Schlüssel verifizieren") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Vergleiche den folgenden Fingerprint mit dem, den dein Kontakt auf seinem Gerät sieht. Stimmt er überein, ist die Verbindung sicher (TOFU).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pendingFingerprint ?: "Fingerprint nicht verfügbar",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text(
                        "⚠️ Brich ab, wenn der Fingerprint nicht übereinstimmt!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFingerprintDialog.value = false
                    showNameDialog.value = true
                }) { Text("✅ Fingerprint stimmt überein") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFingerprintDialog.value = false
                    vm.cancelPendingQR()
                }) { Text("❌ Abbrechen") }
            }
        )
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
            title = { Text("Kontakt manuell hinzufügen") },
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
                    // Fix 2: Fingerprint anzeigen
                    if (contact.fingerprint.isNotEmpty()) {
                        Text("🔏 Schlüssel-Fingerprint:", style = MaterialTheme.typography.labelMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = contact.fingerprint,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
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
                TextButton(onClick = { detailContact.value = null }) { Text("Schließen") }
            }
        )
    }

    // ── Haupt-Layout ─────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedSubTab.intValue) {
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
