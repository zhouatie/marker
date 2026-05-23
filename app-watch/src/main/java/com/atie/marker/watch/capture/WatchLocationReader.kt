package com.atie.marker.watch.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.atie.marker.shared.CaptureStatus
import com.atie.marker.shared.LocationMetadata
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed interface WatchLocationResult {
    data class Success(val location: LocationMetadata) : WatchLocationResult
    data class Unavailable(val status: CaptureStatus) : WatchLocationResult
}

class WatchLocationReader(
    private val context: Context,
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(LocationManager::class.java) ?: return false
        return locationManager.isLocationEnabled
    }

    @SuppressLint("MissingPermission")
    suspend fun readCurrentLocation(timeoutMillis: Long = 30_000): WatchLocationResult {
        if (!hasLocationPermission()) {
            return WatchLocationResult.Unavailable(CaptureStatus.PermissionMissing)
        }
        if (!isLocationEnabled()) {
            return WatchLocationResult.Unavailable(CaptureStatus.LocationDisabled)
        }

        val tokenSource = CancellationTokenSource()
        val location = withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { tokenSource.cancel() }
                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            }
        }
        if (location == null) {
            tokenSource.cancel()
            return WatchLocationResult.Unavailable(CaptureStatus.LocationTimedOut)
        }

        return location.toMetadata()
            ?.let { WatchLocationResult.Success(it) }
            ?: WatchLocationResult.Unavailable(CaptureStatus.LocationNoCoordinates)
    }

    private fun Location.toMetadata(): LocationMetadata? {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            return null
        }
        return LocationMetadata(
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = if (hasAltitude()) altitude else null,
            horizontalAccuracyMeters = if (hasAccuracy()) accuracy else null,
            verticalAccuracyMeters = if (hasVerticalAccuracy()) verticalAccuracyMeters else null,
            provider = provider ?: "fused",
        )
    }
}
