package com.atie.marker.shared

enum class CaptureStatus {
    Pending,
    Located,
    Unavailable,
    Failed,
    PermissionMissing,
    LocationDisabled,
    LocationTimedOut,
    LocationNoCoordinates,
}

enum class SourceDevice {
    Watch,
    Phone,
}

enum class SyncState {
    Unsynced,
    Syncing,
    Synced,
}

data class LocationMetadata(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Float?,
    val verticalAccuracyMeters: Float?,
    val provider: String?,
)

data class MarkerEvent(
    val id: String,
    val triggeredAtEpochMillis: Long,
    val location: LocationMetadata?,
    val captureStatus: CaptureStatus,
    val sourceDevice: SourceDevice,
    val sourceDeviceId: String?,
    val syncState: SyncState,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class MarkerSyncPayload(
    val marker: MarkerEvent,
)

data class IntervalKey(
    val startMarkerId: String,
    val endMarkerId: String?,
)

data class ActivityIntervalLabel(
    val key: IntervalKey,
    val label: String,
)

data class ActivityInterval(
    val key: IntervalKey,
    val startMarker: MarkerEvent,
    val endMarker: MarkerEvent?,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
    val label: String?,
)
