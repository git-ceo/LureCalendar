package com.lurecalendar.app.ui.screens.spots

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurecalendar.app.ui.screens.map.MapScreen
import com.lurecalendar.app.ui.theme.TextSecondaryLight
import com.lurecalendar.app.ui.theme.WaterCyan
import com.lurecalendar.app.ui.theme.DeepSeaBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotManagerScreen(
    onNavigateToMap: () -> Unit = {},
    onAddCatch: (String) -> Unit = {},
    viewModel: SpotManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentView by remember { mutableStateOf(0) } // 0=地图, 1=列表
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部切换栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = currentView == 0,
                onClick = { currentView = 0 },
                label = { Text("地图") },
                leadingIcon = {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WaterCyan,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = currentView == 1,
                onClick = { currentView = 1 },
                label = { Text("列表") },
                leadingIcon = {
                    Icon(Icons.Default.Place, null, modifier = Modifier.size(18.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WaterCyan,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            if (currentView == 1) {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加钓点",
                        tint = Color.White
                    )
                }
            }
        }
    
        // 内容区域 — Box叠加，地图始终保留在composition树中避免重建闪烁
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            // 地图层：始终存在，仅通过 graphicsLayer 控制透明度
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (currentView == 0) 1f else 0f }
                    .then(
                        if (currentView != 0) Modifier.pointerInput(Unit) {
                            // 不可见时拦截所有触摸事件，避免误触地图
                            awaitPointerEventScope {
                                while (true) { awaitPointerEvent() }
                            }
                        } else Modifier
                    )
            ) {
                MapScreen(
                    onAddCatch = onAddCatch,
                    isEmbedded = true
                )
            }

            // 列表层：仅在选中时显示
            if (currentView == 1) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SnackbarHost(
                        snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.spots.isEmpty()) {
                        EmptySpotState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.spots,
                                key = { it.spot.id }
                            ) { spotState ->
                                SpotWeatherCard(
                                    state = spotState,
                                    onRefresh = { viewModel.refreshSpot(spotState.spot) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加钓点对话框
    if (showAddDialog) {
        var spotName by remember { mutableStateOf("") }
        var spotLocation by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加钓点", color = Color.White) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = spotName,
                        onValueChange = { spotName = it },
                        label = { Text("钓点名称", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("例如：南河大桥下", color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = WaterCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            cursorColor = WaterCyan,
                            focusedLabelColor = WaterCyan,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    OutlinedTextField(
                        value = spotLocation,
                        onValueChange = { spotLocation = it },
                        label = { Text("位置描述（可选）", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("例如：绵阳市涪城区南河路", color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = WaterCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            cursorColor = WaterCyan,
                            focusedLabelColor = WaterCyan,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("功能开发中，请前往地图页长按添加钓点")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WaterCyan)
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
private fun EmptySpotState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondaryLight
            )
            Text(
                text = "\u6682\u65e0\u6536\u85cf\u9493\u70b9",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondaryLight
            )
            Text(
                text = "切换到地图视图长按添加钓点\n查看实时天气与路亚评分",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryLight,
                textAlign = TextAlign.Center
            )
        }
    }
}
