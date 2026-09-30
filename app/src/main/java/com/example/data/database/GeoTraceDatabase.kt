package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.TripDao
import com.example.data.model.LocationBreadcrumb
import com.example.data.model.RewardTransaction
import com.example.data.model.TripSession
import com.example.data.model.UserRewardWallet
import com.example.data.model.WaypointMarker

@Database(
    entities = [
        TripSession::class,
        LocationBreadcrumb::class,
        WaypointMarker::class,
        UserRewardWallet::class,
        RewardTransaction::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GeoTraceDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: GeoTraceDatabase? = null

        fun getDatabase(context: Context): GeoTraceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeoTraceDatabase::class.java,
                    "geotrace_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
