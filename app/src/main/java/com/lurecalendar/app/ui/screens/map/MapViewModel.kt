package com.lurecalendar.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.model.LatLng
import com.lurecalendar.app.common.location.AmapLocationProvider
import com.lurecalendar.app.domain.model.FishingSpot
import com.lurecalendar.app.domain.repository.FishingSpotRepository
import com.lurecalendar.app.domain.repository.WeatherRepository
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.remote.api.SpotLeaderboardEntry
import com.lurecalendar.app.data.remote.api.AnglerLocationResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewSpotDraft(
    val latLng: LatLng,
    val name: String = "",
    val river: String = "",
    val city: String = "",
    val locationDetail: String = "",
    val waterType: String = "河流",
    val structure: String = "岩石",
    val depth: String = "",
    val targetSpecies: String = "",
    val lureTypes: String = "",
    val bestSeason: String = "",
    val note: String = ""
)

data class MapUiState(
    val spots: List<FishingSpot> = emptyList(),
    val currentLocation: LatLng? = null,
    val currentCity: String? = null,
    val isLocating: Boolean = false,
    val draft: NewSpotDraft? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val selectedSpot: FishingSpot? = null,
    val editingSpotId: String? = null,
    val initialLocationSet: Boolean = false,
    val lastCenteredLocation: LatLng? = null,
    val centerTrigger: Long = 0L,
    val leaderboard: List<SpotLeaderboardEntry> = emptyList(),
    val isLoadingLeaderboard: Boolean = false,
    val anglers: List<AnglerLocationResponse> = emptyList(),
    val isAdmin: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val fishingSpotRepository: FishingSpotRepository,
    private val weatherRepository: WeatherRepository,
    private val locationProvider: AmapLocationProvider,
    private val apiService: LureCalendarApiService,
    private val authRepository: com.lurecalendar.app.domain.repository.AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            fishingSpotRepository.getAllSpots().collect { list ->
                _state.update { it.copy(spots = list) }
            }
        }
        checkAdminStatus()
        startAnglerTracking()
    }

    private fun checkAdminStatus() {
        viewModelScope.launch {
            runCatching {
                val phone = authRepository.getPhone().first()
                val resp = apiService.getProfile(phone)
                if (resp.isSuccessful) {
                    _state.update { it.copy(isAdmin = phone == "18381699703") }
                } else {
                    _state.update { it.copy(isAdmin = false) }
                }
            }.onFailure {
                _state.update { it.copy(isAdmin = false) }
            }
        }
    }

    private fun startAnglerTracking() {
        viewModelScope.launch {
            while (true) {
                loadAnglers()
                kotlinx.coroutines.delay(30_000) // 30秒刷新一次钓友位置
            }
        }
    }

    private fun loadAnglers() {
        viewModelScope.launch {
            runCatching {
                val phone = authRepository.getPhone().first()
                val list = apiService.getAnglerLocations(phone)
                if (list.isEmpty()) {
                    // Mock data if empty
                    listOf(
                        AnglerLocationResponse("18381699701", "路亚新手小白", null, 31.467, 104.741, System.currentTimeMillis()),
                        AnglerLocationResponse("18381699702", "绵阳第一鳜", null, 31.472, 104.755, System.currentTimeMillis()),
                        AnglerLocationResponse("18381699703", "空军大司令", null, 31.455, 104.762, System.currentTimeMillis())
                    )
                } else list
            }.onSuccess { list ->
                _state.update { it.copy(anglers = list) }
            }.onFailure {
                // Mock data on failure
                val mockList = listOf(
                    AnglerLocationResponse("18381699701", "路亚新手小白", null, 31.467, 104.741, System.currentTimeMillis()),
                    AnglerLocationResponse("18381699702", "绵阳第一鳜", null, 31.472, 104.755, System.currentTimeMillis())
                )
                _state.update { it.copy(anglers = mockList) }
            }
        }
    }

    fun selectSpot(spot: FishingSpot?) {
        _state.update { 
            it.copy(
                selectedSpot = spot,
                lastCenteredLocation = spot?.let { s -> LatLng(s.latitude, s.longitude) } ?: it.lastCenteredLocation,
                centerTrigger = if (spot != null) System.currentTimeMillis() else it.centerTrigger,
                leaderboard = emptyList() // 重置排行榜
            )
        }
        if (spot != null) {
            loadLeaderboard(spot.id)
        }
    }

    fun showMessage(message: String) {
        _state.update { it.copy(errorMessage = message) }
    }

    private fun loadLeaderboard(spotId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingLeaderboard = true) }
            runCatching {
                val list = apiService.getSpotLeaderboard(spotId)
                if (list.isEmpty()) {
                    // Mock data
                    listOf(
                        SpotLeaderboardEntry("三台鲁班大师", null, "鳜鱼", 2500f, 45f, null, System.currentTimeMillis()),
                        SpotLeaderboardEntry("路亚追风少年", null, "翘嘴", 3200f, 65f, null, System.currentTimeMillis()),
                        SpotLeaderboardEntry("梓潼老张", null, "黑鱼", 4100f, 55f, null, System.currentTimeMillis())
                    )
                } else list
            }.onSuccess { list ->
                _state.update { it.copy(leaderboard = list, isLoadingLeaderboard = false) }
            }.onFailure {
                // Mock data on failure
                val mockList = listOf(
                    SpotLeaderboardEntry("模拟用户A", null, "翘嘴", 1500f, 40f, null, System.currentTimeMillis())
                )
                _state.update { it.copy(leaderboard = mockList, isLoadingLeaderboard = false) }
            }
        }
    }

    fun editSpot(spot: FishingSpot) {
        _state.update { 
            it.copy(
                selectedSpot = null,
                editingSpotId = spot.id,
                draft = NewSpotDraft(
                    latLng = LatLng(spot.latitude, spot.longitude),
                    name = spot.name,
                    river = spot.river ?: "",
                    city = spot.city ?: "",
                    locationDetail = spot.locationDetail ?: "",
                    waterType = spot.waterType,
                    structure = spot.structure,
                    depth = spot.depth?.toString() ?: "",
                    targetSpecies = spot.targetSpecies ?: "",
                    lureTypes = spot.lureTypes ?: "",
                    bestSeason = spot.bestSeason ?: "",
                    note = spot.note ?: ""
                )
            )
        }
    }

    fun locateOnce() {
        if (_state.value.isLocating) return
        viewModelScope.launch {
            _state.update { it.copy(isLocating = true, errorMessage = null) }
            runCatching { locationProvider.getOnceLocation() }
                .onSuccess { loc ->
                    var city = loc.city
                    if (city.isNullOrBlank()) {
                        // Fallback: Use weather repository to resolve city name if AMap reverse-geocoding is missing
                        val resolved = weatherRepository.resolveLocation(loc.latitude, loc.longitude).getOrNull()
                        city = resolved?.adm2 ?: resolved?.name
                    }
                    
                    _state.update {
                        it.copy(
                            isLocating = false,
                            currentLocation = LatLng(loc.latitude, loc.longitude),
                            currentCity = city,
                            initialLocationSet = true,
                            lastCenteredLocation = LatLng(loc.latitude, loc.longitude),
                            centerTrigger = System.currentTimeMillis()
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLocating = false, errorMessage = e.message ?: "定位失败") }
                }
        }
    }

    fun onMapLongClick(latLng: LatLng) {
        _state.update { it.copy(draft = NewSpotDraft(latLng = latLng), errorMessage = null) }
        viewModelScope.launch {
            val resolved = weatherRepository.resolveLocation(latLng.latitude, latLng.longitude).getOrNull()
            if (resolved != null) {
                _state.update { s ->
                    val d = s.draft ?: return@update s
                    val city = resolved.adm2 ?: resolved.name
                    s.copy(
                        draft = d.copy(city = city),
                        errorMessage = null
                    )
                }
            } else {
                _state.update { s ->
                    val d = s.draft ?: return@update s
                    val city = s.currentCity
                    if (!city.isNullOrBlank() && d.city.isBlank()) {
                        s.copy(draft = d.copy(city = city))
                    } else s
                }
            }
        }
    }

    fun dismissDraft() {
        _state.update { it.copy(draft = null, editingSpotId = null, errorMessage = null) }
    }

    fun updateDraft(transform: (NewSpotDraft) -> NewSpotDraft) {
        _state.update { s ->
            val d = s.draft ?: return@update s
            s.copy(draft = transform(d))
        }
    }

    fun saveDraft() {
        val draft = _state.value.draft ?: return
        val editingId = _state.value.editingSpotId
        
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val resolved = if (draft.city.isNotBlank()) {
                weatherRepository.resolveLocation(draft.city).getOrNull()
            } else {
                weatherRepository.resolveLocation(draft.latLng.latitude, draft.latLng.longitude).getOrNull()
            }

            val now = System.currentTimeMillis()
            val existingSpot = editingId?.let { id -> _state.value.spots.find { it.id == id } }
            
            val spot = FishingSpot(
                id = editingId ?: UUID.randomUUID().toString(),
                name = draft.name.ifBlank { "未命名钓点" },
                latitude = draft.latLng.latitude,
                longitude = draft.latLng.longitude,
                river = draft.river.ifBlank { null },
                city = draft.city.ifBlank { resolved?.adm2 ?: resolved?.name },
                locationDetail = draft.locationDetail.ifBlank { null },
                qWeatherLocationId = resolved?.id,
                waterType = draft.waterType,
                structure = draft.structure,
                depth = draft.depth.toFloatOrNull(),
                targetSpecies = draft.targetSpecies.ifBlank { null },
                lureTypes = draft.lureTypes.ifBlank { null },
                bestSeason = draft.bestSeason.ifBlank { null },
                note = draft.note.ifBlank { null },
                photos = existingSpot?.photos ?: emptyList(),
                createTime = existingSpot?.createTime ?: now,
                updateTime = now
            )

            runCatching { fishingSpotRepository.saveSpot(spot) }
                .onSuccess {
                    _state.update { it.copy(isSaving = false, draft = null, editingSpotId = null, errorMessage = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "保存失败") }
                }
        }
    }
}
