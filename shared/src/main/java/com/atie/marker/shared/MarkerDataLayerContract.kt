package com.atie.marker.shared

object MarkerDataLayerContract {
    const val MARKER_PATH_PREFIX = "/markers/"
    const val ACK_PATH = "/markers/ack"

    const val KEY_ID = "id"
    const val KEY_TRIGGERED_AT = "triggeredAtEpochMillis"
    const val KEY_CAPTURE_STATUS = "captureStatus"
    const val KEY_SOURCE_DEVICE = "sourceDevice"
    const val KEY_SOURCE_DEVICE_ID = "sourceDeviceId"
    const val KEY_CREATED_AT = "createdAtEpochMillis"
    const val KEY_UPDATED_AT = "updatedAtEpochMillis"

    const val KEY_HAS_LOCATION = "hasLocation"
    const val KEY_LATITUDE = "latitude"
    const val KEY_LONGITUDE = "longitude"
    const val KEY_ALTITUDE = "altitudeMeters"
    const val KEY_HORIZONTAL_ACCURACY = "horizontalAccuracyMeters"
    const val KEY_VERTICAL_ACCURACY = "verticalAccuracyMeters"
    const val KEY_PROVIDER = "provider"

    fun markerPath(markerId: String): String = "$MARKER_PATH_PREFIX$markerId"
}
