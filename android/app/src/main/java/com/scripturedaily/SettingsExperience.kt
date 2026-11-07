package com.scripturedaily

import android.content.Context
import android.graphics.Bitmap
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.CatalogItem
import com.scripturedaily.data.ReligionItem
import com.scripturedaily.data.ScriptureDatabase
import com.scripturedaily.lockscreen.LockScreenVerse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Purple = Color(0xFF512078)
private val DeepPurple = Color(0xFF2C1247)
private val Gold = Color(0xFFD99A4B)
private val SoftGold = Color(0xFFFFE9C9)
private val Muted = Color(0xFF82758E)

private data class SettingEntry(val title: String, val subtitle: String, val icon: ImageVector, val route: String)

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
    onSavePreferences: (Int, Int, String, Boolean, String) -> Unit,
    onReminders: (Boolean) -> Unit,
    onLockScreen: (Boolean) -> Unit,
    onFontSize: (Int) -> Unit,
    onWidgetStudio: () -> Unit,
    onRate: () -> Unit,
    onFeedback: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reader", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var route by remember { mutableStateOf("home") }
    var selectedReligion by remember(religion) { mutableIntStateOf(religion) }
    var selectedLanguage by remember(language) { mutableIntStateOf(language) }
    var selectedFrequency by remember(frequency) { mutableStateOf(if (frequency == "Daily") "Every 24 hours" else frequency) }
    var selectedReminders by remember(reminders) { mutableStateOf(reminders) }
    var availableLanguages by remember(languages) { mutableStateOf(languages) }
    var loadingLanguages by remember { mutableStateOf(false) }
    var autoWallpaper by remember(lockScreen) { mutableStateOf(lockScreen) }
    var wallpaperMode by remember {
        mutableStateOf(prefs.getString("wallpaper_mode", "change_verse")?.let {
            if (it.equals("Keep same verse, change wallpaper", true)) "keep_same_verse" else "change_verse"
        } ?: "change_verse")
    }
    var wallpaperTheme by remember { mutableStateOf(normalizeWallpaperTheme(prefs.getString("wallpaper_theme", "MULTICOLOR"))) }
    var wallpaperCategory by remember { mutableStateOf(prefs.getString("wallpaper_category", "") ?: "") }
    var wallpaperImageIndex by remember { mutableIntStateOf(prefs.getInt("wallpaper_image_index", 1).coerceIn(1, 20)) }
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
    var wallpaperNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedReligion) {
        if (selectedReligion > 0) {
            loadingLanguages = true
            availableLanguages = try { ApiClient.api.languages(selectedReligion) } catch (_: Exception) { emptyList() }
            if (availableLanguages.none { it.id == selectedLanguage }) selectedLanguage = availableLanguages.firstOrNull()?.id ?: 0
            loadingLanguages = false
        }
    }

    BackHandler(enabled = route != "home") { route = "home" }
    val selectedTradition = religions.firstOrNull { it.id == selectedReligion }?.name ?: "Your tradition"
    val categories = wallpaperCategories(selectedTradition)
    var previousTradition by remember { mutableStateOf(selectedTradition) }
    LaunchedEffect(selectedTradition) {
        if (previousTradition != selectedTradition) {
            wallpaperCategory = categories.firstOrNull().orEmpty()
            wallpaperImageIndex = 1
            previousTradition = selectedTradition
        } else if (wallpaperCategory.isBlank() || wallpaperCategory !in categories) {
            wallpaperCategory = categories.firstOrNull().orEmpty()
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (route == "home") {
            // The app shell already renders the WisdomOne brand header; avoid a duplicate logo here.
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Settings", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("Personalise your experience and daily inspiration", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                val entries = listOf(
                    SettingEntry("Tradition & Language", "${selectedTradition} · ${availableLanguages.firstOrNull { it.id == language }?.name ?: "Language"}", Icons.Outlined.Spa, "tradition"),
                    SettingEntry("Verse Frequency", frequencyLabel(frequency), Icons.Outlined.Schedule, "frequency"),
                    SettingEntry("Lock Screen & Wallpaper", "Design, style & colour themes", Icons.Outlined.Wallpaper, "wallpaper"),
                    SettingEntry("Widget Studio", "Create and customise your widgets", Icons.Outlined.Widgets, "widget"),
                    SettingEntry("Reading Preferences", "Font size, verse display & style", Icons.Outlined.TextFields, "reading"),
                    SettingEntry("App Theme", "Light · Dark · System", Icons.Outlined.DarkMode, "theme"),
                    SettingEntry("Notifications", "Verse alerts & reminders", Icons.Outlined.Notifications, "notifications"),
                    SettingEntry("Battery & Performance", "Optimise background updates", Icons.Outlined.BatteryChargingFull, "battery"),
                    SettingEntry("Help & Support", "FAQs, contact us", Icons.Outlined.HelpOutline, "help"),
                    SettingEntry("Rate WisdomOne", "Share your feedback & support", Icons.Outlined.StarOutline, "rate")
                )
                entries.forEach { entry ->
                    SettingMenuCard(entry) {
                        when (entry.route) {
                            "widget" -> onWidgetStudio()
                            "rate" -> onRate()
                            else -> route = entry.route
                        }
                    }
                }
                if (notice != null) NoticeCard(notice)
                if (wallpaperNotice != null) NoticeCard(wallpaperNotice!!)
                OutlinedButton(onClick = onFeedback, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, Gold.copy(alpha = .55f))) {
                    Text("Leave us a feedback", color = Gold, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
            }
        } else {
            SettingsSubHeader(title = routeTitle(route), onBack = { route = "home" })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (route) {
                    "tradition" -> {
                        Text("Choose your tradition and preferred language for daily verses.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 13.sp)
                        SectionTitle("YOUR TRADITION")
                        religions.forEach { item ->
                            val chosen = selectedReligion == item.id
                            SelectionRow(item.name, item.scriptures.joinToString(", ") { it.name }, traditionEmoji(item.code), chosen) { selectedReligion = item.id }
                        }
                        SectionTitle("SELECT LANGUAGE")
                        if (loadingLanguages) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Purple)
                        else availableLanguages.chunked(3).forEach { chunk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                chunk.forEach { item ->
                                    SelectPill(item.name, item.nativeScript.ifBlank { item.code }, selectedLanguage == item.id, Modifier.weight(1f)) { selectedLanguage = item.id }
                                }
                                repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        PrimaryAction("Save preferences") {
                            onSavePreferences(selectedReligion, selectedLanguage, selectedFrequency, selectedReminders, wallpaperMode)
                            route = "home"
                        }
                    }
                    "frequency" -> {
                        Text("Set how often WisdomOne refreshes your inspiration. This one setting controls the Home verse, lock-screen wallpaper and home-screen widget.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 13.sp, lineHeight = 19.sp)
                        FrequencyHero()
                        listOf("Every hour", "Every 6 hours", "Every 12 hours", "Every 24 hours").forEach { option ->
                            SelectionRow(option, frequencyDescription(option), "◷", selectedFrequency == option) { selectedFrequency = option }
                        }
                        InfoCard("One frequency keeps your verse, wallpaper and widget in sync. The wallpaper-only mode can keep the current verse unchanged.")
                        PrimaryAction("Save frequency") {
                            onSavePreferences(selectedReligion, selectedLanguage, selectedFrequency, selectedReminders, wallpaperMode)
                            route = "home"
                        }
                    }
                    "wallpaper" -> {
                        Text("Design your lock-screen experience for $selectedTradition.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 13.sp)
                        ToggleCard("Auto Verse Wallpaper", "Automatically update your lock screen using Verse Frequency", autoWallpaper) {
                            autoWallpaper = it
                            onLockScreen(it)
                        }
                        WallpaperPreviewCard(selectedTradition, selectedReligion, wallpaperCategory, wallpaperTheme, selectedLanguage, wallpaperTextSize, wallpaperTextPosition, wallpaperImageIndex)
                        SectionTitle("WALLPAPER CONTENT")
                        listOf("change_verse" to "Change verse + wallpaper", "keep_same_verse" to "Keep same verse, change wallpaper").forEach { (modeKey, modeLabel) ->
                            SelectionRow(modeLabel, if (modeKey == "change_verse") "A new verse and matching artwork are applied together" else "Keep Home and widget verse unchanged; refresh artwork only", "✦", wallpaperMode == modeKey) { wallpaperMode = modeKey }
                        }
                        SectionTitle("WALLPAPER GALLERY · $selectedTradition")
                        if (selectedTradition.equals("Hinduism", true)) {
                            (1..20).chunked(2).forEach { imageRow ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    imageRow.forEach { imageIndex ->
                                        WallpaperImageTile(context, imageIndex, wallpaperImageIndex == imageIndex, Modifier.weight(1f)) {
                                            wallpaperImageIndex = imageIndex
                                            wallpaperCategory = categoryForHinduImage(imageIndex)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Choose a style for $selectedTradition. Its curated image collection will appear here when added to the app.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 12.sp)
                            categories.chunked(2).forEach { chunk ->
                                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                                    chunk.forEach { category ->
                                        ArtworkTile(category, selectedTradition, wallpaperCategory == category, Modifier.weight(1f)) { wallpaperCategory = category }
                                    }
                                    if (chunk.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        SectionTitle("WALLPAPER CATEGORY · $selectedTradition")
                        categories.chunked(2).forEach { chunk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                                chunk.forEach { category ->
                                    ArtworkTile(category, selectedTradition, wallpaperCategory == category, Modifier.weight(1f)) { wallpaperCategory = category }
                                }
                                if (chunk.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        TextButton(onClick = { route = "gallery" }) { Text("Browse wallpaper gallery  →", color = Purple, fontWeight = FontWeight.Bold) }
                        SectionTitle("COLOUR THEME")
                        val themes = listOf("MULTICOLOR" to "Multicolor\nAutomatic", "GOLDEN" to "Golden", "ROYAL_PURPLE" to "Royal Purple", "FOREST_GREEN" to "Forest Green", "OCEAN_BLUE" to "Ocean Blue", "SOFT_SAND" to "Soft Sand", "MIDNIGHT" to "Midnight")
                        themes.chunked(3).forEach { chunk ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                chunk.forEach { (key, label) -> ThemeSwatch(key, label, wallpaperTheme == key, Modifier.weight(1f)) { wallpaperTheme = key }
                                }
                                repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        InfoCard(if (wallpaperTheme == "MULTICOLOR") "Multicolor changes the complete wallpaper palette and decorative treatment while preserving the selected tradition artwork." else "Your selected colour theme stays fixed when the verse updates.")
                        SectionTitle("VERSE TEXT SIZE")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Small", "Medium", "Large").forEach { option ->
                                SelectPill(option, when (option) { "Small" -> "18sp"; "Medium" -> "23sp"; else -> "28sp" }, wallpaperTextSize == option, Modifier.weight(1f)) { wallpaperTextSize = option }
                            }
                        }
                        InfoCard("Text automatically wraps and scales down to fit long verses. The selected size is your preferred starting size.")
                        SectionTitle("TEXT POSITION")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Top", "Middle", "Bottom").forEach { option ->
                                SelectPill(option, if (option == "Top") "Below clock" else option, wallpaperTextPosition == option, Modifier.weight(1f)) { wallpaperTextPosition = option }
                            }
                        }
                        SectionTitle("VERSE CONTENT")
                        ToggleCard("Show verse text on wallpaper", "Keep the selected verse readable on the image", showVerseText) { showVerseText = it }
                        ToggleCard("Show scripture reference", "Display scripture, chapter and verse", showReference) { showReference = it }
                        if (wallpaperNotice != null) NoticeCard(wallpaperNotice!!)
                        PrimaryAction("Save & Apply wallpaper") {
                            prefs.edit().putInt("religion", selectedReligion).putInt("language", selectedLanguage)
                                .putString("wallpaper_mode", wallpaperMode).putString("wallpaper_theme", wallpaperTheme)
                                .putString("wallpaper_category", wallpaperCategory).putBoolean("wallpaper_show_verse", showVerseText)
                                .putBoolean("wallpaper_show_reference", showReference)
                                .putString("wallpaper_text_size", wallpaperTextSize)
                                .putString("wallpaper_text_position", wallpaperTextPosition)
                                .putInt("wallpaper_image_index", wallpaperImageIndex).apply()
                            wallpaperNotice = "Saving wallpaper preferences and applying the current verse…"
                            scope.launch {
                                val activeId = prefs.getInt("last_verse_id", prefs.getInt("current_verse_id", -1))
                                val active = if (activeId > 0) withContext(Dispatchers.IO) { ScriptureDatabase.get(context).verses().find(activeId) } else null
                                val applied = if (active != null) withContext(Dispatchers.IO) {
                                    LockScreenVerse.applyWallpaperNow(context, active.id, active.scripture, active.text, active.reference())
                                } else false
                                wallpaperNotice = when {
                                    applied && autoWallpaper -> "Wallpaper applied. Automatic updates are enabled."
                                    applied -> "Wallpaper applied. Automatic updates remain paused."
                                    else -> "Preferences saved. Apply will complete when a valid active verse and lock-screen permission are available."
                                }
                            }
                            onSavePreferences(selectedReligion, selectedLanguage, selectedFrequency, selectedReminders, wallpaperMode)
                            route = "home"
                        }
                    }
                    "gallery" -> {
                        Text("Choose artwork made for $selectedTradition. Other traditions' religious artwork is never shown here.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(selectedTradition, color = Purple, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).background(SoftGold).padding(horizontal = 14.dp, vertical = 8.dp))
                            Text("${categories.size} categories", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), modifier = Modifier.padding(vertical = 8.dp))
                        }
                        if (selectedTradition.equals("Hinduism", true)) {
                            (1..20).chunked(2).forEach { imageRow ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    imageRow.forEach { imageIndex ->
                                        WallpaperImageTile(
                                            context = context, imageIndex = imageIndex,
                                            selected = wallpaperImageIndex == imageIndex,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            wallpaperImageIndex = imageIndex
                                            wallpaperCategory = categoryForHinduImage(imageIndex)
                                            prefs.edit().putInt("wallpaper_image_index", imageIndex)
                                                .putString("wallpaper_category", wallpaperCategory).apply()
                                            route = "wallpaper"
                                        }
                                    }
                                }
                            }
                        } else {
                            categories.chunked(2).forEach { categoryRow ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    categoryRow.forEach { category ->
                                        ArtworkTile(category, selectedTradition, wallpaperCategory == category, Modifier.weight(1f)) {
                                            wallpaperCategory = category
                                            prefs.edit().putString("wallpaper_category", category).apply()
                                            route = "wallpaper"
                                        }
                                    }
                                    if (categoryRow.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                            InfoCard("The $selectedTradition artwork gallery is ready for its curated 20-image pack. This build currently includes the complete generated Hindu artwork pack; the other four packs are not yet included.")
                        }
                        InfoCard("The gallery is filtered using your selected tradition. General Wisdom contains only universal, non-religious artwork.")
                    }
                    "reading" -> {
                        Text("Make verses comfortable and clear to read.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 13.sp)
                        SettingCard {
                            Text("Verse font size", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Slider(value = fontSize.toFloat(), onValueChange = { onFontSize(it.toInt()) }, valueRange = 16f..32f, steps = 7, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Purple))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Small", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 11.sp); Text("Medium", color = Gold, fontSize = 11.sp); Text("Large", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 11.sp) }
                        }
                        ToggleCard("Show scripture name", "Display the scripture label above each verse", readingScripture) { readingScripture = it }
                        ToggleCard("Show chapter and verse reference", "Display the source reference", readingReference) { readingReference = it }
                        ToggleCard("Show translation", "Display translation details when available", readingTranslation) { readingTranslation = it }
                        ToggleCard("Show WisdomOne branding", "Display the app signature on verse cards", readingBranding) { readingBranding = it }
                        PrimaryAction("Save reading preferences") {
                            prefs.edit().putBoolean("reading_scripture", readingScripture).putBoolean("reading_reference", readingReference)
                                .putBoolean("reading_translation", readingTranslation).putBoolean("reading_branding", readingBranding).apply()
                            route = "home"
                        }
                    }
                    "theme" -> {
                        Text("Choose how WisdomOne looks on your device.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 13.sp)
                        listOf("Light", "Dark", "System").forEach { theme ->
                            SelectionRow(theme, when (theme) { "Light" -> "Warm cream and soft gold"; "Dark" -> "Deep purple and night tones"; else -> "Follow your Android setting" }, "◐", appTheme == theme) { appTheme = theme }
                        }
                        PrimaryAction("Save app theme") { prefs.edit().putString("app_theme", appTheme).apply(); route = "home" }
                    }
                    "notifications" -> {
                        Text("Choose which updates you want to receive.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 13.sp)
                        ToggleCard("Verse reminders", "Notify when a new verse is scheduled", selectedReminders) { selectedReminders = it; onReminders(it) }
                        ToggleCard("Verse update notifications", "Notify when the active verse changes", notifyVerseUpdates) { notifyVerseUpdates = it; prefs.edit().putBoolean("notify_verse_updates", it).apply() }
                        ToggleCard("Wallpaper update notifications", "Notify when the lock-screen artwork changes", notifyWallpaperUpdates) { notifyWallpaperUpdates = it; prefs.edit().putBoolean("notify_wallpaper_updates", it).apply() }
                        PrimaryAction("Save notification preferences") { onSavePreferences(selectedReligion, selectedLanguage, selectedFrequency, selectedReminders, wallpaperMode); route = "home" }
                        SecondaryAction("Open Android notification settings") { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
                    }
                    "battery" -> {
                        Text("Help WisdomOne refresh reliably without unnecessary battery use.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 13.sp)
                        InfoCard("Verse updates use Android WorkManager and the frequency selected in Verse Frequency. Android may defer background work during Doze mode or battery saver.")
                        SettingRow(Icons.Outlined.Schedule, "Background scheduling", "One central scheduled job controls verse updates.")
                        SettingRow(Icons.Outlined.Widgets, "Widget refresh", "The widget updates when the active verse changes.")
                        SettingRow(Icons.Outlined.Wallpaper, "Wallpaper refresh", "Wallpaper-only mode reuses the active verse and changes artwork only.")
                        SecondaryAction("Open battery optimisation settings") { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                        SecondaryAction("Open app battery settings") { runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))) } }
                    }
                    "help" -> {
                        Text("Find help and learn how WisdomOne works.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 13.sp)
                        InfoCard("Your tradition and language control the verses and tradition-specific wallpaper categories. General Wisdom always uses religion-neutral content.")
                        SettingRow(Icons.Outlined.Lock, "Privacy", "WisdomOne does not require a personal profile to use these settings.")
                        SettingRow(Icons.Outlined.Sync, "Verse synchronisation", "Home, widget and lock screen share the same active verse in Change verse + wallpaper mode.")
                        SecondaryAction("Leave feedback") { onFeedback() }
                    }
                    else -> { onRate(); route = "home" }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable private fun SettingsHeader() {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_wisdomone_ui_logo), contentDescription = "WisdomOne logo", modifier = Modifier.size(44.dp).clip(CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text("WisdomOne", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground); Text("Personalise your experience", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f)) }
        Icon(Icons.Outlined.NotificationsNone, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
    }
}

@Composable private fun SettingsSubHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground) }
        Column { Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground); Text("Personalise your experience", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f)) }
    }
}

@Composable private fun SettingMenuCard(entry: SettingEntry, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp)).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(SoftGold.copy(alpha = .58f)), contentAlignment = Alignment.Center) { Icon(entry.icon, null, tint = Gold, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(entry.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(entry.subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 11.sp) }
        Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, color = Gold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp) }

@Composable private fun SelectionRow(title: String, subtitle: String, symbol: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) Purple.copy(alpha = .14f) else MaterialTheme.colorScheme.surface).border(if (selected) 1.5.dp else 1.dp, if (selected) Purple.copy(alpha = .65f) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(SoftGold.copy(alpha = .55f)), contentAlignment = Alignment.Center) { Text(symbol, fontSize = 22.sp, color = Gold) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) { Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 11.sp, lineHeight = 15.sp) }
        Box(Modifier.size(21.dp).clip(CircleShape).border(1.7.dp, if (selected) Purple else Muted.copy(alpha = .55f), CircleShape), contentAlignment = Alignment.Center) { if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(Purple)) }
    }
}

@Composable private fun SelectPill(title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) Purple.copy(alpha = .14f) else MaterialTheme.colorScheme.surface).border(1.dp, if (selected) Purple else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 10.sp)
    }
}

@Composable private fun FrequencyHero() {
    Box(Modifier.fillMaxWidth().height(135.dp).clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(listOf(Color(0xFFFFE8C8), Color(0xFFD9C9F1), Color(0xFF8E77B5)))), contentAlignment = Alignment.Center) {
        Text("☼", fontSize = 60.sp, color = Color(0xFFFFF3C9)); Text("A rhythm of daily wisdom", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp), color = DeepPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable private fun WallpaperPreviewCard(
    tradition: String, religionId: Int, category: String, theme: String, languageId: Int,
    textSize: String, textPosition: String, imageIndex: Int
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reader", Context.MODE_PRIVATE) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(tradition, religionId, category, theme, languageId, textSize, textPosition, imageIndex) {
        val activeId = prefs.getInt("last_verse_id", prefs.getInt("current_verse_id", -1))
        val cached = if (activeId > 0) withContext(Dispatchers.IO) { ScriptureDatabase.get(context).verses().find(activeId) } else null
        val active = cached?.takeIf { it.religion.equals(tradition, ignoreCase = true) && it.languageId == languageId }
        val scripture = active?.scripture ?: tradition
        val verseText = active?.text ?: "Let wisdom guide your thoughts and bring peace to your day."
        val reference = active?.reference() ?: "Daily inspiration · WisdomOne"
        val rendered = withContext(Dispatchers.IO) { LockScreenVerse.previewBitmap(context, scripture, verseText, reference, theme, category, textSize, textPosition, imageIndex, religionId) }
        val old = bitmap
        bitmap = rendered
        old?.recycle()
    }
    Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)).background(themeBrush(theme))) {
        val current = bitmap
        if (current != null) Image(current.asImageBitmap(), contentDescription = "Live lock-screen wallpaper preview", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
    }
    DisposableEffect(Unit) { onDispose { bitmap?.recycle() } }
}

private fun categoryForHinduImage(index: Int): String = when (index) {
    1, 2 -> "Krishna"
    3 -> "Radha Krishna"
    4, 5 -> "Shiva"
    6, 7 -> "Ganesha"
    8 -> "Bhagavad Gita"
    else -> "Nature"
}

@Composable private fun WallpaperImageTile(context: Context, imageIndex: Int, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // Resolve bundled artwork through generated R references. This avoids a silent
    // getIdentifier() miss when Android resource shrinking or package naming changes.
    val resourceId = when (imageIndex.coerceIn(1, 20)) {
        1 -> R.drawable.wallpaper_hindu_01
        2 -> R.drawable.wallpaper_hindu_02
        3 -> R.drawable.wallpaper_hindu_03
        4 -> R.drawable.wallpaper_hindu_04
        5 -> R.drawable.wallpaper_hindu_05
        6 -> R.drawable.wallpaper_hindu_06
        7 -> R.drawable.wallpaper_hindu_07
        8 -> R.drawable.wallpaper_hindu_08
        9 -> R.drawable.wallpaper_hindu_09
        10 -> R.drawable.wallpaper_hindu_10
        11 -> R.drawable.wallpaper_hindu_11
        12 -> R.drawable.wallpaper_hindu_12
        13 -> R.drawable.wallpaper_hindu_13
        14 -> R.drawable.wallpaper_hindu_14
        15 -> R.drawable.wallpaper_hindu_15
        16 -> R.drawable.wallpaper_hindu_16
        17 -> R.drawable.wallpaper_hindu_17
        18 -> R.drawable.wallpaper_hindu_18
        19 -> R.drawable.wallpaper_hindu_19
        else -> R.drawable.wallpaper_hindu_20
    }
    Column(modifier.clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surface)
        .border(if (selected) 2.dp else 1.dp, if (selected) Purple else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp))
        .clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(148.dp)) {
            Image(painterResource(resourceId), contentDescription = "Hindu wallpaper $imageIndex", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (selected) Box(Modifier.align(Alignment.TopEnd).padding(7.dp).size(23.dp).clip(CircleShape).background(Purple), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            if (selected) "Selected · Artwork ${imageIndex.toString().padStart(2, '0')}" else "Artwork ${imageIndex.toString().padStart(2, '0')}",
            color = if (selected) Purple else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center
        )
    }
}

@Composable private fun ArtworkTile(category: String, tradition: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surface).border(if (selected) 2.dp else 1.dp, if (selected) Purple else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp)).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(105.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height; val cx = w * .5f; val cy = h * .47f
                drawRect(Brush.linearGradient(listOf(artworkTint(tradition, category), Color(0xFF2E1B4D)), Offset.Zero, Offset(w, h)))
                drawCircle(Color(0xFFFFE6A8).copy(alpha = .82f), w * .13f, Offset(w * .76f, h * .24f))
                val ridge = Path().apply { moveTo(0f, h * .82f); lineTo(w * .24f, h * .42f); lineTo(w * .42f, h * .78f); lineTo(w * .64f, h * .35f); lineTo(w, h * .82f); lineTo(w, h); lineTo(0f, h); close() }
                drawPath(ridge, Color(0xFF251638).copy(alpha = .30f))
                val art = Color.White.copy(alpha = .88f)
                when (tradition.lowercase()) {
                    "hinduism" -> {
                        drawOval(art, Offset(cx - w * .09f, cy - h * .31f), Size(w * .18f, h * .47f), style = Stroke(2.dp.toPx()))
                        drawCircle(Color(0xFFFFD76C), w * .045f, Offset(cx, cy - h * .05f))
                        drawLine(art, Offset(cx - w * .22f, cy + h * .26f), Offset(cx + w * .22f, cy + h * .26f), 2.dp.toPx())
                        for (i in 0..4) drawOval(art.copy(alpha = .55f), Offset(cx - w * .13f + i * w * .05f, cy + h * .12f), Size(w * .06f, h * .20f), style = Stroke(1.dp.toPx()))
                    }
                    "christianity" -> {
                        drawLine(art, Offset(cx, cy - h * .35f), Offset(cx, cy + h * .38f), 4.dp.toPx())
                        drawLine(art, Offset(cx - w * .17f, cy - h * .12f), Offset(cx + w * .17f, cy - h * .12f), 4.dp.toPx())
                    }
                    "islam" -> {
                        drawArc(art, 180f, 180f, false, Offset(cx - w * .22f, cy - h * .18f), Size(w * .44f, h * .55f), style = Stroke(3.dp.toPx()))
                        drawLine(art, Offset(cx - w * .22f, cy + h * .10f), Offset(cx + w * .22f, cy + h * .10f), 3.dp.toPx())
                        for (i in -2..2) drawCircle(art.copy(alpha = .5f), w * .012f, Offset(cx + i * w * .07f, cy + h * .25f))
                    }
                    "buddhism" -> {
                        drawCircle(art, w * .07f, Offset(cx, cy - h * .12f))
                        drawOval(art, Offset(cx - w * .15f, cy - h * .02f), Size(w * .30f, h * .38f))
                        for (i in 0..5) drawOval(Color(0xFFFFD78B), Offset(cx - w * .12f + i * w * .045f, cy + h * .22f), Size(w * .07f, h * .15f), style = Stroke(1.dp.toPx()))
                    }
                    else -> {
                        for (i in 0..3) drawArc(art.copy(alpha = .75f), 0f, 180f, false, Offset(cx - w * .28f, cy + h * (.05f + i * .08f)), Size(w * .56f, h * .24f), style = Stroke(2.dp.toPx()))
                        for (i in 0..4) drawCircle(Color(0xFFFFE6A8), w * .012f, Offset(w * (.16f + i * .16f), h * .22f))
                    }
                }
            }
            if (selected) Box(Modifier.align(Alignment.TopEnd).padding(7.dp).size(22.dp).clip(CircleShape).background(Purple), contentAlignment = Alignment.Center) { Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        Text(category, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(9.dp), textAlign = TextAlign.Center)
    }
}

@Composable private fun ThemeSwatch(key: String, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) Purple.copy(alpha = .14f) else MaterialTheme.colorScheme.surface).border(if (selected) 1.6.dp else 1.dp, if (selected) Purple else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(35.dp).clip(RoundedCornerShape(9.dp)).background(themeBrush(key)), contentAlignment = Alignment.Center) { if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable private fun ToggleCard(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 11.sp, lineHeight = 15.sp) }
        Switch(checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Purple))
    }
}

@Composable private fun SettingCard(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp)).padding(15.dp), content = content) }
@Composable private fun SettingRow(icon: ImageVector, title: String, subtitle: String) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Gold); Spacer(Modifier.width(12.dp)); Column { Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f), fontSize = 11.sp) } } }
@Composable private fun InfoCard(text: String) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(SoftGold.copy(alpha = .55f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Info, null, tint = Gold); Spacer(Modifier.width(10.dp)); Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 17.sp) } }
@Composable private fun NoticeCard(text: String) { Text(text, color = Purple, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0E6F7)).padding(12.dp)) }
@Composable private fun PrimaryAction(text: String, onClick: () -> Unit) { Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)) { Text(text, color = Color.White, fontWeight = FontWeight.Bold) } }
@Composable private fun SecondaryAction(text: String, onClick: () -> Unit) { OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Gold)) { Text(text, color = Purple, fontWeight = FontWeight.SemiBold) } }

private fun routeTitle(route: String) = when (route) { "tradition" -> "Tradition & Language"; "frequency" -> "Verse Frequency"; "wallpaper" -> "Lock Screen & Wallpaper"; "gallery" -> "Wallpaper Gallery"; "reading" -> "Reading Preferences"; "theme" -> "App Theme"; "notifications" -> "Notifications"; "battery" -> "Battery & Performance"; "help" -> "Help & Support"; else -> "Settings" }
private fun frequencyLabel(value: String) = when (value) { "Every hour" -> "Every 1 hour"; "Every 6 hours" -> "Every 6 hours"; "Every 12 hours" -> "Every 12 hours"; "Every 24 hours", "Daily" -> "Every 24 hours"; else -> value }
private fun frequencyDescription(value: String) = when (value) { "Every hour" -> "A new verse every hour"; "Every 6 hours" -> "Four inspiration moments each day"; "Every 12 hours" -> "Morning and evening inspiration"; else -> "One verse for each 24-hour cycle" }
private fun wallpaperCategories(tradition: String): List<String> = when (tradition.lowercase()) {
    "hinduism" -> listOf("Krishna", "Shiva", "Ganesha", "Radha Krishna", "Bhagavad Gita")
    "christianity" -> listOf("Jesus", "Cross", "Bible", "Church", "Biblical Scenes")
    "islam" -> listOf("Islamic Geometric Patterns", "Mosques", "Quran Calligraphy", "Arabic Artwork")
    "buddhism" -> listOf("Buddha", "Dharma", "Meditation", "Lotus")
    else -> listOf("Nature", "Mountains", "Sunrise", "Ocean", "Flowers", "Universal Inspiration")
}
private fun traditionEmoji(code: String) = when (code.lowercase()) { "hinduism" -> "ॐ"; "christianity" -> "✝"; "islam" -> "☾"; "buddhism" -> "☸"; else -> "✧" }
private fun themeBrush(theme: String): Brush = when (theme) {
    "GOLDEN" -> Brush.linearGradient(listOf(Color(0xFFFFD58A), Color(0xFFB96D28), Color(0xFF4C2A39)))
    "ROYAL_PURPLE" -> Brush.linearGradient(listOf(Color(0xFF9B6AC0), Color(0xFF492166), Color(0xFF1B102D)))
    "FOREST_GREEN" -> Brush.linearGradient(listOf(Color(0xFF8ABF91), Color(0xFF245C47), Color(0xFF102D2A)))
    "OCEAN_BLUE" -> Brush.linearGradient(listOf(Color(0xFF8AD4F2), Color(0xFF176D9F), Color(0xFF102B55)))
    "SOFT_SAND" -> Brush.linearGradient(listOf(Color(0xFFFFE8C5), Color(0xFFD9B98D), Color(0xFF87694D)))
    "MIDNIGHT" -> Brush.linearGradient(listOf(Color(0xFF55546E), Color(0xFF211C36), Color(0xFF090812)))
    else -> Brush.linearGradient(listOf(Color(0xFFB9A3F4), Color(0xFF5D3188), Color(0xFF21112F)))
}
private fun artworkTint(tradition: String, category: String): Color = when (tradition.lowercase()) {
    "hinduism" -> Color(0xFFD98B36)
    "christianity" -> Color(0xFF8C9FD0)
    "islam" -> Color(0xFF438A78)
    "buddhism" -> Color(0xFFD49B4C)
    else -> Color(0xFF6C9C9B)
}

fun defaultWallpaperCategoryForReligion(religionId: Int): String = when (religionId) {
    1 -> "Krishna"
    2 -> "Jesus"
    3 -> "Islamic Geometric Patterns"
    4 -> "Buddha"
    else -> "Nature"
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
