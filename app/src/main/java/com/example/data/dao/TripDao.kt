package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.TripSession
import com.example.data.model.WaypointMarker
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trip_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<TripSession>>

    @Query("SELECT * FROM trip_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionById(sessionId: Long): Flow<TripSession?>

    @Query("SELECT * FROM trip_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionByIdSync(sessionId: Long): TripSession?

    @Query("SELECT * FROM location_breadcrumbs WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    fun getSessionPoints(sessionId: Long): Flow<List<LocationBreadcrumb>>

    @Query("SELECT * FROM location_breadcrumbs WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    suspend fun getSessionPointsSync(sessionId: Long): List<LocationBreadcrumb>

    @Query("SELECT * FROM trip_waypoints WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    fun getSessionWaypoints(sessionId: Long): Flow<List<WaypointMarker>>

    @Query("SELECT * FROM trip_waypoints WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    suspend fun getSessionWaypointsSync(sessionId: Long): List<WaypointMarker>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TripSession): Long

    @Update
    suspend fun updateSession(session: TripSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: LocationBreadcrumb): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<LocationBreadcrumb>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointMarker): Long

    @Query("DELETE FROM trip_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM location_breadcrumbs WHERE sessionId = :sessionId")
    suspend fun deletePointsForSession(sessionId: Long)

    @Query("DELETE FROM trip_waypoints WHERE sessionId = :sessionId")
    suspend fun deleteWaypointsForSession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM trip_sessions")
    fun getSessionCount(): Flow<Int>

    @Query("SELECT SUM(totalDistanceMeters) FROM trip_sessions WHERE isCompleted = 1")
    fun getTotalDistanceMeters(): Flow<Double?>

    @Query("SELECT SUM(durationSeconds) FROM trip_sessions WHERE isCompleted = 1")
    fun getTotalDurationSeconds(): Flow<Long?>

    @Query("SELECT SUM(elevationGainMeters) FROM trip_sessions WHERE isCompleted = 1")
    fun getTotalElevationGain(): Flow<Double?>

    @Query("SELECT MAX(maxSpeedKmh) FROM trip_sessions WHERE isCompleted = 1")
    fun getTopSpeedKmh(): Flow<Double?>
}
