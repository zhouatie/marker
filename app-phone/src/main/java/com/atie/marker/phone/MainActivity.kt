package com.atie.marker.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atie.marker.phone.ui.TimelineViewModel
import com.atie.marker.phone.ui.TimelineViewModelFactory
import com.atie.marker.shared.ActivityInterval
import com.atie.marker.shared.IntervalKey
import com.atie.marker.shared.MarkerEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel: TimelineViewModel by viewModels {
        val app = application as MarkerPhoneApp
        TimelineViewModelFactory(app.markerRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val intervals by viewModel.intervals.collectAsState()
                    TimelineScreen(
                        date = viewModel.selectedDate,
                        intervals = intervals,
                        onSetIntervalLabel = viewModel::setIntervalLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineScreen(
    date: LocalDate,
    intervals: List<ActivityInterval>,
    onSetIntervalLabel: (ActivityInterval, String) -> Unit,
) {
    var selectedView by remember { mutableStateOf(ReviewView.Timeline) }
    var highlightedIntervalKey by remember { mutableStateOf<IntervalKey?>(null) }
    var editingInterval by remember { mutableStateOf<ActivityInterval?>(null) }
    val displayIntervals = remember(intervals) {
        intervals.sortedWith(
            compareByDescending<ActivityInterval> { it.startEpochMillis }
                .thenBy { it.key.startMarkerId },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = "今日时间线",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = date.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReviewViewButton(
                text = "时间线",
                selected = selectedView == ReviewView.Timeline,
                onClick = { selectedView = ReviewView.Timeline },
            )
            ReviewViewButton(
                text = "地图",
                selected = selectedView == ReviewView.Map,
                onClick = { selectedView = ReviewView.Map },
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        if (displayIntervals.isEmpty()) {
            EmptyTimeline()
        } else if (selectedView == ReviewView.Map) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    MarkerConnectionMap(
                        intervals = intervals,
                        highlightedIntervalKey = highlightedIntervalKey,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(displayIntervals, key = { it.key.startMarkerId + (it.key.endMarkerId ?: "open") }) { interval ->
                    TimelineIntervalRow(
                        interval = interval,
                        selected = interval.key == highlightedIntervalKey,
                        onClick = { highlightedIntervalKey = interval.key },
                        onEditLabel = { editingInterval = interval },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val activeEditingInterval = editingInterval
    if (activeEditingInterval != null) {
        IntervalLabelDialog(
            interval = activeEditingInterval,
            onDismiss = { editingInterval = null },
            onSave = { label ->
                onSetIntervalLabel(activeEditingInterval, label)
                editingInterval = null
            },
        )
    }
}

private enum class ReviewView {
    Timeline,
    Map,
}

@Composable
private fun ReviewViewButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(text)
        }
    }
}

@Composable
private fun WatchShortcutGuide() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "手表快捷入口",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "在 Galaxy Watch7 设置中，将 Home 键双击配置为打开 Marker 手表应用。双击后手表会立即记录时间，并用震动反馈定位结果。",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun EmptyTimeline() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "还没有时间段",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "双击手表 Home 键开始记录当前时间边界。至少两个标记会形成一个可回顾的时间段。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WatchShortcutGuide()
    }
}

@Composable
private fun TimelineIntervalRow(
    interval: ActivityInterval,
    selected: Boolean,
    onClick: () -> Unit,
    onEditLabel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = intervalTimeRange(interval),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (interval.endMarker == null) "当前进行中" else "已结束",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = onEditLabel,
                label = { Text(interval.label ?: "未标记") },
            )
        }
        Text(
            text = locationContext(interval),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IntervalLabelDialog(
    interval: ActivityInterval,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var customLabel by remember(interval.key) { mutableStateOf(interval.label.orEmpty()) }
    val currentInput = customLabel.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "设置时间段标签")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = intervalTimeRange(interval),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PredefinedLabelChips(
                    currentInput = currentInput,
                    onSelect = { customLabel = it },
                )
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("自定义标签") },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = currentInput.isNotEmpty(),
                onClick = { onSave(currentInput) },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PredefinedLabelChips(
    currentInput: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        predefinedLabels().forEach { label ->
            FilterChip(
                selected = currentInput == label,
                onClick = { onSelect(label) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun MarkerConnectionMap(
    intervals: List<ActivityInterval>,
    highlightedIntervalKey: IntervalKey?,
) {
    val markers = intervals
        .flatMap { listOfNotNull(it.startMarker, it.endMarker) }
        .distinctBy { it.id }
        .filter { it.location != null }
        .sortedBy { it.triggeredAtEpochMillis }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (markers.isEmpty()) {
                Text(
                    text = "今天还没有可展示在地图上的位置标记。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val latitudes = markers.mapNotNull { it.location?.latitude }
                    val longitudes = markers.mapNotNull { it.location?.longitude }
                    val minLat = latitudes.minOrNull() ?: return@Canvas
                    val maxLat = latitudes.maxOrNull() ?: return@Canvas
                    val minLng = longitudes.minOrNull() ?: return@Canvas
                    val maxLng = longitudes.maxOrNull() ?: return@Canvas
                    val inset = 18.dp.toPx()
                    val drawWidth = (size.width - inset * 2).coerceAtLeast(1f)
                    val drawHeight = (size.height - inset * 2).coerceAtLeast(1f)
                    val points = markers.mapNotNull { marker ->
                        val location = marker.location ?: return@mapNotNull null
                        val xRatio = if (maxLng == minLng) 0.5f else {
                            ((location.longitude - minLng) / (maxLng - minLng)).toFloat()
                        }
                        val yRatio = if (maxLat == minLat) 0.5f else {
                            ((maxLat - location.latitude) / (maxLat - minLat)).toFloat()
                        }
                        Offset(x = inset + xRatio * drawWidth, y = inset + yRatio * drawHeight)
                    }
                    val pointByMarkerId = markers.zip(points).associate { (marker, point) -> marker.id to point }
                    intervals.sortedBy { it.startEpochMillis }.forEach { interval ->
                        val start = pointByMarkerId[interval.startMarker.id]
                        val end = interval.endMarker?.let { pointByMarkerId[it.id] }
                        if (start == null || end == null) {
                            return@forEach
                        }
                        val highlighted = interval.key == highlightedIntervalKey
                        drawLine(
                            color = if (highlighted) Color(0xFFE03E2F) else Color(0xFF3267D6),
                            start = start,
                            end = end,
                            strokeWidth = if (highlighted) 7.dp.toPx() else 4.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                    points.forEach { point ->
                        drawCircle(
                            color = Color.White,
                            radius = 7.dp.toPx(),
                            center = point,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                        drawCircle(
                            color = Color(0xFF3267D6),
                            radius = 4.dp.toPx(),
                            center = point,
                        )
                    }
                    if (highlightedHasLocation) {
                        listOfNotNull(
                            highlightedInterval?.startMarker?.let { pointByMarkerId[it.id] },
                            highlightedInterval?.endMarker?.let { pointByMarkerId[it.id] },
                        ).forEach { point ->
                            drawCircle(
                                color = Color(0xFFE03E2F),
                                radius = 10.dp.toPx(),
                                center = point,
                                style = Stroke(width = 3.dp.toPx()),
                            )
                        }
                    }
                }
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

private fun intervalTimeRange(interval: ActivityInterval): String {
    val start = formatTime(interval.startEpochMillis)
    val end = if (interval.endMarker == null) "现在" else formatTime(interval.endEpochMillis ?: interval.startEpochMillis)
    return "$start - $end"
}

private fun locationContext(interval: ActivityInterval): String {
    val start = interval.startMarker.location
    val end = interval.endMarker?.location
    return when {
        start != null && end != null -> {
            "位置：${formatCoordinate(start.latitude, start.longitude)} -> ${formatCoordinate(end.latitude, end.longitude)}"
        }
        start != null -> "开始位置：${formatCoordinate(start.latitude, start.longitude)}"
        else -> "暂无位置上下文"
    }
}

private fun formatTime(epochMillis: Long): String {
    return DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))
}

private fun formatCoordinate(latitude: Double, longitude: Double): String {
    return "%.5f, %.5f".format(latitude, longitude)
}

private fun predefinedLabels(): List<String> {
    return listOf("工作", "通勤", "带娃", "用餐", "运动", "休息")
}
