package com.lurecalendar.app.data.remote.api

import com.lurecalendar.app.data.remote.model.StationListResponse
import com.lurecalendar.app.data.remote.model.WaterLevelResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WaterLevelApiService {
    @GET("api/stations")
    suspend fun getStations(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("radius") radius: Int
    ): Response<StationListResponse>

    @GET("api/stations/{stationId}/current")
    suspend fun getCurrentLevel(
        @Path("stationId") stationId: String
    ): Response<WaterLevelResponse>
}