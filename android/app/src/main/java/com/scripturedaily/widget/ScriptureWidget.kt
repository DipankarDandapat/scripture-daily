package com.scripturedaily.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.scripturedaily.MainActivity
import com.scripturedaily.data.ScriptureDatabase

class ScriptureWidget : GlanceAppWidget() {
    suspend fun refreshAll(context: Context) {
        GlanceAppWidgetManager(context).getGlanceIds(ScriptureWidget::class.java).forEach { update(context, it) }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cached = ScriptureDatabase.get(context).verses().matching(
            context.getSharedPreferences("reader", Context.MODE_PRIVATE).getInt("scripture", 1),
            context.getSharedPreferences("reader", Context.MODE_PRIVATE).getInt("language", 1)
        )
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF183D32))).padding(16.dp).clickable(actionStartActivity(Intent(context, MainActivity::class.java)))) {
                Text(cached?.scripture ?: "Scripture Daily", style = TextStyle(color = ColorProvider(Color(0xFFD8AC68)), fontSize = 12.sp))
                Spacer(GlanceModifier.height(8.dp))
                Text(cached?.let { "“${it.text}”" } ?: "Open the app to download today's verse.", style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp))
                Spacer(GlanceModifier.height(6.dp))
                Text(cached?.reference() ?: "Wisdom for every day", style = TextStyle(color = ColorProvider(Color(0xFFD0DAD3)), fontSize = 11.sp))
            }
        }
    }
}

class ScriptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScriptureWidget()
}
