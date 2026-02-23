package com.sebo.seboencrypt.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sebo.seboencrypt.viewmodel.E2EEViewModel

@Composable
fun EncryptTab(vm: E2EEViewModel) {
    val context = LocalContext.current
    val input   by vm.encryptInput.collectAsState()
    val output  by vm.encryptOutput.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔒 Verschlüsseln")
        }

        if (output.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                ) {
                    Text("📋 Kopieren")
                }
                Button(
                    onClick = { vm.shareViaWhatsApp(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📤 Via WhatsApp")
                }
            }
        }
    }
}


