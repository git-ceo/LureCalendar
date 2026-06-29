package com.lurecalendar.app.ui.screens.catchrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.domain.model.FishingSpot
import com.lurecalendar.app.domain.repository.FishingSpotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatchRecordListUiState(
    val spots: List<FishingSpot> = emptyList(),
    val showSpotPicker: Boolean = false
)

@HiltViewModel
class CatchRecordViewModel @Inject constructor(
    private val fishingSpotRepository: FishingSpotRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CatchRecordListUiState())
    val state: StateFlow<CatchRecordListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            fishingSpotRepository.getAllSpots().collect { list ->
                _state.update { it.copy(spots = list) }
            }
        }
    }

    fun openSpotPicker() {
        _state.update { it.copy(showSpotPicker = true) }
    }

    fun closeSpotPicker() {
        _state.update { it.copy(showSpotPicker = false) }
    }
}

