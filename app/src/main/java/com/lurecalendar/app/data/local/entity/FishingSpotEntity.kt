package com.lurecalendar.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fishing_spots")
data class FishingSpotEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val river: String?,
    val city: String?,
    val locationDetail: String?,
    val qWeatherLocationId: String?,
    val waterType: String,
    val structure: String,
    val depth: Float?,
    val targetSpecies: String?,
    val lureTypes: String?,
    val bestSeason: String?,
    val note: String?,
    val photos: String, // JSON array string
    val createTime: Long,
    val updateTime: Long,
    @ColumnInfo(name = "spot_type") val spotType: String = "野河",
    @ColumnInfo(name = "fee_type") val feeType: String = "免费",
    val district: String = ""
)
