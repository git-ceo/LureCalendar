package com.lurecalendar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lurecalendar.app.data.local.entity.WeatherSnapshotEntity

@Dao
interface WeatherSnapshotDao {
    @Query("SELECT * FROM weather_snapshot WHERE locationKey = :locationKey LIMIT 1")
    suspend fun get(locationKey: String): WeatherSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeatherSnapshotEntity)
}

