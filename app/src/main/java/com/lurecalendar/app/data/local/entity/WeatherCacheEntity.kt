package com.lurecalendar.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "weather_cache",
    primaryKeys = ["locationKey", "date"]
)
data class WeatherCacheEntity(
    val locationKey: String,
    val date: Long,
    val tempMax: Float?,
    val tempMin: Float?,
    val humidity: Int?,
    val pressure: Float?,
    val windSpeed: Float?,
    val precipitation: Float?,
    val timestamp: Long
)