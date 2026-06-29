package com.lurecalendar.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.lurecalendar.app.common.location.AmapLocationProvider
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.remote.api.UpdateLocationRequest
import com.lurecalendar.app.domain.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class LocationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val locationProvider: AmapLocationProvider,
    private val apiService: LureCalendarApiService,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val phone = authRepository.getPhone().first()
            if (phone.isBlank()) return Result.success()

            // 获取当前位置
            val loc = locationProvider.getOnceLocation()
            
            // 上报位置
            apiService.updateLocation(
                UpdateLocationRequest(
                    phone = phone,
                    latitude = loc.latitude,
                    longitude = loc.longitude
                )
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
