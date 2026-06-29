package com.lurecalendar.app.ui.screens.calendar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.lurecalendar.app.domain.model.FishingSpot
import com.lurecalendar.app.ui.theme.WaterCyan

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpotSelectorSheet(
    spots: List<FishingSpot>,
    favoriteSpotIds: Set<String>,
    onSpotSelected: (FishingSpot) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
    userLat: Double? = null,
    userLon: Double? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedFeeType by remember { mutableStateOf<String?>(null) }
    var selectedSpotType by remember { mutableStateOf<String?>(null) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var selectedDistrict by remember { mutableStateOf<String?>(null) }

    val feeTypes = listOf("全部", "免费", "收费")
    val spotTypes = listOf("全部", "水库", "野河", "野塘", "黑坑", "收费钓场", "免费钓场")
    val cities = listOf("全部", "绵阳", "梓潼", "江油", "三台", "盐亭", "德阳", "成都")
    val districtMap = mapOf(
        "绵阳" to listOf("全部", "高新区", "游仙区", "涪城区", "安州区", "经开区"),
        "成都" to listOf("全部", "武侯区", "锦江区", "金牛区", "成华区", "青羊区"),
        "德阳" to listOf("全部", "旌阳区", "罗江区", "广汉市", "什邡市")
    )

    // 过滤+排序
    val filteredSpots = spots
        .filter { spot ->
            (selectedFeeType == null || spot.feeType == selectedFeeType) &&
            (selectedSpotType == null || spot.spotType == selectedSpotType) &&
            (selectedCity == null || spot.city == selectedCity) &&
            (selectedDistrict == null || spot.district == selectedDistrict) &&
            (searchQuery.isEmpty() || spot.name.contains(searchQuery, ignoreCase = true))
        }
        .map { it.copy(isFavorite = favoriteSpotIds.contains(it.id)) }
        .sortedWith(
            compareByDescending<FishingSpot> { it.isFavorite }
                .thenBy { spot ->
                    if (userLat != null && userLon != null) {
                        calculateSimpleDistance(userLat, userLon, spot.latitude, spot.longitude)
                    } else {
                        0.0
                    }
                }
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A2332),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // 标题
            Text(
                text = "选择钓点",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 搜索框 + 筛选按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索钓点名称", color = Color.White.copy(alpha = 0.4f)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = WaterCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = WaterCyan
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = { showFilters = !showFilters }
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "筛选",
                        tint = if (showFilters) WaterCyan else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "筛选",
                        color = if (showFilters) WaterCyan else Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }

            // 筛选区域 - 可展开/收起
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    // 收费类型
                    FilterSection(
                        title = "收费",
                        options = feeTypes,
                        selected = selectedFeeType,
                        onSelect = { selectedFeeType = if (it == "全部") null else it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 钓场类型
                    FilterSection(
                        title = "类型",
                        options = spotTypes,
                        selected = selectedSpotType,
                        onSelect = { selectedSpotType = if (it == "全部") null else it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 城市
                    FilterSection(
                        title = "城市",
                        options = cities,
                        selected = selectedCity,
                        onSelect = {
                            selectedCity = if (it == "全部") null else it
                            selectedDistrict = null // 切换城市时重置区县
                        }
                    )

                    // 区县 - 仅当选择了城市且有对应区县数据时显示
                    val districts = selectedCity?.let { districtMap[it] }
                    if (districts != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterSection(
                            title = "区县",
                            options = districts,
                            selected = selectedDistrict,
                            onSelect = { selectedDistrict = if (it == "全部") null else it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 结果计数
            Text(
                text = "共 ${filteredSpots.size} 个钓点",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 钓点列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredSpots, key = { it.id }) { spot ->
                    SpotListItem(
                        spot = spot,
                        onClick = { onSpotSelected(spot) },
                        onToggleFavorite = { onToggleFavorite(spot.id) }
                    )
                }

                if (filteredSpots.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无匹配的钓点",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = (selected == option) || (selected == null && option == "全部")
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(option) },
                    label = {
                        Text(
                            text = option,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WaterCyan.copy(alpha = 0.2f),
                        selectedLabelColor = WaterCyan,
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.White.copy(alpha = 0.1f),
                        selectedBorderColor = WaterCyan.copy(alpha = 0.5f),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}

@Composable
private fun SpotListItem(
    spot: FishingSpot,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                Color.White.copy(alpha = 0.04f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = spot.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                if (spot.spotType.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = spot.spotType,
                        fontSize = 11.sp,
                        color = WaterCyan.copy(alpha = 0.8f),
                        modifier = Modifier
                            .background(
                                WaterCyan.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row {
                if (!spot.city.isNullOrBlank()) {
                    Text(
                        text = spot.city + if (spot.district.isNotBlank()) " · ${spot.district}" else "",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                if (spot.feeType.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = spot.feeType,
                        fontSize = 11.sp,
                        color = if (spot.feeType == "免费") Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
        }

        // 收藏按钮
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (spot.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "收藏",
                tint = if (spot.isFavorite) Color.Red else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun calculateSimpleDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    return 6371000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}
