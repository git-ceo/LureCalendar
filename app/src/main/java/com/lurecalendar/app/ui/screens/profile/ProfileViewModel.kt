package com.lurecalendar.app.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.remote.api.UpdateProfileRequest
import com.lurecalendar.app.data.remote.api.UserProfileResponse
import com.lurecalendar.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val apiService: LureCalendarApiService
) : ViewModel() {

    enum class UserImageTarget {
        Avatar,
        Background
    }

    var profileData by mutableStateOf<UserProfileResponse?>(null)
    var isEditingSignature by mutableStateOf(false)
    var editedSignature by mutableStateOf("")
    
    // 新增：图片更新相关状态
    var imageUpdateTarget by mutableStateOf<UserImageTarget?>(null)

    init {
        loadProfile()
    }

    fun showImageUpdateDialog(target: UserImageTarget) {
        imageUpdateTarget = target
    }

    fun updateUserImage(target: UserImageTarget, url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val phone = authRepository.getPhone().first()
                val request = if (target == UserImageTarget.Avatar) {
                    UpdateProfileRequest(phone = phone, avatarUrl = url)
                } else {
                    UpdateProfileRequest(phone = phone, backgroundUrl = url)
                }
                val resp = apiService.updateProfile(request)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    loadProfile()
                }
            }
        }
    }

    fun uploadAndSetUserImage(target: UserImageTarget, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                // 1. 将 Uri 转为临时文件
                val file = uriToFile(uri) ?: return@launch
                
                // 2. 准备 MultipartBody
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                
                // 3. 上传
                val uploadResp = apiService.uploadImage(body)
                val imageUrl = uploadResp.body()?.url
                
                if (uploadResp.isSuccessful && imageUrl != null) {
                    // 4. 更新用户信息
                    updateUserImage(target, imageUrl)
                }
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun loadProfile() {
        viewModelScope.launch {
            runCatching {
                val phone = authRepository.getPhone().first()
                val resp = apiService.getProfile(phone)
                if (resp.isSuccessful) {
                    profileData = resp.body()
                    editedSignature = profileData?.signature ?: ""
                }
            }
        }
    }

    fun updateSignature() {
        viewModelScope.launch {
            runCatching {
                val phone = authRepository.getPhone().first()
                val resp = apiService.updateProfile(UpdateProfileRequest(phone = phone, signature = editedSignature))
                if (resp.isSuccessful && resp.body()?.success == true) {
                    isEditingSignature = false
                    loadProfile()
                }
            }
        }
    }
}
