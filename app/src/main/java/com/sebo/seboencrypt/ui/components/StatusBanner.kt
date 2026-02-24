package com.sebo.seboencrypt.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sebo.seboencrypt.viewmodel.UiStatus
import kotlinx.coroutines.delay

@Composable
fun StatusBanner(status: UiStatus, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        visible = true
        delay(5000)
        visible = false
    }

    val containerColor = when {
        status.isError -> MaterialTheme.colorScheme.errorContainer
        status.icon == "✅" || status.icon == "🔒" || status.icon == "🔓" ->
            MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        status.isError -> MaterialTheme.colorScheme.onErrorContainer
        status.icon == "✅" || status.icon == "🔒" || status.icon == "🔓" ->
            MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = containerColor,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = status.icon, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        }
    }
}

