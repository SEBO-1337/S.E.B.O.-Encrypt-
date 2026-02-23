package com.sebo.seboencrypt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.sebo.seboencrypt.ui.DecryptTab
import com.sebo.seboencrypt.ui.EncryptTab
import com.sebo.seboencrypt.ui.KeyTab
import com.sebo.seboencrypt.ui.StatusBanner
import com.sebo.seboencrypt.ui.theme.SEBOEncryptTheme
import com.sebo.seboencrypt.viewmodel.E2EEViewModel

class MainActivity : ComponentActivity() {

    // ZXing ActivityResult-Launcher
    private lateinit var qrScanLauncher: androidx.activity.result.ActivityResultLauncher<ScanOptions>

    // ViewModel-Referenz für den Callback außerhalb von Composables
    private var viewModelRef: E2EEViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // QR-Scanner registrieren
        qrScanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            val content = result.contents
            if (content != null) {
                viewModelRef?.onQRScanned(content)
            }
        }

        enableEdgeToEdge()
        setContent {
            SEBOEncryptTheme {
                val vm: E2EEViewModel = viewModel()
                // ViewModel-Referenz für den Launcher-Callback setzen
                LaunchedEffect(vm) { viewModelRef = vm }

                MainScreen(
                    vm = vm,
                    onScanQR = {
                        val options = ScanOptions().apply {
                            setPrompt("Kontakt-QR-Code scannen")
                            setBeepEnabled(true)
                            setOrientationLocked(false)
                        }
                        qrScanLauncher.launch(options)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: E2EEViewModel, onScanQR: () -> Unit) {
    val status by vm.status.collectAsState()

    val tabs = listOf(
        Triple("Verschlüsseln", Icons.Filled.Lock, 0),
        Triple("Entschlüsseln", Icons.Filled.LockOpen, 1),
        Triple("Schlüssel", Icons.Filled.QrCode, 2)
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("S.E.B.O. Encrypt") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Status-Banner
            StatusBanner(
                status = status,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tab-Leiste
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEach { (label, icon, index) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) },
                        icon = { Icon(icon, contentDescription = label) }
                    )
                }
            }

            // Tab-Inhalt
            when (selectedTab) {
                0 -> EncryptTab(vm)
                1 -> DecryptTab(vm)
                2 -> KeyTab(vm, onScanQR = onScanQR)
            }
        }
    }
}