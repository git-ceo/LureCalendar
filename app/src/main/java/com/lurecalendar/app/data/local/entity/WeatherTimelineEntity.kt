package com.lurecalendar.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "weather_timeline",
    primaryKeys = ["locationKey", "timeEpoch"]
)
data class WeatherTimelineEntity(
    val locationKey: String,
    val timeEpoch: Long,
    val timeText: String,
    val fishingIndex: Int,
    val weatherText: String?,
    val airTemperature: Float?,
    val waterTemperature: Float?,
    val pressure: Float?,
    val windSpeed: Float?,
    val windDirection: String?,
    val precipitation: Float?,
    val precipitationProbability: Int?,
    val humidity: Int?,
    val createdAt: Long
)

