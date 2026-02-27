package com.sebo.seboencrypt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sebo.eboard.manager.SettingsManager

@Composable
fun SettingsTab() {
    val context = LocalContext.current

    var hapticEnabled by remember {
        mutableStateOf(SettingsManager.isHapticFeedbackEnabled(context))
    }
    var hapticStrength by remember {
        mutableStateOf(SettingsManager.getHapticStrength(context))
    }
    var soundEnabled by remember {
        mutableStateOf(SettingsManager.isSoundFeedbackEnabled(context))
    }
    var selectedTheme by remember {
        mutableStateOf(SettingsManager.getTheme(context))
    }
    var keyHeight by remember {
        mutableStateOf(SettingsManager.getKeyHeight(context).toFloat())
    }
    var keyTextSize by remember {
        mutableStateOf(SettingsManager.getKeyTextSize(context).toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // === Design Section ===
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Design",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Theme Selection
                Text(
                    text = "Farbschema",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val themes = listOf(
                    SettingsManager.THEME_DARK to "Dunkel",
                    SettingsManager.THEME_LIGHT to "Hell",
                    SettingsManager.THEME_BLUE to "Blau",
                    SettingsManager.THEME_GREEN to "Grün"
                )

                themes.forEach { (themeId, themeName) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedTheme == themeId,
                            onClick = {
                                selectedTheme = themeId
                                SettingsManager.setTheme(context, themeId)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = themeName)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Key Height
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.TextFields,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tastenhöhe: ${keyHeight.toInt()}dp",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Slider(
                    value = keyHeight,
                    onValueChange = { keyHeight = it },
                    onValueChangeFinished = {
                        SettingsManager.setKeyHeight(context, keyHeight.toInt())
                    },
                    valueRange = 40f..70f,
                    steps = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Key Text Size
                Text(
                    text = "Textgröße: ${keyTextSize.toInt()}sp",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Slider(
                    value = keyTextSize,
                    onValueChange = { keyTextSize = it },
                    onValueChangeFinished = {
                        SettingsManager.setKeyTextSize(context, keyTextSize.toInt())
                    },
                    valueRange = 14f..28f,
                    steps = 6
                )
            }
        }

        // === Haptic Feedback Section ===
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Haptisches Feedback",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Haptic Feedback Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Vibration aktivieren",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = {
                            hapticEnabled = it
                            SettingsManager.setHapticFeedbackEnabled(context, it)
                        }
                    )
                }

                // Haptic Strength (only if enabled)
                if (hapticEnabled) {
                    Text(
                        text = "Vibrationsstärke",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val strengths = listOf(
                        SettingsManager.HAPTIC_LIGHT to "Leicht",
                        SettingsManager.HAPTIC_MEDIUM to "Mittel",
                        SettingsManager.HAPTIC_STRONG to "Stark"
                    )

                    strengths.forEach { (strengthId, strengthName) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = hapticStrength == strengthId,
                                onClick = {
                                    hapticStrength = strengthId
                                    SettingsManager.setHapticStrength(context, strengthId)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = strengthName)
                        }
                    }
                }
            }
        }

        // === Sound Feedback Section ===
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sound Feedback",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tastentöne aktivieren",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            SettingsManager.setSoundFeedbackEnabled(context, it)
                        }
                    )
                }
            }
        }

        // Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ℹ️ Hinweis",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Die Einstellungen werden direkt auf die S.E.B.O. E-Board Tastatur angewendet. Wechseln Sie zur Tastatur, um die Änderungen zu sehen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}



