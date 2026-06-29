package com.lurecalendar.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import com.lurecalendar.app.BuildConfig
import com.lurecalendar.app.data.local.ThemeMode
import com.lurecalendar.app.data.local.ThemePreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context.applicationContext) }
    val currentThemeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val coroutineScope = rememberCoroutineScope()

    // States
    var fishingReminderEnabled by remember { mutableStateOf(false) }
    var dataSyncEnabled by remember { mutableStateOf(false) }

    // Dialog states
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showFishingMethodDialog by remember { mutableStateOf(false) }
    var showFishSpeciesDialog by remember { mutableStateOf(false) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }
    var showPressureUnitDialog by remember { mutableStateOf(false) }
    var showTempUnitDialog by remember { mutableStateOf(false) }
    var showWindUnitDialog by remember { mutableStateOf(false) }
    var showIndexModeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    // Selected values (display only, persisted later)
    var selectedLanguage by remember { mutableStateOf("简体中文") }
    var selectedSort by remember { mutableStateOf("距离优先") }
    var selectedFishingMethod by remember { mutableStateOf("路亚") }
    var selectedFishSpecies by remember { mutableStateOf("翘嘴, 鲈鱼") }
    var selectedReminderTime by remember { mutableStateOf("06:00") }
    var selectedPressureUnit by remember { mutableStateOf("hPa") }
    var selectedTempUnit by remember { mutableStateOf("℃") }
    var selectedWindUnit by remember { mutableStateOf("m/s") }
    var selectedIndexMode by remember { mutableStateOf("简单") }

    // Theme display text
    val themeDisplayText = when (currentThemeMode) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.DARK -> "深色"
        ThemeMode.LIGHT -> "浅色"
    }

    fun showTodo() {
        Toast.makeText(context, "功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ==================== 通用设置 ====================
            item { SectionHeader("通用设置") }

            item {
                SettingClickable(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    subtitle = themeDisplayText
                ) { showThemeDialog = true }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.Language,
                    title = "语言设置",
                    subtitle = selectedLanguage
                ) { showLanguageDialog = true }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.Sort,
                    title = "钓点默认排序",
                    subtitle = selectedSort
                ) { showSortDialog = true }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.LocationOn,
                    title = "定位服务",
                    subtitle = "已开启"
                ) { showTodo() }
            }

            // ==================== 钓鱼偏好 ====================
            item { SectionHeader("钓鱼偏好") }

            item {
                SettingClickable(
                    icon = Icons.Default.Phishing,
                    title = "默认钓法",
                    subtitle = selectedFishingMethod
                ) { showFishingMethodDialog = true }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.Pets,
                    title = "常用鱼种",
                    subtitle = selectedFishSpecies
                ) { showFishSpeciesDialog = true }
            }

            item {
                SettingSwitch(
                    icon = Icons.Default.Notifications,
                    title = "钓鱼提醒",
                    subtitle = "每日推送今日钓鱼指数",
                    checked = fishingReminderEnabled,
                    onCheckedChange = { fishingReminderEnabled = it }
                )
            }

            item {
                SettingClickable(
                    icon = Icons.Default.AccessTime,
                    title = "出钓提醒时间",
                    subtitle = selectedReminderTime
                ) { showReminderTimeDialog = true }
            }

            // ==================== 天气与指数 ====================
            item { SectionHeader("天气与指数") }

            item {
                SettingClickable(
                    icon = Icons.Default.Cloud,
                    title = "天气数据源",
                    subtitle = "当前城市自动获取"
                ) { showTodo() }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.Analytics,
                    title = "钓鱼指数模式",
                    subtitle = selectedIndexMode
                ) { showIndexModeDialog = true }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.Speed,
                    title = "气压单位",
                    subtitle = selectedPressureUnit
                ) { showPressureUnitDialog = true }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.Thermostat,
                    title = "温度单位",
                    subtitle = selectedTempUnit
                ) { showTempUnitDialog = true }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.Air,
                    title = "风速单位",
                    subtitle = selectedWindUnit
                ) { showWindUnitDialog = true }
            }

            // ==================== 数据管理 ====================
            item { SectionHeader("数据管理") }

            item {
                SettingSwitch(
                    icon = Icons.Default.Sync,
                    title = "数据同步",
                    subtitle = "同步钓鱼日志到云端",
                    checked = dataSyncEnabled,
                    onCheckedChange = {
                        dataSyncEnabled = it
                        showTodo()
                    }
                )
            }

            item {
                SettingClickable(
                    icon = Icons.Default.FileDownload,
                    title = "导出钓鱼日志",
                    subtitle = "导出为 CSV 文件"
                ) { showTodo() }
            }

            item {
                SettingDanger(
                    icon = Icons.Default.CleaningServices,
                    title = "清除缓存",
                    subtitle = "清除图片及临时缓存"
                ) { showClearCacheDialog = true }
            }

            item {
                SettingDanger(
                    icon = Icons.Default.DeleteForever,
                    title = "清除本地数据",
                    subtitle = "删除所有本地记录（不可恢复）"
                ) { showClearDataDialog = true }
            }

            // ==================== 关于 ====================
            item { SectionHeader("关于") }

            item {
                SettingInfo(
                    icon = Icons.Default.Info,
                    title = "版本号",
                    value = "v${BuildConfig.VERSION_NAME}"
                )
            }

            item {
                SettingClickable(
                    icon = Icons.Default.Description,
                    title = "用户协议",
                    subtitle = ""
                ) { showTodo() }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.PrivacyTip,
                    title = "隐私政策",
                    subtitle = ""
                ) { showTodo() }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.Update,
                    title = "检查更新",
                    subtitle = "当前已是最新版本"
                ) { showTodo() }
            }

            item {
                SettingClickable(
                    icon = Icons.Default.Feedback,
                    title = "意见反馈",
                    subtitle = ""
                ) { showTodo() }
            }

            // 退出登录
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onLogout) {
                        Text(
                            text = "退出登录",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // ======================== Dialogs ========================

    // 深色模式选择
    if (showThemeDialog) {
        val options = listOf("跟随系统", "浅色", "深色")
        val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
        SingleChoiceDialog(
            title = "深色模式",
            options = options,
            selectedIndex = modes.indexOf(currentThemeMode),
            onSelect = { index ->
                coroutineScope.launch {
                    themePreferences.setThemeMode(modes[index])
                }
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // 语言选择
    if (showLanguageDialog) {
        val options = listOf("跟随系统", "简体中文", "English")
        SingleChoiceDialog(
            title = "语言设置",
            options = options,
            selectedIndex = options.indexOf(selectedLanguage).coerceAtLeast(0),
            onSelect = { index ->
                selectedLanguage = options[index]
                showLanguageDialog = false
                Toast.makeText(context, "功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // 排序方式
    if (showSortDialog) {
        val options = listOf("距离优先", "收藏优先", "最近使用")
        SingleChoiceDialog(
            title = "钓点默认排序",
            options = options,
            selectedIndex = options.indexOf(selectedSort).coerceAtLeast(0),
            onSelect = { index ->
                selectedSort = options[index]
                showSortDialog = false
                Toast.makeText(context, "功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSortDialog = false }
        )
    }

    // 默认钓法
    if (showFishingMethodDialog) {
        val options = listOf("路亚", "台钓", "飞蝇", "矶钓", "筏钓")
        SingleChoiceDialog(
            title = "默认钓法",
            options = options,
            selectedIndex = options.indexOf(selectedFishingMethod).coerceAtLeast(0),
            onSelect = { index ->
                selectedFishingMethod = options[index]
                showFishingMethodDialog = false
            },
            onDismiss = { showFishingMethodDialog = false }
        )
    }

    // 常用鱼种（多选）
    if (showFishSpeciesDialog) {
        val allSpecies = listOf("翘嘴", "鲈鱼", "鳜鱼", "黑鱼", "马口", "红尾", "军鱼", "鲶鱼")
        val currentSelected = remember {
            mutableStateListOf<String>().apply {
                addAll(selectedFishSpecies.split(", ").filter { it.isNotBlank() })
            }
        }
        AlertDialog(
            onDismissRequest = { showFishSpeciesDialog = false },
            title = {
                Text(
                    "常用鱼种（可多选）",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    allSpecies.forEach { species ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (currentSelected.contains(species)) {
                                        currentSelected.remove(species)
                                    } else {
                                        currentSelected.add(species)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = currentSelected.contains(species),
                                onCheckedChange = { checked ->
                                    if (checked) currentSelected.add(species)
                                    else currentSelected.remove(species)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = species,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedFishSpecies = if (currentSelected.isEmpty()) "未选择"
                    else currentSelected.joinToString(", ")
                    showFishSpeciesDialog = false
                }) {
                    Text("确定", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFishSpeciesDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // 提醒时间
    if (showReminderTimeDialog) {
        val options = listOf("05:00", "05:30", "06:00", "06:30", "07:00", "07:30", "08:00")
        SingleChoiceDialog(
            title = "出钓提醒时间",
            options = options,
            selectedIndex = options.indexOf(selectedReminderTime).coerceAtLeast(0),
            onSelect = { index ->
                selectedReminderTime = options[index]
                showReminderTimeDialog = false
            },
            onDismiss = { showReminderTimeDialog = false }
        )
    }

    // 气压单位
    if (showPressureUnitDialog) {
        val options = listOf("hPa", "mmHg")
        SingleChoiceDialog(
            title = "气压单位",
            options = options,
            selectedIndex = options.indexOf(selectedPressureUnit).coerceAtLeast(0),
            onSelect = { index ->
                selectedPressureUnit = options[index]
                showPressureUnitDialog = false
                Toast.makeText(context, "功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showPressureUnitDialog = false }
        )
    }

    // 温度单位
    if (showTempUnitDialog) {
        val options = listOf("℃", "℉")
        SingleChoiceDialog(
            title = "温度单位",
            options = options,
            selectedIndex = options.indexOf(selectedTempUnit).coerceAtLeast(0),
            onSelect = { index ->
                selectedTempUnit = options[index]
                showTempUnitDialog = false
                Toast.makeText(context, "功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showTempUnitDialog = false }
        )
    }

    // 风速单位
    if (showWindUnitDialog) {
        val options = listOf("m/s", "km/h")
        SingleChoiceDialog(
            title = "风速单位",
            options = options,
            selectedIndex = options.indexOf(selectedWindUnit).coerceAtLeast(0),
            onSelect = { index ->
                selectedWindUnit = options[index]
                showWindUnitDialog = false
                Toast.makeText(context, "功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showWindUnitDialog = false }
        )
    }

    // 钓鱼指数模式
    if (showIndexModeDialog) {
        val options = listOf("简单", "专业")
        SingleChoiceDialog(
            title = "钓鱼指数评分模式",
            options = options,
            selectedIndex = options.indexOf(selectedIndexMode).coerceAtLeast(0),
            onSelect = { index ->
                selectedIndexMode = options[index]
                showIndexModeDialog = false
                Toast.makeText(context, "功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showIndexModeDialog = false }
        )
    }

    // 清除缓存确认
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = {
                Text("清除缓存", color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    "将清除所有图片缓存和临时文件，不会影响您的数据记录。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // 实际清除 Coil 图片缓存
                    context.imageLoader.diskCache?.clear()
                    context.imageLoader.memoryCache?.clear()
                    showClearCacheDialog = false
                    Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
                }) {
                    Text("确认清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // 清除本地数据确认
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = {
                Text("⚠️ 清除本地数据", color = MaterialTheme.colorScheme.error)
            },
            text = {
                Text(
                    "此操作将删除所有本地数据，包括钓鱼记录、收藏的钓点等。此操作不可恢复！",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataDialog = false
                    Toast.makeText(context, "功能开发中，敬请期待", Toast.LENGTH_SHORT).show()
                }) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ======================== Reusable Components ========================

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun SettingClickable(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun SettingInfo(
    icon: ImageVector,
    title: String,
    value: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingDanger(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
