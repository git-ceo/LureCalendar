package com.lurecalendar.app.domain.repository

import com.lurecalendar.app.domain.model.WaterLevel
import kotlinx.coroutines.flow.Flow

interface WaterLevelRepository {
    fun getFavoriteWaterLevels(): Flow<List<WaterLevel>>
    suspend fun searchWaterLevels(latitude: Double, longitude: Double, radius: Int): Result<List<WaterLevel>>
    suspend fun toggleFavorite(stationId: String, isFavorite: Boolean)
}