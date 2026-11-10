package com.scripturedaily.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
<<<<<<< HEAD
=======
import androidx.compose.material3.Slider
>>>>>>> settings_redesign
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.lockscreentime.TimePalette
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.W
import kotlinx.coroutines.launch

@Composable
fun WidgetStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var theme by remember { mutableStateOf(WidgetPrefs.theme(context)) }
<<<<<<< HEAD
=======
    var textScale by remember { mutableStateOf(context.getSharedPreferences("reader", android.content.Context.MODE_PRIVATE).getInt("widget_text_scale", 100).coerceIn(80, 120)) }
>>>>>>> settings_redesign
    val prefs = context.getSharedPreferences("reader", android.content.Context.MODE_PRIVATE)
    val scripture = prefs.getString("widget_scripture", null) ?: "Bhagavad Gita"
    val text = prefs.getString("widget_text", null) ?: "Do your best, and let go of worrying about the result."
    val reference = prefs.getString("widget_reference", null) ?: "Chapter 2 · Verse 47"

    OverlayScreen("Widget Studio", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
<<<<<<< HEAD
            Text("Pick a look, then add a widget to your home screen or lock screen. Tap the ↻ button on a widget to see a new verse without opening the app.", fontSize = 13.sp, color = W.InkSoft, lineHeight = 19.sp)
            Text("COLOUR THEME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(WidgetTheme.values().toList()) { t ->
=======
            Text("Pick a look, then add a widget to your home screen. The refresh control redraws the current active verse; use Discover on Home when you want a different verse.", fontSize = 13.sp, color = W.InkSoft, lineHeight = 19.sp)
            Text("COLOUR THEME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(WidgetTheme.values().filter { it != WidgetTheme.AUTO }) { t ->
>>>>>>> settings_redesign
                    val c = themeColors(context, t)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                        theme = t; WidgetPrefs.setTheme(context, t)
                        scope.launch { ScriptureWidget.updateEverywhere(context) }
                    }) {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(c.first)
                                .border(if (theme == t) 3.dp else 1.dp, if (theme == t) W.Gold else W.Gold.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("Aa", color = c.second, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        Spacer(Modifier.height(4.dp))
                        Text(t.label, fontSize = 11.sp, color = if (theme == t) W.Ink else W.InkSoft, fontWeight = if (theme == t) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
<<<<<<< HEAD
=======
            Text("TEXT SIZE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("A", color = W.InkSoft, fontSize = 12.sp)
                Slider(value = textScale.toFloat(), onValueChange = { textScale = it.toInt() }, valueRange = 80f..120f, steps = 7, modifier = Modifier.weight(1f))
                Text("A", color = W.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text("${textScale}% · Applies to supported widget layouts", fontSize = 11.sp, color = W.InkSoft)
            OutlinedButton(onClick = {
                prefs.edit().putInt("widget_text_scale", textScale).apply()
                scope.launch { ScriptureWidget.updateEverywhere(context) }
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Apply text size", fontWeight = FontWeight.SemiBold) }
>>>>>>> settings_redesign
            Text("LAYOUTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
            val receivers = mapOf(
                WidgetLayout.STANDARD to ScriptureWidgetReceiver::class.java, WidgetLayout.COMPACT to CompactWidgetReceiver::class.java,
                WidgetLayout.LARGE_TEXT to LargeTextWidgetReceiver::class.java, WidgetLayout.REFERENCE_FIRST to ReferenceFirstWidgetReceiver::class.java
            )
            WidgetLayout.values().forEach { layout ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(layout.label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = W.Ink)
                            Text(layout.description, fontSize = 12.sp, color = W.InkSoft)
                        }
                        OutlinedButton(onClick = {
                            val manager = AppWidgetManager.getInstance(context)
                            if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, receivers.getValue(layout)), null, null)
                            else Toast.makeText(context, "Long-press your home screen, open Widgets and add WisdomOne · ${layout.label}.", Toast.LENGTH_LONG).show()
                        }, shape = RoundedCornerShape(12.dp)) { Text("Add", fontWeight = FontWeight.SemiBold) }
                    }
<<<<<<< HEAD
                    WidgetPreview(layout, theme, scripture, text, reference)
=======
                    WidgetPreview(layout, theme, scripture, text, reference, textScale / 100f)
>>>>>>> settings_redesign
                }
            }
            Text("Lock screen: on Android 14+ and many Samsung/Pixel phones, open Lock screen settings → Widgets and add a WisdomOne widget. The refresh button works there too.", fontSize = 12.sp, color = W.InkSoft, lineHeight = 17.sp)
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun themeColors(context: android.content.Context, t: WidgetTheme): Pair<Color, Color> {
    if (t == WidgetTheme.AUTO) {
<<<<<<< HEAD
        val p = TimePalette.now(context.getSharedPreferences("reader", android.content.Context.MODE_PRIVATE).getInt("palette_interval_hours", 6))
=======
        val p = TimePalette.now()
>>>>>>> settings_redesign
        return Color(p.background) to Color(p.primaryText)
    }
    return Color(t.bg) to Color(t.text)
}

/** A Compose replica of the Glance layouts so users can see what they are adding. */
@Composable
<<<<<<< HEAD
private fun WidgetPreview(layout: WidgetLayout, theme: WidgetTheme, scripture: String, text: String, reference: String) {
=======
private fun WidgetPreview(layout: WidgetLayout, theme: WidgetTheme, scripture: String, text: String, reference: String, textScale: Float) {
>>>>>>> settings_redesign
    val context = LocalContext.current
    val bgText = themeColors(context, theme)
    val p = TimePalette.now()
    val accent = if (theme == WidgetTheme.AUTO) Color(p.accent) else Color(theme.accent)
    val secondary = if (theme == WidgetTheme.AUTO) Color(p.secondaryText) else Color(theme.secondary)
    val (bg, fg) = bgText
    val refresh: @Composable (Boolean) -> Unit = { compact ->
        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(accent.copy(alpha = 0.22f)).padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 7.dp)) {
<<<<<<< HEAD
            Text(if (compact) "↻" else "↻  New verse", color = accent, fontWeight = FontWeight.Bold, fontSize = if (compact) 18.sp else 12.sp)
=======
            Text(if (compact) "↻" else "↻  Refresh", color = accent, fontWeight = FontWeight.Bold, fontSize = if (compact) 18.sp else 12.sp)
>>>>>>> settings_redesign
        }
    }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(bg).padding(if (layout == WidgetLayout.COMPACT) 10.dp else 14.dp)) {
        when (layout) {
            WidgetLayout.COMPACT -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
<<<<<<< HEAD
                    Text("“$text”", color = fg, fontSize = 12.sp, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(reference, color = secondary, fontSize = 9.sp)
=======
                    Text("“$text”", color = fg, fontSize = (12f * textScale).sp, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(reference, color = secondary, fontSize = (9f * textScale).sp)
>>>>>>> settings_redesign
                }
                Spacer(Modifier.width(8.dp)); refresh(true)
            }
            WidgetLayout.LARGE_TEXT -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
<<<<<<< HEAD
                Text("“$text”", color = fg, fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium, maxLines = 5, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(reference, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { refresh(false) }
            }
            WidgetLayout.REFERENCE_FIRST -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(reference, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(scripture, color = secondary, fontSize = 11.sp)
                Text("“$text”", color = fg, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { refresh(false) }
            }
            WidgetLayout.STANDARD -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(scripture.uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("“$text”", color = fg, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(reference, color = secondary, fontSize = 10.sp, modifier = Modifier.weight(1f)); refresh(false)
=======
                Text("“$text”", color = fg, fontSize = (20f * textScale).sp, lineHeight = (27f * textScale).sp, fontWeight = FontWeight.Medium, maxLines = 5, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(reference, color = accent, fontSize = (13f * textScale).sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { refresh(false) }
            }
            WidgetLayout.REFERENCE_FIRST -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(reference, color = accent, fontSize = (17f * textScale).sp, fontWeight = FontWeight.Bold)
                Text(scripture, color = secondary, fontSize = (11f * textScale).sp)
                Text("“$text”", color = fg, fontSize = (14f * textScale).sp, lineHeight = (20f * textScale).sp, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { refresh(false) }
            }
            WidgetLayout.STANDARD -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(scripture.uppercase(), color = accent, fontSize = (10f * textScale).sp, fontWeight = FontWeight.Bold)
                Text("“$text”", color = fg, fontSize = (14f * textScale).sp, lineHeight = (20f * textScale).sp, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(reference, color = secondary, fontSize = (10f * textScale).sp, modifier = Modifier.weight(1f)); refresh(false)
>>>>>>> settings_redesign
                }
            }
        }
    }
}
