package com.lurecalendar.app.di

import com.lurecalendar.app.data.repository.AuthRepositoryImpl
import com.lurecalendar.app.data.repository.CatchRecordRepositoryImpl
import com.lurecalendar.app.data.repository.FishingSpotRepositoryImpl
import com.lurecalendar.app.data.repository.SyncRepositoryImpl
import com.lurecalendar.app.data.repository.WaterLevelRepositoryImpl
import com.lurecalendar.app.data.repository.WeatherRepositoryImpl
import com.lurecalendar.app.data.repository.ReminderRepositoryImpl
import com.lurecalendar.app.domain.repository.AuthRepository
import com.lurecalendar.app.domain.repository.CatchRecordRepository
import com.lurecalendar.app.domain.repository.FishingSpotRepository
import com.lurecalendar.app.domain.repository.SyncRepository
import com.lurecalendar.app.domain.repository.WaterLevelRepository
import com.lurecalendar.app.domain.repository.WeatherRepository
import com.lurecalendar.app.domain.repository.ReminderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindFishingSpotRepository(impl: FishingSpotRepositoryImpl): FishingSpotRepository

    @Binds
    abstract fun bindCatchRecordRepository(impl: CatchRecordRepositoryImpl): CatchRecordRepository

    @Binds
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository

    @Binds
    abstract fun bindWaterLevelRepository(impl: WaterLevelRepositoryImpl): WaterLevelRepository

    @Binds
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
}
