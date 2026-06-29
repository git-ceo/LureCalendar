package com.lurecalendar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.lurecalendar.app.data.local.ThemeMode
import com.lurecalendar.app.data.local.ThemePreferences
import com.lurecalendar.app.domain.repository.AuthRepository
import com.lurecalendar.app.ui.navigation.LureCalendarNavGraph
import com.lurecalendar.app.ui.navigation.Screen
import com.lurecalendar.app.ui.theme.LureCalendarTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isLoggedIn by authRepository.getIsLoggedIn().collectAsState(initial = null)
            val isOnboardingCompleted by authRepository.isOnboardingCompleted().collectAsState(initial = null)

            val themePreferences = remember { ThemePreferences(applicationContext) }
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            LureCalendarTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 等待 DataStore 加载完成
                    if (isLoggedIn != null && isOnboardingCompleted != null) {
                        val start = intent.getStringExtra("start") ?: when {
                            isOnboardingCompleted == false -> Screen.Onboarding.route
                            isLoggedIn == true -> Screen.Home.route
                            else -> Screen.Login.route
                        }
                        LureCalendarNavGraph(startDestination = start)
                    }
                }
            }
        }
    }
}
