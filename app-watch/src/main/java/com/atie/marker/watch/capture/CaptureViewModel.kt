package com.atie.marker.watch.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atie.marker.shared.CaptureStatus
import com.atie.marker.watch.data.WatchMarkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CaptureUiState {
    data object Idle : CaptureUiState
    data object Accepted : CaptureUiState
    data object WaitingForPermission : CaptureUiState
    data object Completed : CaptureUiState
    data class Failed(val message: String) : CaptureUiState
}

class CaptureViewModel(
    private val repository: WatchMarkerRepository,
    private val locationReader: WatchLocationReader,
    private val haptics: WatchHaptics,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private var started = false
    private var activeMarkerId: String? = null

    fun startIfNeeded(sourceDeviceId: String?) {
        if (started) {
            return
        }
        started = true
        viewModelScope.launch {
            runCatching {
                repository.createPendingMarker(
                    triggeredAtEpochMillis = System.currentTimeMillis(),
                    sourceDeviceId = sourceDeviceId,
                )
            }.onSuccess { marker ->
                activeMarkerId = marker.id
                haptics.accepted()
                _uiState.value = CaptureUiState.Accepted

                repository.syncMarker(marker.id)
                if (locationReader.hasLocationPermission()) {
                    captureLocation(marker.id)
                    completeCaptureSession()
                } else {
                    _uiState.value = CaptureUiState.WaitingForPermission
                }
            }.onFailure { throwable ->
                haptics.failed()
                _uiState.value = CaptureUiState.Failed(throwable.message ?: "记录失败")
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        val markerId = activeMarkerId ?: return
        viewModelScope.launch {
            if (granted) {
                _uiState.value = CaptureUiState.Accepted
                captureLocation(markerId)
            } else {
                repository.markLocationUnavailable(markerId, CaptureStatus.PermissionMissing)
            }
            completeCaptureSession()
        }
    }

    private suspend fun captureLocation(markerId: String) {
        runCatching {
            locationReader.readCurrentLocation()
        }.onSuccess { result ->
            when (result) {
                is WatchLocationResult.Success -> {
                    repository.attachLocation(markerId, result.location)
                }
                is WatchLocationResult.Unavailable -> {
                    repository.markLocationUnavailable(markerId, result.status)
                    _uiState.value = CaptureUiState.Accepted
                }
            }
        }.onFailure {
            repository.markLocationUnavailable(markerId, CaptureStatus.Failed)
            _uiState.value = CaptureUiState.Accepted
        }
    }

    private fun completeCaptureSession() {
        activeMarkerId = null
        started = false
        _uiState.value = CaptureUiState.Completed
    }
}
