package com.scripturedaily.features.story

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.StoryDetail
import com.scripturedaily.ui.ErrorBox
import com.scripturedaily.ui.LoadingBox
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.Pill
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface StorySource {
    data class Code(val code: String) : StorySource
    data class ForVerse(val verseId: Int) : StorySource
}

private fun paletteColors(p: String): Pair<Color, Color> = when (p) {
    "forest" -> Color(0xFFEAF4EC) to Color(0xFFD2E8D8)
    "ocean" -> Color(0xFFE7F1F8) to Color(0xFFCDE3F1)
    "rose" -> Color(0xFFF9ECF0) to Color(0xFFF1D3DC)
    "sand" -> Color(0xFFFAF1DF) to Color(0xFFF1E0BC)
    else -> Color(0xFFF1EAF8) to Color(0xFFDFD2F0)
}

@Composable
fun StoryScreen(source: StorySource, languageId: Int, onBack: () -> Unit) {
    var story by remember { mutableStateOf<StoryDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(source, languageId, attempt) {
        story = null; error = null
        try {
            story = withContext(Dispatchers.IO) {
                when (source) {
                    is StorySource.Code -> ApiClient.api.story(source.code, languageId)
                    is StorySource.ForVerse -> {
                        val r = ApiClient.api.storyForVerse(source.verseId, languageId)
                        if (r.isSuccessful) r.body() else throw IllegalStateException("There is no story for this verse yet.")
                    }
                }
            }
        } catch (e: IllegalStateException) { error = e.message } catch (e: Exception) { error = "Couldn't load the story. Check your connection and try again." }
    }

    OverlayScreen("Story behind the verse", onBack, background = story?.let { paletteColors(it.palette).first } ?: W.Parchment) {
        val s = story
        when {
            error != null -> ErrorBox(error!!, if (source is StorySource.Code || error!!.startsWith("Couldn't")) ({ attempt++ }) else null)
            s == null -> LoadingBox("Opening the story…")
            else -> StoryBody(s)
        }
    }
}

@Composable
private fun StoryBody(s: StoryDetail) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = s.scenes.size + 1
    val pager = rememberPagerState(pageCount = { pages })
    var playing by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var speed by remember { mutableStateOf(0.9f) }
    var narratorPage by remember { mutableIntStateOf(-1) }
    val texts = remember(s) { s.scenes.map { it.text } + s.lesson }
    val (bgA, bgB) = paletteColors(s.palette)

    val narrator = remember(s.code, s.language) {
        StoryNarrator(
            context, s.language,
            onSceneStart = { i -> narratorPage = i; scope.launch { pager.animateScrollToPage(i) } },
            onFinished = { playing = false; narratorPage = -1 },
            onProblem = { msg -> problem = msg; playing = false }
        )
    }
    DisposableEffect(narrator) { onDispose { narrator.shutdown() } }
    LaunchedEffect(speed) { narrator.setRate(speed) }

    // If the reader swipes elsewhere while it's playing, continue from the page they chose.
    LaunchedEffect(pager.currentPage) {
        if (playing && narratorPage != -1 && pager.currentPage != narratorPage) narrator.play(texts, pager.currentPage)
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(bgA, bgB)))) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = W.Ink, lineHeight = 24.sp)
                    Text(if (s.reference.startsWith(s.scripture)) s.reference else "${s.scripture} · ${s.reference}", fontSize = 12.sp, color = W.InkSoft)
                }
            }
            if (s.languageFallback) Text("This story isn't available in your language yet, so it is shown in English.", fontSize = 11.sp, color = W.InkSoft, modifier = Modifier.padding(horizontal = 20.dp))
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (page < s.scenes.size) {
                        val scene = s.scenes[page]
                        StoryIllustration(scene.art, s.palette, Modifier.fillMaxWidth().aspectRatio(1.25f).clip(RoundedCornerShape(26.dp)).border(BorderStroke(1.dp, W.Gold.copy(alpha = 0.3f)), RoundedCornerShape(26.dp)))
                        Text("Scene ${page + 1} of ${s.scenes.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.sp)
                        Text(scene.text, fontSize = 20.sp, lineHeight = 31.sp, color = W.Ink, fontWeight = FontWeight.Medium)
                    } else {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.75f)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("WHAT WE LEARN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
                            Text(s.lesson, fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, color = W.Ink)
                            if (s.values.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { s.values.take(3).forEach { Pill(it.replaceFirstChar { c -> c.uppercase() }) } }
                        }
                        s.verse?.let { v ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF1A1025), Color(0xFF2D1B4E)))).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("THE VERSE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
                                Text("“${v.text}”", fontSize = 17.sp, lineHeight = 26.sp, color = Color(0xFFF5F0E8))
                                Text("${v.citation()}", fontSize = 12.sp, color = W.Gold, fontWeight = FontWeight.SemiBold)
                                v.translation?.let { Text("Source: $it", fontSize = 11.sp, color = Color(0xFFB5A8C4)) }
                            }
                        }
                        TextButton(onClick = { scope.launch { pager.animateScrollToPage(0) } }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Read again from the start", color = W.Gold, fontWeight = FontWeight.SemiBold) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            // ── player ──
            problem?.let {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(14.dp)).background(W.Error.copy(alpha = 0.1f)).padding(12.dp)) {
                    Text(it, fontSize = 12.sp, color = W.Error)
                    TextButton(onClick = { runCatching { context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }; problem = null }) { Text("Open voice settings", color = W.Error) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
                repeat(pages) { i -> Box(Modifier.padding(3.dp).size(if (i == pager.currentPage) 9.dp else 7.dp).clip(CircleShape).background(if (i == pager.currentPage) W.Gold else W.Gold.copy(alpha = 0.3f))) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Pill("${"%.1f".format(speed)}x", onClick = { speed = when { speed < 0.85f -> 0.9f; speed < 1.0f -> 1.1f; else -> 0.7f } })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).clickable(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.SkipPrevious, "Previous scene", tint = if (pager.currentPage > 0) W.Ink else W.InkSoft.copy(alpha = 0.4f))
                    }
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(W.Ink).clickable {
                            problem = null
                            if (playing) { narrator.stop(); playing = false; narratorPage = -1 }
                            else { playing = true; narrator.play(texts, if (pager.currentPage >= pages - 1) 0 else pager.currentPage) }
                        },
                        contentAlignment = Alignment.Center
                    ) { Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (playing) "Pause story" else "Listen to the story", tint = Color.White, modifier = Modifier.size(32.dp)) }
                    Box(Modifier.size(44.dp).clip(CircleShape).clickable(enabled = pager.currentPage < pages - 1) { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.SkipNext, "Next scene", tint = if (pager.currentPage < pages - 1) W.Ink else W.InkSoft.copy(alpha = 0.4f))
                    }
                }
                Text(if (playing) "Listening…" else "Read or listen", fontSize = 11.sp, color = W.InkSoft, textAlign = TextAlign.End, modifier = Modifier.width(72.dp))
            }
        }
    }
}
