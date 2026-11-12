package com.scripturedaily.ambient

import android.Manifest
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.scripturedaily.config.AppConfigManager
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.Verse
import com.scripturedaily.lockscreen.LockScreenVerse
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Settings → Ambient Wisdom. One place for every "outside the app" verse surface: lock screen, live wallpaper,
 * activity-aware suggestions and the always-on / ambient notification. Two panes (preview | settings) on wide and
 * unfolded screens, one column on phones.
 */
@Composable
fun AmbientWisdomSettings(
    religion: Int,
    lockScreen: Boolean,
    onLockScreen: (Boolean) -> Unit,
    frequency: String,
    onFrequency: (String) -> Unit,
    onOpenWallpaperDesign: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { AmbientWisdom.prefs(context) }
    val flags = AppConfigManager.state.collectAsState().value.obj("ambient_wisdom")
    fun available(key: String) = runCatching { flags?.get(key)?.asBoolean }.getOrNull() ?: true

    var liveTheme by remember { mutableStateOf(AmbientWisdom.LiveTheme.of(prefs.getString(AmbientWisdom.KEY_LIVE_THEME, null))) }
    var motion by remember { mutableStateOf(AmbientWisdom.Motion.of(prefs.getString(AmbientWisdom.KEY_LIVE_MOTION, null))) }
    var imageOrder by remember { mutableStateOf(AmbientWisdom.ImageOrder.of(prefs.getString(AmbientWisdom.KEY_IMAGE_ORDER, null))) }
    var liveActive by remember { mutableStateOf(VerseLiveWallpaperService.isActive(context)) }
    var ambientNote by remember { mutableStateOf(AmbientNotification.enabled(context)) }
    var previewTab by remember { mutableIntStateOf(0) }

    // Re-check things the user may have changed in Android settings (live wallpaper chosen, permissions granted).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { liveActive = VerseLiveWallpaperService.isActive(context) } }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { prefs.edit().putBoolean(AmbientWisdom.KEY_AMBIENT_NOTIFICATION, true).apply(); ambientNote = true; AmbientNotification.update(context) }
    }

    val preview: @Composable (Modifier) -> Unit = { m ->
        Column(m, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Home", "Lock", "Always-on").forEachIndexed { i, label -> Chip(label, previewTab == i, Modifier.weight(1f)) { previewTab = i } }
            }
            when (previewTab) {
                0 -> LiveWallpaperPreview(liveTheme, motion, Modifier.width(190.dp))
                1 -> LockScreenPreviewCard(Modifier.width(190.dp))
                else -> AlwaysOnPreview(Modifier.width(190.dp))
            }
            Text(
                when (previewTab) { 0 -> "Live preview of the home-screen wallpaper"; 1 -> "Your lock-screen verse wallpaper"; else -> "Illustration — how much your always-on display shows depends on your phone" },
                color = W.InkMid, fontSize = 11.sp, textAlign = TextAlign.Center
            )
        }
    }

    val settings: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ── Lock screen ──
            Section("LOCK SCREEN VERSE")
            Toggle("Verse on lock screen", "Show a verse as your lock-screen wallpaper and refresh it automatically.", lockScreen, Icons.Outlined.Lock, onLockScreen)
            Label("Refresh every")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AmbientWisdom.FREQUENCIES.forEach { (value, label) -> Chip(label, frequency == value || (frequency == "Daily" && value == "Every 24 hours"), Modifier.weight(1f)) { onFrequency(value) } }
            }
            Label("Background image rotation")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AmbientWisdom.ImageOrder.entries.forEach { o ->
                    Chip(o.label, imageOrder == o, Modifier.weight(1f)) { imageOrder = o; prefs.edit().putString(AmbientWisdom.KEY_IMAGE_ORDER, o.key).apply() }
                }
            }
            Hint(imageOrder.detail)
            NavRow(Icons.Outlined.Palette, "Visual style", "Artwork, colour theme, text size and placement", onOpenWallpaperDesign)

            // ── Live wallpaper ──
            if (available("live_wallpaper_available")) {
                Section("LIVE WALLPAPER")
                Toggle(
                    "WisdomOne live wallpaper", if (liveActive) "Active on your home screen." else "A calm animated sky with today's verse.",
                    liveActive, Icons.Outlined.Wallpaper
                ) { on -> if (on) openLiveWallpaperPreview(context) else openWallpaperChooser(context) }
                Label("Visual theme")
                FlowChips(AmbientWisdom.LiveTheme.entries.map { it to it.label }, liveTheme) { t -> liveTheme = t; prefs.edit().putString(AmbientWisdom.KEY_LIVE_THEME, t.key).apply() }
                Hint(if (liveTheme == AmbientWisdom.LiveTheme.AUTO) "Sunrise gold in the morning, bright nature in the afternoon, sunset colours in the evening and deep blue at night." else "The ${liveTheme.label.lowercase()} mood stays all day.")
                Label("Motion")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AmbientWisdom.Motion.entries.forEach { mo -> Chip(mo.label, motion == mo, Modifier.weight(1f)) { motion = mo; prefs.edit().putString(AmbientWisdom.KEY_LIVE_MOTION, mo.key).apply() } }
                }
                Hint("Animation runs only while your home screen is visible, at a low frame rate, and pauses in Battery Saver.")
                OutlinedButton(onClick = { openLiveWallpaperPreview(context) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, W.PurpleMid)) {
                    Icon(Icons.Outlined.Visibility, null, tint = W.PurpleDark, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Preview & apply live wallpaper", color = W.PurpleDark, fontWeight = FontWeight.Bold)
                }
            }

            // ── Activity-aware ──
            if (available("activity_aware_available")) {
                Section("ACTIVITY-AWARE WISDOM")
                ActivityAwareSection(religion)
            }

            // ── Always-on ──
            Section("ALWAYS-ON DISPLAY")
            Info(
                "Android doesn't let apps draw on the always-on display directly, and each phone maker handles it differently. " +
                    "WisdomOne uses what Android supports: a quiet one-line verse notification that is visible on the lock screen — and on the always-on display on phones that show notifications there."
            )
            if (available("ambient_notification_available")) {
                Toggle("One-line ambient verse", "Silent, minimal and only updated when the verse changes.", ambientNote, Icons.Outlined.NightsStay) { on ->
                    if (on && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        prefs.edit().putBoolean(AmbientWisdom.KEY_AMBIENT_NOTIFICATION, on).apply(); ambientNote = on; AmbientNotification.update(context)
                    }
                }
            }
            NavRow(Icons.Outlined.Settings, "Lock screen notification settings", "Let Android show notification content on the lock screen") {
                runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
            }
            Info("Privacy: your activity and preferences stay on this phone. WisdomOne never uploads where you are or what you're doing.")
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            // Unfolded foldables and tablets: preview stays beside the settings.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                preview(Modifier.width(260.dp))
                settings(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                preview(Modifier.fillMaxWidth())
                settings(Modifier.fillMaxWidth())
            }
        }
    }
}

// ── Activity-aware section ──────────────────────────────────────────────────

@Composable
internal fun ActivityAwareSection(religion: Int) {
    val context = LocalContext.current
    val prefs = remember { AmbientWisdom.prefs(context) }
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(AmbientWisdom.ActivityMode.of(prefs.getString(AmbientWisdom.KEY_ACTIVITY_MODE, null))) }
    var manual by remember { mutableStateOf(AmbientWisdom.Category.of(prefs.getString(AmbientWisdom.KEY_MANUAL_CATEGORY, null)) ?: AmbientWisdom.Category.PEACE) }
    var tick by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }
    var suggestion by remember { mutableStateOf<Verse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var seed by remember { mutableIntStateOf((0..9_999).random()) }

    fun setMode(m: AmbientWisdom.ActivityMode) {
        mode = m; prefs.edit().putString(AmbientWisdom.KEY_ACTIVITY_MODE, m.key).apply()
        if (m == AmbientWisdom.ActivityMode.ACTIVITY) ActivityWisdomTracker.start(context) else ActivityWisdomTracker.stop(context)
        suggestion = null; tick++
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { notice = null; setMode(AmbientWisdom.ActivityMode.ACTIVITY) }
        else { notice = "Permission not granted, so WisdomOne uses the time of day instead. You can change this in Android settings."; setMode(AmbientWisdom.ActivityMode.TIME_OF_DAY) }
    }
    // A stale registration (e.g. permission revoked in Android settings) quietly falls back.
    LaunchedEffect(Unit) {
        if (mode == AmbientWisdom.ActivityMode.ACTIVITY && !ActivityWisdomTracker.hasPermission(context)) setMode(AmbientWisdom.ActivityMode.TIME_OF_DAY)
    }

    FlowChips(AmbientWisdom.ActivityMode.entries.map { it to it.label }, mode) { m ->
        // Request the permission only when the user actually chooses activity detection.
        if (m == AmbientWisdom.ActivityMode.ACTIVITY && !ActivityWisdomTracker.hasPermission(context)) permission.launch(ActivityWisdomTracker.permission)
        else setMode(m)
    }
    Hint(mode.detail)
    notice?.let { Info(it) }
    if (mode == AmbientWisdom.ActivityMode.MANUAL) {
        FlowChips(AmbientWisdom.Category.entries.map { it to "${it.emoji} ${it.label}" }, manual) { c ->
            manual = c; prefs.edit().putString(AmbientWisdom.KEY_MANUAL_CATEGORY, c.key).apply(); suggestion = null; tick++
        }
    }

    val rec = remember(mode, manual, tick) { ActivityWisdomTracker.recommendation(context) }
    if (rec != null) {
        val activity = AmbientWisdom.Activity.of(prefs.getString(AmbientWisdom.KEY_LAST_ACTIVITY, null))
        val why = when (mode) {
            AmbientWisdom.ActivityMode.ACTIVITY -> if (activity == AmbientWisdom.Activity.UNKNOWN) "No activity detected yet — using the time of day" else "Because you're ${activity.label.lowercase()}"
            AmbientWisdom.ActivityMode.TIME_OF_DAY -> "Chosen for this time of day"
            else -> "Your choice"
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(W.Surface).border(1.dp, W.Gold.copy(alpha = .35f), RoundedCornerShape(18.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("SUGGESTED NOW", color = W.Gold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Text("${rec.emoji}  ${rec.label}", color = W.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(why, color = W.InkMid, fontSize = 12.sp)
            suggestion?.let { v ->
                Text("“${v.text}”", color = W.Ink, fontSize = 15.sp, lineHeight = 22.sp, fontFamily = FontFamily.Serif)
                Text(v.reference(), color = W.InkMid, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    loading = true; seed = (seed + 1) % 10_000   // API accepts 0..10 000
                    scope.launch {
                        suggestion = runCatching {
                            val truth = withContext(Dispatchers.IO) { ApiClient.api.universalTruth(rec.topic, context.getSharedPreferences("reader", 0).getInt("language", 0).takeIf { it > 0 }, seed) }
                            (truth.cards.firstOrNull { it.religionId == religion } ?: truth.cards.firstOrNull())?.verse
                        }.getOrNull()
                        loading = false
                        if (suggestion == null) notice = "Couldn't load a suggestion. Check your connection."
                    }
                }, enabled = !loading) { Text(if (suggestion == null) "Show a verse" else "Another verse", color = W.PurpleDark, fontWeight = FontWeight.Bold) }
                suggestion?.let { v ->
                    TextButton(onClick = {
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { LockScreenVerse.applyWallpaperNow(context, v.id, v.scripture, v.text, v.reference()) }
                            notice = if (ok) "Placed on your lock screen. Your regular schedule continues with the next refresh." else "Android didn't accept the lock-screen wallpaper on this phone."
                        }
                    }) { Text("Put on lock screen", color = W.PurpleDark, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Hint("Suggestions pick a kind of verse; they never change your verse on their own.")
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Composable
private fun PhoneFrame(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.aspectRatio(9f / 19.5f).clip(RoundedCornerShape(28.dp)).background(Color.Black).padding(5.dp)
            .clip(RoundedCornerShape(24.dp)),
        content = content
    )
}

@Composable
fun LiveWallpaperPreview(theme: AmbientWisdom.LiveTheme, motion: AmbientWisdom.Motion, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val renderer = remember { LiveWallpaperRenderer() }
    val verse = remember { VerseLiveWallpaperService.currentVerse(context) }
    var clock by remember { mutableLongStateOf(40_000L) }
    LaunchedEffect(motion) {
        if (motion == AmbientWisdom.Motion.SUBTLE) { val start = System.currentTimeMillis() - 40_000L; while (isActive) { clock = System.currentTimeMillis() - start; delay(83) } }
    }
    val resolved = AmbientWisdom.resolveTheme(theme, LocalTime.now().hour)
    PhoneFrame(modifier.semantics { contentDescription = "Live wallpaper preview, ${resolved.label} theme" }) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { renderer.draw(it.nativeCanvas, size.width.toInt(), size.height.toInt(), clock, resolved, verse.first, verse.second) }
        }
        StatusRow(Color.White)
    }
}

@Composable
private fun LockScreenPreviewCard(modifier: Modifier) {
    val context = LocalContext.current
    var bmp by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        val p = context.getSharedPreferences("reader", 0)
        bmp = withContext(Dispatchers.Default) {
            runCatching {
                LockScreenVerse.previewBitmap(context, p.getString("widget_scripture", "") ?: "", VerseLiveWallpaperService.currentVerse(context).first,
                    p.getString("widget_reference", "") ?: "", widthPx = 540, heightPx = 1170).asImageBitmap()
            }.getOrNull()
        }
    }
    PhoneFrame(modifier) {
        bmp?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) } ?: Box(Modifier.fillMaxSize().background(W.PurpleDark))
        Column(Modifier.fillMaxWidth().padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(clockText(context), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Light)
        }
        Icon(Icons.Outlined.Lock, null, tint = Color.White.copy(alpha = .8f), modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).size(12.dp))
    }
}

@Composable
private fun AlwaysOnPreview(modifier: Modifier) {
    val context = LocalContext.current
    val verse = remember { VerseLiveWallpaperService.currentVerse(context) }
    PhoneFrame(modifier) {
        Column(Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(clockText(context), color = Color(0xFFBDBDBD), fontSize = 44.sp, fontWeight = FontWeight.Thin)
            Spacer(Modifier.height(18.dp))
            Text(AmbientWisdom.excerpt(verse.first, 44), color = Color(0xFF9E9E9E), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(4.dp))
            Text(verse.second, color = Color(0xFF6E6E6E), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun clockText(context: Context): String =
    LocalTime.now().format(DateTimeFormatter.ofPattern(if (android.text.format.DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"))

@Composable
private fun BoxScope.StatusRow(color: Color) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(clockText(context), color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text("●●●", color = color.copy(alpha = .8f), fontSize = 7.sp)
    }
}

// ── System hand-offs ─────────────────────────────────────────────────────────

fun openLiveWallpaperPreview(context: Context) {
    val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, VerseLiveWallpaperService.component(context))
    try { context.startActivity(direct) } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
    }
}

private fun openWallpaperChooser(context: Context) {
    runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choose a wallpaper")) }
}

// ── Small building blocks (theme-aware) ──────────────────────────────────────

@Composable private fun Section(text: String) { Text(text, color = W.Gold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 6.dp)) }
@Composable private fun Label(text: String) { Text(text, color = W.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
@Composable private fun Hint(text: String) { Text(text, color = W.InkMid, fontSize = 12.sp, lineHeight = 17.sp) }

@Composable
private fun Info(text: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(W.Gold.copy(alpha = .14f)).padding(14.dp)) {
        Icon(Icons.Outlined.Info, null, tint = W.Gold); Spacer(Modifier.width(10.dp))
        Text(text, color = W.Ink, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, icon: ImageVector, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(W.Surface).border(1.dp, W.Gold.copy(alpha = .25f), RoundedCornerShape(17.dp))
            .clickable { onChecked(!checked) }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(W.Gold.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = W.Gold, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = W.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = W.InkMid, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedTrackColor = W.Purple, checkedThumbColor = W.OnPurple))
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(W.Surface).border(1.dp, W.Gold.copy(alpha = .25f), RoundedCornerShape(17.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = W.Gold); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(title, color = W.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(subtitle, color = W.InkMid, fontSize = 12.sp) }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = W.InkMid)
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) W.Purple else W.Surface)
            .border(1.dp, if (selected) W.Purple else W.Gold.copy(alpha = .35f), RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = if (selected) W.OnPurple else W.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) -> Chip(label, value == selected) { onSelect(value) } }
    }
}
