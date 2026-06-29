package com.lurecalendar.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lurecalendar.app.ui.theme.DarkSurface
import com.lurecalendar.app.ui.theme.WaterCyan

class WaterLevelWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(DarkSurface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "路亚日历",
                style = TextStyle(
                    color = ColorProvider(Color.Gray),
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "105.2m",
                style = TextStyle(
                    color = ColorProvider(WaterCyan),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "梓江 · 梓潼",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "指数: ",
                    style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp)
                )
                Text(
                    text = "82",
                    style = TextStyle(color = ColorProvider(Color.Green), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

class WaterLevelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterLevelWidget()
}
