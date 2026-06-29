package com.lurecalendar.app.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CurrentWeatherResponse(
    @Json(name = "code") val code: String,
    @Json(name = "now") val now: NowWeather
)

@JsonClass(generateAdapter = true)
data class NowWeather(
    @Json(name = "temp") val temp: String,
    @Json(name = "feelsLike") val feelsLike: String,
    @Json(name = "text") val text: String,
    @Json(name = "windDir") val windDir: String,
    @Json(name = "windSpeed") val windSpeed: String,
    @Json(name = "humidity") val humidity: String,
    @Json(name = "precip") val precip: String,
    @Json(name = "pressure") val pressure: String,
    @Json(name = "vis") val vis: String
)

@JsonClass(generateAdapter = true)
data class DailyWeatherResponse(
    @Json(name = "code") val code: String,
    @Json(name = "daily") val daily: List<DailyWeather>
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    @Json(name = "fxDate") val fxDate: String,
    @Json(name = "tempMax") val tempMax: String,
    @Json(name = "tempMin") val tempMin: String,
    @Json(name = "textDay") val textDay: String,
    @Json(name = "windSpeedDay") val windSpeedDay: String,
    @Json(name = "precip") val precip: String,
    @Json(name = "humidity") val humidity: String?,
    @Json(name = "pressure") val pressure: String?,
    @Json(name = "uvIndex") val uvIndex: String?
)
