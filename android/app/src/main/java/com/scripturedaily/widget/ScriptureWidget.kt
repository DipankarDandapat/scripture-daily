package com.scripturedaily.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.scripturedaily.MainActivity
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.CachedVerse
import com.scripturedaily.data.ScriptureDatabase
import com.scripturedaily.data.toCached
import com.scripturedaily.lockscreen.LockScreenVerse
import com.scripturedaily.lockscreentime.TimePalette
import java.util.concurrent.TimeUnit

/** The four accessible home/lock-screen layouts the user can pick from the widget picker or Widget Studio. */
enum class WidgetLayout(val label: String, val description: String) {
    STANDARD("Standard", "Scripture label, verse and reference"),
    COMPACT("Compact", "Slim strip for tight home screens"),
    LARGE_TEXT("Large text", "Big, easy-to-read verse"),
    REFERENCE_FIRST("Reference first", "Verse reference up front")
}

/** Calm colour themes; AUTO follows the time of day like the original widget. */
enum class WidgetTheme(val label: String, val bg: Int, val text: Int, val accent: Int, val secondary: Int) {
    AUTO("Time of day", 0, 0, 0, 0),
    DUSK("Dusk", 0xFF2D1B4E.toInt(), 0xFFF5F0E8.toInt(), 0xFFD4A574.toInt(), 0xFFB5A8C4.toInt()),
    SAGE("Sage", 0xFF233D33.toInt(), 0xFFF1F6EF.toInt(), 0xFFB9DDA0.toInt(), 0xFFA9C4B1.toInt()),
    OCEAN("Ocean", 0xFF14344F.toInt(), 0xFFF2F8FC.toInt(), 0xFF9FD8F5.toInt(), 0xFFAECBDD.toInt()),
    SAND("Sand", 0xFFF3E9D6.toInt(), 0xFF2E2418.toInt(), 0xFF9A6B2F.toInt(), 0xFF6F5B45.toInt()),
    MIDNIGHT("Midnight", 0xFF0F0A1A.toInt(), 0xFFEFEAF7.toInt(), 0xFFB9A8F0.toInt(), 0xFF9C93B3.toInt());
}

object WidgetPrefs {
    private const val PREFS = "reader"
    fun theme(context: Context): WidgetTheme =
        WidgetTheme.values().firstOrNull { it.name == context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("widget_theme", null) } ?: WidgetTheme.AUTO
    fun setTheme(context: Context, theme: WidgetTheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("widget_theme", theme.name).apply()
    }
}

private class Colors(val bg: Color, val text: Color, val accent: Color, val secondary: Color)

private class VerseSnapshot(val scripture: String?, val text: String?, val reference: String?, val verseId: Int, val savedAt: Long, val theme: WidgetTheme) {
    companion object {
        fun read(p: SharedPreferences) = VerseSnapshot(
            p.getString("widget_scripture", null), p.getString("widget_text", null), p.getString("widget_reference", null),
            p.getInt("current_verse_id", -1), p.getLong("widget_saved_at", 0L),
            WidgetTheme.values().firstOrNull { it.name == p.getString("widget_theme", null) } ?: WidgetTheme.AUTO
        )
    }
}

private fun colorsFor(context: Context, theme: WidgetTheme): Colors {
    return if (theme == WidgetTheme.AUTO) {
        val p = TimePalette.now(context.getSharedPreferences("reader", Context.MODE_PRIVATE).getInt("palette_interval_hours", 6))
        Colors(Color(p.background), Color(p.primaryText), Color(p.accent), Color(p.secondaryText))
    } else Colors(Color(theme.bg), Color(theme.text), Color(theme.accent), Color(theme.secondary))
}

private fun updatedAgo(savedAt: Long): String {
    if (savedAt <= 0L) return ""
    val mins = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - savedAt)
    return when { mins < 1 -> "Just now"; mins < 60 -> "${mins}m ago"; mins < 1440 -> "${mins / 60}h ago"; else -> "${mins / 1440}d ago" }
}

/** One Glance widget class per layout, all sharing the same content builder. */
open class WisdomWidget(private val layout: WidgetLayout) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("reader", Context.MODE_PRIVATE)
        provideContent {
            // Read inside the composition and observe changes: Glance keeps one session per widget, and values captured
            // before provideContent would stay stale after the tap-to-refresh callback updates the stored verse.
            val snap by produceState(initialValue = VerseSnapshot.read(prefs), prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> value = VerseSnapshot.read(prefs) }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                awaitDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val scripture = snap.scripture
            val text = snap.text
            val reference = snap.reference
            val verseId = snap.verseId
            val savedAt = snap.savedAt
            val colors = colorsFor(context, snap.theme)
            val hasVerse = scripture != null && text != null && reference != null
            val open = actionStartActivity(Intent(context, MainActivity::class.java).apply {
                if (verseId > 0) putExtra("verse_id", verseId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            GlanceTheme {
                val h = LocalSize.current.height
                val tall = h >= 150.dp
                Column(
                    GlanceModifier.fillMaxSize().background(ColorProvider(colors.bg)).padding(if (layout == WidgetLayout.COMPACT) 10.dp else 14.dp)
                ) {
                    // Everything except the refresh button opens the app on this verse.
                    when (layout) {
                        WidgetLayout.COMPACT -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            Column(GlanceModifier.defaultWeight().clickable(open)) {
                                Text(if (hasVerse) "“$text”" else "Open WisdomOne to load a verse",
                                    style = TextStyle(color = ColorProvider(colors.text), fontSize = 12.sp), maxLines = 3)
                                if (hasVerse) Text(reference ?: "", style = TextStyle(color = ColorProvider(colors.secondary), fontSize = 9.sp), maxLines = 1)
                            }
                            Spacer(GlanceModifier.width(8.dp))
                            RefreshButton(colors, compact = true)
                        }
                        WidgetLayout.LARGE_TEXT -> Column(GlanceModifier.fillMaxSize()) {
                            Column(GlanceModifier.defaultWeight().fillMaxWidth().clickable(open)) {
                                Text(if (hasVerse) "“$text”" else "Open WisdomOne to load a verse",
                                    style = TextStyle(color = ColorProvider(colors.text), fontSize = if (tall) 22.sp else 18.sp, fontWeight = FontWeight.Medium), maxLines = if (tall) 7 else 4)
                                Spacer(GlanceModifier.height(6.dp))
                                Text(reference ?: "", style = TextStyle(color = ColorProvider(colors.accent), fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                            }
                            RefreshButton(colors, compact = false)
                        }
                        WidgetLayout.REFERENCE_FIRST -> Column(GlanceModifier.fillMaxSize()) {
                            Column(GlanceModifier.defaultWeight().fillMaxWidth().clickable(open)) {
                                Text(reference ?: "WISDOMONE", style = TextStyle(color = ColorProvider(colors.accent), fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 2)
                                Text(scripture ?: "", style = TextStyle(color = ColorProvider(colors.secondary), fontSize = 11.sp), maxLines = 1)
                                Spacer(GlanceModifier.height(8.dp))
                                Text(if (hasVerse) "“$text”" else "Open WisdomOne to load a verse",
                                    style = TextStyle(color = ColorProvider(colors.text), fontSize = 14.sp), maxLines = if (tall) 6 else 3)
                            }
                            RefreshButton(colors, compact = false)
                        }
                        WidgetLayout.STANDARD -> Column(GlanceModifier.fillMaxSize()) {
                            Column(GlanceModifier.defaultWeight().fillMaxWidth().clickable(open)) {
                                Text(scripture?.uppercase() ?: "WISDOMONE", style = TextStyle(color = ColorProvider(colors.accent), fontSize = 10.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                                Spacer(GlanceModifier.height(8.dp))
                                Text(if (hasVerse) "“$text”" else "Open the app to load today’s verse.",
                                    style = TextStyle(color = ColorProvider(colors.text), fontSize = 14.sp), maxLines = if (tall) 6 else 3)
                            }
                            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(GlanceModifier.defaultWeight()) {
                                    Text(reference ?: "", style = TextStyle(color = ColorProvider(colors.secondary), fontSize = 10.sp), maxLines = 1)
                                    val ago = updatedAgo(savedAt)
                                    if (ago.isNotEmpty() && tall) Text("Updated $ago", style = TextStyle(color = ColorProvider(colors.secondary), fontSize = 9.sp))
                                }
                                RefreshButton(colors, compact = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun RefreshButton(colors: Colors, compact: Boolean) {
    Box(
        GlanceModifier
            .background(ColorProvider(colors.accent.copy(alpha = 0.22f))).cornerRadius(18.dp)
            .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 8.dp else 7.dp)
            .semantics { contentDescription = "Show a different verse" }
            .clickable(actionRunCallback<CycleVerseCallback>()),
        contentAlignment = Alignment.Center
    ) {
        Text(if (compact) "↻" else "↻  New verse", style = TextStyle(color = ColorProvider(colors.accent), fontSize = if (compact) 18.sp else 12.sp, fontWeight = FontWeight.Bold))
    }
}

class ScriptureWidget : WisdomWidget(WidgetLayout.STANDARD) {
    companion object {
        /** Removes the verse snapshot (e.g. after the tradition changes and nothing in the new tradition is cached yet). */
        fun clearCachedVerse(context: Context) {
            context.getSharedPreferences("reader", Context.MODE_PRIVATE).edit()
                .remove("current_verse_id").remove("widget_scripture").remove("widget_text").remove("widget_reference").remove("widget_saved_at").commit()
        }

        fun cacheVerseForWidget(context: Context, verse: CachedVerse) {
            context.getSharedPreferences("reader", Context.MODE_PRIVATE).edit()
                .putInt("current_verse_id", verse.id)
                .putString("widget_scripture", verse.scripture)
                .putString("widget_text", verse.text)
                .putString("widget_reference", verse.reference())
                .putLong("widget_saved_at", verse.savedAt)
                .commit()
        }

        val ALL: List<Pair<WisdomWidget, Class<out GlanceAppWidgetReceiver>>> by lazy {
            listOf(
                ScriptureWidget() to ScriptureWidgetReceiver::class.java,
                CompactWidget() to CompactWidgetReceiver::class.java,
                LargeTextWidget() to LargeTextWidgetReceiver::class.java,
                ReferenceFirstWidget() to ReferenceFirstWidgetReceiver::class.java
            )
        }

        /** Redraws every placed WisdomOne widget, whatever its layout. */
        suspend fun updateEverywhere(context: Context) {
            ALL.forEach { (widget, _) ->
                GlanceAppWidgetManager(context).getGlanceIds(widget.javaClass).forEach { widget.update(context, it) }
            }
        }
    }

    suspend fun refreshAll(context: Context) {
        updateEverywhere(context)
        ALL.forEach { (_, receiver) ->
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, receiver))
            if (ids.isNotEmpty()) {
                context.sendBroadcast(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids); setPackage(context.packageName)
                })
            }
        }
    }
}

class CompactWidget : WisdomWidget(WidgetLayout.COMPACT)
class LargeTextWidget : WisdomWidget(WidgetLayout.LARGE_TEXT)
class ReferenceFirstWidget : WisdomWidget(WidgetLayout.REFERENCE_FIRST)

/**
 * "Tap to refresh": swaps in a different verse straight from the widget, without opening the app.
 * Uses the network when available and falls back to verses already cached on the device.
 */
class CycleVerseCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ApiClient.ensureInit(context)
        val prefs = context.getSharedPreferences("reader", Context.MODE_PRIVATE)
        val religion = prefs.getInt("religion", 1)
        val language = prefs.getInt("language", 0).takeIf { it > 0 } ?: 1
        val scripture = prefs.getInt("scripture", 0)  // only labels the offline cache; the server draws from the whole tradition
        val current = prefs.getInt("current_verse_id", -1)
        val dao = ScriptureDatabase.get(context).verses()

        val fresh: CachedVerse? = try {
            val remote = ApiClient.api.random(religion, language, null, current.takeIf { it > 0 })
            val cached = remote.toCached(scripture, language, dao.find(remote.id)?.favorite == true)
            dao.save(cached)
            cached
        } catch (_: Exception) {
            val pool = if (scripture > 0) dao.forCombo(scripture, language) else emptyList()
            if (pool.isEmpty()) null else pool.firstOrNull { it.id > current } ?: pool.first()
        }
        if (fresh != null) {
            ScriptureWidget.cacheVerseForWidget(context, fresh)
            prefs.edit().putInt("last_verse_id", fresh.id).apply()
            runCatching { LockScreenVerse.applyIfEnabled(context, fresh.id, fresh.scripture, fresh.text, fresh.reference()) }
        }
        ScriptureWidget.updateEverywhere(context)
    }
}

class RefreshWidgetCallback : ActionCallback {  // kept for widgets placed by older app versions
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        CycleVerseCallback().onAction(context, glanceId, parameters)
}

class ScriptureWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ScriptureWidget() }
class CompactWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = CompactWidget() }
class LargeTextWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = LargeTextWidget() }
class ReferenceFirstWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ReferenceFirstWidget() }
