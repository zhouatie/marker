package com.atie.marker.phone.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.atie.marker.phone.data.PhoneMarkerRepository
import com.atie.marker.shared.ActivityInterval
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

class TimelineViewModel(
    private val repository: PhoneMarkerRepository,
) : ViewModel() {
    val selectedDate: LocalDate = LocalDate.now()

    val intervals: StateFlow<List<ActivityInterval>> = repository
        .observeDailyIntervals(
            date = selectedDate,
            zoneId = ZoneId.systemDefault(),
        )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun setIntervalLabel(interval: ActivityInterval, label: String) {
        val normalizedLabel = label.trim()
        if (normalizedLabel.isEmpty()) {
            return
        }
        viewModelScope.launch {
            repository.setIntervalLabel(interval.key, normalizedLabel)
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
