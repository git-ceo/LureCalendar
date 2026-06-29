package com.lurecalendar.app.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.lurecalendar.app.R
import com.lurecalendar.app.data.remote.api.SpotLeaderboardEntry
import com.lurecalendar.app.ui.theme.DeepSeaBlue
import com.lurecalendar.app.ui.theme.SurfaceDark
import com.lurecalendar.app.ui.theme.WaterCyan
import com.lurecalendar.app.ui.theme.glassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit
) {
    // Mock data for global leaderboard
    val mockEntries = remember {
        listOf(
            SpotLeaderboardEntry("打鳜大师", null, "鳜鱼", 2500f, 45f, null, 1715616000000L),
            SpotLeaderboardEntry("路亚追风少年", null, "翘嘴", 3200f, 65f, null, 1715702400000L),
            SpotLeaderboardEntry("梓潼老张", null, "黑鱼", 4100f, 55f, null, 1715788800000L),
            SpotLeaderboardEntry("空军总司令", null, "白条", 50f, 12f, null, 1715875200000L),
            SpotLeaderboardEntry("路亚新手小白", null, "鲈鱼", 1200f, 35f, null, 1715961600000L)
        ).sortedByDescending { it.weight ?: 0f }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("钓友巨物榜", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSeaBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = SurfaceDark
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().glassBackground(20).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("本周之星", color = Color.Gray, fontSize = 12.sp)
                    Text(mockEntries.firstOrNull()?.username ?: "虚位以待", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(mockEntries) { index, entry ->
                    LeaderboardItem(index + 1, entry)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardItem(rank: Int, entry: SpotLeaderboardEntry) {
    Box(modifier = Modifier.fillMaxWidth().glassBackground(12).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$rank", 
                color = when(rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.Gray
                },
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.width(40.dp)
            )
            
            AsyncImage(
                model = if (entry.avatarUrl.isNullOrBlank()) R.drawable.ic_default_avatar else entry.avatarUrl,
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_default_avatar),
                error = painterResource(R.drawable.ic_default_avatar),
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${entry.species} | ${entry.weight?.let { "${it/1000}kg" } ?: "—"} | ${entry.length?.toInt() ?: "—"}cm", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
