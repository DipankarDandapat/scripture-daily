package com.scripturedaily.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.scripturedaily.MainActivity
import com.scripturedaily.data.ScriptureDatabase
import com.scripturedaily.lockscreentime.TimePalette
import java.util.concurrent.TimeUnit

class ScriptureWidget : GlanceAppWidget() {

    companion object {
        val SMALL = DpSize(180.dp, 80.dp)
        val LARGE = DpSize(180.dp, 180.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    suspend fun refreshAll(context: Context) {
        GlanceAppWidgetManager(context).getGlanceIds(ScriptureWidget::class.java).forEach { update(context, it) }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("reader", Context.MODE_PRIVATE)
        val cached = ScriptureDatabase.get(context).verses().matching(
            prefs.getInt("scripture", 1),
            prefs.getInt("language", 1)
        )
        val palette = TimePalette.now(prefs.getInt("palette_interval_hours", 6))

        // Compute "Updated X ago" label
        val updatedLabel = cached?.let {
            val diffMs = System.currentTimeMillis() - it.savedAt
            val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
            when {
                mins < 1   -> "Just now"
                mins < 60  -> "${mins}m ago"
                mins < 1440 -> "${mins / 60}h ago"
                else        -> "${mins / 1440}d ago"
            }
        } ?: ""

        provideContent {
            val size = LocalSize.current
            val isSmall = size.height < 120.dp

            if (isSmall) {
                // ── Small 2×1: scripture label + reference only ──────────────
                Column(
                    GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(palette.background)))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable(actionStartActivity(
                            Intent(context, MainActivity::class.java).apply {
                                cached?.id?.let { putExtra("verse_id", it) }
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        ))
                ) {
                    Text(
                        text = cached?.scripture?.uppercase() ?: "WISDOMONE",
                        style = TextStyle(
                            color = ColorProvider(Color(palette.accent)),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = cached?.let { "\u201c${it.text}\u201d" } ?: "Open app to load verse",
                        style = TextStyle(
                            color = ColorProvider(Color(palette.primaryText)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 2
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = cached?.reference() ?: "",
                        style = TextStyle(
                            color = ColorProvider(Color(palette.secondaryText)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            } else {
                // ── Large: full verse + timestamp + refresh ──────────────────
                Column(
                    GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(palette.background)))
                        .padding(16.dp)
                        .clickable(actionStartActivity(
                            Intent(context, MainActivity::class.java).apply {
                                cached?.id?.let { putExtra("verse_id", it) }
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        ))
                ) {
                    Text(
                        text = cached?.scripture?.uppercase() ?: "WISDOMONE",
                        style = TextStyle(
                            color = ColorProvider(Color(palette.accent)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        text = cached?.let { "\u201c${it.text}\u201d" } ?: "Open the app to load today\u2019s verse.",
                        style = TextStyle(
                            color = ColorProvider(Color(palette.primaryText)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 6
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        text = cached?.reference() ?: "Wisdom for every day",
                        style = TextStyle(
                            color = ColorProvider(Color(palette.secondaryText)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    // ── Bottom row: timestamp + refresh tap ─────────────────
                    Row(
                        GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (updatedLabel.isNotEmpty()) {
                            Text(
                                text = "↻ $updatedLabel",
                                style = TextStyle(
                                    color = ColorProvider(Color(palette.accent).copy(alpha = 0.7f)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                modifier = GlanceModifier.clickable(actionRunCallback<RefreshWidgetCallback>())
                            )
                        }
                    }
                }
            }
        }
    }
}

class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        ScriptureWidget().update(context, glanceId)
    }
}

class ScriptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScriptureWidget()
}
