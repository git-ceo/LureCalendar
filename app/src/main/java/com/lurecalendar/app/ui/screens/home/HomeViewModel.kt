package com.lurecalendar.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.lurecalendar.app.worker.SyncWorker
import com.lurecalendar.app.domain.model.FishingSpot
import com.lurecalendar.app.domain.repository.FishingSpotRepository
import com.lurecalendar.app.domain.repository.SyncRepository
import com.lurecalendar.app.worker.LocationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val authRepository: com.lurecalendar.app.domain.repository.AuthRepository,
    private val fishingSpotRepository: FishingSpotRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val _hotSpots = MutableStateFlow<List<FishingSpot>>(emptyList())
    val hotSpots: StateFlow<List<FishingSpot>> = _hotSpots.asStateFlow()

    init {
        startLocationTracking()
        startSyncWorker()
        performInitialSync()
        loadHotSpots()
    }

    /**
     * 应用启动时立即从后端拉一次全量数据（钉点/鱼获等）。
     * Room 本地库更新后，loadHotSpots() 订阅的 Flow 会自动推送新数据，
     * 热门钓场列表无需手动点同步按钮即可出现。
     * 静默失败（如离线），UI 回跟到本地缓存。
     */
    private fun performInitialSync() {
        viewModelScope.launch {
            runCatching { syncRepository.performFullSync() }
        }
    }

    private fun loadHotSpots() {
        viewModelScope.launch {
            fishingSpotRepository.getAllSpots().collectLatest { spots ->
                // 选取前6个作为热门展示，优先选择水库和黑坑类型
                val hot = spots
                    .filter { it.name.isNotBlank() }
                    .sortedByDescending { it.targetSpecies?.split(",")?.size ?: 0 }
                    .take(6)
                _hotSpots.value = hot
            }
        }
    }

    private fun startSyncWorker() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            1, java.util.concurrent.TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "data_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun startLocationTracking() {
        val locationRequest = PeriodicWorkRequestBuilder<LocationWorker>(
            3, TimeUnit.MINUTES
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            "location_tracking",
            ExistingPeriodicWorkPolicy.KEEP,
            locationRequest
        )
    }

    fun performSync() {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Loading
            syncRepository.performFullSync()
                .onSuccess { summary ->
                    _syncState.value = SyncUiState.Success(summary.message)
                }
                .onFailure { error ->
                    _syncState.value = SyncUiState.Error(error.message ?: "未知错误")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncUiState.Idle
    }
}

sealed class SyncUiState {
    object Idle : SyncUiState()
    object Loading : SyncUiState()
    data class Success(val message: String) : SyncUiState()
    data class Error(val message: String) : SyncUiState()
}
