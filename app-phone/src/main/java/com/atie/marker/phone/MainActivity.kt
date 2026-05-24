package com.atie.marker.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextOverflow
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
    var selectedFilter by remember { mutableStateOf<TimelineLabelFilter>(TimelineLabelFilter.All) }
    val displayIntervals = remember(intervals) {
        intervals.sortedWith(
            compareByDescending<ActivityInterval> { it.startEpochMillis }
                .thenBy { it.key.startMarkerId },
        )
    }
    val filterOptions = remember(displayIntervals) {
        timelineFilterOptions(displayIntervals)
    }
    val visibleFilterOptions = remember(filterOptions, selectedFilter) {
        visibleFilterOptions(filterOptions, selectedFilter)
    }
    val filteredIntervals = remember(displayIntervals, selectedFilter) {
        filterIntervals(displayIntervals, selectedFilter)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReviewHeader(
            date = date,
            selectedView = selectedView,
            onSelectPreviousDate = onSelectPreviousDate,
            onSelectNextDate = onSelectNextDate,
            onOpenPicker = { showingDatePicker = true },
            onSelectView = { selectedView = it },
        )

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
            TimelineFilterBar(
                filters = visibleFilterOptions,
                selectedFilter = selectedFilter,
                onSelectFilter = { selectedFilter = it },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (filteredIntervals.isEmpty()) {
                    item {
                        FilteredTimelineEmpty(
                            selectedFilter = selectedFilter,
                            onClearFilter = { selectedFilter = TimelineLabelFilter.All },
                        )
                    }
                } else {
                    items(filteredIntervals, key = { it.key.startMarkerId + (it.key.endMarkerId ?: "open") }) { interval ->
                        TimelineIntervalRow(
                            interval = interval,
                            selected = interval.key == highlightedIntervalKey,
                            onClick = { highlightedIntervalKey = interval.key },
                            onEditLabel = { editingInterval = interval },
                            onManageMarkers = { managingInterval = interval },
                        )
                    }
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

private sealed class TimelineLabelFilter {
    data object All : TimelineLabelFilter()
    data object Unlabeled : TimelineLabelFilter()
    data class Label(val value: String) : TimelineLabelFilter()
}

private data class LabelColors(
    val container: Color,
    val content: Color,
    val border: Color,
)

@Composable
private fun ReviewHeader(
    date: LocalDate,
    selectedView: ReviewView,
    onSelectPreviousDate: () -> Unit,
    onSelectNextDate: () -> Unit,
    onOpenPicker: () -> Unit,
    onSelectView: (ReviewView) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (date == LocalDate.now()) "今日时间线" else "回顾时间线",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            DateSelector(
                date = date,
                onPrevious = onSelectPreviousDate,
                onNext = onSelectNextDate,
                onOpenPicker = onOpenPicker,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewViewButton(
                    text = "时间线",
                    selected = selectedView == ReviewView.Timeline,
                    onClick = { onSelectView(ReviewView.Timeline) },
                )
                ReviewViewButton(
                    text = "地图",
                    selected = selectedView == ReviewView.Map,
                    onClick = { onSelectView(ReviewView.Map) },
                )
            }
        }
    }
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
        OutlinedButton(
            modifier = Modifier.heightIn(min = 40.dp),
            onClick = onPrevious,
        ) {
            Text("<")
        }
        Button(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp),
            onClick = onOpenPicker,
        ) {
            Text(formatDateLabel(date), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(
            modifier = Modifier.heightIn(min = 40.dp),
            onClick = onNext,
        ) {
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
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
    )
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
            )
            WatchShortcutGuide()
        }
    }
}

@Composable
private fun FilteredTimelineEmpty(
    selectedFilter: TimelineLabelFilter,
    onClearFilter: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "没有匹配的时间段",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "当前筛选：${filterLabel(selectedFilter)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onClearFilter) {
                Text("查看全部")
            }
        }
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
    val labelColors = labelColorsFor(interval.label)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                LabelPill(
                    text = interval.label?.takeIf { it.isNotBlank() } ?: "未标记",
                    colors = labelColors,
                    onClick = onEditLabel,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = locationContext(interval),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onManageMarkers) {
                    Text("点位")
                }
            }
        }
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        predefinedLabels().forEach { label ->
            val colors = labelColorsFor(label)
            FilterChip(
                selected = currentInput == label,
                onClick = { onSelect(label) },
                label = { Text(label) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.container,
                    selectedLabelColor = colors.content,
                    containerColor = colors.container,
                    labelColor = colors.content,
                ),
                border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = currentInput == label,
                    borderColor = colors.border,
                    selectedBorderColor = colors.border,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineFilterBar(
    filters: List<TimelineLabelFilter>,
    selectedFilter: TimelineLabelFilter,
    onSelectFilter: (TimelineLabelFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "标签筛选",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filters.forEach { filter ->
                val selected = filter == selectedFilter
                val colors = filterColorsFor(filter)
                LabelPill(
                    text = filterLabel(filter),
                    colors = colors,
                    selected = selected,
                    onClick = { onSelectFilter(filter) },
                )
            }
        }
    }
}

@Composable
private fun LabelPill(
    text: String,
    colors: LabelColors,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .widthIn(max = 132.dp)
            .clickable(onClick = onClick),
        color = colors.container,
        contentColor = colors.content,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else colors.border,
        ),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

private fun timelineFilterOptions(intervals: List<ActivityInterval>): List<TimelineLabelFilter> {
    val labels = intervals
        .mapNotNull { it.normalizedLabel() }
        .distinct()
    val predefined = predefinedLabels().filter { it in labels }
    val custom = labels
        .filterNot { it in predefinedLabels() }
        .sorted()
    return buildList {
        add(TimelineLabelFilter.All)
        if (intervals.any { it.normalizedLabel() == null }) {
            add(TimelineLabelFilter.Unlabeled)
        }
        (predefined + custom).forEach { label ->
            add(TimelineLabelFilter.Label(label))
        }
    }
}

private fun visibleFilterOptions(
    filters: List<TimelineLabelFilter>,
    selectedFilter: TimelineLabelFilter,
): List<TimelineLabelFilter> {
    return if (selectedFilter in filters) {
        filters
    } else {
        filters + selectedFilter
    }
}

private fun filterIntervals(
    intervals: List<ActivityInterval>,
    selectedFilter: TimelineLabelFilter,
): List<ActivityInterval> {
    return when (selectedFilter) {
        TimelineLabelFilter.All -> intervals
        TimelineLabelFilter.Unlabeled -> intervals.filter { it.normalizedLabel() == null }
        is TimelineLabelFilter.Label -> intervals.filter { it.normalizedLabel() == selectedFilter.value }
    }
}

private fun filterLabel(filter: TimelineLabelFilter): String {
    return when (filter) {
        TimelineLabelFilter.All -> "全部"
        TimelineLabelFilter.Unlabeled -> "未标记"
        is TimelineLabelFilter.Label -> filter.value
    }
}

private fun ActivityInterval.normalizedLabel(): String? {
    return label?.trim()?.takeIf { it.isNotEmpty() }
}

@Composable
private fun filterColorsFor(filter: TimelineLabelFilter): LabelColors {
    return when (filter) {
        TimelineLabelFilter.All -> LabelColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            border = MaterialTheme.colorScheme.primary,
        )
        TimelineLabelFilter.Unlabeled -> unlabeledColors()
        is TimelineLabelFilter.Label -> labelColorsFor(filter.value)
    }
}

@Composable
private fun labelColorsFor(label: String?): LabelColors {
    val normalized = label?.trim()?.takeIf { it.isNotEmpty() }
    if (normalized == null) {
        return unlabeledColors()
    }
    return fixedLabelColors(normalized)
}

@Composable
private fun unlabeledColors(): LabelColors {
    return LabelColors(
        container = MaterialTheme.colorScheme.surfaceVariant,
        content = MaterialTheme.colorScheme.onSurfaceVariant,
        border = MaterialTheme.colorScheme.outlineVariant,
    )
}

private fun fixedLabelColors(label: String): LabelColors {
    return when (label) {
        "工作" -> LabelColors(
            container = Color(0xFFD7E8FF),
            content = Color(0xFF174A7C),
            border = Color(0xFF7EAFDC),
        )
        "通勤" -> LabelColors(
            container = Color(0xFFCFF4F1),
            content = Color(0xFF085F63),
            border = Color(0xFF65BDB8),
        )
        "带娃" -> LabelColors(
            container = Color(0xFFFFE0E6),
            content = Color(0xFF8A1C3A),
            border = Color(0xFFE28AA0),
        )
        "喂奶" -> LabelColors(
            container = Color(0xFFFFE4D6),
            content = Color(0xFF8A360F),
            border = Color(0xFFE7A27B),
        )
        "换尿布" -> LabelColors(
            container = Color(0xFFE0ECFF),
            content = Color(0xFF244A87),
            border = Color(0xFF8FAFE6),
        )
        "用餐" -> LabelColors(
            container = Color(0xFFFFE7BA),
            content = Color(0xFF7A4700),
            border = Color(0xFFE2AD4A),
        )
        "运动" -> LabelColors(
            container = Color(0xFFDDF6D6),
            content = Color(0xFF246B2A),
            border = Color(0xFF87C97D),
        )
        "休息" -> LabelColors(
            container = Color(0xFFE8E0FF),
            content = Color(0xFF4E357D),
            border = Color(0xFFAA98DE),
        )
        else -> customLabelColors(label)
    }
}

private fun customLabelColors(label: String): LabelColors {
    val palette = listOf(
        LabelColors(Color(0xFFE0F2FE), Color(0xFF075985), Color(0xFF7DD3FC)),
        LabelColors(Color(0xFFECFCCB), Color(0xFF3F6212), Color(0xFFA3E635)),
        LabelColors(Color(0xFFFCE7F3), Color(0xFF9D174D), Color(0xFFF9A8D4)),
        LabelColors(Color(0xFFFEF3C7), Color(0xFF92400E), Color(0xFFFCD34D)),
        LabelColors(Color(0xFFEDE9FE), Color(0xFF5B21B6), Color(0xFFC4B5FD)),
        LabelColors(Color(0xFFD1FAE5), Color(0xFF065F46), Color(0xFF6EE7B7)),
    )
    return palette[Math.floorMod(label.hashCode(), palette.size)]
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
    return listOf("工作", "通勤", "带娃", "喂奶", "换尿布", "用餐", "运动", "休息")
}
