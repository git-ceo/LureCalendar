const fs = require('fs');
const path = require('path');

const files = {
  'app/src/main/java/com/lurecalendar/app/domain/model/Models.kt': `
package com.lurecalendar.app.domain.model

data class WeatherData(
    val current: CurrentWeather,
    val daily: List<DailyWeather>,
    val timestamp: Long = System.currentTimeMillis()
)

data class CurrentWeather(
    val temperature: Float,
    val humidity: Int,
    val pressure: Float,
    val windSpeed: Float,
    val windDirection: String,
    val precipitation: Float,
    val visibility: Float
)

data class DailyWeather(
    val date: String,
    val tempMax: Float,
    val tempMin: Float,
    val humidity: Int,
    val pressure: Float,
    val windSpeed: Float,
    val precipitation: Float,
    val uvIndex: Int
)

data class WaterLevel(
    val stationId: String,
    val stationName: String,
    val currentLevel: Float,
    val warningLevel: Float,
    val flowRate: Float?,
    val gateStatus: String?,
    val updateTime: Long,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean
)

data class FishingSpot(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val waterType: String,
    val structure: String,
    val depth: Float?,
    val note: String?,
    val photos: List<String>,
    val createTime: Long,
    val updateTime: Long
)

data class CatchRecord(
    val id: String,
    val spotId: String,
    val species: String,
    val length: Float?,
    val weight: Float?,
    val photoUris: List<String>,
    val catchTime: Long,
    val bait: String?,
    val rod: String?,
    val note: String?,
    val released: Boolean,
    val river: String?,
    val city: String?,
    val locationDetail: String?,
    val count: Int,
    val temperature: Float?,
    val humidity: Int?,
    val pressure: Float?,
    val fishingIndex: Int?
)
`,
  'app/src/main/java/com/lurecalendar/app/domain/repository/WeatherRepository.kt': `
package com.lurecalendar.app.domain.repository

import com.lurecalendar.app.domain.model.WeatherData
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun getWeather(location: String, forceRefresh: Boolean = false): Result<WeatherData>
}
`,
  'app/src/main/java/com/lurecalendar/app/domain/repository/WaterLevelRepository.kt': `
package com.lurecalendar.app.domain.repository

import com.lurecalendar.app.domain.model.WaterLevel
import kotlinx.coroutines.flow.Flow

interface WaterLevelRepository {
    fun getFavoriteWaterLevels(): Flow<List<WaterLevel>>
    suspend fun searchWaterLevels(latitude: Double, longitude: Double, radius: Int): Result<List<WaterLevel>>
    suspend fun toggleFavorite(stationId: String, isFavorite: Boolean)
}
`,
  'app/src/main/java/com/lurecalendar/app/domain/repository/FishingSpotRepository.kt': `
package com.lurecalendar.app.domain.repository

import com.lurecalendar.app.domain.model.FishingSpot
import kotlinx.coroutines.flow.Flow

interface FishingSpotRepository {
    fun getAllSpots(): Flow<List<FishingSpot>>
    suspend fun getSpotById(id: String): FishingSpot?
    suspend fun saveSpot(spot: FishingSpot)
    suspend fun deleteSpot(spot: FishingSpot)
}
`,
  'app/src/main/java/com/lurecalendar/app/domain/repository/CatchRecordRepository.kt': `
package com.lurecalendar.app.domain.repository

import com.lurecalendar.app.domain.model.CatchRecord
import kotlinx.coroutines.flow.Flow

interface CatchRecordRepository {
    fun getAllCatches(): Flow<List<CatchRecord>>
    fun getCatchesBySpot(spotId: String): Flow<List<CatchRecord>>
    suspend fun saveCatch(catchRecord: CatchRecord)
    suspend fun deleteCatch(catchRecord: CatchRecord)
}
`
};

for (const [filePath, content] of Object.entries(files)) {
  const fullPath = path.join(__dirname, filePath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  fs.writeFileSync(fullPath, content.trim());
}

console.log('Domain scaffolding complete.');
