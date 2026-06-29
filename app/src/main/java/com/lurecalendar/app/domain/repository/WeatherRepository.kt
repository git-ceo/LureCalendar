package com.lurecalendar.app.domain.repository

import com.lurecalendar.app.domain.model.ResolvedLocation
import com.lurecalendar.app.domain.model.WeatherData

enum class WeatherProvider {
    AUTO,
    JUHE,
    QWEATHER
}

interface WeatherRepository {
    suspend fun getWeather(
        location: String,
        latitude: Double? = null,
        longitude: Double? = null,
        waterType: String = "河流",
        provider: WeatherProvider = WeatherProvider.AUTO,
        forceRefresh: Boolean = false
    ): Result<WeatherData>
    suspend fun resolveLocation(query: String, provider: WeatherProvider = WeatherProvider.AUTO): Result<ResolvedLocation>
    suspend fun resolveLocation(latitude: Double, longitude: Double, provider: WeatherProvider = WeatherProvider.AUTO): Result<ResolvedLocation>
}
