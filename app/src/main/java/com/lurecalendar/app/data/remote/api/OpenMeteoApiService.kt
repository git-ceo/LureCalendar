package com.lurecalendar.app.data.remote.api

import com.lurecalendar.app.data.remote.model.OpenMeteoForecastResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApiService {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "soil_temperature_0cm",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 2
    ): Response<OpenMeteoForecastResponse>
}

