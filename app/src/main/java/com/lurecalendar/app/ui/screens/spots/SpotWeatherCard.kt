package com.lurecalendar.app.ui.screens.spots

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lurecalendar.app.ui.theme.*

@Composable
fun SpotWeatherCard(
    state: SpotWeatherState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 0.8.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: name + water type + score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = state.spot.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.spot.waterType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.fishingIndex != null) {
                    ScoreBadge(score = state.fishingIndex.score)
                }
            }

            // Weather section
            if (state.isLoadingWeather) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("\u52a0\u8f7d\u5929\u6c14\u4e2d...", style = MaterialTheme.typography.bodySmall)
                }
            } else if (state.weatherError || state.weather == null) {
                Text(
                    text = "\u6682\u65e0\u5929\u6c14\u6570\u636e",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                val current = state.weather.current

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WeatherChip(
                        icon = Icons.Default.Thermostat,
                        label = "${current.temperature.toInt()}\u00B0C",
                        tint = WaterCyan
                    )
                    WeatherChip(
                        icon = Icons.Default.Air,
                        label = "${current.windSpeed}m/s ${current.windDirection}",
                        tint = NatureGreen
                    )
                    WeatherChip(
                        icon = Icons.Default.WaterDrop,
                        label = "${current.humidity}%",
                        tint = DeepSeaBlue
                    )
                }

                // Best window
                if (state.bestWindow != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = SandYellow
                        )
                        Text(
                            text = "\u6700\u4f73\u7a97\u53e3: ${state.bestWindow}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = SandYellow
                        )
                    }
                }

                // Wind-shore advice
                if (state.windShoreAdvice != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = WaterCyan.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = state.windShoreAdvice,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = WaterCyan
                        )
                    }
                }

                // Fishing index summary
                if (state.fishingIndex != null) {
                    Text(
                        text = state.fishingIndex.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Target species chips
            val species = state.spot.targetSpecies
                ?.split(",", "\u3001", " ")
                ?.filter { it.isNotBlank() }
            if (!species.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    species.take(5).forEach { sp ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = sp.trim(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    val bgColor = when {
        score >= 85 -> IndexPerfect
        score >= 70 -> IndexHigh
        score >= 55 -> IndexMid
        else -> IndexLow
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = 0.15f)
    ) {
        Text(
            text = "$score",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = bgColor
        )
    }
}

@Composable
private fun WeatherChip(
    icon: ImageVector,
    label: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp
        )
    }

}
