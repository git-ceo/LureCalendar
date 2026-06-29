const fs = require('fs');
const path = require('path');

const files = {
  'app/src/main/java/com/lurecalendar/app/data/remote/model/WeatherResponses.kt': `
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
    @Json(name = "humidity") val humidity: String,
    @Json(name = "precip") val precip: String,
    @Json(name = "pressure") val pressure: String,
    @Json(name = "uvIndex") val uvIndex: String
)
`,
  'app/src/main/java/com/lurecalendar/app/data/remote/model/WaterLevelResponses.kt': `
package com.lurecalendar.app.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StationListResponse(
    @Json(name = "data") val data: List<Station>
)

@JsonClass(generateAdapter = true)
data class Station(
    @Json(name = "stationId") val stationId: String,
    @Json(name = "stationName") val stationName: String,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double
)

@JsonClass(generateAdapter = true)
data class WaterLevelResponse(
    @Json(name = "stationId") val stationId: String,
    @Json(name = "currentLevel") val currentLevel: Float,
    @Json(name = "warningLevel") val warningLevel: Float,
    @Json(name = "flowRate") val flowRate: Float?,
    @Json(name = "gateStatus") val gateStatus: String?,
    @Json(name = "updateTime") val updateTime: Long
)
`,
  'app/src/main/java/com/lurecalendar/app/data/remote/api/WeatherApiService.kt': `
package com.lurecalendar.app.data.remote.api

import com.lurecalendar.app.data.remote.model.CurrentWeatherResponse
import com.lurecalendar.app.data.remote.model.DailyWeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v7/weather/now")
    suspend fun getCurrentWeather(
        @Query("location") location: String,
        @Query("key") apiKey: String
    ): Response<CurrentWeatherResponse>

    @GET("v7/weather/15d")
    suspend fun getDailyForecast(
        @Query("location") location: String,
        @Query("key") apiKey: String
    ): Response<DailyWeatherResponse>
}
`,
  'app/src/main/java/com/lurecalendar/app/data/remote/api/WaterLevelApiService.kt': `
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
`,
  'app/src/main/java/com/lurecalendar/app/di/NetworkModule.kt': `
package com.lurecalendar.app.di

import com.lurecalendar.app.data.remote.api.WaterLevelApiService
import com.lurecalendar.app.data.remote.api.WeatherApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApiService(okHttpClient: OkHttpClient, moshi: Moshi): WeatherApiService {
        return Retrofit.Builder()
            .baseUrl("https://devapi.qweather.com/") // using devapi for testing
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWaterLevelApiService(okHttpClient: OkHttpClient, moshi: Moshi): WaterLevelApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/") // Placeholder URL
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WaterLevelApiService::class.java)
    }
}
`
};

for (const [filePath, content] of Object.entries(files)) {
  const fullPath = path.join(__dirname, filePath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  fs.writeFileSync(fullPath, content.trim());
}

console.log('Network scaffolding complete.');
