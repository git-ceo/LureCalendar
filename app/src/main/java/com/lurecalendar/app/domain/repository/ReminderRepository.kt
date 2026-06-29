package com.lurecalendar.app.domain.repository

import com.lurecalendar.app.data.local.entity.ReminderSettingsEntity
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getSettings(): Flow<ReminderSettingsEntity?>
    suspend fun saveSettings(settings: ReminderSettingsEntity)
    suspend fun checkAndNotify()
}
