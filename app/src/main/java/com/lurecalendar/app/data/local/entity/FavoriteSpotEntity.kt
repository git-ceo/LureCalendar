package com.lurecalendar.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_spots")
data class FavoriteSpotEntity(
    @PrimaryKey @ColumnInfo(name = "spot_id") val spotId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
