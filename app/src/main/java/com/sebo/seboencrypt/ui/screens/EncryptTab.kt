package com.sebo.seboencrypt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import com.sebo.seboencrypt.viewmodel.E2EEViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptTab(vm: E2EEViewModel) {
    val context       = LocalContext.current
    val input         by vm.encryptInput.collectAsState()
    val output        by vm.encryptOutput.collectAsState()
    val contacts      by vm.contacts.collectAsState()
    val activeContact by vm.activeContact.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Kontakt-Auswahl ──
        if (contacts.isNotEmpty()) {
            Text("Empfänger", style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = activeContact?.name ?: "Kein Kontakt",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kontakt auswählen") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    contacts.forEach { contact ->
                        DropdownMenuItem(
                            text = { Text(contact.name) },
                            onClick = {
                                vm.selectContact(contact)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
            HorizontalDivider()
        }

        // ── Nachricht ──
        Text("Nachricht eingeben", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = input,
            onValueChange = { vm.encryptInput.value = it },
            label = { Text("Klartext") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            minLines = 4,
            maxLines = 10
        )

        Button(
            onClick = { vm.encrypt() },
            modifier = Modifier.fillMaxWidth(),
            enabled = activeContact != null
        ) {
            Text("🔒 Verschlüsseln")
        }

        if (output.isNotEmpty()) {
            HorizontalDivider()
            Text("Verschlüsselte Nachricht:", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = output,
                onValueChange = {},
                readOnly = true,
                label = { Text("Chiffretext (zum Senden)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                minLines = 4,
                maxLines = 12
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { vm.copyEncryptOutput(context) },
                    modifier = Modifier.weight(1f)
                ) { Text("📋 Kopieren") }
                Button(
                    onClick = { vm.shareViaWhatsApp(context) },
                    modifier = Modifier.weight(1f)
                ) { Text("📤 Via WhatsApp") }
            }
        }
    }
}
