package com.lurecalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_levels")
data class WaterLevelEntity(
    @PrimaryKey val stationId: String,
    val stationName: String,
    val currentLevel: Float,
    val warningLevel: Float,
    val flowRate: Float?,
    val gateStatus: String?,
    val updateTime: Long,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = false
)