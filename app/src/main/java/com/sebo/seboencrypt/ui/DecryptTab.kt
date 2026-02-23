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
fun DecryptTab(vm: E2EEViewModel) {
    val context = LocalContext.current
    val input   by vm.decryptInput.collectAsState()
    val output  by vm.decryptOutput.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            ) {
                Text("📋 Einfügen")
            }
            Button(
                onClick = { vm.decrypt() },
                modifier = Modifier.weight(1f)
            ) {
                Text("🔓 Entschlüsseln")
            }
        }

        if (output.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
            ) {
                Text("📋 Klartext kopieren")
            }
        }
    }
}


