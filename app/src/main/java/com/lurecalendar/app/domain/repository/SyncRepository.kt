package com.lurecalendar.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    /**
     * 执行全量同步：本地 -> 服务器
     * 返回同步结果的描述或进度
     */
    suspend fun performFullSync(): Result<SyncSummary>

    data class SyncSummary(
        val spotCount: Int,
        val catchCount: Int,
        val message: String
    )
}
