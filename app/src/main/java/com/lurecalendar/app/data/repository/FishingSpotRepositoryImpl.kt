package com.lurecalendar.app.data.repository

import com.lurecalendar.app.data.local.dao.FavoriteSpotDao
import com.lurecalendar.app.data.local.dao.FishingSpotDao
import com.lurecalendar.app.data.local.entity.FavoriteSpotEntity
import com.lurecalendar.app.data.mapper.JsonListAdapter
import com.lurecalendar.app.data.mapper.toDomain
import com.lurecalendar.app.data.mapper.toEntity
import com.lurecalendar.app.domain.model.FishingSpot
import com.lurecalendar.app.domain.repository.FishingSpotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FishingSpotRepositoryImpl @Inject constructor(
    private val dao: FishingSpotDao,
    private val favoriteSpotDao: FavoriteSpotDao,
    private val jsonListAdapter: JsonListAdapter
) : FishingSpotRepository {
    override fun getAllSpots(): Flow<List<FishingSpot>> {
        return dao.getAllSpots().map { list -> list.map { it.toDomain(jsonListAdapter) } }
    }

    override suspend fun getSpotById(id: String): FishingSpot? {
        return dao.getSpotById(id)?.toDomain(jsonListAdapter)
    }

    override suspend fun saveSpot(spot: FishingSpot) {
        dao.insertSpot(spot.toEntity(jsonListAdapter))
    }

    override suspend fun saveSpots(spots: List<FishingSpot>) {
        dao.insertSpots(spots.map { it.toEntity(jsonListAdapter) })
    }

    override suspend fun deleteSpot(spot: FishingSpot) {
        dao.deleteSpot(spot.toEntity(jsonListAdapter))
    }

    override fun getFavoriteSpotIds(): Flow<List<String>> {
        return favoriteSpotDao.getAllFavoriteIds()
    }

    override suspend fun addFavorite(spotId: String) {
        favoriteSpotDao.addFavorite(FavoriteSpotEntity(spotId = spotId))
    }

    override suspend fun removeFavorite(spotId: String) {
        favoriteSpotDao.removeFavorite(spotId)
    }
}
