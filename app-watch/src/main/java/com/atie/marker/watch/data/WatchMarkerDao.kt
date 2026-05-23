package com.atie.marker.watch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WatchMarkerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(marker: WatchMarkerEntity)

    @Query("SELECT * FROM markers WHERE id = :id")
    suspend fun getById(id: String): WatchMarkerEntity?

    @Query("SELECT * FROM markers WHERE syncState != 'Synced' ORDER BY triggeredAtEpochMillis ASC")
    suspend fun getPendingSyncMarkers(): List<WatchMarkerEntity>

    @Query("UPDATE markers SET syncState = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)

    @Query("UPDATE markers SET syncState = 'Synced' WHERE id = :id AND updatedAtEpochMillis = :acknowledgedUpdatedAt")
    suspend fun markSyncedIfVersion(id: String, acknowledgedUpdatedAt: Long): Int
}
