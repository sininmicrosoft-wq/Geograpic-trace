package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_sessions")
data class TripSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val activityType: String = "Walking", // Walking, Running, Cycling, Hiking, Driving
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val totalDistanceMeters: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val minAltitudeMeters: Double = 0.0,
    val maxAltitudeMeters: Double = 0.0,
    val pointCount: Int = 0,
    val notes: String = "",
    val isCompleted: Boolean = false
)
