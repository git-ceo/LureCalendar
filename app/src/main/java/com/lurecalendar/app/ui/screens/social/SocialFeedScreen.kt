@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
package com.lurecalendar.app.ui.screens.social

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lurecalendar.app.R
import com.lurecalendar.app.data.remote.api.CommentResponse
import com.lurecalendar.app.data.remote.api.MomentResponse
import com.lurecalendar.app.data.remote.api.UserProfileResponse
import com.lurecalendar.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@Composable
fun SocialFeedScreen(
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToRadar: () -> Unit = {},
    viewModel: SocialViewModel = hiltViewModel()
) {
    var showAddFriendSheet by remember { mutableStateOf(false) }
    val feedError = viewModel.feedError
    if (!feedError.isNullOrBlank()) {
        LaunchedEffect(feedError) {
            delay(2500)
            viewModel.feedError = null
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    val listState = rememberLazyListState()
    val headerHeight = 220.dp
    val density = LocalDensity.current
    val headerPx = remember(density) { with(density) { headerHeight.toPx() } }
    val headerOffsetPx by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat().coerceIn(0f, headerPx)
            } else headerPx
        }
    }
    val collapsedFraction by remember { derivedStateOf { (headerOffsetPx / headerPx).coerceIn(0f, 1f) } }

    var menuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // 主内容区
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                item {
                    MomentsHeader(
                        username = viewModel.currentUserProfile?.username ?: viewModel.moments.firstOrNull()?.username,
                        avatarUrl = viewModel.currentUserProfile?.avatarUrl,
                        scrollOffsetPx = headerOffsetPx,
                        headerHeight = headerHeight,
                        onCameraClick = { viewModel.showPostDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(viewModel.moments, key = { it.id }) { moment ->
                    MomentItem(
                        moment = moment,
                        onLikeClick = { viewModel.toggleLike(moment) },
                        onCommentClick = { viewModel.openComments(moment) },
                        commentPreviews = viewModel.commentPreviews[moment.id].orEmpty(),
                        onEnsureCommentPreview = { viewModel.ensureCommentPreview(moment.id) }
                    )
                    HorizontalDivider(color = GlassWhite, thickness = 0.5.dp)
                }

                if (viewModel.moments.isEmpty() && !viewModel.isRefreshing) {
                    item {
                        EmptyFeedHint(
                            isSquare = viewModel.selectedTab == "square",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = viewModel.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = WaterCyan,
                backgroundColor = DarkSurface
            )
        }

        // 顶部悬浮层：Tab切换 + 菜单（叠加在背景图上方）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            // 滚动时渐现的深色背景
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(SurfaceDark.copy(alpha = collapsedFraction))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabItem(text = "广场", isSelected = viewModel.selectedTab == "square") { viewModel.switchTab("square") }
                    TabItem(text = "关注", isSelected = viewModel.selectedTab == "friends") { viewModel.switchTab("friends") }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("发动态") },
                            onClick = {
                                menuExpanded = false
                                viewModel.showPostDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("发现钓友") },
                            onClick = {
                                menuExpanded = false
                                showAddFriendSheet = true
                            },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("排行榜") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToLeaderboard()
                            },
                            leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) }
                        )
                    }
                }
            }
        }

        // 错误提示
        if (!feedError.isNullOrBlank()) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = Color.Black.copy(alpha = 0.85f),
                contentColor = Color.White
            ) { Text(feedError) }
        }

        // 发动态弹窗
        if (viewModel.showPostDialog) {
            PostDialog(
                content = viewModel.newPostContent,
                visibility = viewModel.newPostVisibility,
                onContentChange = { viewModel.newPostContent = it },
                onVisibilityChange = { viewModel.newPostVisibility = it },
                onPost = { viewModel.postMoment() },
                onDismiss = { viewModel.showPostDialog = false }
            )
        }
        if (showAddFriendSheet) {
            AddFriendSheet(
                onDismiss = { showAddFriendSheet = false },
                onNavigateToRadar = {
                    showAddFriendSheet = false
                    onNavigateToRadar()
                }
            )
        }
    }

    val commentMoment = viewModel.commentSheetMoment
    if (commentMoment != null) {
        CommentsSheet(
            moment = commentMoment,
            comments = viewModel.comments,
            isLoading = viewModel.isLoadingComments,
            errorMessage = viewModel.commentError,
            commentContent = viewModel.newCommentContent,
            onCommentContentChange = { viewModel.newCommentContent = it },
            onRefresh = { viewModel.loadComments(commentMoment.id) },
            onSend = { viewModel.postComment() },
            onDismiss = { viewModel.closeComments() }
        )
    }
}

@Composable
private fun MomentsHeader(
    username: String?,
    avatarUrl: String?,
    scrollOffsetPx: Float,
    headerHeight: Dp,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 默认封面图 - 湖面日落钓鱼风景
    val defaultCoverUrl = "https://images.unsplash.com/photo-1440964829947-ca3277bd37f8?w=800&q=80"
    val parallax = -scrollOffsetPx * 0.4f
    Box(modifier = modifier.height(headerHeight)) {
        // 兜底渐变背景（图片加载前 / 加载失败时显示）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A3A4A),
                            DarkSurface
                        )
                    )
                )
        )
        AsyncImage(
            model = defaultCoverUrl,
            contentDescription = "封面",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = parallax },
            contentScale = ContentScale.Crop
        )
        // 半透明遮罩 + 底部渐变，保证文字清晰可见
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, DarkSurface.copy(alpha = 0.85f))))
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = username ?: "钓鱼佬",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 10.dp)
            )
            AsyncImage(
                model = if (avatarUrl.isNullOrBlank()) R.drawable.ic_default_avatar else avatarUrl,
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_default_avatar),
                error = painterResource(R.drawable.ic_default_avatar),
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)).background(GlassWhite),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(999.dp)
            ) {
                IconButton(onClick = onCameraClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "发动态", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedHint(isSquare: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isSquare) "广场暂无内容" else "关注暂无内容",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSquare) "广场只展示公开动态，若你发布的是“仅钓友可见”，这里不会显示" else "关注页展示你关注的人和自己的动态",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun TabItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 18.sp
        )
        if (isSelected) {
            Box(modifier = Modifier.padding(top = 4.dp).size(width = 20.dp, height = 2.dp).background(Color.White, CircleShape))
        }
    }
}

@Composable
fun AddFriendSheet(
    onDismiss: () -> Unit,
    onNavigateToRadar: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GlassWhite) }
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("发现钓友", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            ListItem(
                headlineContent = { Text("搜索昵称/手机号", color = Color.White) },
                leadingContent = { Icon(Icons.Default.Search, contentDescription = null, tint = WaterCyan) },
                modifier = Modifier.clickable { /* TODO: Search Dialog */ },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text("雷达面对面", color = Color.White) },
                leadingContent = { Icon(Icons.Default.Radar, contentDescription = null, tint = WaterCyan) },
                modifier = Modifier.clickable { onNavigateToRadar() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text("扫一扫", color = Color.White) },
                leadingContent = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = WaterCyan) },
                modifier = Modifier.clickable { /* TODO: Scan QR */ },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MomentItem(
    moment: MomentResponse,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    commentPreviews: List<CommentResponse>,
    onEnsureCommentPreview: () -> Unit
) {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val timeStr = sdf.format(Date(moment.createTime))
    var actionsExpanded by remember(moment.id) { mutableStateOf(false) }

    LaunchedEffect(moment.id, moment.commentCount) {
        if (moment.commentCount > 0) {
            onEnsureCommentPreview()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        AsyncImage(
            model = if (moment.avatarUrl.isNullOrBlank()) R.drawable.ic_default_avatar else moment.avatarUrl,
            contentDescription = null,
            placeholder = painterResource(R.drawable.ic_default_avatar),
            error = painterResource(R.drawable.ic_default_avatar),
            modifier = Modifier.size(45.dp).clip(RoundedCornerShape(8.dp)).background(GlassWhite),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = moment.username, fontWeight = FontWeight.Bold, color = WaterCyan, fontSize = 16.sp)
                if (moment.visibility == "friends") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = Color.Gray.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                        Text("仅好友可见", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (moment.isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = WaterCyan)
                } else if (moment.isError) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = moment.content, fontSize = 15.sp, color = Color.White, lineHeight = 20.sp)
            
            NineGridPhotos(photos = moment.photos)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = timeStr, fontSize = 12.sp, color = Color.Gray)
                
                Box {
                    IconButton(
                        onClick = { actionsExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = actionsExpanded,
                        onDismissRequest = { actionsExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (moment.isLiked) "取消" else "赞") },
                            onClick = {
                                actionsExpanded = false
                                onLikeClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (moment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("评论") },
                            onClick = {
                                actionsExpanded = false
                                onCommentClick()
                            },
                            leadingIcon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) }
                        )
                    }
                }
            }

            if (moment.likeCount > 0 || moment.commentCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        if (moment.likeCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = " ${moment.likeCount}人觉得很赞",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        if (moment.commentCount > 0) {
                            Spacer(modifier = Modifier.height(if (moment.likeCount > 0) 8.dp else 0.dp))
                            if (commentPreviews.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    commentPreviews.forEach { c ->
                                        Surface(
                                            color = Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                                Text(
                                                    text = c.username,
                                                    color = WaterCyan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(text = "：", color = Color.Gray, fontSize = 12.sp)
                                                Text(
                                                    text = c.content,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (moment.commentCount > commentPreviews.size) {
                                    Text(
                                        text = "查看全部 ${moment.commentCount} 条评论",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clickable { onCommentClick() }
                                    )
                                }
                            } else {
                                Text(
                                    text = "查看 ${moment.commentCount} 条评论",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { onCommentClick() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NineGridPhotos(photos: List<String>) {
    if (photos.isEmpty()) return
    Spacer(modifier = Modifier.height(10.dp))

    if (photos.size == 1) {
        AsyncImage(
            model = photos.first(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(190.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GlassWhite),
            contentScale = ContentScale.Crop
        )
        return
    }

    val grid = photos.take(9)
    val size = 96.dp
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        grid.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(size)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlassWhite),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun PostDialog(
    content: String,
    visibility: String,
    onContentChange: (String) -> Unit,
    onVisibilityChange: (String) -> Unit,
    onPost: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("分享你的路亚生活", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("此刻的心情或装备实测...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("发布范围", color = Color.Gray, fontSize = 14.sp)
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    VisibilityOption(text = "广场", selected = visibility == "public") { onVisibilityChange("public") }
                    Spacer(modifier = Modifier.width(12.dp))
                    VisibilityOption(text = "仅钓友", selected = visibility == "friends") { onVisibilityChange("friends") }
                }
            }
        },
        confirmButton = {
            Button(onClick = onPost, colors = ButtonDefaults.buttonColors(containerColor = WaterCyan)) {
                Text("发布", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
        }
    )
}

@Composable
fun VisibilityOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) WaterCyan.copy(alpha = 0.2f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) WaterCyan else GlassWhite)
    ) {
        Text(text = text, color = if (selected) WaterCyan else Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun CommentsSheet(
    moment: MomentResponse,
    comments: List<CommentResponse>,
    isLoading: Boolean,
    errorMessage: String?,
    commentContent: String,
    onCommentContentChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GlassWhite) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("评论", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = WaterCyan)
                }
            }

            Text(
                text = moment.content,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = WaterCyan)
                }
            } else if (comments.isEmpty()) {
                Text("暂无评论", color = Color.Gray, modifier = Modifier.padding(vertical = 18.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(comments, key = { it.id }) { c ->
                        CommentBlock(comment = c)
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentContent,
                    onValueChange = onCommentContentChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("写评论…") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = WaterCyan,
                        unfocusedBorderColor = GlassWhiteDeep,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(10.dp))
                FloatingActionButton(
                    onClick = onSend,
                    containerColor = WaterCyan,
                    contentColor = Color.Black,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun CommentBlock(comment: CommentResponse, indent: Int = 0) {
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = remember(comment.createTime) { sdf.format(Date(comment.createTime)) }

    Column(modifier = Modifier.fillMaxWidth().padding(start = (indent * 16).dp)) {
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = if (comment.avatarUrl.isNullOrBlank()) R.drawable.ic_default_avatar else comment.avatarUrl,
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_default_avatar),
                error = painterResource(R.drawable.ic_default_avatar),
                modifier = Modifier.size(32.dp).clip(CircleShape).background(GlassWhite),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.username, color = WaterCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(timeStr, color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(comment.content, color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
            }
        }

        if (comment.replies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                comment.replies.forEach { r ->
                    CommentBlock(comment = r, indent = indent + 1)
                }
            }
        }
    }
}
