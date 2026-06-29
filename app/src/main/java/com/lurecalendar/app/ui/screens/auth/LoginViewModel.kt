package com.lurecalendar.app.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var phone by mutableStateOf("")
    var password by mutableStateOf("")
    var username by mutableStateOf("")
    var isRegisterMode by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    private val _uiEvent = MutableSharedFlow<AuthUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onAuthAction() {
        if (phone.isBlank() || password.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.ShowError("手机号或密码不能为空")) }
            return
        }

        isLoading = true
        viewModelScope.launch {
            val result = if (isRegisterMode) {
                authRepository.register(phone, password, username.ifBlank { "钓鱼佬_${phone.takeLast(4)}" })
            } else {
                authRepository.login(phone, password)
            }

            result.onSuccess {
                _uiEvent.emit(AuthUiEvent.AuthSuccess)
            }.onFailure {
                _uiEvent.emit(AuthUiEvent.ShowError(it.message ?: "操作失败"))
            }
            isLoading = false
        }
    }

    fun toggleMode() {
        isRegisterMode = !isRegisterMode
    }
}

sealed class AuthUiEvent {
    object AuthSuccess : AuthUiEvent()
    data class ShowError(val message: String) : AuthUiEvent()
}
