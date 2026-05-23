package com.atie.marker.watch.sync

import com.atie.marker.shared.CaptureStatus
import com.atie.marker.shared.MarkerDataLayerContract
import com.atie.marker.shared.MarkerEvent
import com.atie.marker.shared.SourceDevice
import com.atie.marker.shared.SyncState
import com.google.android.gms.wearable.DataMap

fun MarkerEvent.toDataMap(): DataMap = DataMap().apply {
    putString(MarkerDataLayerContract.KEY_ID, id)
    putLong(MarkerDataLayerContract.KEY_TRIGGERED_AT, triggeredAtEpochMillis)
    putString(MarkerDataLayerContract.KEY_CAPTURE_STATUS, captureStatus.name)
    putString(MarkerDataLayerContract.KEY_SOURCE_DEVICE, sourceDevice.name)
    sourceDeviceId?.let { putString(MarkerDataLayerContract.KEY_SOURCE_DEVICE_ID, it) }
    putLong(MarkerDataLayerContract.KEY_CREATED_AT, createdAtEpochMillis)
    putLong(MarkerDataLayerContract.KEY_UPDATED_AT, updatedAtEpochMillis)

    val currentLocation = location
    putBoolean(MarkerDataLayerContract.KEY_HAS_LOCATION, currentLocation != null)
    if (currentLocation != null) {
        putDouble(MarkerDataLayerContract.KEY_LATITUDE, currentLocation.latitude)
        putDouble(MarkerDataLayerContract.KEY_LONGITUDE, currentLocation.longitude)
        currentLocation.altitudeMeters?.let { putDouble(MarkerDataLayerContract.KEY_ALTITUDE, it) }
        currentLocation.horizontalAccuracyMeters?.let { putFloat(MarkerDataLayerContract.KEY_HORIZONTAL_ACCURACY, it) }
        currentLocation.verticalAccuracyMeters?.let { putFloat(MarkerDataLayerContract.KEY_VERTICAL_ACCURACY, it) }
        currentLocation.provider?.let { putString(MarkerDataLayerContract.KEY_PROVIDER, it) }
    }
}
