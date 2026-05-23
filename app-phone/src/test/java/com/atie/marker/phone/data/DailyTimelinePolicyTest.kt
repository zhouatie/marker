package com.atie.marker.phone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DailyTimelinePolicyTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun todayUsesCurrentTimeAsOpenIntervalEnd() {
        val now = System.currentTimeMillis()

        val end = DailyTimelinePolicy.openIntervalEndEpochMillis(
            date = LocalDate.now(zoneId),
            zoneId = zoneId,
            nowProvider = { now },
        )

        assertEquals(now, end)
    }

    @Test
    fun historicalDateDoesNotUseCurrentTimeAsOpenIntervalEnd() {
        val end = DailyTimelinePolicy.openIntervalEndEpochMillis(
            date = LocalDate.now(zoneId).minusDays(1),
            zoneId = zoneId,
            nowProvider = { 123L },
        )

        assertNull(end)
    }
}
