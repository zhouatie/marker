package com.atie.marker.phone.data

import com.atie.marker.shared.CaptureStatus
import com.atie.marker.shared.MarkerEvent
import com.atie.marker.shared.SourceDevice
import com.atie.marker.shared.SyncState
import com.atie.marker.shared.TimelineDeriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineDeriverOpenIntervalTest {
    @Test
    fun derivesOpenIntervalEndForCurrentDateInput() {
        val intervals = TimelineDeriver.deriveDailyIntervals(
            markers = listOf(marker("marker-1", 100L)),
            labels = emptyList(),
            openIntervalEndEpochMillis = 500L,
        )

        assertEquals(500L, intervals.single().endEpochMillis)
    }

    @Test
    fun leavesHistoricalOpenIntervalWithoutEndTime() {
        val intervals = TimelineDeriver.deriveDailyIntervals(
            markers = listOf(marker("marker-1", 100L)),
            labels = emptyList(),
            openIntervalEndEpochMillis = null,
        )

        assertNull(intervals.single().endEpochMillis)
    }

    private fun marker(id: String, triggeredAt: Long): MarkerEvent {
        return MarkerEvent(
            id = id,
            triggeredAtEpochMillis = triggeredAt,
            location = null,
            captureStatus = CaptureStatus.Located,
            sourceDevice = SourceDevice.Watch,
            sourceDeviceId = "watch-1",
            syncState = SyncState.Synced,
            createdAtEpochMillis = triggeredAt,
            updatedAtEpochMillis = triggeredAt,
        )
    }
}
