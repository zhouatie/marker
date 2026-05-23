package com.atie.marker.phone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneMarkerDao {
    @Query("SELECT * FROM markers ORDER BY triggeredAtEpochMillis ASC, id ASC")
    fun observeAllMarkers(): Flow<List<PhoneMarkerEntity>>

    @Query(
        """
        SELECT * FROM markers
        WHERE id NOT IN (SELECT markerId FROM marker_deletions)
        ORDER BY triggeredAtEpochMillis ASC, id ASC
        """,
    )
    fun observeActiveMarkers(): Flow<List<PhoneMarkerEntity>>

    @Query(
        """
        SELECT * FROM markers
        WHERE triggeredAtEpochMillis >= :startInclusive
          AND triggeredAtEpochMillis < :endExclusive
          AND id NOT IN (SELECT markerId FROM marker_deletions)
        ORDER BY triggeredAtEpochMillis ASC, id ASC
        """,
    )
    fun observeMarkersForRange(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<PhoneMarkerEntity>>

    @Query("SELECT * FROM markers WHERE id = :id")
    suspend fun getById(id: String): PhoneMarkerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(marker: PhoneMarkerEntity)
}

@Dao
interface IntervalLabelDao {
    @Query("SELECT * FROM interval_labels")
    fun observeLabels(): Flow<List<IntervalLabelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(label: IntervalLabelEntity)

    @Query(
        """
        DELETE FROM interval_labels
        WHERE startMarkerId = :markerId OR endMarkerId = :markerId
        """,
    )
    suspend fun deleteReferencingMarker(markerId: String)
}

@Dao
interface MarkerDeletionDao {
    @Query("SELECT * FROM marker_deletions WHERE markerId = :markerId")
    suspend fun getByMarkerId(markerId: String): MarkerDeletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(deletion: MarkerDeletionEntity)
}
