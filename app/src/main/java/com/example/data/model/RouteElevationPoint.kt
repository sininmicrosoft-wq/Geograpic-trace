package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity storing granular elevation data associated with GPS location points.
 * Captures altitude, distance progression, incline grade slope, and cumulative climb
 * for generating high-fidelity D3 elevation profiles.
 */
@Entity(
    tableName = "route_elevation_points",
    foreignKeys = [
        ForeignKey(
            entity = TripSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "pointIndex"])
    ]
)
data class RouteElevationPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val pointIndex: Int,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
    val distanceFromStartMeters: Double,
    val gradePercentage: Float = 0f, // Slope % (rise / run * 100)
    val cumulativeAscentMeters: Double = 0.0,
    val cumulativeDescentMeters: Double = 0.0,
    val verticalAccuracyMeters: Float = 0f,
    val timestampMillis: Long = System.currentTimeMillis()
)
