package com.atie.marker.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
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
                    val selectedDate by viewModel.selectedDate.collectAsState()
                    TimelineScreen(
                        date = selectedDate,
                        intervals = intervals,
                        onSelectPreviousDate = viewModel::selectPreviousDate,
                        onSelectNextDate = viewModel::selectNextDate,
                        onSelectDate = viewModel::selectDate,
                        onSetIntervalLabel = viewModel::setIntervalLabel,
                        onDeleteMarker = viewModel::deleteMarker,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineScreen(
    date: LocalDate,
    intervals: List<ActivityInterval>,
    onSelectPreviousDate: () -> Unit,
    onSelectNextDate: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSetIntervalLabel: (ActivityInterval, String) -> Unit,
    onDeleteMarker: (MarkerEvent) -> Unit,
) {
    var selectedView by remember { mutableStateOf(ReviewView.Timeline) }
    var highlightedIntervalKey by remember { mutableStateOf<IntervalKey?>(null) }
    var editingInterval by remember { mutableStateOf<ActivityInterval?>(null) }
    var managingInterval by remember { mutableStateOf<ActivityInterval?>(null) }
    var pendingDeleteMarker by remember { mutableStateOf<MarkerEvent?>(null) }
    var showingDatePicker by remember { mutableStateOf(false) }
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
            text = if (date == LocalDate.now()) "今日时间线" else "回顾时间线",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        DateSelector(
            date = date,
            onPrevious = onSelectPreviousDate,
            onNext = onSelectNextDate,
            onOpenPicker = { showingDatePicker = true },
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
                        onManageMarkers = { managingInterval = interval },
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

    val activeManagingInterval = managingInterval
    if (activeManagingInterval != null) {
        MarkerManagementDialog(
            interval = activeManagingInterval,
            onDismiss = { managingInterval = null },
            onRequestDelete = { marker -> pendingDeleteMarker = marker },
        )
    }

    val activeDeleteMarker = pendingDeleteMarker
    if (activeDeleteMarker != null) {
        ConfirmDeleteMarkerDialog(
            marker = activeDeleteMarker,
            onDismiss = { pendingDeleteMarker = null },
            onConfirm = {
                onDeleteMarker(activeDeleteMarker)
                pendingDeleteMarker = null
                managingInterval = null
            },
        )
    }

    if (showingDatePicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showingDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = pickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            onSelectDate(
                                Instant.ofEpochMilli(selectedMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate(),
                            )
                        }
                        showingDatePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showingDatePicker = false }) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private enum class ReviewView {
    Timeline,
    Map,
}

@Composable
private fun DateSelector(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPicker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onPrevious) {
            Text("<")
        }
        Button(
            modifier = Modifier.weight(1f),
            onClick = onOpenPicker,
        ) {
            Text(formatDateLabel(date))
        }
        OutlinedButton(onClick = onNext) {
            Text(">")
        }
    }
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
    onManageMarkers: () -> Unit,
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
                    text = intervalStatus(interval),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(
                onClick = onEditLabel,
                label = { Text(interval.label ?: "未标记") },
            )
            TextButton(onClick = onManageMarkers) {
                Text("点位")
            }
        }
        Text(
            text = locationContext(interval),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MarkerManagementDialog(
    interval: ActivityInterval,
    onDismiss: () -> Unit,
    onRequestDelete: (MarkerEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("管理标记点")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MarkerPointRow(
                    title = "开始点",
                    marker = interval.startMarker,
                    onDelete = { onRequestDelete(interval.startMarker) },
                )
                val endMarker = interval.endMarker
                if (endMarker != null) {
                    MarkerPointRow(
                        title = "结束点",
                        marker = endMarker,
                        onDelete = { onRequestDelete(endMarker) },
                    )
                } else {
                    Text(
                        text = "结束点：暂无",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        },
    )
}

@Composable
private fun MarkerPointRow(
    title: String,
    marker: MarkerEvent,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = markerSummary(marker),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onDelete) {
            Text("删除")
        }
    }
}

@Composable
private fun ConfirmDeleteMarkerDialog(
    marker: MarkerEvent,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("删除标记点？")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(markerSummary(marker))
                Text("删除后会重新计算相邻时间段，引用该点位的时间段标签不会迁移。")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
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
    MarkerAmapView(
        intervals = intervals,
        highlightedIntervalKey = highlightedIntervalKey,
    )
}

private fun intervalTimeRange(interval: ActivityInterval): String {
    val start = formatTime(interval.startEpochMillis)
    val end = when {
        interval.endMarker != null -> formatTime(interval.endEpochMillis ?: interval.startEpochMillis)
        interval.endEpochMillis != null -> "现在"
        else -> "未闭合"
    }
    return "$start - $end"
}

private fun intervalStatus(interval: ActivityInterval): String {
    return when {
        interval.endMarker != null -> "已结束"
        interval.endEpochMillis != null -> "当前进行中"
        else -> "未闭合"
    }
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

private fun formatDateLabel(date: LocalDate): String {
    return if (date == LocalDate.now()) {
        "今天 ${date}"
    } else {
        date.toString()
    }
}

private fun markerSummary(marker: MarkerEvent): String {
    val time = formatTime(marker.triggeredAtEpochMillis)
    val location = marker.location
    return if (location != null) {
        "$time  ${formatCoordinate(location.latitude, location.longitude)}"
    } else {
        "$time  暂无位置"
    }
}

private fun formatCoordinate(latitude: Double, longitude: Double): String {
    return "%.5f, %.5f".format(latitude, longitude)
}

private fun predefinedLabels(): List<String> {
    return listOf("工作", "通勤", "带娃", "用餐", "运动", "休息")
}
