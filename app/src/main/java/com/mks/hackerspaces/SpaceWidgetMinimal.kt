package com.mks.hackerspaces

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SpaceWidgetMinimal : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = SpaceRepository(context)
        val spaceUrl = try {
            context.widgetDataStore.data.map { it[stringPreferencesKey("widget_space_url")] }.first()
        } catch (e: Exception) {
            null
        }
        
        var space: SpaceApi? = null
        var iconBitmap: Bitmap? = null
        
        if (spaceUrl != null) {
            try {
                space = repo.getSpace(spaceUrl)
                // Load state icon
                val iconUrl = if (space.state?.open == true) space.state?.icon?.open else space.state?.icon?.closed
                
                if (!iconUrl.isNullOrEmpty()) {
                    try {
                        val loader = ImageLoader(context)
                        val request = ImageRequest.Builder(context)
                            .data(iconUrl)
                            .size(128, 128) 
                            .allowHardware(false)
                            .build()
                        val result = loader.execute(request)
                        iconBitmap = result.drawable?.toBitmap()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                // Error
            }
        }

        provideContent {
            // Transparent or Minimal background
            // We use a very subtle dark background for visibility or fully transparent if icon is good enough
            // Let's use circle shape background if possible or just rounded corners
            val bgColor = ColorProvider(Color.Black.copy(alpha = 0.4f)) // Semi-transparent
            
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgColor)
                    .clickable(actionRunCallback<UpdateWidgetAction>())
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (space != null) {
                    val isOpen = space.state?.open == true
                    val statusColor = if (isOpen) Color(0xFF00FF00) else Color(0xFFFF0000)
                    
                    if (iconBitmap != null) {
                         Image(
                            provider = ImageProvider(iconBitmap!!),
                            contentDescription = if (isOpen) "Open" else "Closed",
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    } else {
                        // Minimal colored box indicator if no icon
                        Box(
                            modifier = GlanceModifier
                                .size(32.dp)
                                .background(ColorProvider(statusColor))
                        ) {}
                    }
                } else {
                    // Loading or Error State - Minimal dot
                    Box(
                        modifier = GlanceModifier
                            .size(12.dp)
                            .background(ColorProvider(Color.Gray))
                    ) {}
                }
            }
        }
    }
}
