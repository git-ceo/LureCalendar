package com.lurecalendar.app.data.local.dao

import androidx.room.*
import com.lurecalendar.app.data.local.entity.CatchRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatchRecordDao {
    @Query("SELECT * FROM catch_records ORDER BY catchTime DESC")
    fun getAllCatches(): Flow<List<CatchRecordEntity>>

    @Query("SELECT * FROM catch_records WHERE spotId = :spotId ORDER BY catchTime DESC")
    fun getCatchesBySpot(spotId: String): Flow<List<CatchRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatch(catchRecord: CatchRecordEntity)

    @Query("SELECT * FROM catch_records WHERE isSynced = 0")
    suspend fun getUnsyncedCatches(): List<CatchRecordEntity>

    @Query("UPDATE catch_records SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Delete
    suspend fun deleteCatch(catchRecord: CatchRecordEntity)
}