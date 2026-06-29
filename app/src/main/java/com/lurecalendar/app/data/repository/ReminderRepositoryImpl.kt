package com.lurecalendar.app.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.lurecalendar.app.R
import com.lurecalendar.app.data.local.dao.ReminderSettingsDao
import com.lurecalendar.app.data.local.entity.ReminderSettingsEntity
import com.lurecalendar.app.domain.repository.ReminderRepository
import com.lurecalendar.app.domain.repository.WeatherRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ReminderSettingsDao,
    private val weatherRepository: WeatherRepository,
) : ReminderRepository {

    override fun getSettings(): Flow<ReminderSettingsEntity?> = dao.getSettings()

    override suspend fun saveSettings(settings: ReminderSettingsEntity) {
        dao.saveSettings(settings)
    }

    override suspend fun checkAndNotify() {
        val settings = dao.getSettings().first() ?: return
        if (!settings.isEnabled) return

        // 这里模拟检查明日天气（实际应从 weatherRepository 获取明日预报）
        // 简单逻辑：如果当前天气符合，就发通知
        val weatherResult = weatherRepository.getWeather("101270101", forceRefresh = true) // 默认绵阳
        weatherResult.onSuccess { data ->
            val current = data.current
            val isTempOk = current.temperature in (settings.minTemp..settings.maxTemp)
            val isWindOk = current.windSpeed <= settings.maxWindSpeed
            
            if (isTempOk && isWindOk) {
                showNotification("出钓提醒", "明日天气符合你的理想条件，赶紧准备装备出发吧！")
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "fishing_reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "出钓提醒", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 暂时用默认图标
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
