package com.atie.marker.phone.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.atie.marker.shared.ActivityIntervalLabel
import com.atie.marker.shared.CaptureStatus
import com.atie.marker.shared.IntervalKey
import com.atie.marker.shared.LocationMetadata
import com.atie.marker.shared.MarkerEvent
import com.atie.marker.shared.SourceDevice
import com.atie.marker.shared.SyncState

@Entity(tableName = "markers")
data class PhoneMarkerEntity(
    @PrimaryKey val id: String,
    val triggeredAtEpochMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Float?,
    val verticalAccuracyMeters: Float?,
    val provider: String?,
    val captureStatus: String,
    val sourceDevice: String,
    val sourceDeviceId: String?,
    val syncState: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    fun toDomain(): MarkerEvent {
        val location = if (latitude != null && longitude != null) {
            LocationMetadata(
                latitude = latitude,
                longitude = longitude,
                altitudeMeters = altitudeMeters,
                horizontalAccuracyMeters = horizontalAccuracyMeters,
                verticalAccuracyMeters = verticalAccuracyMeters,
                provider = provider,
            )
        } else {
            null
        }
        return MarkerEvent(
            id = id,
            triggeredAtEpochMillis = triggeredAtEpochMillis,
            location = location,
            captureStatus = CaptureStatus.valueOf(captureStatus),
            sourceDevice = SourceDevice.valueOf(sourceDevice),
            sourceDeviceId = sourceDeviceId,
            syncState = SyncState.valueOf(syncState),
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
    }
}

@Entity(tableName = "interval_labels", primaryKeys = ["startMarkerId", "endMarkerId"])
data class IntervalLabelEntity(
    val startMarkerId: String,
    val endMarkerId: String,
    val label: String,
) {
    fun toDomain(): ActivityIntervalLabel = ActivityIntervalLabel(
        key = IntervalKey(
            startMarkerId = startMarkerId,
            endMarkerId = endMarkerId.ifBlank { null },
        ),
        label = label,
    )
}

@Entity(tableName = "marker_deletions")
data class MarkerDeletionEntity(
    @PrimaryKey val markerId: String,
    val deletedAtEpochMillis: Long,
)

fun MarkerEvent.toPhoneEntity(): PhoneMarkerEntity = PhoneMarkerEntity(
    id = id,
    triggeredAtEpochMillis = triggeredAtEpochMillis,
    latitude = location?.latitude,
    longitude = location?.longitude,
    altitudeMeters = location?.altitudeMeters,
    horizontalAccuracyMeters = location?.horizontalAccuracyMeters,
    verticalAccuracyMeters = location?.verticalAccuracyMeters,
    provider = location?.provider,
    captureStatus = captureStatus.name,
    sourceDevice = sourceDevice.name,
    sourceDeviceId = sourceDeviceId,
    syncState = syncState.name,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
