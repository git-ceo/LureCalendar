package com.lurecalendar.app.data.local.dao

import androidx.room.*
import com.lurecalendar.app.data.local.entity.WaterLevelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLevelDao {
    @Query("SELECT * FROM water_levels WHERE isFavorite = 1")
    fun getFavoriteWaterLevels(): Flow<List<WaterLevelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLevel(waterLevel: WaterLevelEntity)

    @Query("UPDATE water_levels SET isFavorite = :isFavorite WHERE stationId = :stationId")
    suspend fun updateFavoriteStatus(stationId: String, isFavorite: Boolean)
}