package com.mks.hackerspaces

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.mks.hackerspaces.ui.theme.HackerSpacesTheme

class SpaceWidgetConfigurationActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultValue)

        setContent {
            HackerSpacesTheme {
                WidgetConfigScreen(
                    onSpaceSelected = { url ->
                        saveAndFinish(url)
                    }
                )
            }
        }
    }

    private fun saveAndFinish(url: String) {
        lifecycleScope.launch {
            try {
                widgetDataStore.edit { 
                    it[stringPreferencesKey("widget_space_url")] = url
                }

                try {
                    val manager = GlanceAppWidgetManager(this@SpaceWidgetConfigurationActivity)
                    val glanceId = manager.getGlanceIdBy(appWidgetId)
                    if (glanceId != null) {
                        // Check if it is Minimal or Normal widget based on ID or try updating both
                        // Since we don't know easily which class this ID belongs to without querying,
                        // and updating the wrong one might be harmless or throw.
                        // GlanceAppWidgetManager.getGlanceIdBy returns a GlanceId which contains the provider class info implicitly.
                        // We can just try to update both classes for this ID, one will succeed.
                        
                        try {
                            SpaceWidget().update(this@SpaceWidgetConfigurationActivity, glanceId)
                        } catch (e: Exception) {}
                        
                        try {
                            SpaceWidgetMinimal().update(this@SpaceWidgetConfigurationActivity, glanceId)
                        } catch (e: Exception) {}
                    }
                } catch (e: Exception) {
                }
            } catch (e: Exception) {
            } finally {
                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(onSpaceSelected: (String) -> Unit) {
    val context = LocalContext.current
    val repo = SpaceRepository(context)
    val favorites by repo.favorites.collectAsState(initial = emptySet())
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Select a Space for Widget") }) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No favorite spaces found. Add some in the app first!")
                }
            } else {
                LazyColumn {
                    items(favorites.toList()) { url ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { onSpaceSelected(url) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Space: $url", style = MaterialTheme.typography.bodyLarge) 
                            }
                        }
                    }
                }
            }
        }
    }
}
