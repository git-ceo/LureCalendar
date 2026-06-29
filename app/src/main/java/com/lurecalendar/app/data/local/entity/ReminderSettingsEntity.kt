package com.lurecalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_settings")
data class ReminderSettingsEntity(
    @PrimaryKey val id: Int = 1, // 仅保存一份设置
    val isEnabled: Boolean = false,
    val minTemp: Float = 18f,
    val maxTemp: Float = 28f,
    val isPressureStable: Boolean = true,
    val maxWindSpeed: Float = 4f,
    val startHour: Int = 4,
    val endHour: Int = 7
)
