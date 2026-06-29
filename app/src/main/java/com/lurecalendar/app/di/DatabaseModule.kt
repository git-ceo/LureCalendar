package com.lurecalendar.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lurecalendar.app.data.local.AppDatabase
import com.lurecalendar.app.data.local.SpotPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val columns = mutableSetOf<String>()
            db.query("PRAGMA table_info(fishing_spots)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(nameIndex))
                }
            }
            if (!columns.contains("river")) db.execSQL("ALTER TABLE fishing_spots ADD COLUMN river TEXT")
            if (!columns.contains("city")) db.execSQL("ALTER TABLE fishing_spots ADD COLUMN city TEXT")
            if (!columns.contains("locationDetail")) db.execSQL("ALTER TABLE fishing_spots ADD COLUMN locationDetail TEXT")
            if (!columns.contains("qWeatherLocationId")) db.execSQL("ALTER TABLE fishing_spots ADD COLUMN qWeatherLocationId TEXT")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS weather_snapshot (
                    locationKey TEXT NOT NULL,
                    json TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    PRIMARY KEY(locationKey)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS weather_timeline (
                    locationKey TEXT NOT NULL,
                    timeEpoch INTEGER NOT NULL,
                    timeText TEXT NOT NULL,
                    fishingIndex INTEGER NOT NULL,
                    weatherText TEXT,
                    airTemperature REAL,
                    waterTemperature REAL,
                    pressure REAL,
                    windSpeed REAL,
                    windDirection TEXT,
                    precipitation REAL,
                    precipitationProbability INTEGER,
                    humidity INTEGER,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(locationKey, timeEpoch)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_weather_timeline_loc_time ON weather_timeline(locationKey, timeEpoch)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_weather_timeline_created ON weather_timeline(createdAt)")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE catch_records ADD COLUMN lure_type TEXT")
            db.execSQL("ALTER TABLE catch_records ADD COLUMN rig_type TEXT")
            db.execSQL("ALTER TABLE catch_records ADD COLUMN structure_zone TEXT")
            db.execSQL("ALTER TABLE catch_records ADD COLUMN water_clarity TEXT")
            db.execSQL("ALTER TABLE catch_records ADD COLUMN wind_shore_relation TEXT")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // fishing_spots 新增字段
            val columns = mutableSetOf<String>()
            db.query("PRAGMA table_info(fishing_spots)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(nameIndex))
                }
            }
            if (!columns.contains("spot_type")) db.execSQL("ALTER TABLE fishing_spots ADD COLUMN spot_type TEXT NOT NULL DEFAULT '野河'")
            if (!columns.contains("fee_type")) db.execSQL("ALTER TABLE fishing_spots ADD COLUMN fee_type TEXT NOT NULL DEFAULT '免费'")
            if (!columns.contains("district")) db.execSQL("ALTER TABLE fishing_spots ADD COLUMN district TEXT NOT NULL DEFAULT ''")
            // 创建收藏表
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS favorite_spots (
                    spot_id TEXT NOT NULL PRIMARY KEY,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lure_calendar.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_6_7, MIGRATION_7_8).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideFishingSpotDao(db: AppDatabase) = db.fishingSpotDao()

    @Provides
    fun provideCatchRecordDao(db: AppDatabase) = db.catchRecordDao()

    @Provides
    fun provideWeatherCacheDao(db: AppDatabase) = db.weatherCacheDao()

    @Provides
    fun provideWeatherSnapshotDao(db: AppDatabase) = db.weatherSnapshotDao()

    @Provides
    fun provideWeatherTimelineDao(db: AppDatabase) = db.weatherTimelineDao()

    @Provides
    fun provideWaterLevelDao(db: AppDatabase) = db.waterLevelDao()

    @Provides
    fun provideReminderSettingsDao(db: AppDatabase) = db.reminderSettingsDao()

    @Provides
    fun provideFavoriteSpotDao(db: AppDatabase) = db.favoriteSpotDao()

    @Provides
    @Singleton
    fun provideSpotPreferences(@ApplicationContext context: Context): SpotPreferences {
        return SpotPreferences(context)
    }
}
