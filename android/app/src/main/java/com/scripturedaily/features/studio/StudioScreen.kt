package com.scripturedaily.features.studio

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.scripturedaily.R
import com.scripturedaily.data.Verse
import com.scripturedaily.features.media.BgMotion
import com.scripturedaily.features.media.FrameComposer
import com.scripturedaily.features.media.MusicPreview
import com.scripturedaily.features.media.MusicStyle
import com.scripturedaily.features.media.TextAnimation
import com.scripturedaily.features.media.VideoGenerator
import com.scripturedaily.features.media.VideoOptions
import com.scripturedaily.features.media.VideoTransition
import com.scripturedaily.features.share.*
import com.scripturedaily.ui.ErrorBox
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.Pill
import com.scripturedaily.ui.W
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class StudioTab(val label: String) { SHARE("Share Studio"), VIDEO("Video Studio") }

/**
 * The single place to create images and videos. Two tabs — Share Studio (cards, wallpapers) and Video Studio —
 * share one set of tradition-aware templates, which refresh whenever the user's tradition changes.
 */
@Composable
fun StudioScreen(verse: Verse, religionId: Int, religionCode: String?, initialTab: StudioTab, onBack: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(initialTab) }
    var templates by remember(religionCode) { mutableStateOf(StudioTemplates.load(context, religionCode)) }
    var style by remember(religionCode) { mutableStateOf(StudioPrefs.load(context, templates)) }

    // Refresh from the server (database-managed templates) whenever the tradition changes.
    LaunchedEffect(religionCode, religionId) {
        StudioTemplates.refresh(context, religionId.takeIf { it > 0 }, religionCode)?.let { fresh ->
            templates = fresh
            style = style.copy(template = StudioTemplates.find(fresh, style.template.code))
        }
    }
    LaunchedEffect(style) { StudioPrefs.save(context, style) }

    OverlayScreen("Studio", onBack) {
        TabRow(selectedTabIndex = tab.ordinal, containerColor = Color.Transparent, contentColor = W.Ink) {
            StudioTab.values().forEach { t ->
                Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(if (t == StudioTab.SHARE) Icons.Outlined.Share else Icons.Outlined.Movie, null, Modifier.size(18.dp)) })
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tab == StudioTab.SHARE) ShareTab(verse, style, templates) { style = it }
            else VideoTab(verse, style, templates) { style = it }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── shared controls ────────────────────────────────────────────────────────────
@Composable
private fun Label(text: String) = Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 6.dp))

@Composable
private fun TemplateRow(templates: List<StudioTemplate>, selected: StudioTemplate, onSelect: (StudioTemplate) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(templates, key = { it.code }) { t ->
            val isSel = selected.code == t.code
            Column(
                Modifier.width(78.dp).clickable { onSelect(t) }.semantics { contentDescription = "${t.label} template" },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(Color(t.top), Color(t.bottom))))
                        .border(BorderStroke(if (isSel) 3.dp else 1.dp, if (isSel) W.Gold else W.Gold.copy(alpha = 0.3f)), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(t.emoji.ifBlank { "Aa" }, fontSize = 22.sp, color = Color(t.text)) }
                Spacer(Modifier.height(4.dp))
                Text(t.label, fontSize = 11.sp, maxLines = 2, lineHeight = 13.sp, color = if (isSel) W.Ink else W.InkSoft,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(if (t.isCommon) "Common" else "For you", fontSize = 9.sp, color = if (t.isCommon) W.InkSoft else W.Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun <T> ChipRow(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(items) { i -> Pill(label(i), selected == i, { onSelect(i) }) } }
}

@Composable
private fun SliderRow(value: Float, range: ClosedFloatingPointRange<Float>, caption: String, onChange: (Float) -> Unit) {
    Column {
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = W.Gold, activeTrackColor = W.Gold, inactiveTrackColor = W.Gold.copy(alpha = 0.25f)))
        Text(caption, fontSize = 11.sp, color = W.InkSoft)
    }
}

@Composable
private fun ColorDots(selected: Int?, onSelect: (Int?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        item {
            Pill("Auto", selected == null, { onSelect(null) })
        }
        items(TEXT_COLORS) { (name, color) ->
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color(color))
                    .border(BorderStroke(if (selected == color) 3.dp else 1.dp, if (selected == color) W.Gold else W.InkSoft.copy(alpha = 0.4f)), CircleShape)
                    .clickable { onSelect(color) }.semantics { contentDescription = "$name text colour" }
            )
        }
    }
}

@Composable
private fun SwatchDots(selected: BgSwatch?, onSelect: (BgSwatch?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        item { Pill("Template", selected == null, { onSelect(null) }) }
        items(StudioTemplates.SWATCHES) { sw ->
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(sw.top), Color(sw.bottom))))
                    .border(BorderStroke(if (selected == sw) 3.dp else 1.dp, if (selected == sw) W.Gold else W.InkSoft.copy(alpha = 0.4f)), CircleShape)
                    .clickable { onSelect(sw) }.semantics { contentDescription = "${sw.label} background" }
            )
        }
    }
}

/** Text controls common to both tabs. */
@Composable
private fun TextControls(style: CardStyle, full: Boolean, onChange: (CardStyle) -> Unit) {
    Label("Font")
    ChipRow(CardFont.values().toList(), style.font, { it.label }) { onChange(style.copy(font = it)) }
    Label("Text size · ${(style.fontScale * 100).toInt()}%")
    SliderRow(style.fontScale, 0.7f..1.5f, "Long verses shrink automatically so they always fit.") { onChange(style.copy(fontScale = Math.round(it * 20) / 20f)) }
    Label("Text position")
    ChipRow(TextPlacement.values().toList(), style.placement, { it.label }) { onChange(style.copy(placement = it)) }
    if (full) {
        Label("Alignment")
        ChipRow(TextAlignment.values().toList(), style.align, { it.label }) { onChange(style.copy(align = it)) }
        Label("Line spacing · ${"%.1f".format(style.spacing)}×")
        SliderRow(style.spacing, 0.8f..1.8f, "") { onChange(style.copy(spacing = Math.round(it * 10) / 10f)) }
    }
    Label("Text colour")
    ColorDots(style.textColor) { onChange(style.copy(textColor = it)) }
}

// ── Share Studio ───────────────────────────────────────────────────────────────
@Composable
private fun ShareTab(verse: Verse, style: CardStyle, templates: List<StudioTemplate>, onChange: (CardStyle) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var wallpaperDialog by remember { mutableStateOf(false) }
    val caption = "“${verse.text}”\n\n— ${verse.citation()}\n\nvia WisdomOne"
    suspend fun renderFull(): Bitmap = withContext(Dispatchers.Default) { CardRenderer.render(context, CardContent.from(verse), style) }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CardPreview(verse, style, Modifier.height(430.dp)) }
    Label("Template")
    TemplateRow(templates, style.template) { onChange(style.copy(template = it)) }
    Label("Format")
    ChipRow(CardFormat.values().toList(), style.format, { "${it.label} · ${it.hint}" }) { onChange(style.copy(format = it)) }
    Label("Background colour")
    SwatchDots(style.swatch) { onChange(style.copy(swatch = it)) }
    Label("Background art")
    ChipRow(CardPattern.values().toList(), style.pattern, { it.label }) { onChange(style.copy(pattern = it)) }
    TextControls(style, full = true, onChange = onChange)
    Label("Branding")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Show WisdomOne mark", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = W.Ink)
            Text("A small logo so friends can find the app. The reference and source always stay visible.", fontSize = 11.sp, color = W.InkSoft, lineHeight = 15.sp)
        }
        Switch(checked = style.showMark, onCheckedChange = { onChange(style.copy(showMark = it)) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = W.Gold))
    }
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { scope.launch { busy = true; ShareActions.shareImage(context, renderFull(), caption); busy = false } },
            enabled = !busy, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = W.Ink), contentPadding = PaddingValues(horizontal = 8.dp)
        ) { Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Share", fontWeight = FontWeight.SemiBold) }
        OutlinedButton(
            onClick = {
                scope.launch {
                    busy = true
                    val uri = ShareActions.writeCacheImage(context, renderFull(), "wisdomone_status.png")
                    ShareActions.shareToWhatsApp(context, uri, "image/png", caption); busy = false
                }
            },
            enabled = !busy, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, Color(0xFF25D366).copy(alpha = 0.7f)), contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1FA855))
        ) { Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_whatsapp), null, Modifier.size(18.dp), tint = Color.Unspecified); Spacer(Modifier.width(6.dp)); Text("WhatsApp", fontWeight = FontWeight.SemiBold) }
        OutlinedButton(
            onClick = {
                scope.launch {
                    busy = true
                    val uri = ShareActions.saveImageToGallery(context, renderFull(), "WisdomOne_${verse.id}_${System.currentTimeMillis()}")
                    Toast.makeText(context, if (uri != null) "Saved to Pictures/WisdomOne" else "Saving needs Android 10+. Use Share instead.", Toast.LENGTH_SHORT).show()
                    busy = false
                }
            },
            enabled = !busy, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, W.Gold.copy(alpha = 0.6f)), contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = W.Ink)
        ) { Icon(Icons.Outlined.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save", fontWeight = FontWeight.SemiBold) }
    }
    OutlinedButton(
        onClick = { wallpaperDialog = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, W.Gold.copy(alpha = 0.6f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = W.Ink)
    ) { Icon(Icons.Outlined.Wallpaper, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Set as wallpaper", fontWeight = FontWeight.SemiBold) }
    if (busy) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(color = W.Gold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp)); Text("Working…", fontSize = 12.sp, color = W.InkSoft)
    }
    if (wallpaperDialog) {
        val metrics = context.resources.displayMetrics
        suspend fun apply(flag: Int, name: String) {
            wallpaperDialog = false; busy = true
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    val bmp = CardRenderer.renderSized(context, CardContent.from(verse), style, metrics.widthPixels, metrics.heightPixels)
                    WallpaperManager.getInstance(context).setBitmap(bmp, null, true, flag); bmp.recycle()
                }
            }
            busy = false
            Toast.makeText(context, if (result.isSuccess) "$name wallpaper updated" else "Couldn't set the wallpaper on this device", Toast.LENGTH_LONG).show()
        }
        AlertDialog(
            onDismissRequest = { wallpaperDialog = false }, containerColor = W.Parchment,
            title = { Text("Set as wallpaper", fontWeight = FontWeight.Bold, color = W.Ink) },
            text = { Text("Sized exactly for your screen (${metrics.widthPixels}×${metrics.heightPixels}) using your current design.", color = W.InkMid, fontSize = 14.sp) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { scope.launch { apply(WallpaperManager.FLAG_SYSTEM, "Home screen") } }) { Text("Home screen", color = W.Ink) }
                    TextButton(onClick = { scope.launch { apply(WallpaperManager.FLAG_LOCK, "Lock screen") } }) { Text("Lock screen", color = W.Ink) }
                    TextButton(onClick = { scope.launch { apply(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, "Home & lock screen") } }) { Text("Both", color = W.Ink, fontWeight = FontWeight.Bold) }
                }
            },
            dismissButton = { TextButton(onClick = { wallpaperDialog = false }) { Text("Cancel", color = W.InkSoft) } }
        )
    }
}

// ── Video Studio ───────────────────────────────────────────────────────────────
@Composable
private fun VideoTab(verse: Verse, baseStyle: CardStyle, templates: List<StudioTemplate>, onChange: (CardStyle) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val style = baseStyle.copy(format = CardFormat.STORY)
    var seconds by remember { mutableStateOf(12) }
    var music by remember { mutableStateOf(MusicStyle.CALM_PAD) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var animation by remember { mutableStateOf(TextAnimation.FADE) }
    var motion by remember { mutableStateOf(BgMotion.ZOOM_IN) }
    var transition by remember { mutableStateOf(VideoTransition.FADE_BLACK) }
    var particles by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<File?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var listening by remember { mutableStateOf(false) }
    val caption = "“${verse.text}”\n\n— ${verse.citation()}\n\nvia WisdomOne"
    val options = VideoOptions(seconds, music, style, animation = animation, motion = motion, transition = transition, particles = particles, volume = volume)

    DisposableEffect(Unit) { onDispose { MusicPreview.stop() } }

    if (result == null) {
        Text("Make a vertical 9:16 video for Status, Reels and Shorts. Pick a template, animate the verse, add calm music — the preview below plays exactly what will be exported.", fontSize = 12.sp, color = W.InkSoft, lineHeight = 17.sp)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LivePreview(verse, options, Modifier.height(420.dp)) }
        Label("Template")
        TemplateRow(templates, style.template) { onChange(baseStyle.copy(template = it)) }
        Label("Background colour")
        SwatchDots(style.swatch) { onChange(baseStyle.copy(swatch = it)) }
        TextControls(baseStyle, full = false, onChange = onChange)
        Label("Length")
        ChipRow(listOf(8, 12, 15, 20, 30), seconds, { "$it sec" }) { if (!running) seconds = it }
        Label("Text animation")
        ChipRow(TextAnimation.values().toList(), animation, { it.label }) { animation = it }
        Label("Background motion")
        ChipRow(BgMotion.values().toList(), motion, { it.label }) { motion = it }
        Label("Transition")
        ChipRow(VideoTransition.values().toList(), transition, { it.label }) { transition = it }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Floating light particles", modifier = Modifier.weight(1f), fontSize = 14.sp, color = W.Ink, fontWeight = FontWeight.SemiBold)
            Switch(checked = particles, onCheckedChange = { particles = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = W.Gold))
        }
        Label("Calm music")
        ChipRow(MusicStyle.values().toList(), music, { it.label }) { if (!running) { music = it; MusicPreview.stop(); listening = false } }
        Text(music.description, fontSize = 11.sp, color = W.InkSoft)
        if (music != MusicStyle.NONE) {
            Label("Volume · ${(volume * 100).toInt()}%")
            SliderRow(volume, 0.1f..1f, "") { volume = it }
            OutlinedButton(
                onClick = {
                    if (listening) { MusicPreview.stop(); listening = false }
                    else { listening = true; scope.launch { MusicPreview.play(music, volume) { listening = false } } }
                },
                modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.5.dp, W.Gold.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = W.Ink)
            ) {
                Icon(if (listening) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text(if (listening) "Stop preview" else "Listen to a sample", fontWeight = FontWeight.SemiBold)
            }
        }
        error?.let { ErrorBox(it, null, Modifier.padding(0.dp)) }
        if (running) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = W.Gold, trackColor = W.Gold.copy(alpha = 0.2f))
            Text("Creating your video… ${(progress * 100).toInt()}%", fontSize = 12.sp, color = W.InkSoft)
            OutlinedButton(onClick = { job?.cancel() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        } else {
            Button(
                onClick = {
                    MusicPreview.stop(); listening = false
                    error = null; running = true; progress = 0f
                    job = scope.launch {
                        try {
                            val out = File(context.cacheDir, "wisdomone_verse_${System.currentTimeMillis()}.mp4")
                            VideoGenerator.generate(context, CardContent.from(verse), options, out) { progress = it }
                            result = out
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { error = "Couldn't create the video on this device (${e.message ?: e.javaClass.simpleName}). Try a shorter length or no music." }
                        finally { running = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Ink)
            ) { Icon(Icons.Outlined.Movie, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Generate video", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
        }
    } else {
        val file = result!!
        Text("Your video is ready", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = W.Ink)
        var view by remember { mutableStateOf<VideoView?>(null) }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.fromFile(file)); setOnPreparedListener { it.isLooping = true; start() }
                        setOnClickListener { if (isPlaying) pause() else start() }
                        view = this
                    }
                },
                modifier = Modifier.height(440.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(18.dp))
            )
        }
        Text("Tap the video to pause or play.", fontSize = 11.sp, color = W.InkSoft)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { ShareActions.shareVideo(context, ShareActions.videoUri(context, file), caption) }, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Ink), contentPadding = PaddingValues(8.dp)) {
                Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Share / Reels", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            OutlinedButton(onClick = { ShareActions.shareToWhatsApp(context, ShareActions.videoUri(context, file), "video/mp4", caption) }, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, Color(0xFF25D366).copy(alpha = 0.7f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1FA855)),
                contentPadding = PaddingValues(8.dp)) { Text("WhatsApp Status", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
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

/** Plays the real frame composer on a loop at low resolution, so the preview matches the exported video. */
@Composable
private fun LivePreview(verse: Verse, options: VideoOptions, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pw = 360; val ph = 640
    val bitmap = remember { Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888) }
    val image: ImageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    var tick by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    val content = remember(verse.id, verse.text) { CardContent.from(verse) }
    LaunchedEffect(options.style, options.animation, options.motion, options.transition, options.particles, options.seconds, content, playing) {
        val composer = withContext(Dispatchers.Default) { FrameComposer(context, content, options.copy(width = pw, height = ph), pw, ph) }
        val canvas = android.graphics.Canvas(bitmap)
        try {
            val start = System.nanoTime()
            while (true) {
                val t = if (playing) ((System.nanoTime() - start) / 1e9f) % (options.seconds + 1.2f) else 2.5f
                withContext(Dispatchers.Default) { composer.draw(canvas, t.coerceAtMost(options.seconds.toFloat())) }
                tick++
                if (!playing) break
                delay(60)
            }
        } finally { composer.release() }
    }
    Box(modifier.aspectRatio(9f / 16f).shadow(10.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).clickable { playing = !playing }) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Animated video preview. Tap to ${if (playing) "pause" else "play"}." }) {
            tick // observe: redraw when a new frame is ready
            drawImage(image, dstOffset = androidx.compose.ui.unit.IntOffset.Zero, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
        }
        Box(Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(if (playing) "Preview · tap to pause" else "Paused · tap to play", color = Color.White, fontSize = 10.sp)
        }
    }
}

/** Live card preview (re-renders off the main thread whenever the style or verse changes). */
@Composable
fun CardPreview(verse: Verse, style: CardStyle, modifier: Modifier = Modifier, previewWidth: Int = 540) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(verse.id, verse.text, style) {
        bitmap = withContext(Dispatchers.Default) { CardRenderer.render(context, CardContent.from(verse), style, previewWidth) }
    }
    Box(
        modifier.aspectRatio(style.format.w.toFloat() / style.format.h).shadow(10.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        val b = bitmap
        if (b != null) Image(b.asImageBitmap(), contentDescription = "Verse card preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else CircularProgressIndicator(color = W.Gold, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
    }
}
