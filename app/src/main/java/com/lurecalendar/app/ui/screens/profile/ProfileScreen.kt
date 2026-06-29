package com.lurecalendar.app.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lurecalendar.app.R
import com.lurecalendar.app.ui.theme.*
import com.lurecalendar.app.ui.screens.profile.ProfileViewModel.UserImageTarget
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToReminderSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToGearStats: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val data = viewModel.profileData
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val regDateStr = data?.createTime?.let { sdf.format(Date(it)) } ?: "未知"
    
    val daysSince = data?.createTime?.let {
        val diff = System.currentTimeMillis() - it
        diff / (1000 * 60 * 60 * 24)
    } ?: 0

    // 动画进度
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "waveProgress"
    )

    // 图片选择器
    var pendingPickTarget by remember { mutableStateOf<UserImageTarget?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val target = pendingPickTarget
        pendingPickTarget = null
        if (uri != null && target != null) {
            viewModel.uploadAndSetUserImage(target, uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SurfaceDark)) {
        // 动态波浪背景
        WaveBackground(progress = waveProgress, color = WaterCyan)
        
        Column(modifier = Modifier.fillMaxSize()) {
            // 背景图 + 设置按钮叠加
            Box(modifier = Modifier.height(260.dp).fillMaxWidth()) {
                AsyncImage(
                    model = data?.backgroundUrl ?: "https://img.zcool.cn/community/01f9e55a1114b0a8012098675c9287.jpg@1280w_1l_2o_100sh.jpg",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, SurfaceDark.copy(alpha = 0.8f))
                    )
                ))
                // 设置入口按钮（右上角叠加）
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = Color.White
                    )
                }
            }

            // 个人信息卡片 (毛玻璃)
            Column(
                modifier = Modifier
                    .offset(y = (-60).dp)
                    .padding(horizontal = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = data?.avatarUrl ?: R.drawable.ic_default_avatar,
                            contentDescription = null,
                            placeholder = painterResource(R.drawable.ic_default_avatar),
                            error = painterResource(R.drawable.ic_default_avatar),
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(GlassWhite)
                                .padding(4.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.showImageUpdateDialog(UserImageTarget.Avatar) },
                            modifier = Modifier.size(32.dp).background(DeepGreen, CircleShape).padding(4.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = data?.username ?: "加载中...",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Surface(
                            color = WaterCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "入坑第 $daysSince 天",
                                fontSize = 12.sp,
                                color = WaterCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 签名区域 (毛玻璃)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBackground(20)
                        .padding(20.dp)
                ) {
                    if (viewModel.isEditingSignature) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = viewModel.editedSignature,
                                onValueChange = { viewModel.editedSignature = it },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = WaterCyan
                                )
                            )
                            IconButton(onClick = { viewModel.updateSignature() }) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = WaterCyan)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.isEditingSignature = true }
                        ) {
                            Text(
                                text = data?.signature ?: "这个钓鱼佬很懒...",
                                fontSize = 15.sp,
                                color = Color.LightGray,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.Edit, contentDescription = null, tint = GlassWhiteDeep, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 统计与操作
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "获赞", 
                        value = "0", 
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "勋章", 
                        value = "3", 
                        modifier = Modifier.weight(1f).clickable { onNavigateToAchievements() }
                    )
                    StatCard(
                        label = "关注", 
                        value = "0", 
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { onNavigateToGearStats("我的主力竿") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("我的装备数据分析")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateToReminderSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("智能出钓提醒设置")
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { viewModel.showImageUpdateDialog(UserImageTarget.Background) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("更换背景墙")
                }

                Spacer(modifier = Modifier.height(40.dp))

                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("退出登录", color = Color.Gray)
                }
            }
        }
    }

    // 图片更新对话框
    val updateTarget = viewModel.imageUpdateTarget
    if (updateTarget != null) {
        var showUrlInput by remember { mutableStateOf(false) }

        if (!showUrlInput) {
            AlertDialog(
                onDismissRequest = { viewModel.imageUpdateTarget = null },
                containerColor = DarkSurface,
                title = {
                    Text(
                        "更新" + (if (updateTarget == UserImageTarget.Avatar) "头像" else "背景"),
                        color = Color.White
                    )
                },
                text = {
                    Column {
                        ListItem(
                            headlineContent = { Text("从相册选择", color = Color.White) },
                            leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = WaterCyan) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                pendingPickTarget = updateTarget
                                imagePickerLauncher.launch("image/*")
                                viewModel.imageUpdateTarget = null
                            }
                        )
                        ListItem(
                            headlineContent = { Text("输入图片 URL", color = Color.White) },
                            leadingContent = { Icon(Icons.Default.Link, contentDescription = null, tint = WaterCyan) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { showUrlInput = true }
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.imageUpdateTarget = null }) { Text("取消") }
                }
            )
        } else {
            var urlInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.imageUpdateTarget = null },
                containerColor = DarkSurface,
                title = { Text("输入图片 URL", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("http://...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = { 
                        viewModel.updateUserImage(updateTarget, urlInput)
                        viewModel.imageUpdateTarget = null
                    }) { Text("确认") }
                },
                dismissButton = {
                    TextButton(onClick = { showUrlInput = false }) { Text("上一步") }
                }
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .glassBackground(16)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WaterCyan)
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}
