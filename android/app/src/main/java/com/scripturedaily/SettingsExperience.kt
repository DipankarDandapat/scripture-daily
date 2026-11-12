package com.scripturedaily

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.scripturedaily.ads.AdSlot
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.CatalogItem
import com.scripturedaily.data.ReligionItem
import com.scripturedaily.data.ScriptureDatabase
import com.scripturedaily.lockscreen.LockScreenVerse
import com.scripturedaily.lockscreen.WallpaperMode
import com.scripturedaily.lockscreen.WallpaperPacks
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class SettingEntry(val title: String, val subtitle: String, val icon: ImageVector, val route: String, val painterRes: Int? = null)

/**
 * Settings, following design/approved_settings_ui_reference.png, "WisdomOne Settings and Wallpaper Suite.png" and
 * "WisdomOne Lock Screen UI Showcase-1.png". All icons share one style (Material outlined, gold, in a rounded tile),
 * and every colour comes from the theme palette so Light/Dark both stay readable.
 */
@Composable
fun SettingsExperienceScreen(
    religion: Int,
    language: Int,
    frequency: String,
    reminders: Boolean,
    lockScreen: Boolean,
    religions: List<ReligionItem>,
    languages: List<CatalogItem>,
    fontSize: Int,
    notice: String?,
    treeEnabled: Boolean,
    onSavePreferences: (Int, Int, String, Boolean, String) -> Unit,
    onReminders: (Boolean) -> Unit,
    onLockScreen: (Boolean) -> Unit,
    onFontSize: (Int) -> Unit,
    onTreeEnabled: (Boolean, (Boolean) -> Unit) -> Unit,
    onWallpaperApplied: () -> Unit,
    onWidgetStudio: () -> Unit,
    onRate: () -> Unit,
    onFeedback: () -> Unit,
    onAdsLab: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reader", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var route by remember { mutableStateOf("home") }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var pendingRoute by remember { mutableStateOf("") }
    var selectedReligion by remember(religion) { mutableIntStateOf(religion) }
    var selectedLanguage by remember(language) { mutableIntStateOf(language) }
    var selectedFrequency by remember(frequency) { mutableStateOf(if (frequency == "Daily") "Every 24 hours" else frequency) }
    var selectedReminders by remember(reminders) { mutableStateOf(reminders) }
    var availableLanguages by remember(languages) { mutableStateOf(languages) }
    var loadingLanguages by remember { mutableStateOf(false) }
    var showAllLanguages by remember { mutableStateOf(false) }
    var autoWallpaper by remember(lockScreen) { mutableStateOf(lockScreen) }
    var wallpaperMode by remember { mutableStateOf(WallpaperMode.normalize(prefs.getString("wallpaper_mode", null))) }
    var wallpaperTheme by remember { mutableStateOf(normalizeWallpaperTheme(prefs.getString("wallpaper_theme", "MULTICOLOR"))) }
    var wallpaperImageIndex by remember { mutableIntStateOf(prefs.getInt("wallpaper_image_index", 1).coerceIn(1, WallpaperPacks.COUNT)) }
    var showVerseText by remember { mutableStateOf(prefs.getBoolean("wallpaper_show_verse", true)) }
    var showReference by remember { mutableStateOf(prefs.getBoolean("wallpaper_show_reference", true)) }
    var wallpaperTextSize by remember { mutableStateOf(prefs.getString("wallpaper_text_size", "Medium") ?: "Medium") }
    var wallpaperTextPosition by remember { mutableStateOf(prefs.getString("wallpaper_text_position", "Bottom") ?: "Bottom") }
    var readingTranslation by remember { mutableStateOf(prefs.getBoolean("reading_translation", true)) }
    var readingScripture by remember { mutableStateOf(prefs.getBoolean("reading_scripture", true)) }
    var readingReference by remember { mutableStateOf(prefs.getBoolean("reading_reference", true)) }
    var readingBranding by remember { mutableStateOf(prefs.getBoolean("reading_branding", true)) }
    var appTheme by remember { mutableStateOf(prefs.getString("app_theme", "Light") ?: "Light") }
    var notifyVerseUpdates by remember { mutableStateOf(prefs.getBoolean("notify_verse_updates", false)) }
    var notifyWallpaperUpdates by remember { mutableStateOf(prefs.getBoolean("notify_wallpaper_updates", false)) }
    var wallpaperNotice by remember { mutableStateOf<Pair<String, Boolean>?>(null) }   // message, isError
    var applySheet by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }
    var fullPreview by remember { mutableStateOf(false) }
    var treeConfirm by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(selectedReligion) {
        if (selectedReligion > 0) {
            loadingLanguages = true
            availableLanguages = try { ApiClient.api.languages(selectedReligion) } catch (_: Exception) { emptyList() }
            if (availableLanguages.none { it.id == selectedLanguage }) selectedLanguage = availableLanguages.firstOrNull()?.id ?: 0
            loadingLanguages = false
        }
    }

    fun hasUnsavedTraditionChanges() = route == "tradition" && (selectedReligion != religion || selectedLanguage != language)
    BackHandler(enabled = route != "home") {
        if (hasUnsavedTraditionChanges()) { pendingRoute = "home"; showUnsavedDialog = true } else route = "home"
    }
    val selectedTraditionItem = religions.firstOrNull { it.id == selectedReligion }
    val selectedTradition = selectedTraditionItem?.name ?: WallpaperPacks.traditionName(selectedReligion)
    var previousReligion by remember { mutableIntStateOf(selectedReligion) }
    LaunchedEffect(selectedReligion) {
        if (previousReligion != selectedReligion) { wallpaperImageIndex = 1; previousReligion = selectedReligion }   // each tradition starts at its own first image
    }

    Column(Modifier.fillMaxSize().background(W.Parchment)) {
        if (route == "home") {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SettingsHeroHeader()
                val entries = listOf(
                    SettingEntry("Tradition & Language", "$selectedTradition \u00b7 ${availableLanguages.firstOrNull { it.id == language }?.name ?: "Language"}", Icons.Outlined.Spa, "tradition", painterRes = traditionIcon(selectedTraditionItem?.code ?: "")),
                    SettingEntry("Verse Frequency", frequencyLabel(frequency), Icons.Outlined.Schedule, "frequency"),
                    SettingEntry("Lock Screen & Wallpaper", "Verse wallpaper, live wallpaper, always-on & design", Icons.Outlined.Image, "wallpaper"),
                    SettingEntry("Widget Studio", "Create and customise widgets", Icons.Outlined.Widgets, "widget"),
                    SettingEntry("Reading Preferences", "Font size, verse display & style", Icons.Outlined.TextFields, "reading"),
                    SettingEntry("App Theme", "Light · Dark · System", Icons.Outlined.DarkMode, "theme"),
                    SettingEntry("Notifications", "Verse alerts & reminders", Icons.Outlined.Notifications, "notifications"),
                    SettingEntry("Battery & Performance", "Optimise background updates", Icons.Outlined.FlashOn, "battery"),
                    SettingEntry("Help & Support", "FAQs, contact us", Icons.AutoMirrored.Outlined.HelpOutline, "help"),
                    SettingEntry("Rate WisdomOne", "Share your feedback & support", Icons.Outlined.StarOutline, "rate")
                )
                entries.forEach { entry ->
                    SettingMenuCard(entry, highlighted = entry.route == "wallpaper") {
                        when (entry.route) { "widget" -> onWidgetStudio(); "rate" -> onRate(); else -> route = entry.route }
                    }
                }
                if (notice != null) NoticeCard(notice, true)
                OutlinedButton(onClick = onFeedback, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, W.Gold.copy(alpha = .7f))) {
                    Text("Leave us a feedback", color = W.Ink, fontWeight = FontWeight.SemiBold)
                }
                MakeItYoursCard()
                AdSlot("settings_bottom_banner")
                Spacer(Modifier.height(16.dp))
            }
        } else {
            SettingsSubHeader(title = routeTitle(route), subtitle = routeSubtitle(route), onBack = {
                if (hasUnsavedTraditionChanges()) { pendingRoute = "home"; showUnsavedDialog = true } else route = "home"
            })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (route) {
                    "tradition" -> {
                        TraditionBanner(religions.firstOrNull { it.id == selectedReligion })
                        SectionTitle("YOUR TRADITION")
                        religions.forEach { item ->
                            TraditionSelectionRow(item, selectedReligion == item.id) { selectedReligion = item.id }
                        }
                        SectionTitle("SELECT LANGUAGE")
                        if (loadingLanguages) LinearProgressIndicator(Modifier.fillMaxWidth(), color = W.Purple)
                        else {
                            val shown = if (showAllLanguages) availableLanguages else availableLanguages.take(3)
                            shown.chunked(3).forEach { chunk ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    chunk.forEach { item -> LanguageChip(item.name, item.nativeScript.ifBlank { item.code }, selectedLanguage == item.id, Modifier.weight(1f)) { selectedLanguage = item.id } }
                                    repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            if (availableLanguages.size > 3) Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(W.Surface).border(1.dp, W.Gold.copy(alpha = .3f), RoundedCornerShape(16.dp)).clickable { showAllLanguages = !showAllLanguages }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Language, null, tint = W.PurpleMid); Spacer(Modifier.width(12.dp))
                                Text(if (showAllLanguages) "Fewer languages" else "More Languages", color = W.Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = W.InkMid)
                            }
                        }
                        PrimaryAction("Save preferences", Icons.Outlined.Check) {
                            onSavePreferences(selectedReligion, selectedLanguage, selectedFrequency, selectedReminders, wallpaperMode)
                            route = "home"
                        }
                    }
                    "frequency" -> {
                                                Box(
                            Modifier.fillMaxWidth()
                                .layout { measurable, constraints ->
                                    val extra = 36.dp.roundToPx()
                                    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra))
                                    layout(placeable.width, placeable.height) { placeable.place(-extra / 2, 0) }
                                }
                                .height(160.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.verse_frequency_banner),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillWidth
                            )
                            Box(Modifier.fillMaxWidth().height(80.dp).align(Alignment.TopCenter)
                                .background(Brush.verticalGradient(listOf(W.Parchment, W.Parchment.copy(alpha = 0.85f), Color.Transparent))))
                            Box(Modifier.fillMaxWidth().height(70.dp).align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, W.Parchment.copy(alpha = 0.9f), W.Parchment))))
                            Box(Modifier.width(64.dp).fillMaxHeight().align(Alignment.CenterStart)
                                .background(Brush.horizontalGradient(listOf(W.Parchment, W.Parchment.copy(alpha = 0.85f), Color.Transparent))))
                            Box(Modifier.width(64.dp).fillMaxHeight().align(Alignment.CenterEnd)
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, W.Parchment.copy(alpha = 0.85f), W.Parchment))))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Every hour" to "1 Hour", "Every 6 hours" to "6 Hours", "Every 12 hours" to "12 Hours", "Every 24 hours" to "24 Hours").forEach { (option, label) ->
                                FrequencyChip(label, selectedFrequency == option, Modifier.weight(1f)) {
                                    selectedFrequency = option
                                    onSavePreferences(selectedReligion, selectedLanguage, option, selectedReminders, wallpaperMode)
                                }
                            }
                        }
                        val hours = frequencyLabel(selectedFrequency).removePrefix("Every ").removePrefix("1 ")
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(W.Surface).border(1.dp, W.Gold.copy(alpha = .3f), RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("When every $hours is reached:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = W.Ink)
                            BulletRow(Icons.AutoMirrored.Outlined.MenuBook, "One new verse is selected (based on your tradition & language).")
                            BulletRow(Icons.Outlined.Image, "Lock-screen wallpaper updates with the new verse.")
                            BulletRow(Icons.Outlined.Widgets, "Home-screen widget shows the same new verse.")
                        }
                        InfoCard("Frequency keeps your verse, wallpaper and widget in sync.")
                    }
                    "wallpaper" -> {
                        // ── SURFACES ──────────────────────────────────────────────────────
                        SectionTitle("LOCK SCREEN")
                        ToggleCard("Verse on lock screen", "Automatically update your lock screen with a new verse.", autoWallpaper, icon = Icons.Outlined.Lock) { autoWallpaper = it; onLockScreen(it) }
                        SectionTitle("REFRESH FREQUENCY")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            com.scripturedaily.ambient.AmbientWisdom.FREQUENCIES.forEach { (value, label) ->
                                FrequencyChip(label, selectedFrequency == value || (selectedFrequency == "Daily" && value == "Every 24 hours"), Modifier.weight(1f)) {
                                    selectedFrequency = value
                                    onSavePreferences(selectedReligion, selectedLanguage, value, selectedReminders, wallpaperMode)
                                }
                            }
                        }
                        SectionTitle("BACKGROUND IMAGE ROTATION")
                        val ambientPrefs = remember { context.getSharedPreferences("reader", Context.MODE_PRIVATE) }
                        var imageOrder by remember { mutableStateOf(com.scripturedaily.ambient.AmbientWisdom.ImageOrder.of(ambientPrefs.getString(com.scripturedaily.ambient.AmbientWisdom.KEY_IMAGE_ORDER, null))) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.scripturedaily.ambient.AmbientWisdom.ImageOrder.entries.forEach { o ->
                                SelectPill(o.label, "", imageOrder == o, Modifier.weight(1f)) {
                                    imageOrder = o
                                    ambientPrefs.edit().putString(com.scripturedaily.ambient.AmbientWisdom.KEY_IMAGE_ORDER, o.key).apply()
                                }
                            }
                        }
                        InfoCard(imageOrder.detail)

                        // ── WALLPAPER CONTENT ──────────────────────────────────────────────
                        SectionTitle("WALLPAPER CONTENT")
                        ToggleCard("Change Verse and Image Together", "Automatically rotate through your selected artwork with every new verse.", wallpaperMode == WallpaperMode.VERSE_AND_IMAGE, icon = Icons.Outlined.AutoAwesome) { on ->
                            wallpaperMode = if (on) WallpaperMode.VERSE_AND_IMAGE else WallpaperMode.COLORS
                        }
                        ToggleCard("Keep Same Image", "Update the verse while keeping your current wallpaper.", wallpaperMode == WallpaperMode.KEEP_IMAGE, icon = Icons.Outlined.Lock) { on ->
                            wallpaperMode = if (on) WallpaperMode.KEEP_IMAGE else WallpaperMode.COLORS
                        }
                        if (wallpaperMode == WallpaperMode.COLORS) InfoCard("Both options are off, so your verse appears on the colour themes below instead of artwork.")

                        SectionTitle("WALLPAPER GALLERY · ${selectedTradition.uppercase()}")
                        WallpaperGallery(context, selectedReligion, wallpaperImageIndex, enabled = wallpaperMode != WallpaperMode.COLORS) { wallpaperImageIndex = it }
                        Text(
                            when (wallpaperMode) {
                                WallpaperMode.VERSE_AND_IMAGE -> "Artwork starts at image $wallpaperImageIndex and moves on with every new verse."
                                WallpaperMode.KEEP_IMAGE -> "Image $wallpaperImageIndex stays on your wallpaper; only the verse changes."
                                else -> "Turn on one of the options above to use artwork."
                            }, color = W.InkMid, fontSize = 12.sp
                        )
                        AdSlot("wallpaper_gallery_banner")

                        // ── PREVIEW TABS ───────────────────────────────────────────────────
                        SectionTitle("PREVIEW")
                        var previewTab by remember { mutableIntStateOf(0) }
                        val showAlwaysOn = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        val previewTabs = if (showAlwaysOn) listOf("Lock Screen", "Live Wallpaper", "Always-on") else listOf("Lock Screen", "Live Wallpaper")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            previewTabs.forEachIndexed { i, label ->
                                SelectPill(label, "", previewTab == i, Modifier.weight(1f)) { previewTab = i }
                            }
                        }
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            when (previewTab) {
                                0 -> LockScreenPreview(selectedReligion, selectedLanguage, wallpaperMode, wallpaperImageIndex, wallpaperTheme, wallpaperTextSize, wallpaperTextPosition, showVerseText, showReference, Modifier.width(210.dp))
                                1 -> {
                                    var liveTheme by remember { mutableStateOf(com.scripturedaily.ambient.AmbientWisdom.LiveTheme.of(ambientPrefs.getString(com.scripturedaily.ambient.AmbientWisdom.KEY_LIVE_THEME, null))) }
                                    var motion by remember { mutableStateOf(com.scripturedaily.ambient.AmbientWisdom.Motion.of(ambientPrefs.getString(com.scripturedaily.ambient.AmbientWisdom.KEY_LIVE_MOTION, null))) }
                                    com.scripturedaily.ambient.LiveWallpaperPreview(liveTheme, motion, Modifier.width(210.dp))
                                }
                                else -> AlwaysOnPreviewCard(Modifier.width(210.dp))
                            }
                        }

                        if (wallpaperMode == WallpaperMode.COLORS) {
                            SectionTitle("COLOUR THEME")
                            val themes = listOf("MULTICOLOR" to "Multicolor\n(Auto)", "GOLDEN" to "Golden", "ROYAL_PURPLE" to "Royal Purple", "FOREST_GREEN" to "Forest Green", "OCEAN_BLUE" to "Ocean Blue", "SOFT_SAND" to "Soft Sand", "MIDNIGHT" to "Midnight")
                            themes.chunked(4).forEach { chunk ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    chunk.forEach { (key, label) -> ThemeSwatch(key, label, wallpaperTheme == key, Modifier.weight(1f)) { wallpaperTheme = key } }
                                    repeat(4 - chunk.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            InfoCard(if (wallpaperTheme == "MULTICOLOR") "Multicolor rotates the whole palette each time the verse changes." else "Your colour theme stays fixed when the verse updates.")
                        }
                        SectionTitle("VERSE TEXT SIZE")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Small", "Medium", "Large").forEach { option -> SelectPill(option, "", wallpaperTextSize == option, Modifier.weight(1f)) { wallpaperTextSize = option } }
                        }
                        SectionTitle("TEXT PLACEMENT")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Top", "Middle", "Bottom").forEach { option -> SelectPill(option, "", wallpaperTextPosition == option, Modifier.weight(1f)) { wallpaperTextPosition = option } }
                        }
                        InfoCard("Text automatically wraps and scales to fit.")
                        SectionTitle("VERSE CONTENT")
                        ToggleCard("Show verse text on wallpaper", "Keep the verse readable on the image", showVerseText) { showVerseText = it }
                        ToggleCard("Show scripture reference", "Display scripture, chapter and verse", showReference) { showReference = it }

                        // ── LIVE WALLPAPER ─────────────────────────────────────────────────
                        val flags = com.scripturedaily.config.AppConfigManager.state.collectAsState().value.obj("ambient_wisdom")
                        fun available(key: String) = runCatching { flags?.get(key)?.asBoolean }.getOrNull() ?: true
                        if (available("live_wallpaper_available")) {
                            SectionTitle("LIVE WALLPAPER")
                            var liveTheme by remember { mutableStateOf(com.scripturedaily.ambient.AmbientWisdom.LiveTheme.of(ambientPrefs.getString(com.scripturedaily.ambient.AmbientWisdom.KEY_LIVE_THEME, null))) }
                            var motion by remember { mutableStateOf(com.scripturedaily.ambient.AmbientWisdom.Motion.of(ambientPrefs.getString(com.scripturedaily.ambient.AmbientWisdom.KEY_LIVE_MOTION, null))) }
                            var liveActive by remember { mutableStateOf(com.scripturedaily.ambient.VerseLiveWallpaperService.isActive(context)) }
                            val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
                            DisposableEffect(lifecycle) {
                                val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
                                    if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) liveActive = com.scripturedaily.ambient.VerseLiveWallpaperService.isActive(context)
                                }
                                lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
                            }
                            ToggleCard(
                                "WisdomOne live wallpaper",
                                if (liveActive) "Active on your home screen." else "A calm animated sky with today's verse.",
                                liveActive, Icons.Outlined.Wallpaper
                            ) { on -> if (on) com.scripturedaily.ambient.openLiveWallpaperPreview(context) else scope.launch { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choose a wallpaper")) } }
                            SectionTitle("LIVE WALLPAPER THEME")
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                com.scripturedaily.ambient.AmbientWisdom.LiveTheme.entries.forEach { t ->
                                    SelectPill(t.label, "", liveTheme == t, Modifier.wrapContentWidth()) {
                                        liveTheme = t
                                        ambientPrefs.edit().putString(com.scripturedaily.ambient.AmbientWisdom.KEY_LIVE_THEME, t.key).apply()
                                    }
                                }
                            }
                            InfoCard(if (liveTheme == com.scripturedaily.ambient.AmbientWisdom.LiveTheme.AUTO) "Sunrise gold in the morning, bright nature in the afternoon, sunset colours in the evening and deep blue at night." else "The ${liveTheme.label.lowercase()} mood stays all day.")
                            SectionTitle("MOTION")
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                com.scripturedaily.ambient.AmbientWisdom.Motion.entries.forEach { mo ->
                                    SelectPill(mo.label, "", motion == mo, Modifier.weight(1f)) {
                                        motion = mo
                                        ambientPrefs.edit().putString(com.scripturedaily.ambient.AmbientWisdom.KEY_LIVE_MOTION, mo.key).apply()
                                    }
                                }
                            }
                            InfoCard("Animation runs only while your home screen is visible and pauses in Battery Saver.")
                            OutlinedButton(
                                onClick = { com.scripturedaily.ambient.openLiveWallpaperPreview(context) },
                                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, W.PurpleMid)
                            ) {
                                Icon(Icons.Outlined.Visibility, null, tint = W.PurpleDark, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                                Text("Preview & apply live wallpaper", color = W.PurpleDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        // ── ACTIVITY-AWARE ─────────────────────────────────────────────────
                        if (available("activity_aware_available")) {
                            SectionTitle("ACTIVITY-AWARE WISDOM")
                            com.scripturedaily.ambient.ActivityAwareSection(selectedReligion)
                        }

                        // ── ALWAYS-ON DISPLAY (Android 14+ only) ───────────────────────────
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            SectionTitle("ALWAYS-ON DISPLAY")
                            InfoCard(
                                "Android doesn't let apps draw on the always-on display directly. " +
                                "WisdomOne uses a quiet one-line verse notification visible on the lock screen — and on the always-on display on phones that show notifications there."
                            )
                            if (available("ambient_notification_available")) {
                                var ambientNote by remember { mutableStateOf(com.scripturedaily.ambient.AmbientNotification.enabled(context)) }
                                val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                                    if (granted) {
                                        ambientPrefs.edit().putBoolean(com.scripturedaily.ambient.AmbientWisdom.KEY_AMBIENT_NOTIFICATION, true).apply()
                                        ambientNote = true
                                        com.scripturedaily.ambient.AmbientNotification.update(context)
                                    }
                                }
                                ToggleCard("One-line ambient verse", "Silent, minimal and only updated when the verse changes.", ambientNote, Icons.Outlined.NightsStay) { on ->
                                    if (on && android.os.Build.VERSION.SDK_INT >= 33 &&
                                        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        ambientPrefs.edit().putBoolean(com.scripturedaily.ambient.AmbientWisdom.KEY_AMBIENT_NOTIFICATION, on).apply()
                                        ambientNote = on
                                        com.scripturedaily.ambient.AmbientNotification.update(context)
                                    }
                                }
                            }
                            SecondaryAction("Lock screen notification settings") {
                                runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
                            }
                            InfoCard("Privacy: your activity and preferences stay on this phone. WisdomOne never uploads where you are or what you're doing.")
                        }

                        // ── SAVE & APPLY ───────────────────────────────────────────────────
                        wallpaperNotice?.let { (msg, isError) -> NoticeCard(msg, isError) }
                        Button(
                            onClick = {
                                prefs.edit().putInt("religion", selectedReligion).putInt("language", selectedLanguage).putString("wallpaper_mode", wallpaperMode)
                                    .putString("wallpaper_theme", wallpaperTheme).putBoolean("wallpaper_show_verse", showVerseText).putBoolean("wallpaper_show_reference", showReference)
                                    .putString("wallpaper_text_size", wallpaperTextSize).putString("wallpaper_text_position", wallpaperTextPosition)
                                    .putInt("wallpaper_image_index", wallpaperImageIndex).apply()
                                onSavePreferences(selectedReligion, selectedLanguage, selectedFrequency, selectedReminders, wallpaperMode)
                                wallpaperNotice = null
                                applySheet = true
                            },
                            enabled = !applying, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = W.Purple, contentColor = W.OnPurple)
                        ) {
                            if (applying) CircularProgressIndicator(color = W.OnPurple, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else { Icon(Icons.Outlined.Check, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Save & Apply Wallpaper", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1) }
                        }
                        OutlinedButton(onClick = { fullPreview = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, W.PurpleMid)) {
                            Icon(Icons.Outlined.Visibility, null, tint = W.PurpleDark, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Preview full screen", color = W.PurpleDark, fontWeight = FontWeight.Bold)
                        }
                    }
                    "reading" -> {
                        SettingCard {
                            Text("Verse font size", fontWeight = FontWeight.Bold, color = W.Ink)
                            Slider(value = fontSize.toFloat(), onValueChange = { onFontSize(it.toInt()) }, valueRange = 16f..32f, steps = 7, colors = SliderDefaults.colors(thumbColor = W.Gold, activeTrackColor = W.Purple))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Small", color = W.InkMid, fontSize = 11.sp); Text("Medium", color = W.InkMid, fontSize = 11.sp); Text("Large", color = W.InkMid, fontSize = 11.sp) }
                        }
                        ToggleCard("Show scripture name", "Display the scripture label above each verse", readingScripture) { readingScripture = it }
                        ToggleCard("Show chapter and verse reference", "Display the source reference", readingReference) { readingReference = it }
                        ToggleCard("Show translation", "Display translation details when available", readingTranslation) { readingTranslation = it }
                        ToggleCard("Show WisdomOne branding", "Display the app signature on verse cards", readingBranding) { readingBranding = it }
                        PrimaryAction("Save reading preferences", Icons.Outlined.Check) {
                            prefs.edit().putBoolean("reading_scripture", readingScripture).putBoolean("reading_reference", readingReference)
                                .putBoolean("reading_translation", readingTranslation).putBoolean("reading_branding", readingBranding).apply()
                            route = "home"
                        }
                        SectionTitle("WISDOM TREE")
                        ToggleCard("Wisdom Tree", "Show your tree and the activities for each of the five monthly levels on Home.", treeEnabled, icon = Icons.Outlined.Spa) { treeConfirm = it }
                        InfoCard("Turning the Wisdom Tree off hides it. Turning it back on restarts the 150-day journey from Level 1's first activity. Your reading history is kept.")
                    }
                    "theme" -> {
                        listOf("Light" to "Warm cream and soft gold", "Dark" to "Deep purple and night tones", "System" to "Follow your Android setting").forEach { (theme, desc) ->
                            SelectionRow(theme, desc, rememberVector(when (theme) { "Light" -> Icons.Outlined.LightMode; "Dark" -> Icons.Outlined.DarkMode; else -> Icons.Outlined.PhoneAndroid }), appTheme == theme) {
                                appTheme = theme; prefs.edit().putString("app_theme", theme).apply()   // applies immediately
                            }
                        }
                        InfoCard("The theme applies to the whole app straight away.")
                    }
                    "notifications" -> {
                        // Re-read SharedPrefs every time this section is shown so values reflect
                        // permission grants that happened outside the app (e.g. lock-screen toggle on Home).
                        LaunchedEffect(Unit) {
                            selectedReminders = prefs.getBoolean("reminders", false)
                            notifyVerseUpdates = prefs.getBoolean("notify_verse_updates", false)
                            notifyWallpaperUpdates = prefs.getBoolean("notify_wallpaper_updates", false)
                        }
                        // When master reminders switch is turned ON, also enable the sub-toggles
                        // so they reflect the actual notification state.
                        LaunchedEffect(selectedReminders) {
                            if (selectedReminders) {
                                if (!notifyVerseUpdates) {
                                    notifyVerseUpdates = true
                                    prefs.edit().putBoolean("notify_verse_updates", true).apply()
                                }
                                if (!notifyWallpaperUpdates) {
                                    notifyWallpaperUpdates = true
                                    prefs.edit().putBoolean("notify_wallpaper_updates", true).apply()
                                }
                            }
                        }
                        ToggleCard("Verse reminders", "Notify when a new verse is scheduled", selectedReminders, icon = Icons.Outlined.Notifications) { selectedReminders = it; onReminders(it) }
                        ToggleCard("Verse update notifications", "Notify when the active verse changes", notifyVerseUpdates, icon = Icons.AutoMirrored.Outlined.MenuBook) { notifyVerseUpdates = it; prefs.edit().putBoolean("notify_verse_updates", it).apply() }
                        ToggleCard("Wallpaper update notifications", "Notify when the lock-screen artwork changes", notifyWallpaperUpdates, icon = Icons.Outlined.Image) { notifyWallpaperUpdates = it; prefs.edit().putBoolean("notify_wallpaper_updates", it).apply() }
                        PrimaryAction("Save notification preferences", Icons.Outlined.Check) { onSavePreferences(selectedReligion, selectedLanguage, selectedFrequency, selectedReminders, wallpaperMode); route = "home" }
                        SecondaryAction("Open Android notification settings") { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
                    }
                    "battery" -> {
                        SettingRow(Icons.Outlined.Schedule, "Background scheduling", "One central scheduled job controls verse updates.")
                        SettingRow(Icons.Outlined.Widgets, "Widget refresh", "The widget updates when the active verse changes.")
                        SettingRow(Icons.Outlined.Image, "Wallpaper refresh", "The wallpaper follows your lock-screen wallpaper mode.")
                        SecondaryAction("Open battery optimisation settings") { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                        SecondaryAction("Open app battery settings") { runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))) } }
                    }
                    "help" -> {
                        InfoCard("Your tradition and language control the verses and the wallpaper gallery. General Wisdom always uses religion-neutral content.")
                        SettingRow(Icons.Outlined.Lock, "Privacy", "WisdomOne does not require a personal profile to use these settings.")
                        SettingRow(Icons.Outlined.Sync, "Verse synchronisation", "Home, widget and lock screen share the same active verse.")
                        SecondaryAction("Leave feedback") { onFeedback() }
                    }
                    else -> { onRate(); route = "home" }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    // Save & Apply Wallpaper → choose where it goes.
    if (applySheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(onDismissRequest = { applySheet = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = W.Parchment) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Apply wallpaper", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = W.Ink)
                Text("Choose where your verse wallpaper should appear.", fontSize = 13.sp, color = W.InkMid)
                listOf(
                    Triple("Set as Home Screen", Icons.Outlined.Home, WallpaperManager.FLAG_SYSTEM),
                    Triple("Set as Lock Screen", Icons.Outlined.Lock, WallpaperManager.FLAG_LOCK),
                    Triple("Set as Home Screen & Lock Screen", Icons.Outlined.PhoneAndroid, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                ).forEach { (label, icon, flag) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(W.Surface).border(1.dp, W.Gold.copy(alpha = .35f), RoundedCornerShape(16.dp))
                            .clickable {
                                applySheet = false; applying = true
                                scope.launch {
                                    val activeId = prefs.getInt("last_verse_id", prefs.getInt("current_verse_id", -1))
                                    val active = if (activeId > 0) withContext(Dispatchers.IO) { ScriptureDatabase.get(context).verses().find(activeId) } else null
                                    val result = if (active == null) LockScreenVerse.ApplyResult.Failed("Open a verse on Home first, then apply the wallpaper.")
                                    else withContext(Dispatchers.IO) { LockScreenVerse.applyTo(context, flag, active.id, active.scripture, active.text, active.reference()) }
                                    applying = false
                                    when (result) {
                                        is LockScreenVerse.ApplyResult.Success -> {
                                            wallpaperNotice = "Wallpaper applied to your ${label.removePrefix("Set as ").lowercase()}." to false
                                            onWallpaperApplied()
                                        }
                                        is LockScreenVerse.ApplyResult.Failed -> wallpaperNotice = result.reason to true
                                    }
                                }
                            }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(icon); Spacer(Modifier.width(14.dp))
                        Text(label, fontWeight = FontWeight.Bold, color = W.Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = W.InkMid)
                    }
                }
                TextButton(onClick = { applySheet = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Not now", color = W.InkMid) }
            }
        }
    }
    if (fullPreview) Dialog(onDismissRequest = { fullPreview = false }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LockScreenPreview(selectedReligion, selectedLanguage, wallpaperMode, wallpaperImageIndex, wallpaperTheme, wallpaperTextSize, wallpaperTextPosition, showVerseText, showReference, Modifier.fillMaxWidth(0.82f))
            Button(onClick = { fullPreview = false }, colors = ButtonDefaults.buttonColors(containerColor = W.Purple, contentColor = W.OnPurple)) { Text("Close") }
        }
    }
    treeConfirm?.let { turnOn ->
        AlertDialog(
            onDismissRequest = { treeConfirm = null }, containerColor = W.Surface,
            title = { Text(if (turnOn) "Turn Wisdom Tree on?" else "Turn Wisdom Tree off?", fontWeight = FontWeight.Bold, color = W.Ink) },
            text = { Text(if (turnOn) "Your 150-day journey will restart from Level 1's first activity. Your reading history is kept." else "The tree and its activities will be hidden from Home. You can turn it back on any time (the journey will restart).", color = W.InkMid, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    treeConfirm = null
                    onTreeEnabled(turnOn) { ok -> if (!ok) Toast.makeText(context, "Couldn't update the Wisdom Tree. Check your connection and try again.", Toast.LENGTH_LONG).show() }
                }) { Text(if (turnOn) "Turn on & restart" else "Turn off", color = W.PurpleDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { treeConfirm = null }) { Text("Cancel", color = W.InkMid) } }
        )
    }
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false }, containerColor = W.Surface,
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold, color = W.Ink) },
            text = { Text("You've changed your preference. Save before leaving?", color = W.InkMid, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onSavePreferences(selectedReligion, selectedLanguage, selectedFrequency, selectedReminders, wallpaperMode)
                    route = pendingRoute
                }) { Text("Save", color = W.PurpleDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    selectedReligion = religion
                    selectedLanguage = language
                    route = pendingRoute
                }) { Text("Discard", color = W.InkMid) }
            }
        )
    }
}

// ── Shared building blocks ────────────────────────────────────────────────────────────────

@Composable private fun rememberVector(v: ImageVector): ImageVector = v

@Composable private fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(W.Gold.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = W.Gold, modifier = Modifier.size(24.dp)) }
}

@Composable private fun IconBadge(painter: Painter, modifier: Modifier = Modifier, tint: Color = W.Gold) {
    Box(modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(W.Gold.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Icon(painter, null, tint = tint, modifier = Modifier.size(26.dp)) }
}

@Composable private fun SettingsSubHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = W.Ink) }
        Column(Modifier.padding(top = 6.dp)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = W.Ink)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 13.sp, color = W.InkMid, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable private fun SettingMenuCard(entry: SettingEntry, highlighted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(if (highlighted) W.SurfaceAlt else W.Surface)
            .border(1.dp, if (highlighted) W.PurpleMid.copy(alpha = .35f) else W.Gold.copy(alpha = .22f), RoundedCornerShape(17.dp)).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (entry.painterRes != null) IconBadge(painterResource(entry.painterRes), tint = Color.Unspecified) else IconBadge(entry.icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(entry.title, color = W.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(entry.subtitle, color = W.InkMid, fontSize = 12.sp) }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = W.InkMid)
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, color = W.Gold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp) }

@Composable private fun SelectionRow(title: String, subtitle: String, icon: Any, selected: Boolean, iconTint: Color = W.Gold, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) W.SurfaceAlt else W.Surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) W.PurpleMid else W.Gold.copy(alpha = .25f), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(12.dp)
            .semantics { contentDescription = "$title${if (selected) ", selected" else ""}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (icon) { is Painter -> IconBadge(icon, tint = iconTint); is ImageVector -> IconBadge(icon) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(title, color = W.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp); if (subtitle.isNotBlank()) Text(subtitle, color = W.InkMid, fontSize = 12.sp, lineHeight = 16.sp) }
        Box(Modifier.size(22.dp).clip(CircleShape).border(1.8.dp, if (selected) W.PurpleMid else W.InkSoft, CircleShape), contentAlignment = Alignment.Center) { if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(W.PurpleMid)) }
    }
}

@Composable private fun LanguageChip(name: String, native: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) W.SurfaceAlt else W.Surface).border(if (selected) 1.5.dp else 1.dp, if (selected) W.PurpleMid else W.Gold.copy(alpha = .25f), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (selected) { Box(Modifier.size(18.dp).clip(CircleShape).border(1.6.dp, W.PurpleMid, CircleShape), contentAlignment = Alignment.Center) { Box(Modifier.size(9.dp).clip(CircleShape).background(W.PurpleMid)) }; Spacer(Modifier.width(8.dp)) }
        Column { Text(name, color = W.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1); Text(native, color = W.InkMid, fontSize = 12.sp, maxLines = 1) }
    }
}

@Composable private fun SelectPill(title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(50)).background(if (selected) W.Purple else W.Surface).border(1.dp, if (selected) W.Purple else W.Gold.copy(alpha = .35f), RoundedCornerShape(50)).clickable(onClick = onClick).defaultMinSize(minWidth = 72.dp).padding(vertical = 12.dp, horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = if (selected) W.OnPurple else W.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        if (subtitle.isNotBlank()) Text(subtitle, color = if (selected) W.OnPurple.copy(alpha = .85f) else W.InkMid, fontSize = 10.sp)
    }
}

@Composable private fun FrequencyChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) W.Purple else W.Surface).border(1.dp, if (selected) W.Purple else W.Gold.copy(alpha = .35f), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick).padding(vertical = 14.dp).semantics { contentDescription = "$label${if (selected) ", selected" else ""}" },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Outlined.Schedule, null, tint = if (selected) W.OnPurple else W.Gold, modifier = Modifier.size(24.dp))
            Text(label, color = if (selected) W.OnPurple else W.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        }
        if (selected) Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(22.dp).clip(CircleShape).background(W.PurpleMid), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
    }
}

@Composable private fun BulletRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = W.Gold, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(12.dp)); Text(text, color = W.Ink, fontSize = 13.sp, lineHeight = 18.sp) }
}

/** Settings top: title on the left, the sunrise landscape (design/settings_header_landscape.jpg) fading in from the right. */
@Composable private fun SettingsHeroHeader() {
    Box(
        Modifier.fillMaxWidth()
            .layout { measurable, constraints ->   // bleed to the screen edges past the 18 dp column padding
                val extra = 36.dp.roundToPx()
                val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.maxWidth + extra))
                layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
            }
            .height(150.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.settings_header_landscape), contentDescription = null,
            contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd,
            modifier = Modifier.fillMaxHeight().fillMaxWidth(0.72f).align(Alignment.CenterEnd)
        )
        // Soft veils so the art melts into the page (left edge, top and bottom) and the title stays readable.
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to W.Parchment, 0.38f to W.Parchment, 0.62f to W.Parchment.copy(alpha = 0.35f), 1f to Color.Transparent)))
        Box(Modifier.fillMaxWidth().height(44.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(W.Parchment, Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(36.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, W.Parchment))))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 18.dp).fillMaxWidth(0.66f)) {
            Text("Settings", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = W.Ink)
            Spacer(Modifier.height(4.dp))
            Text("Personalise your experience and daily inspiration", color = W.InkMid, fontSize = 14.sp, lineHeight = 19.sp)
        }
    }
}

/** Settings bottom: the "Make it Yours" card from the approved design (design/settings_make_it_yours_banner.jpg). */
@Composable private fun MakeItYoursCard() {
    Spacer(Modifier.height(6.dp))
    Image(
        painter = painterResource(R.drawable.settings_make_it_yours), contentScale = ContentScale.Crop,
        contentDescription = "Make it Yours. Customise your widget, lock screen and reading experience.",
        modifier = Modifier.fillMaxWidth().aspectRatio(964f / 296f).clip(RoundedCornerShape(22.dp))
            .border(1.dp, W.Gold.copy(alpha = .22f), RoundedCornerShape(22.dp))
    )
}

/**
 * Tradition & Language: a full, crisp banner of the selected tradition's artwork in a rounded card. A parchment veil on the
 * left carries the tradition name and scriptures; switching tradition cross-fades to the new art.
 */
@Composable private fun TraditionBanner(religion: ReligionItem?) {
    val code = religion?.code?.lowercase() ?: ""
    val bannerRes = when {
        code.contains("hindu")     -> R.drawable.banner_hinduism
        code.contains("christian") -> R.drawable.banner_christianity
        code.contains("islam")     -> R.drawable.banner_islam
        code.contains("buddh")     -> R.drawable.banner_buddhism
        else                       -> R.drawable.banner_general_wisdom
    }
    val title = religion?.name ?: "General Wisdom"
    val scriptures = religion?.scriptures?.joinToString(" · ") { it.name }.orEmpty()
    androidx.compose.animation.Crossfade(targetState = Triple(bannerRes, title, scriptures), animationSpec = androidx.compose.animation.core.tween(450), label = "traditionBanner") { (res, name, books) ->
        Box(
            Modifier.fillMaxWidth().height(168.dp).clip(RoundedCornerShape(24.dp))
                .border(1.dp, W.Gold.copy(alpha = .28f), RoundedCornerShape(24.dp))
                .semantics { contentDescription = "$name banner" }
        ) {
            // Crop removes the rounded corners baked into the source art; the landmark sits on the right of every image.
            // General Wisdom's lotus is centred in its art, so show the left part of that image to keep the lotus on the right.
            Image(painterResource(res), null, contentScale = ContentScale.Crop,
                alignment = if (res == R.drawable.banner_general_wisdom) Alignment.CenterStart else Alignment.CenterEnd, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to W.Parchment.copy(alpha = .95f), 0.38f to W.Parchment.copy(alpha = .84f), 0.66f to Color.Transparent)))
            Box(Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, W.Purple.copy(alpha = .10f)))))
            Column(Modifier.align(Alignment.CenterStart).padding(start = 18.dp, end = 12.dp).fillMaxWidth(0.62f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("YOUR TRADITION", color = W.Gold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                Text(name, color = W.PurpleDark, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
                if (books.isNotBlank()) Text(books, color = W.InkMid, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2)
            }
        }
    }
}

/**
 * Each tradition row: the tradition's banner artwork bleeds in from the right (same veil technique as
 * SettingsHeroHeader), with the tradition icon, name, scriptures and radio button on the left.
 * Tapping cross-fades the TraditionBanner above and highlights this row.
 */
@Composable private fun TraditionSelectionRow(item: ReligionItem, selected: Boolean, onClick: () -> Unit) {
    val bannerRes = when {
        item.code.lowercase().contains("hindu")     -> R.drawable.banner_hinduism
        item.code.lowercase().contains("christian") -> R.drawable.banner_christianity
        item.code.lowercase().contains("islam")     -> R.drawable.banner_islam
        item.code.lowercase().contains("buddh")     -> R.drawable.banner_buddhism
        else                                        -> R.drawable.banner_general_wisdom
    }
    val subtitle = item.scriptures.joinToString(", ") { it.name }
    Box(
        Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(16.dp))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) W.PurpleMid else W.Gold.copy(alpha = .25f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${item.name}${if (selected) ", selected" else ""}" }
    ) {
        // Artwork on the right — same ContentScale.Crop + CenterEnd as SettingsHeroHeader
        Image(
            painter = painterResource(bannerRes), contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = if (bannerRes == R.drawable.banner_general_wisdom) Alignment.CenterStart else Alignment.CenterEnd,
            modifier = Modifier.fillMaxHeight().fillMaxWidth(0.65f).align(Alignment.CenterEnd)
        )
        // Horizontal veil: surface colour → transparent (mirrors SettingsHeroHeader)
        val bg = if (selected) W.SurfaceAlt else W.Surface
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to bg, 0.42f to bg, 0.68f to bg.copy(alpha = 0.4f), 1f to Color.Transparent)))
        // Top + bottom edge veils so the card blends into the list
        Box(Modifier.fillMaxWidth().height(22.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(bg, Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(22.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, bg))))
        // Content: icon + text + radio
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(painterResource(traditionIcon(item.code)), tint = Color.Unspecified)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, color = W.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (subtitle.isNotBlank()) Text(subtitle, color = W.InkMid, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1)
            }
            Box(Modifier.size(22.dp).clip(CircleShape).border(1.8.dp, if (selected) W.PurpleMid else W.InkSoft, CircleShape), contentAlignment = Alignment.Center) {
                if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(W.PurpleMid))
            }
        }
    }
}

@Composable private fun ThemeSwatch(key: String, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).semantics { contentDescription = "${label.replace("\n", " ")} colour theme${if (selected) ", selected" else ""}" }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(12.dp)).background(themeBrush(key)).border(if (selected) 2.dp else 1.dp, if (selected) W.PurpleMid else W.Gold.copy(alpha = .3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.TopEnd) {
            if (selected) Box(Modifier.padding(3.dp).size(18.dp).clip(CircleShape).background(W.PurpleMid), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
        }
        Text(label, color = W.Ink, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable private fun ToggleCard(title: String, subtitle: String, checked: Boolean, icon: ImageVector? = null, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(W.Surface).border(1.dp, if (checked) W.PurpleMid.copy(alpha = .45f) else W.Gold.copy(alpha = .25f), RoundedCornerShape(16.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { IconBadge(icon); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) { Text(title, color = W.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(subtitle, color = W.InkMid, fontSize = 12.sp, lineHeight = 16.sp) }
        Switch(checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = W.Purple, uncheckedThumbColor = W.InkMid, uncheckedTrackColor = W.SurfaceAlt, uncheckedBorderColor = W.InkSoft))
    }
}

@Composable private fun SettingCard(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(W.Surface).border(1.dp, W.Gold.copy(alpha = .25f), RoundedCornerShape(17.dp)).padding(15.dp), content = content) }
@Composable private fun SettingRow(icon: ImageVector, title: String, subtitle: String) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(W.Surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = W.Gold); Spacer(Modifier.width(12.dp)); Column { Text(title, color = W.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(subtitle, color = W.InkMid, fontSize = 12.sp) } } }
@Composable private fun InfoCard(text: String) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(W.Gold.copy(alpha = .16f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Info, null, tint = W.Gold); Spacer(Modifier.width(10.dp)); Text(text, color = W.Ink, fontSize = 13.sp, lineHeight = 18.sp) } }
@Composable private fun NoticeCard(text: String, isError: Boolean) { Text(text, color = if (isError) W.Error else W.Success, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background((if (isError) W.Error else W.Success).copy(alpha = .12f)).padding(12.dp)) }
@Composable private fun PrimaryAction(text: String, icon: ImageVector? = null, onClick: () -> Unit) { Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Purple, contentColor = W.OnPurple)) { if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }; Text(text, fontWeight = FontWeight.Bold) } }
@Composable private fun SecondaryAction(text: String, onClick: () -> Unit) { OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, W.Gold)) { Text(text, color = W.Ink, fontWeight = FontWeight.SemiBold) } }

@Composable
private fun AlwaysOnPreviewCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val verse = remember { com.scripturedaily.ambient.VerseLiveWallpaperService.currentVerse(context) }
    val now = remember { java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern(if (android.text.format.DateFormat.is24HourFormat(context)) "H:mm" else "h:mm")) }
    Box(
        modifier.aspectRatio(9f / 19.5f).clip(RoundedCornerShape(28.dp)).background(Color.Black).padding(5.dp).clip(RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text(now, color = Color(0xFFBDBDBD), fontSize = 44.sp, fontWeight = FontWeight.Thin)
            Spacer(Modifier.height(18.dp))
            Text(
                com.scripturedaily.ambient.AmbientWisdom.excerpt(verse.first, 44),
                color = Color(0xFF9E9E9E), fontSize = 11.sp, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
            )
            Spacer(Modifier.height(4.dp))
            Text(verse.second, color = Color(0xFF6E6E6E), fontSize = 9.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

// ── Wallpaper gallery: 20 images, 4 per visible group, swipe horizontally ─────────────────

@Composable private fun WallpaperGallery(context: Context, religionId: Int, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    val pages = WallpaperPacks.COUNT / 4
    val pager = rememberPagerState(initialPage = ((selected - 1) / 4).coerceIn(0, pages - 1)) { pages }
    Column(Modifier.fillMaxWidth().then(if (enabled) Modifier else Modifier.alpha(.45f))) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), pageSpacing = 12.dp) { page ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (n in 0 until 4) {
                    val index = page * 4 + n + 1
                    WallpaperThumb(context, religionId, index, index == selected, Modifier.weight(1f)) { if (enabled) onSelect(index) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages) { Box(Modifier.padding(horizontal = 3.dp).size(if (it == pager.currentPage) 18.dp else 7.dp, 7.dp).clip(RoundedCornerShape(4.dp)).background(if (it == pager.currentPage) W.PurpleMid else W.InkSoft.copy(alpha = .4f))) }
        }
    }
}

@Composable private fun WallpaperThumb(context: Context, religionId: Int, index: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bitmap by produceState<Bitmap?>(null, religionId, index) { value = withContext(Dispatchers.IO) { WallpaperPacks.load(context, religionId, index, maxWidthPx = 240) } }
    Box(
        modifier.aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp)).background(W.SurfaceAlt)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) W.PurpleMid else W.Gold.copy(alpha = .3f), RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .semantics { contentDescription = "Wallpaper $index${if (selected) ", selected" else ""}" }
    ) {
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (selected) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(20.dp).clip(CircleShape).background(W.PurpleMid), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
        Text("$index", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 5.dp, vertical = 1.dp))
    }
}

// ── Realistic lock-screen preview ──────────────────────────────────────────────────────────

@Composable
private fun LockScreenPreview(
    religionId: Int, languageId: Int, mode: String, imageIndex: Int, theme: String, textSize: String, textPosition: String,
    showVerse: Boolean, showReference: Boolean, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reader", Context.MODE_PRIVATE) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Re-rendered immediately whenever the image, mode, theme or layout options change.
    LaunchedEffect(religionId, languageId, mode, imageIndex, theme, textSize, textPosition, showVerse, showReference) {
        val activeId = prefs.getInt("last_verse_id", prefs.getInt("current_verse_id", -1))
        val cached = if (activeId > 0) withContext(Dispatchers.IO) { ScriptureDatabase.get(context).verses().find(activeId) } else null
        val tradition = WallpaperPacks.traditionName(religionId)
        val active = cached?.takeIf { it.religion.equals(tradition, ignoreCase = true) && it.languageId == languageId }
        val scripture = active?.scripture ?: tradition
        val verseText = active?.text ?: "Let wisdom guide your thoughts and bring peace to your day."
        val reference = active?.reference() ?: "Daily inspiration · WisdomOne"
        val rendered = withContext(Dispatchers.IO) {
            LockScreenVerse.previewBitmap(context, scripture, verseText, reference, theme, null, textSize, textPosition, imageIndex, religionId, mode, showVerse, showReference, 360, 780)
        }
        bitmap = rendered
    }
    val now = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())) }
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())) }
    Box(
        modifier.aspectRatio(360f / 780f).clip(RoundedCornerShape(30.dp)).background(Color.Black).border(4.dp, Color(0xFF2A2433), RoundedCornerShape(30.dp)).padding(3.dp).clip(RoundedCornerShape(27.dp))
            .semantics { contentDescription = "Lock screen preview" }
    ) {
        val current = bitmap
        if (current != null) Image(current.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp)) }
        // Lock-screen furniture drawn over the artwork so the preview looks like the real thing.
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(now, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Outlined.SignalCellularAlt, null, tint = Color.White, modifier = Modifier.size(10.dp)); Icon(Icons.Outlined.BatteryFull, null, tint = Color.White, modifier = Modifier.size(10.dp)) }
        }
        Column(Modifier.align(Alignment.TopCenter).padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Lock, null, tint = Color.White, modifier = Modifier.size(12.dp))
            Text(now, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Light, style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = .5f), blurRadius = 8f)))
            Text(date, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Box(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 16.dp).size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FlashlightOn, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 16.dp).size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).width(70.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .8f)))
    }
}

private fun routeTitle(route: String) = when (route) { "tradition" -> "Tradition & Language"; "frequency" -> "Verse Frequency"; "wallpaper" -> "Lock Screen & Wallpaper"; "reading" -> "Reading Preferences"; "theme" -> "App Theme"; "notifications" -> "Notifications"; "battery" -> "Battery & Performance"; "help" -> "Help & Support"; else -> "Settings" }
private fun routeSubtitle(route: String) = when (route) {
    "tradition" -> "Choose your tradition and preferred language for daily verses."
    "frequency" -> "Set how often WisdomOne refreshes your inspiration."
    "wallpaper" -> "Verse wallpaper, live wallpaper, always-on display and visual design."
    "reading" -> "Make verses comfortable and clear to read."
    "theme" -> "Choose how WisdomOne looks on your device."
    "notifications" -> "Choose which updates you want to receive."
    "battery" -> "Help WisdomOne refresh reliably without unnecessary battery use."
    "help" -> "Find help and learn how WisdomOne works."
    else -> ""
}
private fun frequencyLabel(value: String) = when (value) { "Every hour" -> "Every 1 hour"; "Every 6 hours" -> "Every 6 hours"; "Every 12 hours" -> "Every 12 hours"; "Every 24 hours", "Daily" -> "Every 24 hours"; else -> value }
private fun traditionIcon(code: String) = when (code.lowercase()) { "hinduism" -> R.drawable.ic_tradition_hinduism; "christianity" -> R.drawable.ic_tradition_christianity; "islam" -> R.drawable.ic_tradition_islam; "buddhism" -> R.drawable.ic_tradition_buddhism; else -> R.drawable.ic_tradition_general_wisdom }
private fun themeBrush(theme: String): Brush = when (theme) {
    "GOLDEN" -> Brush.linearGradient(listOf(Color(0xFFFFD58A), Color(0xFFB96D28), Color(0xFF4C2A39)))
    "ROYAL_PURPLE" -> Brush.linearGradient(listOf(Color(0xFF9B6AC0), Color(0xFF492166), Color(0xFF1B102D)))
    "FOREST_GREEN" -> Brush.linearGradient(listOf(Color(0xFF8ABF91), Color(0xFF245C47), Color(0xFF102D2A)))
    "OCEAN_BLUE" -> Brush.linearGradient(listOf(Color(0xFF8AD4F2), Color(0xFF176D9F), Color(0xFF102B55)))
    "SOFT_SAND" -> Brush.linearGradient(listOf(Color(0xFFFFE8C5), Color(0xFFD9B98D), Color(0xFF87694D)))
    "MIDNIGHT" -> Brush.linearGradient(listOf(Color(0xFF55546E), Color(0xFF211C36), Color(0xFF090812)))
    else -> Brush.linearGradient(listOf(Color(0xFFFFB3C1), Color(0xFFFFD58A), Color(0xFF8AD4F2), Color(0xFFB9A3F4)))
}

fun normalizeWallpaperTheme(value: String?): String = when (value?.trim()?.uppercase()) {
    "GOLDEN", "ROYAL_PURPLE", "FOREST_GREEN", "OCEAN_BLUE", "SOFT_SAND", "MIDNIGHT", "MULTICOLOR" -> value!!.trim().uppercase()
    "SAGE", "FOREST" -> "FOREST_GREEN"
    "OCEAN", "BLUE" -> "OCEAN_BLUE"
    "SAND", "DAY" -> "SOFT_SAND"
    "DUSK", "PURPLE" -> "ROYAL_PURPLE"
    "AUTO", "TIME OF DAY", null, "" -> "MULTICOLOR"
    else -> "MULTICOLOR"
}
