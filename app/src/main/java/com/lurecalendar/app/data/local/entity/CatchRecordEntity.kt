package com.lurecalendar.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "catch_records",
    indices = [
        Index("spotId"),
        Index("catchTime"),
        Index("species")
    ]
)
data class CatchRecordEntity(
    @PrimaryKey val id: String,
    val spotId: String = "",
    val species: String = "",
    val length: Float? = null,
    val weight: Float? = null,
    val photoUris: String = "[]", // JSON array string
    val weatherKey: String? = null,
    val catchTime: Long = System.currentTimeMillis(),
    val bait: String? = null,
    val rod: String? = null,
    val note: String? = null,
    val released: Boolean = false,
    val river: String? = null,
    val city: String? = null,
    val locationDetail: String? = null,
    val count: Int = 1,
    val temperature: Float? = null,
    val humidity: Int? = null,
    val pressure: Float? = null,
    val fishingIndex: Int? = null,
    val isSynced: Boolean = false,
    @ColumnInfo(name = "lure_type") val lureType: String? = null,
    @ColumnInfo(name = "rig_type") val rigType: String? = null,
    @ColumnInfo(name = "structure_zone") val structureZone: String? = null,
    @ColumnInfo(name = "water_clarity") val waterClarity: String? = null,
    @ColumnInfo(name = "wind_shore_relation") val windShoreRelation: String? = null
)