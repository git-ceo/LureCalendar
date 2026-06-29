package com.lurecalendar.app.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JuheSimpleWeatherResponse(
    @Json(name = "error_code") val errorCode: Int,
    @Json(name = "reason") val reason: String?,
    @Json(name = "result") val result: JuheSimpleWeatherResult?
)

@JsonClass(generateAdapter = true)
data class JuheSimpleWeatherResult(
    @Json(name = "city") val city: String?,
    @Json(name = "realtime") val realtime: JuheRealtime?,
    @Json(name = "future") val future: List<JuheFutureDay>?
)

@JsonClass(generateAdapter = true)
data class JuheRealtime(
    @Json(name = "info") val info: String?,
    @Json(name = "wid") val wid: String?,
    @Json(name = "temperature") val temperature: String?,
    @Json(name = "humidity") val humidity: String?,
    @Json(name = "direct") val direct: String?,
    @Json(name = "power") val power: String?,
    @Json(name = "aqi") val aqi: String?
)

@JsonClass(generateAdapter = true)
data class JuheFutureDay(
    @Json(name = "date") val date: String?,
    @Json(name = "temperature") val temperature: String?,
    @Json(name = "weather") val weather: String?,
    @Json(name = "direct") val direct: String?
)
