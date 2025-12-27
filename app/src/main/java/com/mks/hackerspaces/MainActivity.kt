package com.mks.hackerspaces

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.mks.hackerspaces.ui.theme.HackerSpacesTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SpaceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HackerSpacesTheme {
                HackerSpacesApp(viewModel)
            }
        }
    }
}

@Composable
fun HackerSpacesApp(viewModel: SpaceViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MY_SPACES) }
    val selectedSpace by viewModel.selectedSpace.collectAsState()

    if (selectedSpace != null) {
        SpaceDetailScreen(
            space = selectedSpace!!,
            onBack = { viewModel.clearSelectedSpace() }
        )
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = {
                            Icon(
                                it.icon,
                                contentDescription = it.label
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentDestination) {
                        AppDestinations.MY_SPACES -> MySpacesScreen(viewModel)
                        AppDestinations.DIRECTORY -> DirectoryScreen(viewModel)
                        AppDestinations.MAP -> MapScreen(viewModel)
                        AppDestinations.SETTINGS -> SettingsScreen(viewModel)
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    MY_SPACES("MySpaces", Icons.Default.Favorite),
    DIRECTORY("Directory", Icons.Default.List),
    MAP("Map", Icons.Default.LocationOn),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MySpacesScreen(viewModel: SpaceViewModel) {
    val mySpaces by viewModel.mySpaces.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (mySpaces.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorite spaces yet. Add some from the Directory!")
            }
        } else {
            LazyColumn {
                items(mySpaces) { space ->
                    SpaceItem(
                        space = space,
                        onClick = { viewModel.selectSpace(space) },
                        onRemove = { viewModel.toggleFavorite(space.url) },
                        isFavorite = true
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.refreshMySpaces() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh Status")
        }
    }
}

@Composable
fun DirectoryScreen(viewModel: SpaceViewModel) {
    val directory by viewModel.directory.collectAsState()
    val favorites by viewModel.favorites.collectAsState(initial = emptySet())
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Search spaces...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            LazyColumn {
                val filteredItems = directory.toList().filter { (name, _) ->
                    name.contains(searchQuery, ignoreCase = true)
                }

                items(filteredItems) { (name, url) ->
                    DirectoryItem(
                        name = name,
                        url = url,
                        isFavorite = favorites.contains(url),
                        onToggleFavorite = { viewModel.toggleFavorite(url) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.fetchDirectory() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh Directory")
        }
    }
}

@Composable
fun SpaceItem(
    space: SpaceApi,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    isFavorite: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isOpen = space.state?.open == true
            val iconUrl = if (isOpen) space.state?.icon?.open else space.state?.icon?.closed
            
            if (iconUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(iconUrl),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                 Icon(
                     if (isOpen) Icons.Default.Home else Icons.Default.Info, // Placeholder if no icon
                     contentDescription = null,
                     tint = if (isOpen) Color.Green else Color.Red,
                     modifier = Modifier.size(40.dp)
                 )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = space.space ?: "Unknown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isOpen) "Open" else "Closed",
                    color = if (isOpen) Color.Green else Color.Red
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}


@Composable
fun DirectoryItem(
    name: String,
    url: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.Add,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) Color.Red else LocalContentColor.current
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDetailScreen(space: SpaceApi, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(space.space ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Home, contentDescription = "Back") // Using Home as back for now or ArrowBack
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Address: ${space.location?.address ?: "Unknown"}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${if (space.state?.open == true) "Open" else "Closed"}")
            
            space.contact?.let { contact ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Contact", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                contact.phone?.let { Text("Phone: $it") }
                contact.email?.let { Text("Email: $it") }
                contact.twitter?.let { Text("Twitter: $it") }
                contact.instagram?.let { Text("Instagram: $it") }
            }

            space.sensors?.let { sensors ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Sensors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                sensors.temperature?.forEach { sensor ->
                    Text("Temperature (${sensor.location ?: "Unknown"}): ${sensor.value} ${sensor.unit}")
                }
                sensors.humidity?.forEach { sensor ->
                    Text("Humidity (${sensor.location ?: "Unknown"}): ${sensor.value} ${sensor.unit}")
                }
                sensors.powerConsumption?.forEach { sensor ->
                    Text("Power (${sensor.location ?: "Unknown"}): ${sensor.value} ${sensor.unit}")
                }
            }
        }
    }
}
