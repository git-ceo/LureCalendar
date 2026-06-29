package com.lurecalendar.app.data.remote.api

import com.lurecalendar.app.data.remote.model.CurrentWeatherResponse
import com.lurecalendar.app.data.remote.model.DailyWeatherResponse
import com.lurecalendar.app.data.remote.model.HourlyWeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v7/weather/now")
    suspend fun getCurrentWeather(
        @Query("location") location: String,
        @Query("key") apiKey: String
    ): Response<CurrentWeatherResponse>

    @GET("v7/weather/24h")
    suspend fun getHourlyForecast(
        @Query("location") location: String,
        @Query("key") apiKey: String
    ): Response<HourlyWeatherResponse>

    @GET("v7/weather/15d")
    suspend fun getDailyForecast(
        @Query("location") location: String,
        @Query("key") apiKey: String
    ): Response<DailyWeatherResponse>
}
