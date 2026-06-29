package com.lurecalendar.app.data.mapper

import com.lurecalendar.app.data.local.entity.CatchRecordEntity
import com.lurecalendar.app.data.local.entity.FishingSpotEntity
import com.lurecalendar.app.data.local.entity.WaterLevelEntity
import com.lurecalendar.app.domain.model.CatchRecord
import com.lurecalendar.app.domain.model.FishingSpot
import com.lurecalendar.app.domain.model.WaterLevel

fun FishingSpotEntity.toDomain(jsonListAdapter: JsonListAdapter): FishingSpot {
    return FishingSpot(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        river = river,
        city = city,
        locationDetail = locationDetail,
        qWeatherLocationId = qWeatherLocationId,
        waterType = waterType,
        structure = structure,
        depth = depth,
        targetSpecies = targetSpecies,
        lureTypes = lureTypes,
        bestSeason = bestSeason,
        note = note,
        photos = jsonListAdapter.fromJson(photos),
        createTime = createTime,
        updateTime = updateTime,
        spotType = spotType,
        feeType = feeType,
        district = district
    )
}

fun FishingSpot.toEntity(jsonListAdapter: JsonListAdapter): FishingSpotEntity {
    return FishingSpotEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        river = river,
        city = city,
        locationDetail = locationDetail,
        qWeatherLocationId = qWeatherLocationId,
        waterType = waterType,
        structure = structure,
        depth = depth,
        targetSpecies = targetSpecies,
        lureTypes = lureTypes,
        bestSeason = bestSeason,
        note = note,
        photos = jsonListAdapter.toJson(photos),
        createTime = createTime,
        updateTime = updateTime,
        spotType = spotType,
        feeType = feeType,
        district = district
    )
}

fun CatchRecordEntity.toDomain(jsonListAdapter: JsonListAdapter): CatchRecord {
    return CatchRecord(
        id = id,
        spotId = spotId,
        species = species,
        length = length,
        weight = weight,
        photoUris = jsonListAdapter.fromJson(photoUris),
        weatherKey = weatherKey,
        catchTime = catchTime,
        bait = bait,
        rod = rod,
        note = note,
        released = released,
        river = river,
        city = city,
        locationDetail = locationDetail,
        count = count,
        temperature = temperature,
        humidity = humidity,
        pressure = pressure,
        fishingIndex = fishingIndex,
        lureType = lureType,
        rigType = rigType,
        structureZone = structureZone,
        waterClarity = waterClarity,
        windShoreRelation = windShoreRelation
    )
}

fun CatchRecord.toEntity(jsonListAdapter: JsonListAdapter): CatchRecordEntity {
    return CatchRecordEntity(
        id = id,
        spotId = spotId,
        species = species,
        length = length,
        weight = weight,
        photoUris = try {
            jsonListAdapter.toJson(photoUris.filter { it.isNotBlank() })
        } catch (e: Exception) {
            "[]"
        },
        weatherKey = weatherKey,
        catchTime = catchTime,
        bait = bait,
        rod = rod,
        note = note,
        released = released,
        river = river,
        city = city,
        locationDetail = locationDetail,
        count = count,
        temperature = temperature,
        humidity = humidity,
        pressure = pressure,
        fishingIndex = fishingIndex,
        isSynced = false,
        lureType = lureType,
        rigType = rigType,
        structureZone = structureZone,
        waterClarity = waterClarity,
        windShoreRelation = windShoreRelation
    )
}

fun WaterLevelEntity.toDomain(): WaterLevel {
    return WaterLevel(
        stationId = stationId,
        stationName = stationName,
        currentLevel = currentLevel,
        warningLevel = warningLevel,
        flowRate = flowRate,
        gateStatus = gateStatus,
        updateTime = updateTime,
        latitude = latitude,
        longitude = longitude,
        isFavorite = isFavorite
    )
}

fun WaterLevel.toEntity(): WaterLevelEntity {
    return WaterLevelEntity(
        stationId = stationId,
        stationName = stationName,
        currentLevel = currentLevel,
        warningLevel = warningLevel,
        flowRate = flowRate,
        gateStatus = gateStatus,
        updateTime = updateTime,
        latitude = latitude,
        longitude = longitude,
        isFavorite = isFavorite
    )
}

