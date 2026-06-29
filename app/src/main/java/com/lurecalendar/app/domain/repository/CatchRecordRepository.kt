package com.lurecalendar.app.domain.repository

import com.lurecalendar.app.domain.model.CatchRecord
import kotlinx.coroutines.flow.Flow

interface CatchRecordRepository {
    fun getAllCatches(): Flow<List<CatchRecord>>
    fun getCatchesBySpot(spotId: String): Flow<List<CatchRecord>>
    suspend fun saveCatch(catchRecord: CatchRecord)
    suspend fun deleteCatch(catchRecord: CatchRecord)
}