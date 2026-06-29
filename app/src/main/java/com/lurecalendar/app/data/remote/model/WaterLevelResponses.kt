package com.lurecalendar.app.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StationListResponse(
    @Json(name = "data") val data: List<Station>
)

@JsonClass(generateAdapter = true)
data class Station(
    @Json(name = "stationId") val stationId: String,
    @Json(name = "stationName") val stationName: String,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double
)

@JsonClass(generateAdapter = true)
data class WaterLevelResponse(
    @Json(name = "stationId") val stationId: String,
    @Json(name = "currentLevel") val currentLevel: Float,
    @Json(name = "warningLevel") val warningLevel: Float,
    @Json(name = "flowRate") val flowRate: Float?,
    @Json(name = "gateStatus") val gateStatus: String?,
    @Json(name = "updateTime") val updateTime: Long
)