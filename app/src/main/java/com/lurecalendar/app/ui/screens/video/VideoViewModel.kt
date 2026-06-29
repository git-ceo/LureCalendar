package com.lurecalendar.app.ui.screens.video

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.remote.api.VideoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val apiService: LureCalendarApiService
) : ViewModel() {

    var videos by mutableStateOf<List<VideoResponse>>(emptyList())
    var isLoading by mutableStateOf(false)

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            isLoading = true
            runCatching {
                apiService.getTechniqueVideos()
            }.onSuccess {
                videos = it
            }
            isLoading = false
        }
    }
}
