package com.sebo.seboencrypt.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.sebo.seboencrypt.R
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
            .fillMaxWidth()
            .padding(16.dp)
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
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Nachricht eingeben",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.encryptInput.value = "" }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eingabe leeren",
                    tint = colorResource(R.color.delete_red_dark)
                )
            }
        }


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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    vm.encrypt()
                    vm.encryptInput.value = ""
                          },
                modifier = Modifier.weight(1f),
                enabled = activeContact != null
            ) {
                Text("🔒 Verschlüsseln")
            }
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
