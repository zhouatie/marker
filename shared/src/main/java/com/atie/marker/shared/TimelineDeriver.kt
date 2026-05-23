package com.atie.marker.shared

object TimelineDeriver {
    fun deriveDailyIntervals(
        markers: List<MarkerEvent>,
        labels: List<ActivityIntervalLabel>,
        nowEpochMillis: Long,
    ): List<ActivityInterval> {
        val orderedMarkers = markers
            .distinctBy { it.id }
            .sortedWith(compareBy<MarkerEvent> { it.triggeredAtEpochMillis }.thenBy { it.id })

        if (orderedMarkers.isEmpty()) {
            return emptyList()
        }

        val labelByKey = labels.associateBy { it.key }
        return orderedMarkers.mapIndexed { index, startMarker ->
            val endMarker = orderedMarkers.getOrNull(index + 1)
            val key = IntervalKey(
                startMarkerId = startMarker.id,
                endMarkerId = endMarker?.id,
            )
            ActivityInterval(
                key = key,
                startMarker = startMarker,
                endMarker = endMarker,
                startEpochMillis = startMarker.triggeredAtEpochMillis,
                endEpochMillis = endMarker?.triggeredAtEpochMillis ?: nowEpochMillis,
                label = labelByKey[key]?.label,
            )
        }
    }
}
