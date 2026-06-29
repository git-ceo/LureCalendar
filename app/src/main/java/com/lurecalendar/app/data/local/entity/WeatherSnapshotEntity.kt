package com.lurecalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_snapshot")
data class WeatherSnapshotEntity(
    @PrimaryKey val locationKey: String,
    val json: String,
    val timestamp: Long
)

