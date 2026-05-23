package com.atie.marker.phone.data

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PhoneMarkerEntity::class,
        IntervalLabelEntity::class,
        MarkerDeletionEntity::class,
    ],
    version = 2,
)
abstract class PhoneMarkerDatabase : RoomDatabase() {
    abstract fun markerDao(): PhoneMarkerDao

    abstract fun intervalLabelDao(): IntervalLabelDao

    abstract fun markerDeletionDao(): MarkerDeletionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `marker_deletions` (
                        `markerId` TEXT NOT NULL,
                        `deletedAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`markerId`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
