package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_waypoints",
    indices = [Index(value = ["sessionId"])]
)
data class WaypointMarker(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val title: String,
    val description: String = "",
    val category: String = "FLAG", // FLAG, WATER, REST, PHOTO, HAZARD, PEAK
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val timestampMillis: Long = System.currentTimeMillis()
)
