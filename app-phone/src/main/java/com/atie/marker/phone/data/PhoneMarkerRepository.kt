package com.atie.marker.phone.data

import com.atie.marker.shared.ActivityInterval
import com.atie.marker.shared.IntervalKey
import com.atie.marker.shared.MarkerEvent
import com.atie.marker.shared.TimelineDeriver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

class PhoneMarkerRepository(
    private val markerDao: PhoneMarkerDao,
    private val labelDao: IntervalLabelDao,
    private val deletionDao: MarkerDeletionDao,
) {
    fun observeDailyIntervals(
        date: LocalDate,
        zoneId: ZoneId,
        nowProvider: () -> Long = { System.currentTimeMillis() },
    ): Flow<List<ActivityInterval>> {
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return combine(
            markerDao.observeMarkersForRange(start, end),
            labelDao.observeLabels(),
        ) { markerEntities, labelEntities ->
            val openIntervalEnd = DailyTimelinePolicy.openIntervalEndEpochMillis(
                date = date,
                zoneId = zoneId,
                nowProvider = nowProvider,
            )
            TimelineDeriver.deriveDailyIntervals(
                markers = markerEntities.map { it.toDomain() },
                labels = labelEntities.map { it.toDomain() },
                openIntervalEndEpochMillis = openIntervalEnd,
            )
        }
    }

    suspend fun ingestMarker(marker: MarkerEvent) {
        val deletion = deletionDao.getByMarkerId(marker.id)
        if (!MarkerDeletionPolicy.shouldAcceptIncomingMarker(deletion)) {
            return
        }
        val existing = markerDao.getById(marker.id)?.toDomain()
        if (existing != null && existing.updatedAtEpochMillis > marker.updatedAtEpochMillis) {
            return
        }
        markerDao.upsert(marker.toPhoneEntity())
    }

    suspend fun setIntervalLabel(key: IntervalKey, label: String) {
        labelDao.upsert(
            IntervalLabelEntity(
                startMarkerId = key.startMarkerId,
                endMarkerId = key.endMarkerId.orEmpty(),
                label = label.trim(),
            ),
        )
    }

    suspend fun deleteMarker(markerId: String, deletedAtEpochMillis: Long = System.currentTimeMillis()) {
        deletionDao.upsert(
            MarkerDeletionEntity(
                markerId = markerId,
                deletedAtEpochMillis = deletedAtEpochMillis,
            ),
        )
        labelDao.deleteReferencingMarker(markerId)
    }
}
