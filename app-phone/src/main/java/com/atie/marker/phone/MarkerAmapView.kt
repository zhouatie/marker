package com.atie.marker.phone

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.atie.marker.shared.ActivityInterval
import com.atie.marker.shared.IntervalKey

@Composable
fun MarkerAmapView(
    intervals: List<ActivityInterval>,
    highlightedIntervalKey: IntervalKey?,
) {
    val context = LocalContext.current
    val markers = remember(intervals) {
        intervals
            .flatMap { listOfNotNull(it.startMarker, it.endMarker) }
            .distinctBy { it.id }
            .filter { it.location != null }
            .sortedBy { it.triggeredAtEpochMillis }
    }
    val highlightedInterval = intervals.firstOrNull { it.key == highlightedIntervalKey }
    val highlightedHasLocation = highlightedInterval?.let { interval ->
        interval.startMarker.location != null || interval.endMarker?.location != null
    } ?: false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "标记地图",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${markers.size} 个带位置的标记点",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            !hasAmapApiKey(context) -> {
                MapUnavailableState("缺少高德地图 API Key，无法加载地图。")
            }

            markers.isEmpty() -> {
                MapUnavailableState("所选日期还没有可展示在地图上的位置标记。")
            }

            else -> {
                AmapCanvas(
                    intervals = intervals,
                    highlightedIntervalKey = highlightedIntervalKey,
                    highlightedInterval = highlightedInterval,
                    markers = markers,
                )
            }
        }

        if (highlightedInterval != null && !highlightedHasLocation) {
            Text(
                text = "当前选中的时间段没有可用于地图高亮的位置。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "这里只连接标记点，不表示真实行走路线或连续 GPS 轨迹。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AmapCanvas(
    intervals: List<ActivityInterval>,
    highlightedIntervalKey: IntervalKey?,
    highlightedInterval: ActivityInterval?,
    markers: List<com.atie.marker.shared.MarkerEvent>,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        MapView(context).apply {
            onCreate(null)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
            ),
        factory = { mapView },
        update = { view ->
            renderMarkersOnMap(
                mapView = view,
                intervals = intervals,
                highlightedIntervalKey = highlightedIntervalKey,
                highlightedInterval = highlightedInterval,
                markers = markers,
            )
        },
    )
}

private fun renderMarkersOnMap(
    mapView: MapView,
    intervals: List<ActivityInterval>,
    highlightedIntervalKey: IntervalKey?,
    highlightedInterval: ActivityInterval?,
    markers: List<com.atie.marker.shared.MarkerEvent>,
) {
    val amap = mapView.map
    amap.clear()

    val latLngByMarkerId = markers.associate { marker ->
        val location = requireNotNull(marker.location)
        marker.id to LatLng(location.latitude, location.longitude)
    }
    val highlightedMarkerIds = setOfNotNull(
        highlightedInterval?.startMarker?.id,
        highlightedInterval?.endMarker?.id,
    )

    markers.forEach { marker ->
        val latLng = latLngByMarkerId.getValue(marker.id)
        val highlighted = marker.id in highlightedMarkerIds
        amap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(markerTimeTitle(marker.triggeredAtEpochMillis))
                .icon(
                    BitmapDescriptorFactory.defaultMarker(
                        if (highlighted) {
                            BitmapDescriptorFactory.HUE_RED
                        } else {
                            BitmapDescriptorFactory.HUE_AZURE
                        },
                    ),
                ),
        )
    }

    intervals.sortedBy { it.startEpochMillis }.forEach { interval ->
        val start = latLngByMarkerId[interval.startMarker.id] ?: return@forEach
        val end = interval.endMarker?.let { latLngByMarkerId[it.id] } ?: return@forEach
        val highlighted = interval.key == highlightedIntervalKey
        amap.addPolyline(
            PolylineOptions()
                .add(start, end)
                .width(if (highlighted) 14f else 8f)
                .color(if (highlighted) 0xFFE03E2F.toInt() else 0xFF3267D6.toInt()),
        )
    }

    if (latLngByMarkerId.size == 1) {
        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngByMarkerId.values.first(), 16f))
    } else {
        val boundsBuilder = LatLngBounds.builder()
        latLngByMarkerId.values.forEach { boundsBuilder.include(it) }
        amap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80))
    }
}

@Composable
private fun MapUnavailableState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun hasAmapApiKey(context: Context): Boolean {
    val appInfo = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA,
    )
    return appInfo.metaData?.getString("com.amap.api.v2.apikey").orEmpty().isNotBlank()
}

private fun markerTimeTitle(epochMillis: Long): String {
    return java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
        .format(java.time.Instant.ofEpochMilli(epochMillis))
}
