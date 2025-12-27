package com.mks.hackerspaces

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.*
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

val Context.widgetDataStore by preferencesDataStore("widget_settings")

class SpaceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = SpaceRepository(context)
        val spaceUrl = try {
            context.widgetDataStore.data.map { it[stringPreferencesKey("widget_space_url")] }.first()
        } catch (e: Exception) {
            null
        }
        
        var space: SpaceApi? = null
        var errorMessage: String? = null
        var iconBitmap: Bitmap? = null
        
        if (spaceUrl != null) {
            try {
                space = repo.getSpace(spaceUrl)
                
                // Try to load space state icon (open/closed) instead of logo
                val iconUrl = if (space.state?.open == true) space.state?.icon?.open else space.state?.icon?.closed
                
                if (!iconUrl.isNullOrEmpty()) {
                    try {
                        val loader = ImageLoader(context)
                        val request = ImageRequest.Builder(context)
                            .data(iconUrl)
                            .size(128, 128) 
                            .allowHardware(false) // Important for Widgets
                            .build()
                        val result = loader.execute(request)
                        iconBitmap = result.drawable?.toBitmap()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Connection Error"
            }
        } else {
             errorMessage = "Tap to configure"
        }

        provideContent {
            // Semi-transparent background
            val bgColor = ColorProvider(Color.Black.copy(alpha = 0.6f))
            val textColor = ColorProvider(Color.White)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgColor)
                    .clickable(actionRunCallback<UpdateWidgetAction>())
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (space != null) {
                    val isOpen = space.state?.open == true
                    val statusColor = if (isOpen) Color(0xFF00FF00) else Color(0xFFFF0000)

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // State Icon
                        if (iconBitmap != null) {
                            Image(
                                provider = ImageProvider(iconBitmap!!),
                                contentDescription = "Space Status Icon",
                                modifier = GlanceModifier.size(48.dp)
                            )
                        } else {
                            // Fallback to Grasshopper if no specific icon
                            Image(
                                provider = ImageProvider(R.drawable.ic_launcher_foreground),
                                contentDescription = "App Icon",
                                modifier = GlanceModifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(16.dp))

                        Column(modifier = GlanceModifier.defaultWeight()) {
                            // Space Name
                            val name = space.space ?: "Unknown"
                            Text(
                                text = name,
                                style = TextStyle(
                                    color = textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            
                            Spacer(modifier = GlanceModifier.height(4.dp))

                            // Status Text
                            Text(
                                text = if (isOpen) "OPEN" else "CLOSED",
                                style = TextStyle(
                                    color = ColorProvider(statusColor),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                     Text(errorMessage ?: "Loading...", style = TextStyle(color = textColor))
                }
            }
        }
    }
}

class UpdateWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        SpaceWidget().update(context, glanceId)
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build())
    }
}

class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repo = SpaceRepository(applicationContext)
        val notificationsEnabled = repo.notificationsEnabled.first()
        val notificationSpaces = repo.notificationSpaces.first()

        // 1. Update Widget Data
        val widgetSpaceUrl = try {
            applicationContext.widgetDataStore.data.map { it[stringPreferencesKey("widget_space_url")] }.first()
        } catch (e: Exception) { null }

        if (widgetSpaceUrl != null) {
            try {
                repo.getSpace(widgetSpaceUrl, forceRefresh = true)
                try {
                    val manager = androidx.glance.appwidget.GlanceAppWidgetManager(applicationContext)
                    val glanceIds = manager.getGlanceIds(SpaceWidget::class.java)
                    glanceIds.forEach { glanceId ->
                        SpaceWidget().update(applicationContext, glanceId)
                    }
                    val minimalIds = manager.getGlanceIds(SpaceWidgetMinimal::class.java)
                    minimalIds.forEach { glanceId ->
                         SpaceWidgetMinimal().update(applicationContext, glanceId)
                    }
                } catch (e: Exception) {}
            } catch (e: Exception) {}
        }
        
        // 2. Check Notifications for ALL monitored spaces
        if (notificationsEnabled && notificationSpaces.isNotEmpty()) {
             notificationSpaces.forEach { spaceUrl ->
                 try {
                     val newSpaceData = repo.getSpace(spaceUrl, forceRefresh = true)
                     
                     val prefs = applicationContext.widgetDataStore 
                     val lastStatusKey = stringPreferencesKey("last_status_$spaceUrl")
                     val lastStatus = prefs.data.map { it[lastStatusKey] }.first()
                     val currentStatus = if (newSpaceData.state?.open == true) "open" else "closed"
                     
                     if (lastStatus != null && lastStatus != currentStatus) {
                         showNotification(applicationContext, newSpaceData.space ?: "Space", currentStatus, spaceUrl.hashCode())
                     }
                     
                     applicationContext.widgetDataStore.edit { it[lastStatusKey] = currentStatus }
                 } catch (e: Exception) {
                     // Continue to next space
                 }
             }
        }

        return Result.success()
    }

    private fun showNotification(context: Context, spaceName: String, status: String, notificationId: Int) {
        val channelId = "space_status_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Space Status Updates"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentTitle("Space Status Update")
            .setContentText("$spaceName is now $status!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {}
        }
    }
}
