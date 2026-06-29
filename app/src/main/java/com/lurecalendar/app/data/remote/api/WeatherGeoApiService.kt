package com.lurecalendar.app.data.remote.api

import com.lurecalendar.app.data.remote.model.GeoLookupResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherGeoApiService {
    @GET("city/lookup")
    suspend fun lookup(
        @Query("location") location: String,
        @Query("key") apiKey: String,
        @Query("number") number: Int = 1
    ): Response<GeoLookupResponse>
}
