package com.scripturedaily.features.discover

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.R
import com.scripturedaily.ads.AdSlot
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.StorySummary
import com.scripturedaily.features.story.StoryScreen
import com.scripturedaily.features.story.StorySource
import com.scripturedaily.ui.EmptyBox
import com.scripturedaily.ui.ErrorBox
import com.scripturedaily.ui.LoadingBox
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.SurfaceCard
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DiscoverItem(val emoji: String, val title: String, val subtitle: String, val tint: Color) {
    TRUTHS("🌍", "Universal Truths", "See how different traditions speak about love, patience and more — side by side.", Color(0xFFE8833A)),
    CHALLENGES("🏅", "Challenges", "7, 14, 21 or 30 days of gratitude, patience, courage and more. Earn badges.", Color(0xFFD4A574)),
    STORIES("📖", "Stories for Children", "Illustrated stories behind the verses. Read or listen together.", Color(0xFF5E9E6E)),
    STUDIO("🎨", "Studio", "Create verse images, wallpapers and calm videos — with templates for your tradition.", Color(0xFF8A5CC8)),
    WIDGETS("🧩", "Widget Studio", "Compact, large-text and calm-colour widgets with tap-to-refresh.", Color(0xFFB06A7E)),
    JOURNAL("🙂", "Journal", "Your personal space for mood tracking and daily reflections.", Color(0xFF7A62A8))
}

@Composable
fun DiscoverScreen(onOpen: (DiscoverItem) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Hero header — identical to Settings, Saved and Explore
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            Image(
                painter = painterResource(R.drawable.settings_header_landscape), contentDescription = null,
                contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxHeight().fillMaxWidth(0.72f).align(Alignment.CenterEnd)
            )
            Box(Modifier.fillMaxWidth().fillMaxHeight().background(Brush.horizontalGradient(0f to W.Parchment, 0.38f to W.Parchment, 0.62f to W.Parchment.copy(alpha = 0.35f), 1f to Color.Transparent)))
            Box(Modifier.fillMaxWidth().height(44.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(W.Parchment, Color.Transparent))))
            Box(Modifier.fillMaxWidth().height(36.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, W.Parchment))))
            Column(Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 18.dp)) {
                Text("Discover", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = W.Ink)
                Spacer(Modifier.height(4.dp))
                Text("Go deeper with your daily verse.", fontSize = 13.sp, color = W.InkSoft)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AdSlot("discover_top_banner")
        DiscoverItem.values().forEachIndexed { index, item ->
            if (index == 3) AdSlot("discover_inline_native")
            SurfaceCard(onClick = { onOpen(item) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(item.tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Text(item.emoji, fontSize = 27.sp) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = W.Ink)
                        Text(item.subtitle, fontSize = 12.sp, color = W.InkSoft, lineHeight = 17.sp)
                    }
                }
            }
        }
        AdSlot("discover_bottom_banner")
        Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun StoryLibraryScreen(languageId: Int, religionId: Int, onBack: () -> Unit) {
    var stories by remember { mutableStateOf<List<StorySummary>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(languageId, religionId, attempt) {
        error = null
        try { stories = withContext(Dispatchers.IO) { ApiClient.api.stories(languageId, religionId.takeIf { it > 0 }) } }
        catch (_: Exception) { error = "Couldn't load stories. Check your connection and try again." }
    }
    val code = open
    if (code != null) { StoryScreen(StorySource.Code(code), languageId) { open = null }; return }

    OverlayScreen("Stories for Children", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Gentle stories with a verse and a \"What we learn\" line. Swipe through the pictures, or tap play to listen.", fontSize = 13.sp, color = W.InkSoft, lineHeight = 19.sp)
            val s = stories
            when {
                error != null -> ErrorBox(error!!, { attempt++ })
                s == null -> LoadingBox("Loading stories…")
                s.isEmpty() -> EmptyBox("No stories are available in your language yet.")
                else -> s.forEach { st ->
                    SurfaceCard(onClick = { open = st.code }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(W.Gold.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Text(st.emoji, fontSize = 27.sp) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(st.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = W.Ink)
                                Text(if (st.reference.startsWith(st.scripture)) st.reference else "${st.scripture} · ${st.reference}", fontSize = 11.sp, color = W.Gold, fontWeight = FontWeight.SemiBold)
                                if (st.summary.isNotBlank()) Text(st.summary, fontSize = 12.sp, color = W.InkSoft, lineHeight = 17.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text("💡 ${st.lesson}", fontSize = 11.sp, color = W.InkMid, lineHeight = 15.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
