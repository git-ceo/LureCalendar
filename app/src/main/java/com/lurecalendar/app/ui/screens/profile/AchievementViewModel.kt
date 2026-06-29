package com.lurecalendar.app.ui.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.data.remote.api.AchievementResponse
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementViewModel @Inject constructor(
    private val apiService: LureCalendarApiService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private fun mockAchievements(): List<AchievementResponse> = listOf(
        AchievementResponse("1", "路亚萌新", "成功记录1条鱼获", true, "1/1"),
        AchievementResponse("2", "结构大师", "在3个不同结构的钓点作钓", true, "3/3"),
        AchievementResponse("3", "巨物捕手", "捕获一条超过5kg的巨物", false, "0/1"),
        AchievementResponse("4", "打卡达人", "累计打卡7天", true, "7/7"),
        AchievementResponse("5", "全能选手", "使用5种不同类型的路亚饵获鱼", false, "2/5")
    )

    var achievements by mutableStateOf<List<AchievementResponse>>(mockAchievements())
    var isLoading by mutableStateOf(false)

    init {
        loadAchievements()
    }

    fun loadAchievements() {
        viewModelScope.launch {
            isLoading = true
            runCatching {
                val phone = authRepository.getPhone().first()
                val list = apiService.getAchievements(phone)
                if (list.isEmpty()) mockAchievements() else list
            }.onSuccess {
                achievements = it
            }.onFailure {
                achievements = mockAchievements()
            }
            isLoading = false
        }
    }
}
