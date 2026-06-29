package com.lurecalendar.app.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
    @Json(name = "hourly") val hourly: OpenMeteoHourly?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourly(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "soil_temperature_0cm") val soilTemperature0cm: List<Double>?
)

