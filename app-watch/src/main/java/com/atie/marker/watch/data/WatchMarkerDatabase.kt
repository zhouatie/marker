package com.atie.marker.watch.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WatchMarkerEntity::class],
    version = 1,
)
abstract class WatchMarkerDatabase : RoomDatabase() {
    abstract fun markerDao(): WatchMarkerDao
}
