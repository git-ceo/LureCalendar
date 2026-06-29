package com.lurecalendar.app.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HourlyWeatherResponse(
    @Json(name = "code") val code: String,
    @Json(name = "hourly") val hourly: List<HourlyWeatherItem>?
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherItem(
    @Json(name = "fxTime") val fxTime: String,
    @Json(name = "temp") val temp: String,
    @Json(name = "text") val text: String?,
    @Json(name = "icon") val icon: String?,
    @Json(name = "humidity") val humidity: String?,
    @Json(name = "pressure") val pressure: String?,
    @Json(name = "windSpeed") val windSpeed: String?,
    @Json(name = "windDir") val windDir: String?,
    @Json(name = "pop") val pop: String?,
    @Json(name = "precip") val precip: String?
)
