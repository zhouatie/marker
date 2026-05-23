package com.atie.marker.watch

import android.app.Application
import androidx.room.Room
import com.atie.marker.watch.data.WatchMarkerDatabase
import com.atie.marker.watch.data.WatchMarkerRepository
import com.atie.marker.watch.sync.MarkerDataClientSyncer

class MarkerWatchApp : Application() {
    val database: WatchMarkerDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            WatchMarkerDatabase::class.java,
            "marker-watch.db",
        ).build()
    }

    val markerRepository: WatchMarkerRepository by lazy {
        WatchMarkerRepository(
            markerDao = database.markerDao(),
            syncer = MarkerDataClientSyncer(applicationContext),
        )
    }
}
