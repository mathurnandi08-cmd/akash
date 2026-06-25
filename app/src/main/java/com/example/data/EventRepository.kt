package com.example.data

import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {

    val allEvents: Flow<List<EngagementEvent>> = eventDao.getAllEvents()
    
    fun getRecentEvents(limit: Int): Flow<List<EngagementEvent>> = eventDao.getRecentEvents(limit)
    
    val totalEventCount: Flow<Int> = eventDao.getEventsCount()

    suspend fun insert(event: EngagementEvent) {
        eventDao.insertEvent(event)
    }

    suspend fun insertMultiple(events: List<EngagementEvent>) {
        eventDao.insertEvents(events)
    }

    suspend fun clear() {
        eventDao.clearAllEvents()
    }

    suspend fun prune(keepLimit: Int) {
        eventDao.pruneOldEvents(keepLimit)
    }
}
