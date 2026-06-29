package com.lurecalendar.app.ui.screens.catchrecord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurecalendar.app.ui.theme.DeepSeaBlue
import com.lurecalendar.app.ui.theme.NatureGreen
import com.lurecalendar.app.ui.theme.WaterCyan

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatchRecordScreen(
    onNavigateBack: () -> Unit,
    onAddCatchForSpot: (String) -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToGearStats: (String) -> Unit = {},
    viewModel: CatchRecordViewModel = hiltViewModel(),
    listViewModel: CatchRecordListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState by listViewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("鱼获记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (listState.items.isNotEmpty()) {
                        IconButton(
                            onClick = { listViewModel.exportPdfReport() },
                            enabled = !listViewModel.isExporting
                        ) {
                            if (listViewModel.isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Default.Share, contentDescription = "Export")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSeaBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openSpotPicker() },
                containerColor = NatureGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Record", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "统计总览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CatchStatisticsCard(listState.stats)
            }
            item {
                Text(
                    text = "记录列表",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            items(listState.items.size) { index ->
                CatchRecordListItem(listState.items[index], onNavigateToGearStats)
            }
        }
    }

    if (state.showSpotPicker) {
        var searchQuery by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::closeSpotPicker,
            title = { Text("选择钓点") },
            text = {
                if (state.spots.isEmpty()) {
                    Text("还没有钓点，先去地图长按新增一个吧")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索钓点...", color = Color.White.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = WaterCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                cursorColor = WaterCyan
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清除", tint = Color.White.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        )

                        val filteredSpots = state.spots.filter {
                            searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredSpots.forEach { spot ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            viewModel.closeSpotPicker()
                                            onAddCatchForSpot(spot.id)
                                        },
                                        label = { Text(spot.name, maxLines = 1, color = Color.White) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            selectedContainerColor = WaterCyan.copy(alpha = 0.2f),
                                            selectedLabelColor = WaterCyan
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            selectedBorderColor = WaterCyan,
                                            enabled = true,
                                            selected = false
                                        )
                                    )
                                }
                            }

                            if (filteredSpots.isEmpty() && searchQuery.isNotEmpty()) {
                                Text(
                                    "未找到匹配的钓点",
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (state.spots.isEmpty()) {
                    Button(onClick = {
                        viewModel.closeSpotPicker()
                        onNavigateToMap()
                    }) { Text("去新增钓点") }
                } else {
                    Button(onClick = viewModel::closeSpotPicker) { Text("关闭") }
                }
            }
        )
    }
}

@Composable
fun CatchStatisticsCard(stats: CatchStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "总尾数", value = stats.totalCount.toString())
                StatItem(label = "总重量", value = String.format("%.1fkg", stats.totalWeightKg))
                StatItem(label = "最大单尾", value = String.format("%.1fkg", stats.maxSingleKg))
                StatItem(label = "出钓天数", value = stats.tripDays.toString())
            }
            if (stats.topSpecies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("TOP 物种", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                stats.topSpecies.forEach { (name, count) ->
                    Text("$name · $count", color = Color.DarkGray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepSeaBlue)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun CatchRecordListItem(item: CatchListItemUi, onNavigateToGearStats: (String) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.species, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(item.dateText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                val sizeText = listOfNotNull(item.lengthCm, item.weightKg).joinToString(" • ")
                if (sizeText.isNotBlank()) {
                    Text(sizeText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                val meta = listOfNotNull(item.spotName, item.bait).joinToString(" • ")
                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                if (item.rod != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistChip(
                        onClick = { onNavigateToGearStats(item.rod) },
                        label = { Text(item.rod, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = NatureGreen)
                    )
                }
            }
        }
    }
}
