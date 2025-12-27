package com.mks.hackerspaces

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: SpaceViewModel) {
    val refreshInterval by viewModel.refreshInterval.collectAsState(initial = 15L)
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState(initial = false)
    val unsafeSsl by viewModel.unsafeSsl.collectAsState(initial = true)
    val maxSpaces by viewModel.maxSpaces.collectAsState(initial = 200)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Refresh Interval
        Text("Refresh Interval (minutes)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = refreshInterval.toString(),
            onValueChange = { 
                if (it.isNotEmpty()) {
                    viewModel.updateRefreshInterval(it.toLongOrNull() ?: 15L)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Notifications (Mock)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Enable Notifications", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
            )
        }
        Text("Get notified when your favorite spaces open/close.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Unsafe SSL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Allow Unsafe SSL", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = unsafeSsl,
                onCheckedChange = { viewModel.updateUnsafeSsl(it) }
            )
        }
        Text("Enable this to connect to spaces with self-signed certificates.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Max Spaces for Map
        Text("Max Spaces to Load on Map", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = maxSpaces.toString(),
            onValueChange = { 
                 if (it.isNotEmpty()) {
                    viewModel.updateMaxSpaces(it.toIntOrNull() ?: 100)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
