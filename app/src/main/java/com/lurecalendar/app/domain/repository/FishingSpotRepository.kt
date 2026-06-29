package com.lurecalendar.app.domain.repository

import com.lurecalendar.app.domain.model.FishingSpot
import kotlinx.coroutines.flow.Flow

interface FishingSpotRepository {
    fun getAllSpots(): Flow<List<FishingSpot>>
    suspend fun getSpotById(id: String): FishingSpot?
    suspend fun saveSpot(spot: FishingSpot)
    suspend fun saveSpots(spots: List<FishingSpot>)
    suspend fun deleteSpot(spot: FishingSpot)
    fun getFavoriteSpotIds(): Flow<List<String>>
    suspend fun addFavorite(spotId: String)
    suspend fun removeFavorite(spotId: String)
}