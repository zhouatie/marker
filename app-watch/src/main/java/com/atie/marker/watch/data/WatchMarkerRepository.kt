package com.atie.marker.watch.data

import com.atie.marker.shared.CaptureStatus
import com.atie.marker.shared.LocationMetadata
import com.atie.marker.shared.MarkerEvent
import com.atie.marker.shared.SourceDevice
import com.atie.marker.shared.SyncState
import com.atie.marker.watch.sync.MarkerSyncer
import java.util.UUID

class WatchMarkerRepository(
    private val markerDao: WatchMarkerDao,
    private val syncer: MarkerSyncer,
) {
    suspend fun createPendingMarker(
        triggeredAtEpochMillis: Long,
        sourceDeviceId: String?,
    ): MarkerEvent {
        val now = System.currentTimeMillis()
        val marker = MarkerEvent(
            id = UUID.randomUUID().toString(),
            triggeredAtEpochMillis = triggeredAtEpochMillis,
            location = null,
            captureStatus = CaptureStatus.Pending,
            sourceDevice = SourceDevice.Watch,
            sourceDeviceId = sourceDeviceId,
            syncState = SyncState.Unsynced,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        markerDao.upsert(marker.toWatchEntity())
        return marker
    }

    suspend fun attachLocation(markerId: String, location: LocationMetadata?) {
        val existing = markerDao.getById(markerId)?.toDomain() ?: return
        val updated = existing.copy(
            location = location,
            captureStatus = if (location == null) CaptureStatus.Unavailable else CaptureStatus.Located,
            syncState = SyncState.Unsynced,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        markerDao.upsert(updated.toWatchEntity())
        syncMarkerEvent(updated)
    }

    suspend fun markLocationUnavailable(markerId: String, status: CaptureStatus) {
        val existing = markerDao.getById(markerId)?.toDomain() ?: return
        val updated = existing.copy(
            captureStatus = status,
            syncState = SyncState.Unsynced,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        markerDao.upsert(updated.toWatchEntity())
        syncMarkerEvent(updated)
    }

    suspend fun markCaptureFailed(markerId: String) {
        val existing = markerDao.getById(markerId)?.toDomain() ?: return
        val updated = existing.copy(
            captureStatus = CaptureStatus.Failed,
            syncState = SyncState.Unsynced,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        markerDao.upsert(updated.toWatchEntity())
        syncMarkerEvent(updated)
    }

    suspend fun syncPendingMarkers() {
        markerDao.getPendingSyncMarkers()
            .map { it.toDomain() }
            .forEach { syncMarkerEvent(it) }
    }

    suspend fun syncMarker(markerId: String) {
        val marker = markerDao.getById(markerId)?.toDomain() ?: return
        syncMarkerEvent(marker)
    }

    suspend fun markSynced(markerId: String, acknowledgedUpdatedAt: Long) {
        markerDao.markSyncedIfVersion(markerId, acknowledgedUpdatedAt)
    }

    private suspend fun syncMarkerEvent(marker: MarkerEvent) {
        markerDao.updateSyncState(marker.id, SyncState.Syncing.name)
        val delivered = syncer.sync(marker)
        if (!delivered) {
            markerDao.updateSyncState(marker.id, SyncState.Unsynced.name)
        }
    }
}
