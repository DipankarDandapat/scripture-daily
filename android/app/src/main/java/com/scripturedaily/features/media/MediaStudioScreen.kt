package com.scripturedaily.features.media

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.scripturedaily.data.Verse
import com.scripturedaily.features.share.CardContent
import com.scripturedaily.features.share.CardFormat
import com.scripturedaily.features.share.CardPreview
import com.scripturedaily.features.share.CardRenderer
import com.scripturedaily.features.share.CardStyle
import com.scripturedaily.features.share.FontControls
import com.scripturedaily.features.share.PatternRow
import com.scripturedaily.features.share.SectionLabel
import com.scripturedaily.features.share.ShareActions
import com.scripturedaily.features.share.StudioPrefs
import com.scripturedaily.features.share.TemplateRow
import com.scripturedaily.ui.ErrorBox
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.Pill
import com.scripturedaily.ui.W
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MediaStudioScreen(verse: Verse, onBack: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var style by remember { mutableStateOf(StudioPrefs.load(context).copy(format = CardFormat.STORY)) }
    OverlayScreen("Video & Wallpaper", onBack) {
        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = W.Ink) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Wallpaper", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Outlined.Wallpaper, null, Modifier.size(18.dp)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Video", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Outlined.Movie, null, Modifier.size(18.dp)) })
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tab == 0) WallpaperTab(verse, style) { style = it } else VideoTab(verse, style) { style = it }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StyleControls(style: CardStyle, onChange: (CardStyle) -> Unit) {
    SectionLabel("Template"); TemplateRow(style.template) { onChange(style.copy(template = it)) }
    SectionLabel("Background"); PatternRow(style.pattern) { onChange(style.copy(pattern = it)) }
    SectionLabel("Text size"); FontControls(style, onChange)
}

// ── Wallpaper ────────────────────────────────────────────────────────────────
@Composable
private fun WallpaperTab(verse: Verse, style: CardStyle, onChange: (CardStyle) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val metrics = context.resources.displayMetrics
    val screenW = metrics.widthPixels; val screenH = metrics.heightPixels
    val wallStyle = style.copy(showMark = style.showMark)
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(verse.id, wallStyle) {
        preview = withContext(Dispatchers.Default) {
            CardRenderer.renderSized(context, CardContent.from(verse), wallStyle, 540, (540f * screenH / screenW).toInt())
        }
    }
    Text("A wallpaper sized exactly for your screen (${screenW}×${screenH}). The reference and source are always shown.", fontSize = 12.sp, color = W.InkSoft)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.height(420.dp).aspectRatio(screenW.toFloat() / screenH).shadow(10.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) { preview?.let { Image(it.asImageBitmap(), "Wallpaper preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) } }
    }
    StyleControls(style, onChange)
    SectionLabel("Set as")
    suspend fun apply(flag: Int, label: String) {
        busy = true
        val result = runCatching {
            withContext(Dispatchers.Default) {
                val bmp = CardRenderer.renderSized(context, CardContent.from(verse), wallStyle, screenW, screenH)
                WallpaperManager.getInstance(context).setBitmap(bmp, null, true, flag)
                bmp.recycle()
            }
        }
        busy = false
        Toast.makeText(context, if (result.isSuccess) "$label wallpaper updated" else "Couldn't set the wallpaper on this device", Toast.LENGTH_LONG).show()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { scope.launch { apply(WallpaperManager.FLAG_SYSTEM, "Home screen") } }, enabled = !busy, modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Ink), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text("Home", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        Button(onClick = { scope.launch { apply(WallpaperManager.FLAG_LOCK, "Lock screen") } }, enabled = !busy, modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Ink), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text("Lock", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        Button(onClick = { scope.launch { apply(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, "Home & lock screen") } }, enabled = !busy, modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Ink), contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text("Both", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    }
    OutlinedButton(
        onClick = {
            scope.launch {
                busy = true
                val uri = withContext(Dispatchers.Default) {
                    ShareActions.saveImageToGallery(context, CardRenderer.renderSized(context, CardContent.from(verse), wallStyle, screenW, screenH), "WisdomOne_wallpaper_${System.currentTimeMillis()}")
                }
                busy = false
                Toast.makeText(context, if (uri != null) "Saved to Pictures/WisdomOne" else "Saving needs Android 10+", Toast.LENGTH_SHORT).show()
            }
        },
        enabled = !busy, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.5.dp, W.Gold.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = W.Ink)
    ) { Icon(Icons.Outlined.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save to gallery", fontWeight = FontWeight.SemiBold) }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = W.Gold)
}

// ── Video ────────────────────────────────────────────────────────────────────
@Composable
private fun VideoTab(verse: Verse, style: CardStyle, onChange: (CardStyle) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var seconds by remember { mutableStateOf(12) }
    var music by remember { mutableStateOf(MusicStyle.CALM_PAD) }
    var progress by remember { mutableFloatStateOf(0f) }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<File?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val caption = "“${verse.text}”\n\n— ${verse.citation()}\n\nvia WisdomOne"

    if (result == null) {
        Text("Creates a vertical 9:16 video with a slow zoom, floating light and your verse fading in, with calm music generated on your device. The verse reference and source stay on screen.", fontSize = 12.sp, color = W.InkSoft, lineHeight = 17.sp)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CardPreview(verse, style.copy(format = CardFormat.STORY), Modifier.height(400.dp)) }
        StyleControls(style, onChange)
        SectionLabel("Length")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf(8, 12, 15)) { s -> Pill("$s sec", seconds == s, { if (!running) seconds = s }) } }
        SectionLabel("Calm music")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(MusicStyle.values().toList()) { m -> Pill(m.label, music == m, { if (!running) music = m }) } }
        Text(music.description, fontSize = 11.sp, color = W.InkSoft)
        error?.let { ErrorBox(it, null, Modifier.padding(0.dp)) }
        if (running) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = W.Gold, trackColor = W.Gold.copy(alpha = 0.2f))
            Text("Creating your video… ${(progress * 100).toInt()}%", fontSize = 12.sp, color = W.InkSoft)
            OutlinedButton(onClick = { job?.cancel() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        } else {
            Button(
                onClick = {
                    error = null; running = true; progress = 0f
                    job = scope.launch {
                        try {
                            val out = File(context.cacheDir, "wisdomone_verse_${System.currentTimeMillis()}.mp4")
                            VideoGenerator.generate(context, CardContent.from(verse), VideoOptions(seconds, music, style.copy(format = CardFormat.STORY)), out) { progress = it }
                            result = out
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = "Couldn't create the video on this device (${e.message ?: e.javaClass.simpleName}). Try a shorter length or no music."
                        } finally { running = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Ink)
            ) { Icon(Icons.Outlined.Movie, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Create video", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
        }
    } else {
        val file = result!!
        Text("Your video is ready", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = W.Ink)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.fromFile(file))
                        setOnPreparedListener { it.isLooping = true; start() }
                    }
                },
                modifier = Modifier.height(440.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(18.dp))
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { ShareActions.shareVideo(context, ShareActions.videoUri(context, file), caption) }, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Ink), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Share / Reels", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            OutlinedButton(onClick = { ShareActions.shareToWhatsApp(context, ShareActions.videoUri(context, file), "video/mp4", caption) }, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, Color(0xFF25D366).copy(alpha = 0.7f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1FA855)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) { Text("WhatsApp Status", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        }
        OutlinedButton(
            onClick = {
                val uri = ShareActions.saveVideoToGallery(context, file, "WisdomOne_${System.currentTimeMillis()}")
                Toast.makeText(context, if (uri != null) "Saved to Movies/WisdomOne" else "Saving needs Android 10+. Use Share instead.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.5.dp, W.Gold.copy(alpha = 0.6f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = W.Ink)
        ) { Icon(Icons.Outlined.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save to gallery", fontWeight = FontWeight.SemiBold) }
        OutlinedButton(onClick = { result = null }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) { Text("Make another") }
    }
}
