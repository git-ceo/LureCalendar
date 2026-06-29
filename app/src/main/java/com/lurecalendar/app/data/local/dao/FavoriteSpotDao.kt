package com.lurecalendar.app.data.local.dao

import androidx.room.*
import com.lurecalendar.app.data.local.entity.FavoriteSpotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteSpotDao {
    @Query("SELECT spot_id FROM favorite_spots")
    fun getAllFavoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteSpotEntity)

    @Query("DELETE FROM favorite_spots WHERE spot_id = :spotId")
    suspend fun removeFavorite(spotId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_spots WHERE spot_id = :spotId)")
    suspend fun isFavorite(spotId: String): Boolean
}
