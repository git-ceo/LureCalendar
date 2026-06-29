
package com.lurecalendar.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurecalendar.app.ui.screens.home.HomeScreen
import com.lurecalendar.app.ui.screens.auth.LoginScreen
import com.lurecalendar.app.ui.screens.auth.LoginViewModel
import com.lurecalendar.app.ui.screens.weather.WeatherScreen
import com.lurecalendar.app.ui.screens.weather.WeatherDetailScreen
import com.lurecalendar.app.ui.screens.waterlevel.WaterLevelScreen
import com.lurecalendar.app.ui.screens.map.MapScreen
import com.lurecalendar.app.ui.screens.catchrecord.CatchRecordScreen
import com.lurecalendar.app.ui.screens.catchrecord.CatchFormScreen
import com.lurecalendar.app.ui.screens.settings.ReminderSettingsScreen
import com.lurecalendar.app.ui.screens.settings.SettingsScreen
import com.lurecalendar.app.ui.screens.gear.GearStatsScreen
import com.lurecalendar.app.ui.screens.profile.AchievementWallScreen
import com.lurecalendar.app.ui.screens.video.TechniqueVideoScreen
import com.lurecalendar.app.ui.screens.social.LeaderboardScreen
import com.lurecalendar.app.ui.screens.social.RadarScreen
import com.lurecalendar.app.ui.screens.onboarding.OnboardingScreen
import com.lurecalendar.app.ui.screens.encyclopedia.EncyclopediaScreen
import com.lurecalendar.app.ui.screens.calendar.CalendarScreen
import com.lurecalendar.app.ui.screens.index.IndexExplanationScreen
import com.lurecalendar.app.ui.screens.journal.JournalScreen
import com.lurecalendar.app.ui.screens.spots.SpotManagerScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object Journal : Screen("journal")
    object SpotManager : Screen("spot_manager")
    object Weather : Screen("weather")
    object WaterLevel : Screen("water_level")
    object Map : Screen("map")
    object CatchRecord : Screen("catch_record")
    object Social : Screen("social")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object ReminderSettings : Screen("reminder_settings")
    object Achievements : Screen("achievements")
    object TechniqueVideos : Screen("technique_videos")
    object Leaderboard : Screen("leaderboard")
    object Radar : Screen("radar")
    object Encyclopedia : Screen("encyclopedia")
    object GearStats : Screen("gear_stats?rodName={rodName}") {
        fun createRoute(rodName: String): String = "gear_stats?rodName=$rodName"
    }
    object WeatherDetail : Screen("weather_detail")
    object IndexExplanation : Screen("index_explanation")
    object CatchForm : Screen("catch_form?spotId={spotId}") {
        fun createRoute(spotId: String): String {
            val encoded = URLEncoder.encode(spotId, "UTF-8")
            return "catch_form?spotId=$encoded"
        }
    }
}

@Composable
fun LureCalendarNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Calendar.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToWeather = { navController.navigate(Screen.Weather.route) },
                onNavigateToWaterLevel = { navController.navigate(Screen.WaterLevel.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToCatchRecord = { navController.navigate(Screen.CatchRecord.route) },
                onNavigateToCatchForm = { spotId -> navController.navigate(Screen.CatchForm.createRoute(spotId)) },
                onNavigateToReminderSettings = { navController.navigate(Screen.ReminderSettings.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onNavigateToGearStats = { rodName -> navController.navigate(Screen.GearStats.createRoute(rodName)) },
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToEncyclopedia = { navController.navigate(Screen.Encyclopedia.route) },
                onNavigateToIndexExplanation = { navController.navigate(Screen.IndexExplanation.route) },
                onNavigateToWeatherDetail = { navController.navigate(Screen.WeatherDetail.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Weather.route) {
            WeatherScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.WaterLevel.route) {
            WaterLevelScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddCatch = { spotId -> navController.navigate(Screen.CatchForm.createRoute(spotId)) }
            )
        }
        composable(Screen.CatchRecord.route) {
            CatchRecordScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddCatchForSpot = { spotId -> navController.navigate(Screen.CatchForm.createRoute(spotId)) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToGearStats = { rodName -> navController.navigate(Screen.GearStats.createRoute(rodName)) }
            )
        }
        composable(
            route = Screen.CatchForm.route,
            arguments = listOf(navArgument("spotId") { type = NavType.StringType; defaultValue = "" })
        ) {
            CatchFormScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ReminderSettings.route) {
            ReminderSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Achievements.route) {
            AchievementWallScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.TechniqueVideos.route) {
            TechniqueVideoScreen()
        }
        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Radar.route) {
            RadarScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.WeatherDetail.route) {
            WeatherDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateToIndexExplanation = { navController.navigate(Screen.IndexExplanation.route) },
                onNavigateToWeatherDetail = { navController.navigate(Screen.WeatherDetail.route) }
            )
        }
        composable(Screen.Journal.route) {
            JournalScreen(
                onNavigateToCatchForm = {
                    navController.navigate(Screen.CatchForm.createRoute(""))
                }
            )
        }
        composable(Screen.SpotManager.route) {
            SpotManagerScreen(
                onNavigateToMap = { navController.navigate(Screen.Map.route) }
            )
        }
        composable(Screen.Encyclopedia.route) {
            EncyclopediaScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.IndexExplanation.route) {
            IndexExplanationScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.GearStats.route,
            arguments = listOf(navArgument("rodName") { type = NavType.StringType })
        ) {
            val rodName = it.arguments?.getString("rodName") ?: ""
            GearStatsScreen(rodName = rodName, onNavigateBack = { navController.popBackStack() })
        }
    }
}
