package com.lurecalendar.app.di

import com.lurecalendar.app.data.remote.api.OpenMeteoApiService
import com.lurecalendar.app.data.remote.api.JuheWeatherApiService
import com.lurecalendar.app.data.remote.api.WaterLevelApiService
import com.lurecalendar.app.data.remote.api.WeatherApiService
import com.lurecalendar.app.data.remote.api.WeatherGeoApiService
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
            .baseUrl("https://devapi.qweather.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherGeoApiService(okHttpClient: OkHttpClient, moshi: Moshi): WeatherGeoApiService {
        return Retrofit.Builder()
            .baseUrl("https://geoapi.qweather.com/v2/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherGeoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenMeteoApiService(okHttpClient: OkHttpClient, moshi: Moshi): OpenMeteoApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideJuheWeatherApiService(okHttpClient: OkHttpClient, moshi: Moshi): JuheWeatherApiService {
        return Retrofit.Builder()
            .baseUrl("https://apis.juhe.cn/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JuheWeatherApiService::class.java)
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

    @Provides
    @Singleton
    fun provideLureCalendarApiService(okHttpClient: OkHttpClient, moshi: Moshi): com.lurecalendar.app.data.remote.api.LureCalendarApiService {
        // 这里填写你公网服务器的地址
        return Retrofit.Builder()
            .baseUrl("http://125.67.191.50:8001/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(com.lurecalendar.app.data.remote.api.LureCalendarApiService::class.java)
    }
}
