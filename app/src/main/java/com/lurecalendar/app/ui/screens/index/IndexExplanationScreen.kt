package com.lurecalendar.app.ui.screens.index

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurecalendar.app.ui.theme.DarkSurface
import com.lurecalendar.app.ui.theme.IndexDanger
import com.lurecalendar.app.ui.theme.IndexHigh
import com.lurecalendar.app.ui.theme.IndexLow
import com.lurecalendar.app.ui.theme.IndexMid
import com.lurecalendar.app.ui.theme.IndexPerfect
import com.lurecalendar.app.ui.theme.WaterCyan

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IndexExplanationScreen(
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("分数", "时段", "鱼种", "走势", "怎么算")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "指数解读",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkSurface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 顶部说明卡片
            item {
                Spacer(modifier = Modifier.height(4.dp))
                TopExplanationCard()
            }

            // "钓鱼指数怎么看" 文字说明
            item {
                HowToReadSection()
            }

            // 三步骤卡片
            item {
                ThreeStepsRow()
            }

            // Tab 标签行
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = {
                                Text(
                                    text = title,
                                    color = if (selectedTab == index) Color.White
                                    else Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WaterCyan,
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.White.copy(alpha = 0.2f),
                                selectedBorderColor = WaterCyan,
                                enabled = true,
                                selected = selectedTab == index
                            )
                        )
                    }
                }
            }

            // Tab 内容区域
            item {
                when (selectedTab) {
                    0 -> ScoreRangeContent()
                    1 -> TimeSlotContent()
                    2 -> FishSpeciesContent()
                    3 -> TrendContent()
                    4 -> HowToCalculateContent()
                }
            }

            // 底部留白
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TopExplanationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "钓鱼指数说明",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "看懂分数，再决定怎么钓",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(IndexPerfect.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "出钓前参考工具",
                        fontSize = 12.sp,
                        color = IndexPerfect,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 右侧评分圆环示意
            MiniScoreRing()
        }
    }
}

@Composable
private fun MiniScoreRing() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(WaterCyan.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "0-100",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WaterCyan
            )
            Text(
                text = "分",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HowToReadSection() {
    Column {
        Text(
            text = "钓鱼指数怎么看",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "指数不是保证鱼获，而是把天气、时间、鱼种和风险放在一起，帮你判断今天值不值得去、几点去更合适。",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.75f),
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun ThreeStepsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StepCard(
            step = "先看",
            content = "总分",
            modifier = Modifier.weight(1f)
        )
        StepCard(
            step = "再看",
            content = "好时段",
            modifier = Modifier.weight(1f)
        )
        StepCard(
            step = "最后看",
            content = "目标鱼",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StepCard(
    step: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WaterCyan.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = step,
                fontSize = 12.sp,
                color = WaterCyan
            )
            Text(
                text = "→",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = content,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ScoreRangeContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "分数代表什么",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        // 2x2 网格
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScoreCard(
                range = "0-39",
                label = "不太建议",
                description = "天气、风雨或风险不占优，更适合练竿、短试，别专门跑远路。",
                color = IndexDanger,
                icon = Icons.Default.Close,
                modifier = Modifier.weight(1f)
            )
            ScoreCard(
                range = "40-59",
                label = "一般",
                description = "能钓，但别从早守到晚。优先选熟悉钓点，没口就及时调整。",
                color = IndexLow,
                icon = Icons.Default.DragHandle,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScoreCard(
                range = "60-79",
                label = "可以去",
                description = "条件还不错，重点看推荐时段和目标鱼种，不要只盯总分。",
                color = IndexHigh,
                icon = Icons.Default.Done,
                modifier = Modifier.weight(1f)
            )
            ScoreCard(
                range = "80-100",
                label = "很适合",
                description = "值得安排出钓，但钓位、饵料、钓法和现场水情仍然很关键。",
                color = IndexPerfect,
                icon = Icons.Default.Star,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScoreCard(
    range: String,
    label: String,
    description: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = range,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun TimeSlotContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "推荐时段",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "系统会根据当天气压变化、光照强度和鱼种活跃规律，为你推荐最佳出钓时间窗口。",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.75f),
            lineHeight = 22.sp
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = WaterCyan.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TimeSlotItem("晨间窗口", "05:00 - 08:00", "低光照 + 气温上升期，掠食鱼活性高")
                Spacer(modifier = Modifier.height(12.dp))
                TimeSlotItem("傍晚窗口", "17:00 - 19:30", "光线减弱 + 气压稳定，小鱼聚集引发掠食")
                Spacer(modifier = Modifier.height(12.dp))
                TimeSlotItem("夜间窗口", "21:00 - 23:00", "月光照射水面，适合夜钓鳜鱼、鲶鱼")
            }
        }
    }
}

@Composable
private fun TimeSlotItem(title: String, time: String, desc: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WaterCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = time, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
        }
        Text(
            text = desc,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun FishSpeciesContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "目标鱼种",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "不同鱼种对气压、温度、光照的敏感度不同，系统会根据当天条件推荐最适合作钓的目标鱼。",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.75f),
            lineHeight = 22.sp
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = IndexPerfect.copy(alpha = 0.08f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FishItem("翘嘴", "气压升高时活跃，偏好清晨水面系")
                Spacer(modifier = Modifier.height(10.dp))
                FishItem("鳜鱼", "低光照环境活性最高，适合傍晚/夜间")
                Spacer(modifier = Modifier.height(10.dp))
                FishItem("鲈鱼", "全天候可钓，气压稳定时口最好")
                Spacer(modifier = Modifier.height(10.dp))
                FishItem("鲶鱼", "阴天/小雨后活跃，夜间觅食旺盛")
            }
        }
    }
}

@Composable
private fun FishItem(name: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "•",
            fontSize = 14.sp,
            color = IndexPerfect,
            modifier = Modifier.padding(end = 8.dp, top = 1.dp)
        )
        Column {
            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = desc, fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun TrendContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "指数走势",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "通过近7天的指数走势，帮你判断窗口期是在靠近还是远离。上升趋势意味着接下来几天条件更好。",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.75f),
            lineHeight = 22.sp
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.06f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TrendItem("持续上升", "近几日条件逐渐改善，值得关注后续几天")
                Spacer(modifier = Modifier.height(10.dp))
                TrendItem("高位平稳", "当前处于最佳窗口，建议尽快安排出钓")
                Spacer(modifier = Modifier.height(10.dp))
                TrendItem("开始下降", "窗口期正在关闭，今天出发比明天好")
                Spacer(modifier = Modifier.height(10.dp))
                TrendItem("持续低迷", "短期内条件不佳，建议等待下一个窗口")
            }
        }
    }
}

@Composable
private fun TrendItem(title: String, desc: String) {
    Column {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = IndexMid)
        Text(
            text = desc,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun HowToCalculateContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "路亚评分怎么算",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "综合以下5个因子加权计算，满分100分：",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.75f),
            lineHeight = 22.sp
        )

        FactorCard(
            icon = Icons.Default.Compress,
            name = "气压",
            weight = "30分",
            desc = "气压稳定或缓升有利",
            color = WaterCyan
        )
        FactorCard(
            icon = Icons.Default.Air,
            name = "风速",
            weight = "25分",
            desc = "微风2-4级最佳",
            color = IndexHigh
        )
        FactorCard(
            icon = Icons.Default.LightMode,
            name = "光照",
            weight = "20分",
            desc = "低光照时段活性高",
            color = IndexMid
        )
        FactorCard(
            icon = Icons.Default.NightsStay,
            name = "月相",
            weight = "15分",
            desc = "新月满月前后活跃",
            color = Color(0xFFB39DDB)
        )
        FactorCard(
            icon = Icons.Default.WaterDrop,
            name = "降水",
            weight = "10分",
            desc = "小雨前后鱼口好",
            color = Color(0xFF64B5F6)
        )
    }
}

@Composable
private fun FactorCard(
    icon: ImageVector,
    name: String,
    weight: String,
    desc: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = weight,
                        fontSize = 12.sp,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
