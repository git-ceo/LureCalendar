package com.lurecalendar.app.ui.screens.map

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.lurecalendar.app.ui.theme.DeepSeaBlue
import com.lurecalendar.app.ui.theme.WaterCyan
import com.lurecalendar.app.ui.theme.SurfaceDark
import com.lurecalendar.app.ui.theme.GlassWhite
import com.lurecalendar.app.ui.theme.glassBackground
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit = {},
    onAddCatch: (String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
    isEmbedded: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by viewModel.state.collectAsState()

    val hasAmapKey = com.lurecalendar.app.BuildConfig.AMAP_API_KEY.isNotBlank()

    var showRationale by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                showRationale = false
            }
        }
    )

    val hasLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isSatellite by remember { mutableStateOf(true) }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "钓点列表（${state.spots.size}）",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "长按地图新增钓点",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                if (state.spots.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("暂无钓点", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "回到地图长按任意位置新增",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.spots, key = { it.id }) { spot ->
                            Card(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    viewModel.selectSpot(spot)
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = DeepSeaBlue
                                        )
                                    },
                                    headlineContent = { Text(spot.name) },
                                    supportingContent = {
                                        val line = listOfNotNull(
                                            spot.city?.takeIf { it.isNotBlank() },
                                            spot.river?.takeIf { it.isNotBlank() })
                                            .joinToString(" · ")
                                        Text(if (line.isBlank()) "—" else line)
                                    },
                                    trailingContent = {
                                        Text(
                                            spot.waterType,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        val mapInnerContent: @Composable (PaddingValues) -> Unit = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (!hasAmapKey) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("未配置高德 AMAP_API_KEY，地图无法初始化")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "请在项目根目录 local.properties 添加：AMAP_API_KEY=你的Key",
                            color = Color.Gray
                        )
                    }
                } else if (hasLocationPermission) {
                    LaunchedEffect(Unit) {
                        viewModel.locateOnce()
                    }
                    val markers = state.spots.map {
                        MapMarkerItem(
                            id = it.id,
                            latLng = com.amap.api.maps.model.LatLng(it.latitude, it.longitude),
                            title = it.name
                        )
                    }

                    val anglerMarkers = state.anglers.map {
                        MapMarkerItem(
                            id = "angler_${it.phone}",
                            latLng = com.amap.api.maps.model.LatLng(it.latitude, it.longitude),
                            title = "${it.username} (在线)"
                        )
                    }

                    AmapMapView(
                        modifier = Modifier.fillMaxSize(),
                        markers = markers + anglerMarkers,
                        centerOn = state.lastCenteredLocation,
                        centerTrigger = state.centerTrigger,
                        myLocationEnabled = true,
                        mapType = if (isSatellite) com.amap.api.maps.AMap.MAP_TYPE_SATELLITE else com.amap.api.maps.AMap.MAP_TYPE_NORMAL,
                        onMapLongClick = viewModel::onMapLongClick,
                        onMarkerClick = { id ->
                            val spot = state.spots.find { it.id == id }
                            if (spot != null) {
                                viewModel.selectSpot(spot)
                            }
                        }
                    )

                    SmallFloatingActionButton(
                        onClick = { viewModel.locateOnce() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = DeepSeaBlue
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Locate",
                            tint = Color.White
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = { isSatellite = !isSatellite },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 64.dp, end = 16.dp),
                        containerColor = DeepSeaBlue
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Layer", tint = Color.White)
                    }

                    if (state.isLocating) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("定位中…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        state.currentCity?.let { city ->
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shadowElevation = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 10.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "当前位置：$city",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("需要定位权限以显示当前位置与标记钓点")
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            ) {
                                showRationale = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }) {
                            Text("授予定位权限")
                        }
                    }
                }

                state.errorMessage?.let { msg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 90.dp)
                    ) { Text(msg) }
                }

                if (state.spots.isEmpty() && !state.isLocating && hasLocationPermission && hasAmapKey) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            "长按地图任意位置可新增钓点",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 嵌入模式下显示钓点列表入口（无 TopAppBar）
                if (isEmbedded) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 112.dp, end = 16.dp),
                        containerColor = DeepSeaBlue
                    ) {
                        Icon(Icons.Default.List, contentDescription = "钓点列表", tint = Color.White)
                    }
                }
            }
        }

        if (isEmbedded) {
            mapInnerContent(PaddingValues())
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("钓点地图") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.List, contentDescription = "Spots List")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = DeepSeaBlue,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )
                }
            ) { padding ->
                mapInnerContent(padding)
            }
        }

        if (showRationale) {
            AlertDialog(
                onDismissRequest = { showRationale = false },
                title = { Text("位置权限说明") },
                text = { Text("用于在地图中显示当前位置、计算钓点距离，并支持长按地图新增钓点。") },
                confirmButton = {
                    Button(onClick = {
                        showRationale = false
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }) { Text("继续") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRationale = false }) { Text("取消") }
                }
            )
        }

        state.draft?.let { draft ->
            NewSpotDialog(
                draft = draft,
                isSaving = state.isSaving,
                onDismiss = viewModel::dismissDraft,
                onUpdate = viewModel::updateDraft,
                onSave = viewModel::saveDraft
            )
        }

        state.selectedSpot?.let { spot ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.selectSpot(null) },
                containerColor = SurfaceDark,
                dragHandle = { BottomSheetDefaults.DragHandle(color = GlassWhite) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(spot.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { },
                            enabled = false,
                            label = { Text(spot.waterType, color = WaterCyan) },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = WaterCyan.copy(alpha = 0.1f),
                                disabledLabelColor = WaterCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, WaterCyan.copy(alpha = 0.3f))
                        )
                        AssistChip(
                            onClick = { },
                            enabled = false,
                            label = { Text(spot.structure, color = Color.Gray) },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = Color.White.copy(alpha = 0.05f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassWhite)
                        )
                        val cityText = spot.city?.takeIf { it.isNotBlank() }
                        if (cityText != null) {
                            AssistChip(
                                onClick = { },
                                enabled = false,
                                label = { Text(cityText, color = Color.Gray) },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassWhite)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.selectSpot(null)
                                onAddCatch(spot.id)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepSeaBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+鱼获", color = Color.White)
                        }
                        FilledTonalButton(
                            onClick = {
                                navigateToSpot(context, spot.latitude, spot.longitude, spot.name) {
                                    viewModel.showMessage(it)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导航")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.editSpot(spot) },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("编辑钓点", color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("本点巨物榜 (TOP 10)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.isLoadingLeaderboard) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally), color = WaterCyan)
                    } else if (state.leaderboard.isEmpty()) {
                        Text("暂无记录，快去拿下首个巨物吧！", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        state.leaderboard.forEachIndexed { index, entry ->
                            LeaderboardItem(index + 1, entry)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }
    }
}

private fun navigateToSpot(
    context: android.content.Context,
    lat: Double,
    lon: Double,
    name: String,
    onError: (String) -> Unit
) {
    // 优先高德地图
    val amapUri = Uri.parse("amapuri://route/plan/?dlat=$lat&dlon=$lon&dname=${Uri.encode(name)}&dev=0&t=0")
    val amapIntent = Intent(Intent.ACTION_VIEW, amapUri).apply {
        setPackage("com.autonavi.minimap")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (amapIntent.resolveActivity(context.packageManager) != null) {
        runCatching { context.startActivity(amapIntent) }
            .onFailure { onError("无法打开高德地图") }
        return
    }

    // 百度地图
    val baiduUri = Uri.parse("baidumap://map/direction?destination=latlng:$lat,$lon|name:${Uri.encode(name)}&coord_type=gcj02&mode=driving")
    val baiduIntent = Intent(Intent.ACTION_VIEW, baiduUri).apply {
        setPackage("com.baidu.BaiduMap")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (baiduIntent.resolveActivity(context.packageManager) != null) {
        runCatching { context.startActivity(baiduIntent) }
            .onFailure { onError("无法打开百度地图") }
        return
    }

    // 腾讯地图
    val tencentUri = Uri.parse("qqmap://map/routeplan?type=drive&tocoord=$lat,$lon&to=${Uri.encode(name)}")
    val tencentIntent = Intent(Intent.ACTION_VIEW, tencentUri).apply {
        setPackage("com.tencent.map")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (tencentIntent.resolveActivity(context.packageManager) != null) {
        runCatching { context.startActivity(tencentIntent) }
            .onFailure { onError("无法打开腾讯地图") }
        return
    }

    // 兜底：浏览器打开高德网页版导航
    val webUri = Uri.parse("https://uri.amap.com/navigation?to=$lon,$lat,${Uri.encode(name)}&mode=car&coordinate=gaode")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }.onFailure { onError("无法打开浏览器导航") }
}

@Composable
private fun LeaderboardItem(rank: Int, entry: com.lurecalendar.app.data.remote.api.SpotLeaderboardEntry) {
    Box(modifier = Modifier.fillMaxWidth().glassBackground(12).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#$rank", 
                color = when(rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.Gray
                },
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(36.dp)
            )
            
            AsyncImage(
                model = entry.avatarUrl ?: "https://img1.baidu.com/it/u=3616688756,2343834384&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500",
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${entry.species} | ${entry.weight?.let { "${it/1000}kg" } ?: "—"} | ${entry.length?.toInt() ?: "—"}cm", color = Color.Gray, fontSize = 12.sp)
            }
            
            if (entry.photo != null) {
                AsyncImage(
                    model = entry.photo,
                    contentDescription = null,
                    modifier = Modifier.size(45.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewSpotDialog(
    draft: NewSpotDraft,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onUpdate: ((NewSpotDraft) -> NewSpotDraft) -> Unit,
    onSave: () -> Unit
) {
    val waterTypes = listOf("河流", "水库", "湖泊", "池塘")
    val structures = listOf("岩石", "水草", "沙底")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增钓点") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { v -> onUpdate { it.copy(name = v) } },
                    label = { Text("名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.city,
                    onValueChange = { v -> onUpdate { it.copy(city = v) } },
                    label = { Text("城市") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.river,
                    onValueChange = { v -> onUpdate { it.copy(river = v) } },
                    label = { Text("河流/水域") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.locationDetail,
                    onValueChange = { v -> onUpdate { it.copy(locationDetail = v) } },
                    label = { Text("位置说明") },
                    singleLine = true
                )

                var waterTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = waterTypeExpanded,
                    onExpandedChange = { waterTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        value = draft.waterType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("水域类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = waterTypeExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = waterTypeExpanded,
                        onDismissRequest = { waterTypeExpanded = false }) {
                        waterTypes.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    onUpdate { it.copy(waterType = opt) }
                                    waterTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                var structureExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = structureExpanded,
                    onExpandedChange = { structureExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        value = draft.structure,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("结构") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = structureExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = structureExpanded,
                        onDismissRequest = { structureExpanded = false }) {
                        structures.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    onUpdate { it.copy(structure = opt) }
                                    structureExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = draft.depth,
                    onValueChange = { v -> onUpdate { it.copy(depth = v) } },
                    label = { Text("深度(米)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { v -> onUpdate { it.copy(note = v) } },
                    label = { Text("备注") }
                )
                Text(
                    "坐标：${draft.latLng.latitude}, ${draft.latLng.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving
            ) { Text(if (isSaving) "保存中..." else "保存") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) { Text("取消") }
        }
    )
}
