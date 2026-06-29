package com.lurecalendar.app.data.repository

import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.domain.repository.FishingSpotRepository
import com.lurecalendar.app.domain.repository.SyncRepository
import com.lurecalendar.app.data.local.dao.CatchRecordDao
import com.lurecalendar.app.data.mapper.JsonListAdapter
import com.lurecalendar.app.data.mapper.toDomain
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val apiService: LureCalendarApiService,
    private val spotRepository: FishingSpotRepository,
    private val catchDao: CatchRecordDao,
    private val jsonListAdapter: JsonListAdapter
) : SyncRepository {

    override suspend fun performFullSync(): Result<SyncRepository.SyncSummary> {
        return runCatching {
            // 1. 从后端拉取钓场数据到本地
            var pulledSpots = 0
            runCatching {
                val remoteSpots = apiService.getAllSpots()
                spotRepository.saveSpots(remoteSpots)
                pulledSpots = remoteSpots.size
            }

            // 2. 获取本地所有钓点
            val allSpots = spotRepository.getAllSpots().first()
            
            // 3. 获取本地所有鱼获
            val allCatches = catchDao.getAllCatches().first().map { it.toDomain(jsonListAdapter) }

            // 4. 调用同步接口 (全量推送)
            val spotResponse = apiService.syncSpots(allSpots)
            val spotResult = spotResponse.body()
            if (!spotResponse.isSuccessful || spotResult?.success != true) {
                throw Exception("钓点同步失败: ${spotResult?.message ?: spotResponse.message()}")
            }

            val catchResponse = apiService.syncCatches(allCatches)
            val catchResult = catchResponse.body()
            if (!catchResponse.isSuccessful || catchResult?.success != true) {
                throw Exception("鱼获同步失败: ${catchResult?.message ?: catchResponse.message()}")
            }

            SyncRepository.SyncSummary(
                spotCount = pulledSpots.coerceAtLeast(allSpots.size),
                catchCount = allCatches.size,
                message = "同步成功！拉取钓场 ${pulledSpots} 个"
            )
        }
    }
}
