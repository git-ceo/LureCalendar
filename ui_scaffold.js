const fs = require('fs');
const path = require('path');

const files = {
  'app/src/main/java/com/lurecalendar/app/ui/theme/Color.kt': `
package com.lurecalendar.app.ui.theme

import androidx.compose.ui.graphics.Color

val DeepSeaBlue = Color(0xFF1E3A8A)
val NatureGreen = Color(0xFF10B981)
val WarningRed = Color(0xFFEF4444)
val SurfaceLight = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF0F172A)
val TextPrimaryLight = Color(0xFF1E293B)
val TextSecondaryLight = Color(0xFF64748B)
val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)
`,
  'app/src/main/java/com/lurecalendar/app/ui/theme/Theme.kt': `
package com.lurecalendar.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DeepSeaBlue,
    secondary = NatureGreen,
    tertiary = DeepSeaBlue,
    background = SurfaceDark,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = WarningRed
)

private val LightColorScheme = lightColorScheme(
    primary = DeepSeaBlue,
    secondary = NatureGreen,
    tertiary = DeepSeaBlue,
    background = SurfaceLight,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = WarningRed
)

@Composable
fun LureCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
`,
  'app/src/main/java/com/lurecalendar/app/ui/navigation/NavGraph.kt': `
package com.lurecalendar.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lurecalendar.app.ui.screens.home.HomeScreen
import com.lurecalendar.app.ui.screens.weather.WeatherScreen
import com.lurecalendar.app.ui.screens.waterlevel.WaterLevelScreen
import com.lurecalendar.app.ui.screens.map.MapScreen
import com.lurecalendar.app.ui.screens.catchrecord.CatchRecordScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Weather : Screen("weather")
    object WaterLevel : Screen("water_level")
    object Map : Screen("map")
    object CatchRecord : Screen("catch_record")
}

@Composable
fun LureCalendarNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToWeather = { navController.navigate(Screen.Weather.route) },
                onNavigateToWaterLevel = { navController.navigate(Screen.WaterLevel.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToCatchRecord = { navController.navigate(Screen.CatchRecord.route) }
            )
        }
        composable(Screen.Weather.route) {
            WeatherScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.WaterLevel.route) {
            WaterLevelScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Map.route) {
            MapScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.CatchRecord.route) {
            CatchRecordScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
`,
  'app/src/main/java/com/lurecalendar/app/ui/screens/home/HomeScreen.kt': `
package com.lurecalendar.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
                title = { Text("路亚日历") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onNavigateToWeather, modifier = Modifier.fillMaxWidth()) {
                Text("天气查询")
            }
            Button(onClick = onNavigateToWaterLevel, modifier = Modifier.fillMaxWidth()) {
                Text("水位查询")
            }
            Button(onClick = onNavigateToMap, modifier = Modifier.fillMaxWidth()) {
                Text("钓点地图")
            }
            Button(onClick = onNavigateToCatchRecord, modifier = Modifier.fillMaxWidth()) {
                Text("鱼获记录")
            }
        }
    }
}
`,
  'app/src/main/java/com/lurecalendar/app/ui/screens/weather/WeatherScreen.kt': `
package com.lurecalendar.app.ui.screens.weather

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("天气查询") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("天气查询模块开发中...")
        }
    }
}
`,
  'app/src/main/java/com/lurecalendar/app/ui/screens/waterlevel/WaterLevelScreen.kt': `
package com.lurecalendar.app.ui.screens.waterlevel

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterLevelScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水位查询") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("水位查询模块开发中...")
        }
    }
}
`,
  'app/src/main/java/com/lurecalendar/app/ui/screens/map/MapScreen.kt': `
package com.lurecalendar.app.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("钓点地图") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("钓点地图模块开发中...")
        }
    }
}
`,
  'app/src/main/java/com/lurecalendar/app/ui/screens/catchrecord/CatchRecordScreen.kt': `
package com.lurecalendar.app.ui.screens.catchrecord

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchRecordScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("鱼获记录") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("鱼获记录模块开发中...")
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

console.log('UI scaffolding complete.');
