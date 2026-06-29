package com.lurecalendar.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.lurecalendar.app.data.local.dao.CatchRecordDao
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.mapper.JsonListAdapter
import com.lurecalendar.app.data.mapper.toDomain
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val catchDao: CatchRecordDao,
    private val apiService: LureCalendarApiService,
    private val jsonListAdapter: JsonListAdapter
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val unsynced = catchDao.getUnsyncedCatches()
            if (unsynced.isEmpty()) return Result.success()

            val domainCatches = unsynced.map { it.toDomain(jsonListAdapter) }
            val resp = apiService.syncCatches(domainCatches)

            if (resp.isSuccessful && resp.body()?.success == true) {
                catchDao.markAsSynced(unsynced.map { it.id })
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}