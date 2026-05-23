package com.atie.marker.phone.data

import java.time.LocalDate
import java.time.ZoneId

object DailyTimelinePolicy {
    fun openIntervalEndEpochMillis(
        date: LocalDate,
        zoneId: ZoneId,
        nowProvider: () -> Long,
    ): Long? {
        return if (date == LocalDate.now(zoneId)) {
            nowProvider()
        } else {
            null
        }
    }
}
