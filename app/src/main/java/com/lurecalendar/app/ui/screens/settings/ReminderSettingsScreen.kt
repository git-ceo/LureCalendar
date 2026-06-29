package com.lurecalendar.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurecalendar.app.ui.theme.SurfaceDark
import com.lurecalendar.app.ui.theme.WaterCyan
import com.lurecalendar.app.ui.theme.glassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReminderSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能出钓提醒", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = SurfaceDark
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 开关
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassBackground()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("开启智能提醒", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("当明日天气符合理想条件时通知我", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { viewModel.updateEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = WaterCyan)
                    )
                }
            }

            if (settings.isEnabled) {
                // 温度范围
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBackground()
                        .padding(16.dp)
                ) {
                    Column {
                        Text("理想温度范围", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${settings.minTemp.toInt()}°C - ${settings.maxTemp.toInt()}°C", color = WaterCyan)
                        RangeSlider(
                            value = settings.minTemp..settings.maxTemp,
                            onValueChange = { viewModel.updateTempRange(it.start, it.endInclusive) },
                            valueRange = 0f..40f,
                            colors = SliderDefaults.colors(thumbColor = WaterCyan, activeTrackColor = WaterCyan)
                        )
                    }
                }

                // 最大风速
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBackground()
                        .padding(16.dp)
                ) {
                    Column {
                        Text("最大风速 (m/s)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("不超过 ${settings.maxWindSpeed.toInt()} m/s", color = WaterCyan)
                        Slider(
                            value = settings.maxWindSpeed,
                            onValueChange = { viewModel.updateWindSpeed(it) },
                            valueRange = 0f..15f,
                            colors = SliderDefaults.colors(thumbColor = WaterCyan, activeTrackColor = WaterCyan)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.testNotification() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WaterCyan)
                ) {
                    Text("立即测试提醒 (模拟)")
                }
            }
        }
    }
}
