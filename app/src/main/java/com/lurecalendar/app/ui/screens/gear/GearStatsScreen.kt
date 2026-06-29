package com.lurecalendar.app.ui.screens.gear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurecalendar.app.ui.theme.SurfaceDark
import com.lurecalendar.app.ui.theme.WaterCyan
import com.lurecalendar.app.ui.theme.glassBackground
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GearStatsScreen(
    rodName: String,
    onNavigateBack: () -> Unit,
    viewModel: GearStatsViewModel = hiltViewModel()
) {
    LaunchedEffect(rodName) {
        viewModel.loadStats(rodName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(rodName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = SurfaceDark
    ) { padding ->
        val stats = viewModel.stats

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WaterCyan)
            }
        } else if (stats == null || stats.total_count == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无该装备的使用数据", color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 概览
                Box(modifier = Modifier.fillMaxWidth().glassBackground().padding(20.dp)) {
                    Column {
                        Text("累计记录", color = Color.Gray, fontSize = 12.sp)
                        Text("${stats.total_count} 条", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }

                // 鱼获重量趋势 (Vico 折线图)
                Text("鱼获重量趋势 (g)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Box(modifier = Modifier.fillMaxWidth().height(250.dp).glassBackground().padding(16.dp)) {
                    val model = entryModelOf(*stats.weight_trend.map { it.weight }.toTypedArray())
                    Chart(
                        chart = lineChart(),
                        model = model,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 捕获鱼种分布
                Text("鱼种分布", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    stats.species_dist.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().glassBackground().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.species, color = Color.White)
                            Text("${item.count} 尾", color = WaterCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
