package com.atie.marker.phone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.atie.marker.phone.data.PhoneMarkerRepository
import com.atie.marker.shared.ActivityInterval
import com.atie.marker.shared.MarkerEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val repository: PhoneMarkerRepository,
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val intervals: StateFlow<List<ActivityInterval>> = selectedDate
        .flatMapLatest { date ->
            repository.observeDailyIntervals(
                date = date,
                zoneId = ZoneId.systemDefault(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun selectPreviousDate() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun selectNextDate() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun setIntervalLabel(interval: ActivityInterval, label: String) {
        val normalizedLabel = label.trim()
        if (normalizedLabel.isEmpty()) {
            return
        }
        viewModelScope.launch {
            repository.setIntervalLabel(interval.key, normalizedLabel)
        }
    }

    fun deleteMarker(marker: MarkerEvent) {
        viewModelScope.launch {
            repository.deleteMarker(marker.id)
        }
    }
}

class TimelineViewModelFactory(
    private val repository: PhoneMarkerRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TimelineViewModel(repository) as T
    }
}
