package com.sebo.seboencrypt

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanOptions
import com.sebo.seboencrypt.ui.components.StatusBanner
import com.sebo.seboencrypt.ui.screens.DecryptTab
import com.sebo.seboencrypt.ui.screens.EncryptTab
import com.sebo.seboencrypt.ui.screens.KeyTab
import com.sebo.seboencrypt.ui.theme.SEBOEncryptTheme
import com.sebo.seboencrypt.viewmodel.E2EEViewModel

class MainActivity : FragmentActivity() {

    companion object {
        private const val QR_SCAN_REQUEST_CODE = 42
    }

    private var viewModelRef: E2EEViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fix 4: Screenshot-Schutz – verhindert Screenshots und App-Switcher-Preview
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        enableEdgeToEdge()
        setContent {
            SEBOEncryptTheme {
                val vm: E2EEViewModel = viewModel()
                LaunchedEffect(vm) { viewModelRef = vm }

                // Share-Intent beim App-Start auswerten
                LaunchedEffect(Unit) {
                    handleSharedText(intent, vm)
                }

                // Fix 5: App-Inhalt erst nach erfolgreicher Authentifizierung anzeigen
                val isAuthenticated by vm.isAuthenticated.collectAsState()
                if (isAuthenticated) {
                    MainScreen(
                        vm = vm,
                        onScanQR = { launchQRScanner() }
                    )
                } else {
                    LockScreen()
                }
            }
        }

        // Fix 5: Beim ersten Start Authentifizierung anfordern
        triggerAuth()
    }

    /** QR-Scanner direkt starten – ohne ScanContract, um den 16-Bit-Request-Code-Fehler zu umgehen */
    @Suppress("DEPRECATION")
    private fun launchQRScanner() {
        val options = ScanOptions().apply {
            setPrompt("Kontakt-QR-Code scannen")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setCaptureActivity(PortraitCaptureActivity::class.java)
        }
        val intent = options.createScanIntent(this)
        startActivityForResult(intent, QR_SCAN_REQUEST_CODE)
    }

    /** QR-Scan-Ergebnis empfangen */
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == QR_SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            val content = data?.getStringExtra("SCAN_RESULT")
            if (content != null) {
                viewModelRef?.onQRScanned(content)
            }
        }
    }

    /** Fix 5: Bei jedem Zurückkehren zur App erneut authentifizieren */
    override fun onResume() {
        super.onResume()
        val vm = viewModelRef ?: return
        if (!vm.isAuthenticated.value) {
            triggerAuth()
        }
    }

    /** Fix 5: Biometrie / Geräte-PIN anfordern */
    private fun triggerAuth() {
        BiometricAuthHelper.authenticate(
            activity   = this,
            onSuccess  = { viewModelRef?.isAuthenticated?.value = true },
            onFailure  = { /* App bleibt gesperrt – Nutzer sieht LockScreen */ },
            onNotEnrolled = {
                // Kein PIN/Biometrie eingerichtet → App trotzdem nutzbar (Sicherheitshinweis)
                viewModelRef?.isAuthenticated?.value = true
            }
        )
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

/** Fix 5: Sperr-Bildschirm der angezeigt wird bis zur erfolgreichen Authentifizierung */
@Composable
fun LockScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🔐", style = MaterialTheme.typography.displayLarge)
            Text(
                "S.E.B.O. Encrypt",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Bitte authentifiziere dich mit Biometrie oder Geräte-PIN, um die App zu entsperren.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

                PrimaryTabRow(selectedTabIndex = selectedTab) {
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