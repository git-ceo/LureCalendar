package com.lurecalendar.app.data.remote.api

import com.lurecalendar.app.data.remote.model.JuheSimpleWeatherResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface JuheWeatherApiService {
    @GET("simpleWeather/query")
    suspend fun query(
        @Query("city") city: String,
        @Query("key") key: String
    ): Response<JuheSimpleWeatherResponse>

    @FormUrlEncoded
    @POST("simpleWeather/query")
    suspend fun queryPost(
        @Field("city") city: String,
        @Field("key") key: String
    ): Response<JuheSimpleWeatherResponse>
}
