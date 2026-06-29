package com.lurecalendar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lurecalendar.app.data.local.entity.WeatherTimelineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherTimelineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<WeatherTimelineEntity>)

    @Query("SELECT * FROM weather_timeline WHERE locationKey = :locationKey ORDER BY timeEpoch ASC")
    fun observeByLocation(locationKey: String): Flow<List<WeatherTimelineEntity>>

    @Query("DELETE FROM weather_timeline WHERE createdAt < :expirationTime")
    suspend fun cleanOld(expirationTime: Long)
}

