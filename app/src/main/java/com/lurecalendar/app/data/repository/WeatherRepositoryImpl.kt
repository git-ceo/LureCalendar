package com.lurecalendar.app.data.repository

import com.lurecalendar.app.BuildConfig
import com.lurecalendar.app.common.fishing.FishingIndexCalculator
import com.lurecalendar.app.data.local.dao.WeatherSnapshotDao
import com.lurecalendar.app.data.local.dao.WeatherTimelineDao
import com.lurecalendar.app.data.local.entity.WeatherSnapshotEntity
import com.lurecalendar.app.data.local.entity.WeatherTimelineEntity
import com.lurecalendar.app.data.remote.api.JuheWeatherApiService
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.remote.api.OpenMeteoApiService
import com.lurecalendar.app.data.remote.api.WeatherApiService
import com.lurecalendar.app.data.remote.api.WeatherGeoApiService
import com.lurecalendar.app.data.remote.api.WeatherCacheRequest
import com.lurecalendar.app.domain.model.CurrentWeather
import com.lurecalendar.app.domain.model.DailyWeather
import com.lurecalendar.app.domain.model.HourlyWeather
import com.lurecalendar.app.domain.model.ResolvedLocation
import com.lurecalendar.app.domain.model.WeatherData
import com.lurecalendar.app.domain.repository.WeatherProvider
import com.lurecalendar.app.domain.repository.WeatherRepository
import com.squareup.moshi.Moshi
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import java.time.Instant

class WeatherRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService,
    private val geoApiService: WeatherGeoApiService,
    private val juheWeatherApiService: JuheWeatherApiService,
    private val openMeteoApiService: OpenMeteoApiService,
    private val lureCalendarApiService: LureCalendarApiService,
    private val snapshotDao: WeatherSnapshotDao,
    private val timelineDao: WeatherTimelineDao,
    private val moshi: Moshi
) : WeatherRepository {
    private val adapter = moshi.adapter(WeatherData::class.java)

    override suspend fun getWeather(
        location: String,
        latitude: Double?,
        longitude: Double?,
        waterType: String,
        provider: WeatherProvider,
        forceRefresh: Boolean
    ): Result<WeatherData> {
        val juheKey = BuildConfig.JUHE_WEATHER_KEY
        val qWeatherKey = BuildConfig.QWEATHER_API_KEY

        val effectiveProvider = when (provider) {
            WeatherProvider.JUHE -> WeatherProvider.JUHE
            WeatherProvider.QWEATHER -> WeatherProvider.QWEATHER
            WeatherProvider.AUTO -> if (juheKey.isNotBlank()) WeatherProvider.JUHE else WeatherProvider.QWEATHER
        }

        if (effectiveProvider == WeatherProvider.JUHE && juheKey.isBlank()) return Result.failure(IllegalStateException("未配置聚合天气 KEY"))
        if (effectiveProvider == WeatherProvider.QWEATHER && qWeatherKey.isBlank()) return Result.failure(IllegalStateException("未配置和风天气 KEY"))

        val cacheKey = "${effectiveProvider.name}_$location"
        val now = System.currentTimeMillis()

        // 1. 尝试从本地缓存读取
        val cached = snapshotDao.get(cacheKey)
        val cacheValid = cached?.let { now - it.timestamp <= 30.minutes.inWholeMilliseconds } == true
        if (!forceRefresh && cacheValid) {
            val data = cached?.json?.let { adapter.fromJson(it) }
            if (data != null) return Result.success(data)
        }

        // 2. 尝试从后端服务器缓存读取 (节约 API 调用)
        if (!forceRefresh) {
            runCatching {
                val remoteCacheResp = lureCalendarApiService.getWeatherCache(cacheKey)
                if (remoteCacheResp.isSuccessful) {
                    val remoteCache = remoteCacheResp.body()
                    if (remoteCache?.success == true && remoteCache.data != null && remoteCache.timestamp != null) {
                        // 检查后端数据是否还在时效内 (比如 30 分钟)
                        if (now - remoteCache.timestamp <= 30.minutes.inWholeMilliseconds) {
                            val data = remoteCache.data
                            // 同步到本地缓存
                            snapshotDao.upsert(
                                WeatherSnapshotEntity(
                                    locationKey = cacheKey,
                                    json = adapter.toJson(data),
                                    timestamp = remoteCache.timestamp
                                )
                            )
                            return Result.success(data)
                        }
                    }
                }
            }
        }

        return runCatching {
            val fetchedData = if (effectiveProvider == WeatherProvider.JUHE) {
                val resp = juheWeatherApiService.query(city = location, key = juheKey)
                val body = if (resp.isSuccessful) {
                    resp.body()
                } else {
                    val resp2 = juheWeatherApiService.queryPost(city = location, key = juheKey)
                    if (resp2.isSuccessful) resp2.body() else null
                }
                if (body?.errorCode != 0) {
                    error(body?.reason ?: "天气接口返回异常")
                }
                val result = body?.result ?: error("天气接口返回空结果")
                val realtime = result.realtime
                val currentTemp = realtime?.temperature?.toFloatOrNull() ?: 0f
                val humidity = realtime?.humidity?.toIntOrNull() ?: 0
                val windDir = realtime?.direct ?: ""
                val windSpeed = windPowerToMs(realtime?.power) ?: 0f
                val waterTemp = FishingIndexCalculator.estimateWaterTempC(currentTemp, waterType)
                val idx = FishingIndexCalculator.calculate(
                    pressureHpa = null,
                    windSpeedMs = windSpeed,
                    airTempC = currentTemp,
                    waterTempC = waterTemp,
                    precipitationMm = null,
                    precipitationProbability = null
                )

                val dailyList = result.future.orEmpty().mapNotNull { d ->
                    val date = d.date ?: return@mapNotNull null
                    val (minT, maxT) = parseMinMaxTemperature(d.temperature)
                    DailyWeather(
                        date = date,
                        tempMax = maxT,
                        tempMin = minT,
                        humidity = 0,
                        pressure = 0f,
                        windSpeed = 0f,
                        precipitation = 0f,
                        uvIndex = 0,
                        weatherText = d.weather ?: ""
                    )
                }

                WeatherData(
                    current = CurrentWeather(
                        temperature = currentTemp,
                        humidity = humidity,
                        pressure = 0f,
                        windSpeed = windSpeed,
                        windDirection = windDir,
                        precipitation = 0f,
                        visibility = 0f
                    ),
                    hourly = listOf(
                        HourlyWeather(
                            time = Instant.ofEpochMilli(now).toString(),
                            temperature = currentTemp,
                            humidity = humidity,
                            pressure = null,
                            windSpeed = windSpeed,
                            windDirection = windDir,
                            precipitationProbability = null,
                            precipitation = null,
                            weatherText = realtime?.info,
                            waterTemperature = waterTemp,
                            fishingIndex = idx.score
                        )
                    ),
                    daily = dailyList,
                    timestamp = now
                )
            } else {
                val key = qWeatherKey
                val currentResp = weatherApiService.getCurrentWeather(location = location, apiKey = key)
                val hourlyResp = weatherApiService.getHourlyForecast(location = location, apiKey = key)
                val dailyResp = weatherApiService.getDailyForecast(location = location, apiKey = key)

                val currentBody = currentResp.body()
                val hourlyBody = hourlyResp.body()
                val dailyBody = dailyResp.body()

                val current = currentBody?.now?.takeIf { currentBody.code == "200" } ?: error("天气接口返回异常")
                val dailyList = dailyBody?.daily?.takeIf { dailyBody.code == "200" }.orEmpty()
                val hourlyList = hourlyBody?.hourly?.takeIf { hourlyBody.code == "200" }.orEmpty()

                val waterTempByTime: Map<String, Float> = if (latitude != null && longitude != null) {
                    runCatching {
                        val resp = openMeteoApiService.forecast(latitude = latitude, longitude = longitude)
                        val body = resp.body()
                        val time = body?.hourly?.time.orEmpty()
                        val wt = body?.hourly?.soilTemperature0cm.orEmpty()
                        val size = minOf(time.size, wt.size)
                        buildMap {
                            for (i in 0 until size) {
                                put(time[i], wt[i].toFloat())
                            }
                        }
                    }.getOrDefault(emptyMap())
                } else {
                    emptyMap()
                }

                WeatherData(
                    current = CurrentWeather(
                        temperature = current.temp.toFloatOrNull() ?: 0f,
                        humidity = current.humidity.toIntOrNull() ?: 0,
                        pressure = current.pressure.toFloatOrNull() ?: 0f,
                        windSpeed = current.windSpeed.toFloatOrNull() ?: 0f,
                        windDirection = current.windDir,
                        precipitation = current.precip.toFloatOrNull() ?: 0f,
                        visibility = current.vis.toFloatOrNull() ?: 0f
                    ),
                    hourly = hourlyList.map { h ->
                        val airTemp = h.temp.toFloatOrNull()
                        val keyTime = h.fxTime.take(16)
                        val waterTemp = waterTempByTime[keyTime] ?: FishingIndexCalculator.estimateWaterTempC(airTemp, waterType)
                        val p = h.pressure?.toFloatOrNull()
                        val w = h.windSpeed?.toFloatOrNull()
                        val pr = h.precip?.toFloatOrNull()
                        val pop = h.pop?.toIntOrNull()
                        val idx = FishingIndexCalculator.calculate(
                            pressureHpa = p,
                            windSpeedMs = w,
                            airTempC = airTemp,
                            waterTempC = waterTemp,
                            precipitationMm = pr,
                            precipitationProbability = pop
                        )
                        HourlyWeather(
                            time = h.fxTime,
                            temperature = airTemp ?: 0f,
                            humidity = h.humidity?.toIntOrNull(),
                            pressure = p,
                            windSpeed = w,
                            windDirection = h.windDir,
                            precipitationProbability = pop,
                            precipitation = pr,
                            weatherText = h.text,
                            waterTemperature = waterTemp,
                            fishingIndex = idx.score
                        )
                    },
                    daily = dailyList.map {
                        DailyWeather(
                            date = it.fxDate,
                            tempMax = it.tempMax.toFloatOrNull() ?: 0f,
                            tempMin = it.tempMin.toFloatOrNull() ?: 0f,
                            humidity = it.humidity?.toIntOrNull() ?: 0,
                            pressure = it.pressure?.toFloatOrNull() ?: 0f,
                            windSpeed = it.windSpeedDay.toFloatOrNull() ?: 0f,
                            precipitation = it.precip.toFloatOrNull() ?: 0f,
                            uvIndex = it.uvIndex?.toIntOrNull() ?: 0,
                            weatherText = it.textDay
                        )
                    },
                    timestamp = now
                )
            }

            // 保存到本地数据库
            runCatching {
                val timeline = fetchedData.hourly.mapNotNull { h ->
                    val epoch = runCatching { java.time.Instant.parse(h.time).toEpochMilli() }.getOrNull() ?: return@mapNotNull null
                    WeatherTimelineEntity(
                        locationKey = location,
                        timeEpoch = epoch,
                        timeText = h.time,
                        fishingIndex = h.fishingIndex ?: 0,
                        weatherText = h.weatherText,
                        airTemperature = h.temperature,
                        waterTemperature = h.waterTemperature,
                        pressure = h.pressure,
                        windSpeed = h.windSpeed,
                        windDirection = h.windDirection,
                        precipitation = h.precipitation,
                        precipitationProbability = h.precipitationProbability,
                        humidity = h.humidity,
                        createdAt = now
                    )
                }
                timelineDao.upsertAll(timeline)
                timelineDao.cleanOld(now - 2L * 24 * 60 * 60 * 1000)
            }

            snapshotDao.upsert(
                WeatherSnapshotEntity(
                    locationKey = cacheKey,
                    json = adapter.toJson(fetchedData),
                    timestamp = now
                )
            )

            // 上报到后端服务器缓存 (供其他用户/设备使用)
            runCatching {
                lureCalendarApiService.saveWeatherCache(
                    WeatherCacheRequest(
                        location_key = cacheKey,
                        weather_data = fetchedData,
                        timestamp = now
                    )
                )
            }

            fetchedData
        }.recoverCatching {
            val fallback = cached?.json?.let { adapter.fromJson(it) }
            if (fallback != null) fallback else throw it
        }
    }

    override suspend fun resolveLocation(query: String, provider: WeatherProvider): Result<ResolvedLocation> {
        val qWeatherKey = BuildConfig.QWEATHER_API_KEY
        val juheKey = BuildConfig.JUHE_WEATHER_KEY

        val parts = query.trim().split(',')
        val isCoordinateQuery = parts.size == 2 &&
            parts[0].trim().toDoubleOrNull() != null &&
            parts[1].trim().toDoubleOrNull() != null

        val useJuhe = when (provider) {
            WeatherProvider.JUHE -> true
            WeatherProvider.QWEATHER -> false
            WeatherProvider.AUTO -> juheKey.isNotBlank() && !isCoordinateQuery
        }

        if (useJuhe) {
            if (juheKey.isBlank()) return Result.failure(IllegalStateException("未配置聚合天气 KEY"))
            val q = query.trim()
            if (q.isBlank()) {
                return Result.failure(IllegalArgumentException("请输入城市名称"))
            }
            if (isCoordinateQuery) {
                return Result.failure(IllegalArgumentException("聚合天气仅支持城市名称查询"))
            }
            return Result.success(
                ResolvedLocation(
                    id = q,
                    name = q,
                    adm1 = null,
                    adm2 = null,
                    lat = null,
                    lon = null
                )
            )
        }

        if (qWeatherKey.isBlank()) return Result.failure(IllegalStateException("QWEATHER_API_KEY 未配置"))
        return runCatching {
            val resp = geoApiService.lookup(location = query, apiKey = qWeatherKey)
            if (!resp.isSuccessful) {
                error("城市检索失败(HTTP ${resp.code()})")
            }
            val body = resp.body() ?: error("城市检索失败(空响应)")
            if (body.code != "200") {
                error("城市检索失败(code=${body.code})")
            }
            val loc = body.location?.firstOrNull() ?: error("城市检索失败(无匹配)")
            ResolvedLocation(
                id = loc.id,
                name = loc.name,
                adm1 = loc.adm1,
                adm2 = loc.adm2,
                lat = loc.lat?.toDoubleOrNull(),
                lon = loc.lon?.toDoubleOrNull()
            )
        }
    }

    override suspend fun resolveLocation(latitude: Double, longitude: Double, provider: WeatherProvider): Result<ResolvedLocation> {
        return resolveLocation("$longitude,$latitude", provider)
    }

    private fun parseMinMaxTemperature(raw: String?): Pair<Float, Float> {
        val t = raw.orEmpty()
        val nums = Regex("-?\\d+").findAll(t).mapNotNull { it.value.toIntOrNull() }.toList()
        val min = nums.getOrNull(0)?.toFloat() ?: 0f
        val max = nums.getOrNull(1)?.toFloat() ?: min
        return min to max
    }

    private fun windPowerToMs(power: String?): Float? {
        val p = power?.trim().orEmpty()
        if (p.isBlank()) return null
        val level = Regex("\\d+").find(p)?.value?.toIntOrNull() ?: return null
        return when (level) {
            0 -> 0f
            1 -> 1.0f
            2 -> 2.4f
            3 -> 4.4f
            4 -> 6.7f
            5 -> 9.3f
            6 -> 12.3f
            7 -> 15.5f
            8 -> 18.9f
            9 -> 22.6f
            10 -> 26.4f
            11 -> 30.5f
            else -> 33.0f
        }
    }
}
