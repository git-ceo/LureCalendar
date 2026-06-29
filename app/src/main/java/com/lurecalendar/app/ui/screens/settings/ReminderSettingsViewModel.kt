package com.lurecalendar.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.data.local.entity.ReminderSettingsEntity
import com.lurecalendar.app.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val repository: ReminderRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(ReminderSettingsEntity())
    val settings: StateFlow<ReminderSettingsEntity> = _settings

    init {
        viewModelScope.launch {
            repository.getSettings().collectLatest {
                it?.let { _settings.value = it }
            }
        }
    }

    fun updateEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(isEnabled = enabled)
        save()
    }

    fun updateTempRange(min: Float, max: Float) {
        _settings.value = _settings.value.copy(minTemp = min, maxTemp = max)
        save()
    }

    fun updateWindSpeed(speed: Float) {
        _settings.value = _settings.value.copy(maxWindSpeed = speed)
        save()
    }

    fun testNotification() {
        viewModelScope.launch {
            repository.checkAndNotify()
        }
    }

    private fun save() {
        viewModelScope.launch {
            repository.saveSettings(_settings.value)
        }
    }
}
