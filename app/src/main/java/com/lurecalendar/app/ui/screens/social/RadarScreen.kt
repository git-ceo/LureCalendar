package com.lurecalendar.app.ui.screens.social

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.domain.repository.AuthRepository
import com.lurecalendar.app.ui.theme.SurfaceDark
import com.lurecalendar.app.ui.theme.WaterCyan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadarViewModel @Inject constructor(
    private val apiService: LureCalendarApiService,
    private val authRepository: AuthRepository
) : ViewModel() {
    var myCode by mutableStateOf<String?>(null)
    var timeLeft by mutableStateOf(0)
    var targetCode by mutableStateOf("")
    var isAdding by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    fun startRadar() {
        viewModelScope.launch {
            val phone = authRepository.getPhone().first()
            runCatching { apiService.startRadar(phone) }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    myCode = resp.body()?.code
                    timeLeft = resp.body()?.expires_in ?: 60
                    startTimer()
                }
            }
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            myCode = null
        }
    }

    fun addFriend() {
        if (targetCode.length != 6) return
        viewModelScope.launch {
            isAdding = true
            val phone = authRepository.getPhone().first()
            runCatching { apiService.addRadar(phone, mapOf("code" to targetCode)) }.onSuccess { resp ->
                message = if (resp.isSuccessful) "添加成功！" else "码无效或已过期"
            }
            isAdding = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    onNavigateBack: () -> Unit,
    viewModel: RadarViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("雷达面对面", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
            )
        },
        containerColor = SurfaceDark
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            RadarAnimation()
            
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (viewModel.myCode == null) {
                    Button(
                        onClick = { viewModel.startRadar() },
                        modifier = Modifier.size(160.dp),
                        shape = RoundedCornerShape(80.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WaterCyan.copy(alpha = 0.8f))
                    ) {
                        Text("生成我的\n识别码", textAlign = TextAlign.Center, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("我的识别码", color = Color.Gray)
                    Text(viewModel.myCode!!, fontSize = 48.sp, fontWeight = FontWeight.Black, color = WaterCyan, letterSpacing = 8.sp)
                    Text("有效期剩 ${viewModel.timeLeft} 秒", color = Color.Gray, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(64.dp))

                OutlinedTextField(
                    value = viewModel.targetCode,
                    onValueChange = { if (it.length <= 6) viewModel.targetCode = it },
                    label = { Text("输入对方识别码") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.addFriend() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = viewModel.targetCode.length == 6 && !viewModel.isAdding,
                    colors = ButtonDefaults.buttonColors(containerColor = WaterCyan)
                ) {
                    if (viewModel.isAdding) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    else Text("确 认 添 加", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                viewModel.message?.let {
                    Text(it, color = if (it.contains("成功")) WaterCyan else Color.Red, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}
