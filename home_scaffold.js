const fs = require('fs');
const path = require('path');

const files = {
  'app/src/main/java/com/lurecalendar/app/ui/screens/home/HomeScreen.kt': `
package com.lurecalendar.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurecalendar.app.ui.theme.DeepSeaBlue
import com.lurecalendar.app.ui.theme.NatureGreen
import com.lurecalendar.app.ui.theme.WarningRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToWeather: () -> Unit,
    onNavigateToWaterLevel: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToCatchRecord: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("路亚日历", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSeaBlue,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCatchRecord,
                containerColor = NatureGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "快速记鱼获", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WeatherCard(onClick = onNavigateToWeather)
            }
            item {
                WaterLevelCard(onClick = onNavigateToWaterLevel)
            }
            item {
                QuickActionsGrid(
                    onNavigateToMap = onNavigateToMap,
                    onNavigateToCatchRecord = onNavigateToCatchRecord
                )
            }
            item {
                Text(
                    text = "最近鱼获",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(3) { index ->
                RecentCatchItem()
            }
        }
    }
}

@Composable
fun WeatherCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("当前位置: 杭州市", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("24°C", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = DeepSeaBlue)
                Text("多云 | 湿度 65% | 气压 1012hPa", style = MaterialTheme.typography.bodySmall)
            }
            // Placeholder for Weather Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
fun WaterLevelCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("关注水域: 千岛湖", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Warning, contentDescription = "Warning", tint = WarningRed)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("当前水位", style = MaterialTheme.typography.bodySmall)
                    Text("105.2m", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepSeaBlue)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("警戒水位", style = MaterialTheme.typography.bodySmall)
                    Text("108.0m", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WarningRed)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = 105.2f / 108.0f,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = NatureGreen,
                trackColor = Color.LightGray
            )
        }
    }
}

@Composable
fun QuickActionsGrid(
    onNavigateToMap: () -> Unit,
    onNavigateToCatchRecord: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onNavigateToMap,
            modifier = Modifier.weight(1f).height(80.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepSeaBlue)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LocationOn, contentDescription = "Map")
                Spacer(modifier = Modifier.height(4.dp))
                Text("钓点地图")
            }
        }
        Button(
            onClick = onNavigateToCatchRecord,
            modifier = Modifier.weight(1f).height(80.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NatureGreen)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = "Record")
                Spacer(modifier = Modifier.height(4.dp))
                Text("鱼获记录")
            }
        }
    }
}

@Composable
fun RecentCatchItem() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("翘嘴 45cm 2.5kg", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text("千岛湖 • 米诺 • 2026-05-10", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
`
};

for (const [filePath, content] of Object.entries(files)) {
  const fullPath = path.join(__dirname, filePath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  fs.writeFileSync(fullPath, content.trim());
}

console.log('Home Screen scaffolding complete.');
