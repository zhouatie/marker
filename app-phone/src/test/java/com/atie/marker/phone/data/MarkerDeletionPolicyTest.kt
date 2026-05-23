package com.atie.marker.phone.data

import com.atie.marker.shared.ActivityIntervalLabel
import com.atie.marker.shared.CaptureStatus
import com.atie.marker.shared.IntervalKey
import com.atie.marker.shared.SourceDevice
import com.atie.marker.shared.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerDeletionPolicyTest {
    @Test
    fun acceptsIncomingMarkerWhenNoDeletionExists() {
        assertTrue(MarkerDeletionPolicy.shouldAcceptIncomingMarker(null))
    }

    @Test
    fun rejectsIncomingMarkerWhenDeletionExists() {
        val deletion = MarkerDeletionEntity(
            markerId = "marker-1",
            deletedAtEpochMillis = 200L,
        )

        assertFalse(MarkerDeletionPolicy.shouldAcceptIncomingMarker(deletion))
    }

    @Test
    fun filtersDeletedMarkersFromDisplayInput() {
        val markers = listOf(
            marker("marker-1"),
            marker("marker-2"),
        )
        val deletions = listOf(
            MarkerDeletionEntity(markerId = "marker-1", deletedAtEpochMillis = 300L),
        )

        val activeMarkers = MarkerDeletionPolicy.filterDeletedMarkers(markers, deletions)

        assertEquals(listOf("marker-2"), activeMarkers.map { it.id })
    }

    @Test
    fun removesLabelsReferencingDeletedMarkerWithoutMigratingThem() {
        val labels = listOf(
            ActivityIntervalLabel(IntervalKey("marker-1", "marker-2"), "工作"),
            ActivityIntervalLabel(IntervalKey("marker-2", "marker-3"), "通勤"),
            ActivityIntervalLabel(IntervalKey("marker-4", null), "休息"),
        )

        val remaining = MarkerDeletionPolicy.filterLabelsReferencingDeletedMarker(
            labels = labels,
            deletedMarkerId = "marker-2",
        )

        assertEquals(listOf("休息"), remaining.map { it.label })
    }

    private fun marker(id: String): PhoneMarkerEntity {
        return PhoneMarkerEntity(
            id = id,
            triggeredAtEpochMillis = 100L,
            latitude = null,
            longitude = null,
            altitudeMeters = null,
            horizontalAccuracyMeters = null,
            verticalAccuracyMeters = null,
            provider = null,
            captureStatus = CaptureStatus.Located.name,
            sourceDevice = SourceDevice.Watch.name,
            sourceDeviceId = "watch-1",
            syncState = SyncState.Synced.name,
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 100L,
        )
    }
}
