package com.lurecalendar.app.ui.screens.encyclopedia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lurecalendar.app.data.remote.api.FishEncyclopediaResponse
import com.lurecalendar.app.data.remote.api.FishingGuideResponse
import com.lurecalendar.app.data.remote.api.LureResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaScreen(
    onNavigateBack: () -> Unit,
    viewModel: EncyclopediaViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("路亚学院 · 知识百科", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = state.currentTab) {
                listOf(
                    Triple("鱼种百科", Icons.Default.Pets, 0),
                    Triple("路亚饵库", Icons.Default.Phishing, 1),
                    Triple("钓法教程", Icons.Default.MenuBook, 2),
                ).forEach { (label, icon, idx) ->
                    Tab(
                        selected = state.currentTab == idx,
                        onClick = { viewModel.setTab(idx) },
                        text = { Text(label) },
                        icon = { Icon(icon, contentDescription = label) }
                    )
                }
            }

            when (state.currentTab) {
                0 -> FishTab(state, viewModel)
                1 -> LureTab(state, viewModel)
                2 -> GuideTab(state, viewModel)
            }
        }
    }

    state.selectedFish?.let { FishDetailDialog(it, onClose = viewModel::closeFishDetail) }
    state.selectedLure?.let { LureDetailDialog(it, onClose = viewModel::closeLureDetail) }
    state.selectedGuide?.let { GuideDetailDialog(it, onClose = viewModel::closeGuideDetail) }
}

@Composable
private fun FishTab(state: EncyclopediaUiState, vm: EncyclopediaViewModel) {
    Column {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = {
                vm.setSearchQuery(it)
                vm.loadFish(query = it.takeIf { q -> q.isNotBlank() })
            },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            placeholder = { Text("搜索鱼种、别名...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        if (state.isLoadingFish && state.fishList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.fishList, key = { it.id }) { fish ->
                    FishCard(fish, onClick = { vm.openFishDetail(fish.name) })
                }
            }
        }
    }
}

@Composable
private fun FishCard(fish: FishEncyclopediaResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            if (!fish.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = fish.imageUrl,
                    contentDescription = fish.name,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(120.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐟", fontSize = 48.sp)
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(fish.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    fish.category ?: "对象鱼",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                fish.bestSeason?.let {
                    Spacer(Modifier.height(2.dp))
                    Text("旺季: $it", fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun LureTab(state: EncyclopediaUiState, vm: EncyclopediaViewModel) {
    val categories = remember(state.lureList) {
        listOf("全部") + state.lureList.mapNotNull { it.category }.distinct()
    }
    var selectedCategory by remember { mutableStateOf("全部") }

    Column {
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            edgePadding = 12.dp
        ) {
            categories.forEach { c ->
                Tab(
                    selected = c == selectedCategory,
                    onClick = {
                        selectedCategory = c
                        vm.loadLures(category = c.takeIf { it != "全部" })
                    },
                    text = { Text(c, fontSize = 13.sp) }
                )
            }
        }
        if (state.isLoadingLure && state.lureList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.lureList, key = { it.id }) { lure ->
                    LureCard(lure, onClick = { vm.openLureDetail(lure) })
                }
            }
        }
    }
}

@Composable
private fun LureCard(lure: LureResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!lure.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = lure.imageUrl, contentDescription = lure.name,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) { Text("🎣", fontSize = 32.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lure.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "  ${lure.category ?: "饵"}  ",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                lure.subType?.let {
                    Text(it, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(4.dp))
                lure.swimLayer?.let {
                    Text("泳层: $it · 重量: ${lure.weightRange ?: "-"}", fontSize = 11.sp)
                }
                lure.targetSpecies?.let {
                    Text("适用: $it", fontSize = 11.sp, maxLines = 1, color = Color(0xFF1B7F4F))
                }
            }
        }
    }
}

@Composable
private fun GuideTab(state: EncyclopediaUiState, vm: EncyclopediaViewModel) {
    val categories = listOf("全部", "入门", "进阶", "钓法", "季节", "装备")
    var selected by remember { mutableStateOf("全部") }

    Column {
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selected),
            edgePadding = 12.dp
        ) {
            categories.forEach { c ->
                Tab(
                    selected = c == selected,
                    onClick = {
                        selected = c
                        vm.loadGuides(category = c.takeIf { it != "全部" })
                    },
                    text = { Text(c) }
                )
            }
        }
        if (state.isLoadingGuide && state.guideList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.guideList, key = { it.id }) { guide ->
                    GuideCard(guide, onClick = { vm.openGuideDetail(guide.id) })
                }
            }
        }
    }
}

@Composable
private fun GuideCard(guide: FishingGuideResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "  ${guide.category ?: "钓法"}  ",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(Modifier.width(8.dp))
                guide.season?.let {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text("  $it  ", fontSize = 11.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(guide.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            guide.summary?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, fontSize = 13.sp, color = Color.Gray, maxLines = 3)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "来源: ${guide.source ?: "LureCalendar"}",
                    fontSize = 11.sp, color = Color.Gray
                )
                Spacer(Modifier.weight(1f))
                Text("阅读 ${guide.viewCount ?: 0}", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun FishDetailDialog(fish: FishEncyclopediaResponse, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("关闭") } },
        title = { Text(fish.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (!fish.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = fish.imageUrl, contentDescription = fish.name,
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(12.dp))
                }
                fish.scientificName?.let { LabelRow("学名", it, italic = true) }
                fish.alias?.let { LabelRow("别名", it) }
                fish.family?.let { LabelRow("科属", it) }
                fish.distribution?.let { LabelRow("分布", it) }
                fish.habitat?.let { LabelRow("栖息", it) }
                fish.feedingHabit?.let { LabelRow("食性", it) }
                fish.bodySize?.let { LabelRow("体型", it) }
                fish.bestSeason?.let { LabelRow("旺季", it) }
                fish.bestHours?.let { LabelRow("最佳时段", it) }
                fish.optimalTemp?.let { LabelRow("适宜水温", "${it}℃") }
                fish.recommendedLures?.let { LabelRow("推荐饵型", it) }
                fish.techniqueTips?.let { LabelRow("钓法要点", it) }
                fish.description?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 13.sp, color = Color.DarkGray)
                }
                fish.source?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("来源: $it", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    )
}

@Composable
private fun LureDetailDialog(lure: LureResponse, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("关闭") } },
        title = { Text("${lure.name} · ${lure.category ?: ""}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (!lure.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = lure.imageUrl, contentDescription = lure.name,
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(10.dp))
                }
                lure.subType?.let { LabelRow("类型", it) }
                lure.swimLayer?.let { LabelRow("泳层", it) }
                lure.weightRange?.let { LabelRow("重量", it) }
                lure.lengthRange?.let { LabelRow("长度", it) }
                lure.divingDepth?.let { LabelRow("潜深", it) }
                lure.targetSpecies?.let { LabelRow("适用鱼种", it) }
                lure.suitableWaterTemp?.let { LabelRow("适用水温", "${it}℃") }
                lure.suitableWaterType?.let { LabelRow("适用水域", it) }
                lure.colorTip?.let { LabelRow("配色建议", it) }
                lure.technique?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("操作手法", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(it, fontSize = 13.sp, color = Color.DarkGray)
                }
                lure.pros?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("✓ 优势", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                    Text(it, fontSize = 13.sp)
                }
                lure.cons?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("× 局限", fontWeight = FontWeight.SemiBold, color = Color(0xFFC62828))
                    Text(it, fontSize = 13.sp)
                }
                lure.description?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 13.sp, color = Color.DarkGray)
                }
            }
        }
    )
}

@Composable
private fun GuideDetailDialog(guide: FishingGuideResponse, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("关闭") } },
        title = {
            Text(guide.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (!guide.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = guide.coverUrl, contentDescription = guide.title,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Text(guide.content ?: "", fontSize = 13.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "来源: ${guide.source ?: "LureCalendar"} · 阅读 ${guide.viewCount ?: 0}",
                    fontSize = 11.sp, color = Color.Gray
                )
            }
        }
    )
}

@Composable
private fun LabelRow(label: String, value: String, italic: Boolean = false) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            "$label: ",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            value,
            fontSize = 13.sp,
            fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic
                        else androidx.compose.ui.text.font.FontStyle.Normal
        )
    }
}
