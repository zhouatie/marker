package com.atie.marker.phone.sync

import com.atie.marker.shared.CaptureStatus
import com.atie.marker.shared.LocationMetadata
import com.atie.marker.shared.MarkerDataLayerContract
import com.atie.marker.shared.MarkerEvent
import com.atie.marker.shared.SourceDevice
import com.atie.marker.shared.SyncState
import com.google.android.gms.wearable.DataMap

fun DataMap.toMarkerEvent(): MarkerEvent {
    val hasLocation = getBoolean(MarkerDataLayerContract.KEY_HAS_LOCATION)
    val location = if (hasLocation) {
        LocationMetadata(
            latitude = getDouble(MarkerDataLayerContract.KEY_LATITUDE),
            longitude = getDouble(MarkerDataLayerContract.KEY_LONGITUDE),
            altitudeMeters = if (containsKey(MarkerDataLayerContract.KEY_ALTITUDE)) {
                getDouble(MarkerDataLayerContract.KEY_ALTITUDE)
            } else {
                null
            },
            horizontalAccuracyMeters = if (containsKey(MarkerDataLayerContract.KEY_HORIZONTAL_ACCURACY)) {
                getFloat(MarkerDataLayerContract.KEY_HORIZONTAL_ACCURACY)
            } else {
                null
            },
            verticalAccuracyMeters = if (containsKey(MarkerDataLayerContract.KEY_VERTICAL_ACCURACY)) {
                getFloat(MarkerDataLayerContract.KEY_VERTICAL_ACCURACY)
            } else {
                null
            },
            provider = getString(MarkerDataLayerContract.KEY_PROVIDER),
        )
    } else {
        null
    }

    return MarkerEvent(
        id = getString(MarkerDataLayerContract.KEY_ID).orEmpty(),
        triggeredAtEpochMillis = getLong(MarkerDataLayerContract.KEY_TRIGGERED_AT),
        location = location,
        captureStatus = CaptureStatus.valueOf(getString(MarkerDataLayerContract.KEY_CAPTURE_STATUS).orEmpty()),
        sourceDevice = SourceDevice.valueOf(getString(MarkerDataLayerContract.KEY_SOURCE_DEVICE).orEmpty()),
        sourceDeviceId = getString(MarkerDataLayerContract.KEY_SOURCE_DEVICE_ID),
        syncState = SyncState.Synced,
        createdAtEpochMillis = getLong(MarkerDataLayerContract.KEY_CREATED_AT),
        updatedAtEpochMillis = getLong(MarkerDataLayerContract.KEY_UPDATED_AT),
    )
}
