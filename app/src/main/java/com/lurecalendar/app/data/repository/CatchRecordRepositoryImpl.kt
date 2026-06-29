package com.lurecalendar.app.data.repository

import com.lurecalendar.app.data.local.dao.CatchRecordDao
import com.lurecalendar.app.data.mapper.JsonListAdapter
import com.lurecalendar.app.data.mapper.toDomain
import com.lurecalendar.app.data.mapper.toEntity
import com.lurecalendar.app.domain.model.CatchRecord
import com.lurecalendar.app.domain.repository.CatchRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CatchRecordRepositoryImpl @Inject constructor(
    private val dao: CatchRecordDao,
    private val jsonListAdapter: JsonListAdapter
) : CatchRecordRepository {
    override fun getAllCatches(): Flow<List<CatchRecord>> {
        return dao.getAllCatches().map { list -> list.map { it.toDomain(jsonListAdapter) } }
    }

    override fun getCatchesBySpot(spotId: String): Flow<List<CatchRecord>> {
        return dao.getCatchesBySpot(spotId).map { list -> list.map { it.toDomain(jsonListAdapter) } }
    }

    override suspend fun saveCatch(catchRecord: CatchRecord) {
        dao.insertCatch(catchRecord.toEntity(jsonListAdapter))
    }

    override suspend fun deleteCatch(catchRecord: CatchRecord) {
        dao.deleteCatch(catchRecord.toEntity(jsonListAdapter))
    }
}

