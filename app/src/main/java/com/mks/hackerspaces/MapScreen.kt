package com.mks.hackerspaces

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.graphics.drawable.toBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb

@Composable
fun MapScreen(viewModel: SpaceViewModel) {
    val context = LocalContext.current
    val spaces by viewModel.spacesWithLocation.collectAsState()
    val mapFilter by viewModel.mapFilter.collectAsState()

    DisposableEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        onDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(5.0)
                    controller.setCenter(GeoPoint(50.0, 10.0)) // Default center Europe
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
                
                spaces.forEach { space ->
                    val lat = space.location?.lat
                    val lon = space.location?.lon
                    
                    if (lat != null && lon != null) {
                        val marker = Marker(mapView)
                        marker.position = GeoPoint(lat, lon)
                        marker.title = space.space ?: "Unknown"
                        
                        // Create a custom marker icon
                        val color = if (space.state?.open == true) Color.GREEN else Color.GRAY
                        val drawable = createCustomMarker(context, color)
                        marker.icon = drawable
                        
                        marker.setOnMarkerClickListener { m, _ ->
                            viewModel.selectSpace(space)
                            true
                        }
                        
                        mapView.overlays.add(marker)
                    }
                }
                
                mapView.invalidate()
            }
        )

        // Filter chips
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = mapFilter == MapFilter.MY_SPACES,
                onClick = { viewModel.setMapFilter(MapFilter.MY_SPACES) },
                label = { Text("My Spaces") },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = mapFilter == MapFilter.ALL_SPACES,
                onClick = { viewModel.setMapFilter(MapFilter.ALL_SPACES) },
                label = { Text("All Spaces") }
            )
        }
    }
}

fun createCustomMarker(context: android.content.Context, color: Int): Drawable {
    val radius = 20f
    val bitmap = Bitmap.createBitmap((radius * 2).toInt(), (radius * 2).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        this.color = color
        style = Paint.Style.FILL
    }
    
    // Draw circle
    canvas.drawCircle(radius, radius, radius, paint)
    
    // Draw border
    paint.style = Paint.Style.STROKE
    paint.color = Color.BLACK
    paint.strokeWidth = 2f
    canvas.drawCircle(radius, radius, radius - 1, paint)
    
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}
