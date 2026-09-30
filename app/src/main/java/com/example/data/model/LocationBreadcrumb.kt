package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_breadcrumbs",
    indices = [Index(value = ["sessionId"])]
)
data class LocationBreadcrumb(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val altitudeAccuracyMeters: Float = 0f,
    val gradePercentage: Float = 0f,
    val speed: Float = 0f, // in m/s
    val accuracy: Float = 0f, // in meters
    val bearing: Float = 0f, // in degrees
    val timestampMillis: Long = System.currentTimeMillis(),
    val cumulativeDistanceMeters: Double = 0.0
)
