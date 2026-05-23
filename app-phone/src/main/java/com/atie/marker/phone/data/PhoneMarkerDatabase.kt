package com.atie.marker.phone.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PhoneMarkerEntity::class,
        IntervalLabelEntity::class,
    ],
    version = 1,
)
abstract class PhoneMarkerDatabase : RoomDatabase() {
    abstract fun markerDao(): PhoneMarkerDao

    abstract fun intervalLabelDao(): IntervalLabelDao
}
