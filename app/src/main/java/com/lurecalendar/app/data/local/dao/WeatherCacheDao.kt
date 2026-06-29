package com.lurecalendar.app.data.local.dao

import androidx.room.*
import com.lurecalendar.app.data.local.entity.WeatherCacheEntity

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE locationKey = :locationKey AND date = :date")
    suspend fun getWeatherCache(locationKey: String, date: Long): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherCache(cache: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE timestamp < :expirationTime")
    suspend fun cleanOldCache(expirationTime: Long)
}