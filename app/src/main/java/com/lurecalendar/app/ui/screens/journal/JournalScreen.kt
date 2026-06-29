package com.lurecalendar.app.ui.screens.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurecalendar.app.domain.model.CatchRecord
import com.lurecalendar.app.ui.theme.WaterCyan
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onNavigateToCatchForm: () -> Unit = {},
    viewModel: JournalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("鱼获记录", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.showFilter() }) {
                        Icon(Icons.Default.FilterList, contentDescription = "筛选")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToCatchForm() },
                containerColor = WaterCyan
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加记录", tint = Color.White)
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WaterCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // 1. 统计概览卡片
                item {
                    StatsOverviewCard(stats = state.stats)
                }

                // 2. 筛选指示器
                val filter = state.filter
                if (filter.species != null || filter.lureType != null || filter.timeRange != TimeRange.ALL) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "已筛选: ${state.filteredRecords.size}条记录",
                                style = MaterialTheme.typography.bodySmall,
                                color = WaterCyan
                            )
                            TextButton(onClick = { viewModel.clearFilter() }) {
                                Text("清除筛选", color = WaterCyan)
                            }
                        }
                    }
                }

                // 3. 鱼获列表
                if (state.filteredRecords.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "暂无鱼获记录",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "点击 + 开始记录你的鱼获",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                } else {
                    items(state.filteredRecords, key = { it.id }) { record ->
                        JournalListItem(record = record)
                    }
                }
            }
        }
    }

    // Filter Dialog
    if (state.showFilterDialog) {
        FilterDialog(
            currentFilter = state.filter,
            availableSpecies = viewModel.availableSpecies,
            availableLureTypes = viewModel.availableLureTypes,
            onApply = { viewModel.updateFilter(it) },
            onDismiss = { viewModel.hideFilter() },
            onClear = { viewModel.clearFilter() }
        )
    }
}

@Composable
fun StatsOverviewCard(stats: JournalStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "统计总览",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "总尾数", value = stats.totalCount.toString())
                StatItem(label = "总重量", value = String.format("%.1fkg", stats.totalWeightKg))
                StatItem(label = "最大", value = String.format("%.1fkg", stats.maxSingleKg))
                StatItem(label = "出钓天数", value = stats.tripDays.toString())
            }
            if (stats.topSpecies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("TOP 物种", fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(6.dp))
                stats.topSpecies.forEach { (name, count) ->
                    Text(
                        "$name · ${count}尾",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WaterCyan)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
fun JournalListItem(record: CatchRecord) {
    val dateTime = remember(record.catchTime) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(record.catchTime), ZoneId.systemDefault())
    }
    val dateText = dateTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Date + Weather
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                if (record.temperature != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${record.temperature?.toInt()}°C",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Species + Size/Weight
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.species,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = WaterCyan
                )
                Spacer(modifier = Modifier.width(12.dp))
                val sizeInfo = buildList {
                    record.length?.let { add("${it.toInt()}cm") }
                    record.weight?.let { add("${it.toInt()}g") }
                    if (record.count > 1) add("${record.count}尾")
                }.joinToString(" / ")
                if (sizeInfo.isNotBlank()) {
                    Text(
                        text = sizeInfo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Lure + Rig
            val lureRigText = buildList {
                record.lureType?.let { add(it) }
                record.rigType?.let { add(it) }
                record.bait?.let { if (record.lureType == null) add("饵: $it") }
            }.joinToString(" · ")
            if (lureRigText.isNotBlank()) {
                Text(
                    text = lureRigText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WaterCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Row 4: Structure zone tags
            val tags = buildList {
                record.structureZone?.let { add(it) }
                record.waterClarity?.let { add(it) }
                record.windShoreRelation?.let { add(it) }
            }
            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = WaterCyan.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = WaterCyan
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Row 5: Location
            val location = listOfNotNull(record.river, record.city).joinToString(" · ")
            if (location.isNotBlank()) {
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    currentFilter: JournalFilter,
    availableSpecies: List<String>,
    availableLureTypes: List<String>,
    onApply: (JournalFilter) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    var selectedSpecies by remember { mutableStateOf(currentFilter.species) }
    var selectedLureType by remember { mutableStateOf(currentFilter.lureType) }
    var selectedTimeRange by remember { mutableStateOf(currentFilter.timeRange) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选日志") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Time range
                Text("时间范围", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeRange.entries.forEach { range ->
                        FilterChip(
                            selected = selectedTimeRange == range,
                            onClick = { selectedTimeRange = range },
                            label = { Text(range.label, fontSize = 12.sp) }
                        )
                    }
                }

                // Species filter
                if (availableSpecies.isNotEmpty()) {
                    Text("鱼种", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableSpecies.take(6).forEach { species ->
                            FilterChip(
                                selected = selectedSpecies == species,
                                onClick = {
                                    selectedSpecies = if (selectedSpecies == species) null else species
                                },
                                label = { Text(species, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Lure type filter
                if (availableLureTypes.isNotEmpty()) {
                    Text("饵料类型", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableLureTypes.take(6).forEach { lure ->
                            FilterChip(
                                selected = selectedLureType == lure,
                                onClick = {
                                    selectedLureType = if (selectedLureType == lure) null else lure
                                },
                                label = { Text(lure, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClear) { Text("清除") }
                Button(onClick = {
                    onApply(
                        JournalFilter(
                            species = selectedSpecies,
                            lureType = selectedLureType,
                            timeRange = selectedTimeRange
                        )
                    )
                }) { Text("应用") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
