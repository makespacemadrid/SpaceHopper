package com.mks.hackerspaces

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SpaceViewModel) {
    val refreshInterval by viewModel.refreshInterval.collectAsState(initial = 15L)
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState(initial = false)
    val unsafeSsl by viewModel.unsafeSsl.collectAsState(initial = true)
    val maxSpaces by viewModel.maxSpaces.collectAsState(initial = 200)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
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

            // Notifications
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Notifications (Open/Close)", modifier = Modifier.weight(1f))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Unsafe SSL
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Allow Unsafe SSL")
                    Text("Enable to access spaces with self-signed certs", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = unsafeSsl,
                    onCheckedChange = { viewModel.updateUnsafeSsl(it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Max Spaces
            Text("Max Spaces to Load (Map)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = maxSpaces.toString(),
                onValueChange = { 
                    if (it.isNotEmpty()) {
                        viewModel.updateMaxSpaces(it.toIntOrNull() ?: 200)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
