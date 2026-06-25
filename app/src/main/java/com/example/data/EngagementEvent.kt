package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "engagement_events")
data class EngagementEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val eventType: String, // "click", "view", "purchase", "session_start", "error"
    val elementName: String, // e.g., "MainScreen", "CheckoutButton"
    val platform: String, // "Android", "iOS", "Web"
    val country: String, // e.g., "US", "UK", "DE", "JP", "IN"
    val timestamp: Long = System.currentTimeMillis(),
    val value: Double = 0.0 // Optional double value (e.g., purchase price or duration or scroll percentage)
)
