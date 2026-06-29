package com.lurecalendar.app.ui.screens.catchrecord

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurecalendar.app.common.fish.BuiltInFishSpecies
import com.lurecalendar.app.ui.theme.DeepSeaBlue
import com.lurecalendar.app.ui.theme.WaterCyan
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: CatchFormViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            try {
                onNavigateBack()
            } catch (e: Exception) {
                // 防止导航异常导致闪退
                android.util.Log.e("CatchFormScreen", "导航返回失败", e)
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
        onResult = { uris ->
            viewModel.addPhotoUris(uris.map { it.toString() })
        }
    )

    var showSpeciesPicker by remember { mutableStateOf(false) }
    var speciesFilter by remember { mutableStateOf("") }

    val dateTime = remember(state.catchTime) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(state.catchTime), ZoneId.systemDefault())
    }

    // 语音识别逻辑
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { isListening = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    viewModel.parseVoiceInput(matches[0])
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            speechRecognizer.startListening(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新增鱼获", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSeaBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // 钓点选择卡片
                    OutlinedCard(
                        onClick = { viewModel.openSpotPicker() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = WaterCyan
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "钓点",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    state.spot?.name ?: "点击选择钓点",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (state.spot != null)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 语音录入按钮
                        IconButton(
                            onClick = { 
                                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isListening) Color.Red else WaterCyan.copy(alpha = 0.1f),
                                contentColor = if (isListening) Color.White else WaterCyan
                            )
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "语音输入")
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.species,
                            onValueChange = { v -> viewModel.update { it.copy(species = v) } },
                            modifier = Modifier.weight(1f),
                            label = { Text("鱼种") },
                            singleLine = true
                        )
                        OutlinedButton(
                            onClick = { showSpeciesPicker = true },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text("选择")
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.count,
                            onValueChange = { v -> viewModel.update { it.copy(count = v) } },
                            modifier = Modifier.weight(1f),
                            label = { Text("条数") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.lengthCm,
                            onValueChange = { v -> viewModel.update { it.copy(lengthCm = v) } },
                            modifier = Modifier.weight(1f),
                            label = { Text("长度(cm)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.weightG,
                            onValueChange = { v -> viewModel.update { it.copy(weightG = v) } },
                            modifier = Modifier.weight(1f),
                            label = { Text("重量(g)") },
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.bait,
                            onValueChange = { v -> viewModel.update { it.copy(bait = v) } },
                            modifier = Modifier.weight(1f),
                            label = { Text("饵") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.rod,
                            onValueChange = { v -> viewModel.update { it.copy(rod = v) } },
                            modifier = Modifier.weight(1f),
                            label = { Text("竿") },
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = state.river,
                        onValueChange = { v -> viewModel.update { it.copy(river = v) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("河流/水域") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.city,
                        onValueChange = { v -> viewModel.update { it.copy(city = v) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("城市") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.locationDetail,
                        onValueChange = { v -> viewModel.update { it.copy(locationDetail = v) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("具体位置说明") },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = { v -> viewModel.update { it.copy(note = v) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("备注") }
                    )
                }

                // ===== 路亚专属字段 =====
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "路亚专属信息（选填）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WaterCyan
                    )
                }

                // 饵料类型
                item {
                    LureChipSelector(
                        label = "饵料类型",
                        options = listOf("水面系", "潜水米诺", "悬浮米诺", "VIB", "软虫", "铅笔", "亮片", "复合亮片", "雷蛙", "胡须佬"),
                        selected = state.lureType,
                        onSelected = { v -> viewModel.update { it.copy(lureType = v) } }
                    )
                }

                // 钓组类型
                item {
                    LureChipSelector(
                        label = "钓组类型",
                        options = listOf("德州钓组", "卡罗莱纳钓组", "无铅钓组", "铅头钩", "倒吊", "自由钓组", "直柄+纺车轮", "枪柄+水滴轮"),
                        selected = state.rigType,
                        onSelected = { v -> viewModel.update { it.copy(rigType = v) } }
                    )
                }

                // 结构区
                item {
                    LureChipSelector(
                        label = "结构区",
                        options = listOf("草区", "倒树/木桩", "岩壁/石堆", "深浅交界", "明水", "桥墩/人工结构", "回水湾", "入水口"),
                        selected = state.structureZone,
                        onSelected = { v -> viewModel.update { it.copy(structureZone = v) } }
                    )
                }

                // 水体能见度
                item {
                    LureRadioSelector(
                        label = "水体能见度",
                        options = listOf("清澈(>1m)", "微浊(30cm-1m)", "浑浊(<30cm)"),
                        selected = state.waterClarity,
                        onSelected = { v -> viewModel.update { it.copy(waterClarity = v) } }
                    )
                }

                // 风向岸线关系
                item {
                    LureRadioSelector(
                        label = "风向岸线关系",
                        options = listOf("迎风岸", "背风岸", "侧风岸"),
                        selected = state.windShoreRelation,
                        onSelected = { v -> viewModel.update { it.copy(windShoreRelation = v) } }
                    )
                }

                item {
                    val y = dateTime.year
                    val m = dateTime.monthValue
                    val d = dateTime.dayOfMonth
                    val hh = dateTime.hour
                    val mm = dateTime.minute
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newDt = dateTime.withYear(year).withMonth(month + 1).withDayOfMonth(day)
                                    val millis = newDt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    viewModel.update { it.copy(catchTime = millis) }
                                }, y, m - 1, d).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$y-$m-$d")
                        }
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(context, { _, hour, minute ->
                                    val newDt = dateTime.withHour(hour).withMinute(minute)
                                    val millis = newDt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    viewModel.update { it.copy(catchTime = millis) }
                                }, hh, mm, true).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(String.format("%02d:%02d", hh, mm))
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.height(0.dp))
                            Text("选择照片(${state.photoUris.size}/4)", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                if (state.photoUris.isNotEmpty()) {
                    items(state.photoUris) { uri ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(uri, modifier = Modifier.weight(1f), maxLines = 1)
                            IconButton(onClick = { viewModel.removePhoto(uri) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }

            if (state.errorMessage != null) {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(state.errorMessage ?: "") }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving
                ) { Text("取消") }
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) {
                        Text("保存中…")
                    } else {
                        Text("保存")
                    }
                }
            }
        }
    }

    // 钓点选择弹窗
    if (state.showSpotPicker) {
        SpotPickerInFormDialog(
            spots = state.allSpots,
            onSelect = { spot -> viewModel.selectSpot(spot) },
            onDismiss = { viewModel.closeSpotPicker() }
        )
    }

    if (showSpeciesPicker) {
        val options = BuiltInFishSpecies.filter { it.contains(speciesFilter.trim()) }.take(50)
        AlertDialog(
            onDismissRequest = { showSpeciesPicker = false },
            title = { Text("选择鱼种") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = speciesFilter,
                        onValueChange = { speciesFilter = it },
                        label = { Text("搜索") },
                        singleLine = true
                    )
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(options) { item ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.update { it.copy(species = item) }
                                    showSpeciesPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(item)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSpeciesPicker = false }) { Text("完成") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LureChipSelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = {
                        onSelected(if (selected == option) "" else option)
                    },
                    label = { Text(option, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
fun LureRadioSelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected == option,
                        onClick = {
                            onSelected(if (selected == option) "" else option)
                        }
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(option, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpotPickerInFormDialog(
    spots: List<com.lurecalendar.app.domain.model.FishingSpot>,
    onSelect: (com.lurecalendar.app.domain.model.FishingSpot) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择钓点") },
        text = {
            if (spots.isEmpty()) {
                Text("还没有钓点，先去地图长按新增一个吧", color = Color.White.copy(alpha = 0.7f))
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

                    val filteredSpots = spots.filter {
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
                                    onClick = { onSelect(spot) },
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
            Button(onClick = onDismiss) { Text("关闭") }
        }
    )
}
