package com.lurecalendar.app.data.repository

import com.lurecalendar.app.data.local.dao.WaterLevelDao
import com.lurecalendar.app.data.mapper.toDomain
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.remote.api.WaterLevelApiService
import com.lurecalendar.app.data.remote.api.WaterLevelCacheRequest
import com.lurecalendar.app.domain.model.WaterLevel
import com.lurecalendar.app.domain.repository.WaterLevelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class WaterLevelRepositoryImpl @Inject constructor(
    private val apiService: WaterLevelApiService,
    private val lureCalendarApiService: LureCalendarApiService,
    private val waterLevelDao: WaterLevelDao
) : WaterLevelRepository {

    override fun getFavoriteWaterLevels(): Flow<List<WaterLevel>> {
        return waterLevelDao.getFavoriteWaterLevels().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun searchWaterLevels(latitude: Double, longitude: Double, radius: Int): Result<List<WaterLevel>> {
        return runCatching {
            val resp = apiService.getStations(latitude, longitude, radius)
            val stationList = resp.body()?.data.orEmpty()
            val now = System.currentTimeMillis()
            
            stationList.map { station ->
                // 1. 优先尝试从后端缓存获取
                val cacheResp = lureCalendarApiService.getWaterLevelCache(station.stationId)
                if (cacheResp.isSuccessful) {
                    val cache = cacheResp.body()
                    if (cache?.success == true && cache.data != null && cache.timestamp != null) {
                        // 缓存 60 分钟有效
                        if (now - cache.timestamp < 60.minutes.inWholeMilliseconds) {
                            return@map cache.data
                        }
                    }
                }
                
                // 2. 缓存失效或不存在，调用原始 API
                val levelResp = apiService.getCurrentLevel(station.stationId)
                val levelData = levelResp.body()
                
                val waterLevel = WaterLevel(
                    stationId = station.stationId,
                    stationName = station.stationName,
                    currentLevel = levelData?.currentLevel ?: 0f,
                    warningLevel = levelData?.warningLevel ?: 0f,
                    flowRate = levelData?.flowRate,
                    gateStatus = levelData?.gateStatus,
                    updateTime = levelData?.updateTime ?: now,
                    latitude = station.lat,
                    longitude = station.lon,
                    isFavorite = false
                )
                
                // 3. 异步上报到后端
                runCatching {
                    lureCalendarApiService.saveWaterLevelCache(
                        WaterLevelCacheRequest(station.stationId, waterLevel, now)
                    )
                }
                
                waterLevel
            }
        }
    }

    override suspend fun toggleFavorite(stationId: String, isFavorite: Boolean) {
        waterLevelDao.updateFavoriteStatus(stationId, isFavorite)
    }
}
