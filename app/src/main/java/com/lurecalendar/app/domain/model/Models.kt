package com.lurecalendar.app.domain.model

data class WeatherData(
    val current: CurrentWeather,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>,
    val timestamp: Long = System.currentTimeMillis()
)

data class CurrentWeather(
    val temperature: Float,
    val humidity: Int,
    val pressure: Float,
    val windSpeed: Float,
    val windDirection: String,
    val precipitation: Float,
    val visibility: Float
)

data class HourlyWeather(
    val time: String,
    val temperature: Float,
    val humidity: Int?,
    val pressure: Float?,
    val windSpeed: Float?,
    val windDirection: String?,
    val precipitationProbability: Int?,
    val precipitation: Float?,
    val weatherText: String? = null,
    val waterTemperature: Float? = null,
    val fishingIndex: Int? = null
)

data class DailyWeather(
    val date: String,
    val tempMax: Float,
    val tempMin: Float,
    val humidity: Int,
    val pressure: Float,
    val windSpeed: Float,
    val precipitation: Float,
    val uvIndex: Int,
    val weatherText: String = ""
)

data class ResolvedLocation(
    val id: String,
    val name: String,
    val adm1: String?,
    val adm2: String?,
    val lat: Double?,
    val lon: Double?
)

data class WaterLevel(
    val stationId: String,
    val stationName: String,
    val currentLevel: Float,
    val warningLevel: Float,
    val flowRate: Float?,
    val gateStatus: String?,
    val updateTime: Long,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean
)

data class FishingSpot(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val river: String?,
    val city: String?,
    val locationDetail: String?,
    val qWeatherLocationId: String?,
    val waterType: String,
    val structure: String,
    val depth: Float?,
    val targetSpecies: String?,
    val lureTypes: String?,
    val bestSeason: String?,
    val note: String?,
    val photos: List<String>,
    val createTime: Long,
    val updateTime: Long,
    val spotType: String = "野河",
    val feeType: String = "免费",
    val district: String = "",
    val isFavorite: Boolean = false
)

data class CatchRecord(
    val id: String,
    val spotId: String,
    val species: String,
    val length: Float?,
    val weight: Float?,
    val photoUris: List<String>,
    val weatherKey: String?,
    val catchTime: Long,
    val bait: String?,
    val rod: String?,
    val note: String?,
    val released: Boolean,
    val river: String?,
    val city: String?,
    val locationDetail: String?,
    val count: Int,
    val temperature: Float?,
    val humidity: Int?,
    val pressure: Float?,
    val fishingIndex: Int?,
    val lureType: String? = null,
    val rigType: String? = null,
    val structureZone: String? = null,
    val waterClarity: String? = null,
    val windShoreRelation: String? = null
)
