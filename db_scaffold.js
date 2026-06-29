const fs = require('fs');
const path = require('path');

const files = {
  'app/src/main/java/com/lurecalendar/app/data/local/entity/FishingSpotEntity.kt': `
package com.lurecalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fishing_spots")
data class FishingSpotEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val waterType: String,
    val structure: String,
    val depth: Float?,
    val note: String?,
    val photos: String, // JSON array string
    val createTime: Long,
    val updateTime: Long
)
`,
  'app/src/main/java/com/lurecalendar/app/data/local/entity/CatchRecordEntity.kt': `
package com.lurecalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "catch_records",
    foreignKeys = [
        ForeignKey(
            entity = FishingSpotEntity::class,
            parentColumns = ["id"],
            childColumns = ["spotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("spotId"),
        Index("catchTime"),
        Index("species")
    ]
)
data class CatchRecordEntity(
    @PrimaryKey val id: String,
    val spotId: String,
    val species: String,
    val length: Float?,
    val weight: Float?,
    val photoUris: String, // JSON array string
    val weatherKey: String?,
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
  'app/src/main/java/com/lurecalendar/app/data/local/entity/WeatherCacheEntity.kt': `
package com.lurecalendar.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "weather_cache",
    primaryKeys = ["locationKey", "date"]
)
data class WeatherCacheEntity(
    val locationKey: String,
    val date: Long,
    val tempMax: Float?,
    val tempMin: Float?,
    val humidity: Int?,
    val pressure: Float?,
    val windSpeed: Float?,
    val precipitation: Float?,
    val timestamp: Long
)
`,
  'app/src/main/java/com/lurecalendar/app/data/local/entity/WaterLevelEntity.kt': `
package com.lurecalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_levels")
data class WaterLevelEntity(
    @PrimaryKey val stationId: String,
    val stationName: String,
    val currentLevel: Float,
    val warningLevel: Float,
    val flowRate: Float?,
    val gateStatus: String?,
    val updateTime: Long,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = false
)
`,
  'app/src/main/java/com/lurecalendar/app/data/local/dao/FishingSpotDao.kt': `
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

    @Delete
    suspend fun deleteSpot(spot: FishingSpotEntity)
}
`,
  'app/src/main/java/com/lurecalendar/app/data/local/dao/CatchRecordDao.kt': `
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

    @Delete
    suspend fun deleteCatch(catchRecord: CatchRecordEntity)
}
`,
  'app/src/main/java/com/lurecalendar/app/data/local/dao/WeatherCacheDao.kt': `
package com.lurecalendar.app.data.local.dao

import androidx.room.*
import com.lurecalendar.app.data.local.entity.WeatherCacheEntity

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE locationKey = :locationKey AND date = :date")
    suspend fun getWeatherCache(locationKey: String, date: Long): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherCache(cache: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE timestamp < :expirationTime")
    suspend fun cleanOldCache(expirationTime: Long)
}
`,
  'app/src/main/java/com/lurecalendar/app/data/local/dao/WaterLevelDao.kt': `
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
`,
  'app/src/main/java/com/lurecalendar/app/data/local/AppDatabase.kt': `
package com.lurecalendar.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lurecalendar.app.data.local.dao.*
import com.lurecalendar.app.data.local.entity.*

@Database(
    entities = [
        FishingSpotEntity::class,
        CatchRecordEntity::class,
        WeatherCacheEntity::class,
        WaterLevelEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fishingSpotDao(): FishingSpotDao
    abstract fun catchRecordDao(): CatchRecordDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun waterLevelDao(): WaterLevelDao
}
`,
  'app/src/main/java/com/lurecalendar/app/di/DatabaseModule.kt': `
package com.lurecalendar.app.di

import android.content.Context
import androidx.room.Room
import com.lurecalendar.app.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lure_calendar.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideFishingSpotDao(db: AppDatabase) = db.fishingSpotDao()

    @Provides
    fun provideCatchRecordDao(db: AppDatabase) = db.catchRecordDao()

    @Provides
    fun provideWeatherCacheDao(db: AppDatabase) = db.weatherCacheDao()

    @Provides
    fun provideWaterLevelDao(db: AppDatabase) = db.waterLevelDao()
}
`
};

for (const [filePath, content] of Object.entries(files)) {
  const fullPath = path.join(__dirname, filePath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  fs.writeFileSync(fullPath, content.trim());
}

console.log('Database scaffolding complete.');
