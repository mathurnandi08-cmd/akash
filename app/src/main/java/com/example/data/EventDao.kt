package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM engagement_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EngagementEvent>>

    @Query("SELECT * FROM engagement_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<EngagementEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EngagementEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EngagementEvent>)

    @Query("DELETE FROM engagement_events")
    suspend fun clearAllEvents()

    @Query("SELECT COUNT(*) FROM engagement_events")
    fun getEventsCount(): Flow<Int>

    // Retrieve old events to prune database size to avoid memory issues
    @Query("DELETE FROM engagement_events WHERE id NOT IN (SELECT id FROM engagement_events ORDER BY timestamp DESC LIMIT :keepLimit)")
    suspend fun pruneOldEvents(keepLimit: Int)
}
