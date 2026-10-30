package com.scripturedaily.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.scripturedaily.lockscreentime.TimePalette
import com.scripturedaily.data.CachedVerse
import com.scripturedaily.data.VerseSyncWorker
import java.util.concurrent.TimeUnit

class ScriptureWidget : GlanceAppWidget() {

    companion object {
        val SMALL = DpSize(180.dp, 80.dp)
        val LARGE = DpSize(270.dp, 110.dp)

        fun cacheVerseForWidget(context: Context, verse: CachedVerse) {
            context.getSharedPreferences("reader", Context.MODE_PRIVATE).edit()
                .putInt("current_verse_id", verse.id)
                .putString("widget_scripture", verse.scripture)
                .putString("widget_text", verse.text)
                .putString("widget_reference", verse.reference())
                .putLong("widget_saved_at", verse.savedAt)
                .commit()
        }
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    suspend fun refreshAll(context: Context) {
        GlanceAppWidgetManager(context).getGlanceIds(ScriptureWidget::class.java).forEach { update(context, it) }
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, ScriptureWidgetReceiver::class.java))
        if (ids.isNotEmpty()) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("reader", Context.MODE_PRIVATE)
        // Read verse directly from SharedPrefs — no Room query, no SQLite race condition
        val widgetScripture = prefs.getString("widget_scripture", null)
        val widgetText      = prefs.getString("widget_text", null)
        val widgetReference = prefs.getString("widget_reference", null)
        val widgetSavedAt   = prefs.getLong("widget_saved_at", 0L)
        val hasWidgetCache  = widgetScripture != null && widgetText != null && widgetReference != null
        val palette = TimePalette.now(prefs.getInt("palette_interval_hours", 6))

        // Compute "Updated X ago" label
        val updatedLabel = if (hasWidgetCache && widgetSavedAt > 0L) {
            val diffMs = System.currentTimeMillis() - widgetSavedAt
            val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
            when {
                mins < 1    -> "Just now"
                mins < 60   -> "${mins}m ago"
                mins < 1440 -> "${mins / 60}h ago"
                else        -> "${mins / 1440}d ago"
            }
        } else ""

        provideContent {
            val size = LocalSize.current
            val isSmall = size.height < 100.dp

            if (isSmall) {
                // ── Small 2×1: scripture label + reference only ──────────────
                Column(
                    GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(palette.background)))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable(actionStartActivity(
                            Intent(context, MainActivity::class.java).apply {
                                val verseId = prefs.getInt("current_verse_id", -1)
                                if (verseId > 0) putExtra("verse_id", verseId)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        ))
                ) {
                    Text(
                        text = widgetScripture?.uppercase() ?: "WISDOMONE",
                        style = TextStyle(
                            color = ColorProvider(Color(palette.accent)),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Text(
                        text = if (hasWidgetCache) "\u201c${widgetText}\u201d" else "Open app to load verse",
                        style = TextStyle(
                            color = ColorProvider(Color(palette.primaryText)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 2
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Row(
                        GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = widgetReference ?: "",
                            style = TextStyle(
                                color = ColorProvider(Color(palette.secondaryText)),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
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
                                val verseId = prefs.getInt("current_verse_id", -1)
                                if (verseId > 0) putExtra("verse_id", verseId)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        ))
                ) {
                    Text(
                        text = widgetScripture?.uppercase() ?: "WISDOMONE",
                        style = TextStyle(
                            color = ColorProvider(Color(palette.accent)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(14.dp))
                    Text(
                        text = if (hasWidgetCache) "\u201c${widgetText}\u201d" else "Open the app to load today\u2019s verse.",
                        style = TextStyle(
                            color = ColorProvider(Color(palette.primaryText)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 6
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Row(
                        GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = widgetReference ?: "",
                            style = TextStyle(
                                color = ColorProvider(Color(palette.secondaryText)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    // ── Bottom row: timestamp + refresh tap ─────────────────
                    Row(
                        GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (updatedLabel.isNotEmpty()) {
                            Text(
                                text = "Tap to refresh · $updatedLabel",
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
        val request = OneTimeWorkRequestBuilder<VerseSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("widget-refresh", ExistingWorkPolicy.REPLACE, request)
    }
}

class ScriptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScriptureWidget()
}
