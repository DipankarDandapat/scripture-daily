package com.scripturedaily.features.mood

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.MoodItem
import com.scripturedaily.data.MoodSummary
import com.scripturedaily.ui.EmptyBox
import com.scripturedaily.ui.ErrorBox
import com.scripturedaily.ui.LoadingBox
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.SurfaceCard
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Offline fallback only; the app normally loads the list from GET /moods. */
val DefaultMoods = listOf(
    MoodItem("anxious", "Anxious", "😟", "Take a slow breath. Here is a verse to steady your heart."),
    MoodItem("happy", "Happy", "😊", "Wonderful! Here is a verse to carry your joy further."),
    MoodItem("sad", "Sad", "😔", "It is okay to feel low. Here is a verse to hold you gently."),
    MoodItem("angry", "Angry", "😠", "Pause for a moment. Here is a verse to cool the fire.")
)

@Composable
fun moodColor(code: String): Color = when (code) {
    "anxious" -> Color(0xFF6C8EAD); "happy" -> Color(0xFFE0A530); "sad" -> Color(0xFF7A62A8); "angry" -> Color(0xFFC0563F); else -> W.Gold
}

/** "How are you feeling today?" — picking a mood swaps the daily verse for one that fits. */
@Composable
fun MoodCard(
    moods: List<MoodItem>, selected: String?, loading: Boolean, message: String?, error: String?,
    onSelect: (String) -> Unit, onAnother: () -> Unit, onClear: () -> Unit, onJournal: () -> Unit
) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("How are you feeling today?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = W.Ink, modifier = Modifier.weight(1f))
            TextButton(onClick = onJournal, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("Journal", fontSize = 12.sp, color = W.Gold, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            moods.forEach { m ->
                val isSel = selected == m.code
                val tint = moodColor(m.code)
                val bg by animateColorAsState(if (isSel) tint else tint.copy(alpha = 0.10f), label = "moodBg")
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(bg)
                        .border(BorderStroke(1.dp, tint.copy(alpha = if (isSel) 1f else 0.35f)), RoundedCornerShape(16.dp))
                        .clickable(enabled = !loading) { onSelect(m.code) }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(m.emoji, fontSize = 26.sp)
                    Text(m.label, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else W.InkMid)
                }
            }
        }
        if (loading) Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = W.Gold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp)); Text("Finding a verse for you…", fontSize = 12.sp, color = W.InkSoft)
        }
        if (error != null) Text(error, fontSize = 12.sp, color = W.Error, modifier = Modifier.padding(top = 10.dp))
        if (selected != null && message != null && !loading) {
            Text(message, fontSize = 13.sp, color = W.InkMid, lineHeight = 19.sp, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onAnother) { Icon(Icons.Outlined.Refresh, null, Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Another verse", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onClear) { Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Today's verse", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
fun MoodJournalScreen(onBack: () -> Unit) {
    OverlayScreen("Mood Journal", onBack) { MoodJournalContent(Modifier.weight(1f), onOpenReflections = {}) }
}

/** Mood statistics + recent check-ins. [onOpenReflections] adds the "Your Reflections" shortcut card from the Journal design. */
@Composable
fun MoodJournalContent(modifier: Modifier = Modifier, onOpenReflections: (() -> Unit)? = null) {
    var summary by remember { mutableStateOf<MoodSummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        error = null
        try { summary = withContext(Dispatchers.IO) { ApiClient.api.moodSummary(30) } }
        catch (_: Exception) { error = "Couldn't load your mood journal. Check your connection." }
    }
    val s = summary
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        when {
            error != null -> ErrorBox(error!!, { attempt++ })
            s == null -> LoadingBox()
            s.total == 0 -> EmptyBox("No check-ins yet. Tap a mood on the Home screen to start your journal.")
            else -> {
                SurfaceCard {
                    Text("Last ${s.days} days · ${s.total} check-ins", fontSize = 13.sp, color = W.InkSoft)
                    s.topMood?.let { top ->
                        val m = DefaultMoods.firstOrNull { it.code == top }
                        Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(W.Gold.copy(alpha = 0.18f)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Most often:  ${m?.emoji ?: ""} ${m?.label ?: top}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = W.Ink)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    DefaultMoods.forEach { m ->
                        val n = s.counts[m.code] ?: 0
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${m.emoji} ${m.label}", fontSize = 13.sp, modifier = Modifier.width(96.dp), color = W.InkMid)
                            Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(moodColor(m.code).copy(alpha = 0.15f))) {
                                Box(Modifier.fillMaxWidth(if (s.total == 0) 0f else n.toFloat() / s.total).height(12.dp).clip(RoundedCornerShape(6.dp)).background(moodColor(m.code)))
                            }
                            Text("$n", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.width(32.dp), color = W.Ink)
                        }
                    }
                }
                val cutoff = LocalDate.now(ZoneId.systemDefault()).minusDays(4) // today + 4 prior = 5 days
                val recentFiveDays = s.recent.filter { c ->
                    runCatching {
                        val local = ZonedDateTime.parse(c.createdAt).withZoneSameInstant(ZoneId.systemDefault())
                        !local.toLocalDate().isBefore(cutoff)
                    }.getOrDefault(true)
                }
                if (recentFiveDays.isNotEmpty()) {
                    Text("My Daily Journal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = W.Ink)
                    recentFiveDays.forEach { c ->
                        val m = DefaultMoods.firstOrNull { it.code == c.mood }
                        val localLabel = remember(c.createdAt) {
                            runCatching {
                                val local = ZonedDateTime.parse(c.createdAt).withZoneSameInstant(ZoneId.systemDefault())
                                local.format(DateTimeFormatter.ofPattern("MMM d · h:mm a", Locale.getDefault()))
                            }.getOrDefault(c.createdAt.take(16).replace('T', ' '))
                        }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(W.Surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(m?.emoji ?: "🙂", fontSize = 22.sp); Spacer(Modifier.width(10.dp))
                            Text(m?.label ?: c.mood, fontWeight = FontWeight.SemiBold, color = W.Ink, modifier = Modifier.weight(1f))
                            Text(localLabel, fontSize = 11.sp, color = W.InkSoft)
                        }
                    }
                }
            }
        }
        if (onOpenReflections != null) SurfaceCard(onClick = onOpenReflections) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(W.SurfaceAlt), contentAlignment = Alignment.Center) { Text("🌱", fontSize = 26.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your Reflections", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = W.Ink)
                    Text("See your daily thoughts and how you're growing over time.", fontSize = 12.sp, color = W.InkMid, lineHeight = 17.sp)
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = W.InkMid)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
