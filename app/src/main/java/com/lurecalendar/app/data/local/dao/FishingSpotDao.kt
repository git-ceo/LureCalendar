package com.lurecalendar.app.data.local.dao

import androidx.room.*
import com.lurecalendar.app.data.local.entity.FishingSpotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FishingSpotDao {
    @Query("SELECT * FROM fishing_spots ORDER BY createTime DESC")
    fun getAllSpots(): Flow<List<FishingSpotEntity>>

    @Query("SELECT * FROM fishing_spots WHERE id = :id")
    suspend fun getSpotById(id: String): FishingSpotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: FishingSpotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpots(spots: List<FishingSpotEntity>)

    @Delete
    suspend fun deleteSpot(spot: FishingSpotEntity)
}