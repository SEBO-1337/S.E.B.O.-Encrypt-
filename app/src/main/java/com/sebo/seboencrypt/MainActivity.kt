package com.sebo.seboencrypt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.sebo.seboencrypt.ui.screens.DecryptTab
import com.sebo.seboencrypt.ui.screens.EncryptTab
import com.sebo.seboencrypt.ui.screens.KeyTab
import com.sebo.seboencrypt.ui.components.StatusBanner
import com.sebo.seboencrypt.ui.theme.SEBOEncryptTheme
import com.sebo.seboencrypt.viewmodel.E2EEViewModel

class MainActivity : ComponentActivity() {

    private lateinit var qrScanLauncher: androidx.activity.result.ActivityResultLauncher<ScanOptions>

    private var viewModelRef: E2EEViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                LaunchedEffect(vm) { viewModelRef = vm }

                // Share-Intent beim App-Start auswerten
                LaunchedEffect(Unit) {
                    handleSharedText(intent, vm)
                }

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

    /** Beim Zurückkehren zur App Zwischenablage auf verschlüsselten Text prüfen */
    override fun onResume() {
        super.onResume()
        val vm = viewModelRef ?: return
        if (vm.sharedTextPending.value != null) return

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: return

        val base64Regex = Regex("^[A-Za-z0-9+/]+=*$")
        if (text.length >= 32 && !text.contains(' ') && base64Regex.matches(text)) {
            if (text != vm.decryptInput.value) {
                vm.onSharedTextReceived(text)
            }
        }
    }

    /** Wird aufgerufen, wenn die App bereits läuft und ein neuer Intent reinkommt */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModelRef?.let { handleSharedText(intent, it) }
    }

    private fun handleSharedText(intent: Intent, vm: E2EEViewModel) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                vm.onSharedTextReceived(sharedText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: E2EEViewModel, onScanQR: () -> Unit) {
    val status by vm.status.collectAsState()
    val sharedTextPending by vm.sharedTextPending.collectAsState()

    val tabs = listOf(
        Triple("Verschlüsseln", Icons.Filled.Lock, 0),
        Triple("Entschlüsseln", Icons.Filled.LockOpen, 1),
        Triple("Schlüssel", Icons.Filled.QrCode, 2)
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(sharedTextPending) {
        if (sharedTextPending != null) {
            selectedTab = 1
            vm.consumeSharedText()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.banner),
                    contentDescription = "S.E.B.O. Encrypt",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentScale = ContentScale.FillWidth
                )

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

                when (selectedTab) {
                    0 -> EncryptTab(vm)
                    1 -> DecryptTab(vm)
                    2 -> KeyTab(vm, onScanQR = onScanQR)
                }
            }

            StatusBanner(
                status = status,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}