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
fun DecryptTab(vm: E2EEViewModel) {
    val context       = LocalContext.current
    val input         by vm.decryptInput.collectAsState()
    val output        by vm.decryptOutput.collectAsState()
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
            Text("Absender", style = MaterialTheme.typography.titleMedium)
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

        // ── Chiffretext eingeben ──
        Text("Verschlüsselte Nachricht einfügen", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = input,
            onValueChange = { vm.decryptInput.value = it },
            label = { Text("Chiffretext") },
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
                onClick = { vm.pasteToDecryptInput(context) },
                modifier = Modifier.weight(1f)
            ) { Text("📋 Einfügen") }
            Button(
                onClick = { vm.decrypt() },
                modifier = Modifier.weight(1f),
                enabled = activeContact != null
            ) { Text("🔓 Entschlüsseln") }
        }

        if (output.isNotEmpty()) {
            HorizontalDivider()
            Text("Entschlüsselte Nachricht:", style = MaterialTheme.typography.titleMedium)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = output,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            OutlinedButton(
                onClick = { vm.copyDecryptOutput(context) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📋 Klartext kopieren") }
        }
    }
}
