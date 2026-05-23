package com.atie.marker.watch.sync

import com.atie.marker.shared.MarkerEvent

interface MarkerSyncer {
    suspend fun sync(marker: MarkerEvent): Boolean
}
