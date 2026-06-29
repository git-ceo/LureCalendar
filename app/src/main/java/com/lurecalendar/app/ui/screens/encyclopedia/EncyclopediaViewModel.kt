package com.lurecalendar.app.ui.screens.encyclopedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.data.remote.api.FishEncyclopediaResponse
import com.lurecalendar.app.data.remote.api.FishingGuideResponse
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.remote.api.LureResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 内容百科 ViewModel：聚合鱼种、路亚饵、教程三类内容并持有详情态。
 */
@HiltViewModel
class EncyclopediaViewModel @Inject constructor(
    private val api: LureCalendarApiService
) : ViewModel() {

    private val _state = MutableStateFlow(EncyclopediaUiState())
    val state: StateFlow<EncyclopediaUiState> = _state.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        loadFish()
        loadLures()
        loadGuides()
    }

    fun loadFish(category: String? = null, query: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingFish = true, errorMessage = null) }
            runCatching { api.listFishEncyclopedia(category, query) }
                .onSuccess { list ->
                    _state.update { it.copy(fishList = list, isLoadingFish = false) }
                }.onFailure { e ->
                    _state.update {
                        it.copy(isLoadingFish = false, errorMessage = e.message ?: "鱼种加载失败")
                    }
                }
        }
    }

    fun loadLures(category: String? = null, target: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingLure = true) }
            runCatching { api.listLures(category, target) }
                .onSuccess { list ->
                    _state.update { it.copy(lureList = list, isLoadingLure = false) }
                }.onFailure { e ->
                    _state.update {
                        it.copy(isLoadingLure = false, errorMessage = e.message ?: "饵库加载失败")
                    }
                }
        }
    }

    fun loadGuides(category: String? = null, season: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingGuide = true) }
            runCatching { api.listGuides(category, season) }
                .onSuccess { list ->
                    _state.update { it.copy(guideList = list, isLoadingGuide = false) }
                }.onFailure { e ->
                    _state.update {
                        it.copy(isLoadingGuide = false, errorMessage = e.message ?: "教程加载失败")
                    }
                }
        }
    }

    fun openFishDetail(name: String) {
        viewModelScope.launch {
            runCatching { api.getFishDetail(name) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.update { it.copy(selectedFish = resp.body()?.data) }
                    }
                }
        }
    }

    fun openGuideDetail(id: Int) {
        viewModelScope.launch {
            runCatching { api.getGuideDetail(id) }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.update { it.copy(selectedGuide = resp.body()?.data) }
                    }
                }
        }
    }

    fun closeFishDetail() = _state.update { it.copy(selectedFish = null) }
    fun closeGuideDetail() = _state.update { it.copy(selectedGuide = null) }
    fun openLureDetail(lure: LureResponse) = _state.update { it.copy(selectedLure = lure) }
    fun closeLureDetail() = _state.update { it.copy(selectedLure = null) }

    fun setTab(index: Int) = _state.update { it.copy(currentTab = index) }
    fun setSearchQuery(q: String) = _state.update { it.copy(searchQuery = q) }

    private inline fun <T> MutableStateFlow<T>.update(block: (T) -> T) {
        value = block(value)
    }
}

data class EncyclopediaUiState(
    val currentTab: Int = 0,
    val searchQuery: String = "",
    val fishList: List<FishEncyclopediaResponse> = emptyList(),
    val lureList: List<LureResponse> = emptyList(),
    val guideList: List<FishingGuideResponse> = emptyList(),
    val isLoadingFish: Boolean = false,
    val isLoadingLure: Boolean = false,
    val isLoadingGuide: Boolean = false,
    val selectedFish: FishEncyclopediaResponse? = null,
    val selectedLure: LureResponse? = null,
    val selectedGuide: FishingGuideResponse? = null,
    val errorMessage: String? = null
)
