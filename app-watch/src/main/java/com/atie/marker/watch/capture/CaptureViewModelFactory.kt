package com.atie.marker.watch.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.atie.marker.watch.data.WatchMarkerRepository

class CaptureViewModelFactory(
    private val repository: WatchMarkerRepository,
    private val locationReader: WatchLocationReader,
    private val haptics: WatchHaptics,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CaptureViewModel(repository, locationReader, haptics) as T
    }
}
