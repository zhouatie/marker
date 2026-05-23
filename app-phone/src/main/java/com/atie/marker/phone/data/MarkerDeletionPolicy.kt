package com.atie.marker.phone.data

import com.atie.marker.shared.ActivityIntervalLabel

object MarkerDeletionPolicy {
    fun shouldAcceptIncomingMarker(deletion: MarkerDeletionEntity?): Boolean {
        return deletion == null
    }

    fun filterDeletedMarkers(
        markers: List<PhoneMarkerEntity>,
        deletions: List<MarkerDeletionEntity>,
    ): List<PhoneMarkerEntity> {
        val deletedIds = deletions.mapTo(mutableSetOf()) { it.markerId }
        return markers.filterNot { it.id in deletedIds }
    }

    fun filterLabelsReferencingDeletedMarker(
        labels: List<ActivityIntervalLabel>,
        deletedMarkerId: String,
    ): List<ActivityIntervalLabel> {
        return labels.filterNot { label ->
            label.key.startMarkerId == deletedMarkerId || label.key.endMarkerId == deletedMarkerId
        }
    }
}
