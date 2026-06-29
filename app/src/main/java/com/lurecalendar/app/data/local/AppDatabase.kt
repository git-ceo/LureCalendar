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
        WeatherSnapshotEntity::class,
        WeatherTimelineEntity::class,
        WaterLevelEntity::class,
        ReminderSettingsEntity::class,
        FavoriteSpotEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fishingSpotDao(): FishingSpotDao
    abstract fun catchRecordDao(): CatchRecordDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun weatherSnapshotDao(): WeatherSnapshotDao
    abstract fun weatherTimelineDao(): WeatherTimelineDao
    abstract fun waterLevelDao(): WaterLevelDao
    abstract fun reminderSettingsDao(): ReminderSettingsDao
    abstract fun favoriteSpotDao(): FavoriteSpotDao
}
