package com.scripturedaily

import android.Manifest
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.scripturedaily.ads.AdManager
import com.scripturedaily.ads.AdSlot
import com.scripturedaily.ads.savedVerseAdPositions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.ReadOnlyComposable
import com.scripturedaily.ui.W
import com.scripturedaily.ui.WisdomOneTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import android.net.Uri
import com.scripturedaily.config.AppConfigManager
import com.scripturedaily.gate.AppGateHost
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.AuthInput
import com.scripturedaily.data.CachedCatalogItem
import com.scripturedaily.data.CachedVerse
import com.scripturedaily.data.CatalogItem
import com.scripturedaily.data.DeviceRegInput
import com.scripturedaily.data.FavoriteInput
import com.scripturedaily.data.FeedbackInput
import com.scripturedaily.data.MoodCheckinInput
import com.scripturedaily.features.Overlay
import com.scripturedaily.features.challenges.ChallengesScreen
import com.scripturedaily.features.discover.DiscoverItem
import com.scripturedaily.features.discover.DiscoverScreen
import com.scripturedaily.features.discover.StoryLibraryScreen
import com.scripturedaily.features.explain.ExplainSheet
import com.scripturedaily.features.mood.DefaultMoods
import com.scripturedaily.features.mood.MoodCard
import com.scripturedaily.features.journal.JournalScreen
import com.scripturedaily.features.journal.TodayReflectionSheet
import com.scripturedaily.features.home.ThemedVerseCard
import com.scripturedaily.features.home.VerseThemes
import com.scripturedaily.features.studio.StudioScreen
import com.scripturedaily.features.studio.StudioTab
import com.scripturedaily.features.tree.MilestoneDialog
import com.scripturedaily.features.tree.TreeJourneyScreen
import com.scripturedaily.features.tree.TreeLogic
import com.scripturedaily.features.tree.TreeStage
import com.scripturedaily.features.tree.TreeState
import com.scripturedaily.features.tree.TreeStore
import com.scripturedaily.features.tree.WisdomTreeCard
import com.scripturedaily.features.story.StoryScreen
import com.scripturedaily.features.story.StorySource
import com.scripturedaily.features.truths.UniversalTruthsScreen
import com.scripturedaily.widget.WidgetStudioScreen
import com.scripturedaily.data.PreferenceInput
import com.scripturedaily.data.PaletteRefreshWorker
import com.scripturedaily.data.ReligionItem
import com.scripturedaily.data.ScriptureDatabase
import com.scripturedaily.data.ScriptureItem
import com.scripturedaily.data.Verse
import com.scripturedaily.data.VerseSyncWorker
import com.scripturedaily.data.toCached
import com.scripturedaily.lockscreen.LockScreenVerse
import com.scripturedaily.onboarding.FirstRunSetupScreen
import com.scripturedaily.onboarding.SplashScreen
import com.google.firebase.messaging.FirebaseMessaging
import com.scripturedaily.analytics.MetaAnalytics
import com.scripturedaily.notification.WisdomOneMessagingService
import com.scripturedaily.notification.VerseNotifier
import com.scripturedaily.widget.ScriptureWidget
import com.scripturedaily.widget.ScriptureWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Theme-aware aliases of the shared palette (see ui/WTheme.kt) so the whole app follows Light / Dark.
private val Purple: Color @Composable @ReadOnlyComposable get() = W.Purple
private val PurpleDark: Color @Composable @ReadOnlyComposable get() = W.PurpleDark
private val PurpleMid: Color @Composable @ReadOnlyComposable get() = W.PurpleMid
private val Gold: Color @Composable @ReadOnlyComposable get() = W.Gold
private val GoldLight: Color @Composable @ReadOnlyComposable get() = W.GoldLight
private val Parchment: Color @Composable @ReadOnlyComposable get() = W.Parchment
private val Ink: Color @Composable @ReadOnlyComposable get() = W.Ink
private val InkMid: Color @Composable @ReadOnlyComposable get() = W.InkMid
private val InkSoft: Color @Composable @ReadOnlyComposable get() = W.InkSoft
private val Amber: Color @Composable @ReadOnlyComposable get() = W.Gold
private val AmberLight: Color @Composable @ReadOnlyComposable get() = W.GoldLight
private val Forest: Color @Composable @ReadOnlyComposable get() = W.PurpleDark

// ── Custom Toast ─────────────────────────────────────────────────────────────
enum class ToastType { SUCCESS, ERROR, INFO }

data class ToastData(val message: String, val type: ToastType = ToastType.INFO)

@Composable
fun AppToastHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        val isError = data.visuals.message.lowercase().let {
            it.contains("couldn't") || it.contains("error") || it.contains("failed") || it.contains("turned off") || it.contains("cleared")
        }
        val isSuccess = data.visuals.message.lowercase().let {
            it.contains("saved") || it.contains("thank") || it.contains("removed") || it.contains("success") || it.contains("enabled") || it.contains("wallpaper")
        }
        val bgColor = PurpleDark
        val icon = when {
            isError   -> "✕"
            isSuccess -> "✓"
            else      -> "✦"
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .border(BorderStroke(1.dp, Gold.copy(alpha = 0.38f)), RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Gold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 13.sp, color = GoldLight, fontWeight = FontWeight.Bold)
                }
                Text(
                    data.visuals.message,
                    fontSize = 14.sp,
                    color = Parchment,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
private sealed class HealthState {
    object Checking : HealthState()
    data class Retrying(val attempt: Int) : HealthState()
    object Ready : HealthState()
    object Failed : HealthState()
}

class MainActivity : ComponentActivity() {

    private val deepLinkVerseId = mutableStateOf<Int?>(null)

    private val shortcutTab = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WisdomOneMessagingService.createChannel(this)
        ApiClient.init(this) {
            getSharedPreferences("reader", 0).getString("bearer_token", null)
        }
        // Fetch (or retrieve cached) FCM token so it is ready for backend registration
        @Suppress("DEPRECATION")
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener { token ->
            android.util.Log.d("FCM", "Device FCM token: $token")
            val prefs = getSharedPreferences("reader", 0)
            prefs.edit().putString("fcm_token", token).apply()
            FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            kotlinx.coroutines.MainScope().launch {
                runCatching {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        ApiClient.api.updateFcmToken(com.scripturedaily.data.FcmTokenInput(token))
                    }
                }
            }
        }
        deepLinkVerseId.value = intent?.getIntExtra("verse_id", -1)?.takeIf { it > 0 }
        shortcutTab.value = intent?.getStringExtra("shortcut_tab")
        setContent {
            val themePrefs = remember { getSharedPreferences("reader", MODE_PRIVATE) }
            var selectedTheme by remember { mutableStateOf(themePrefs.getString("app_theme", "Light") ?: "Light") }
            DisposableEffect(themePrefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "app_theme") selectedTheme = themePrefs.getString("app_theme", "Light") ?: "Light"
                }
                themePrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { themePrefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val dark = selectedTheme == "Dark" || (selectedTheme == "System" && isSystemInDarkTheme())
            // Keep status/navigation-bar icons readable when the in-app theme differs from the system theme.
            SideEffect {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                )
            }
            WisdomOneTheme(dark) {
                // Mandatory update > maintenance > optional update > app. While blocked, pending deep links and
                // notification targets are dropped so they cannot open app screens behind the gate.
                AppGateHost(onBlocked = { deepLinkVerseId.value = null; shortcutTab.value = null }) {
                    ScriptureApp(deepLinkVerseId, shortcutTab)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkVerseId.value = intent.getIntExtra("verse_id", -1).takeIf { it > 0 }
        shortcutTab.value = intent.getStringExtra("shortcut_tab")
    }
}

// ── Device identity helpers ───────────────────────────────────────────────────
// ANDROID_ID: 16-char hex, unique per (device × user-account × app-signing-key).
// It is the standard Android unique identifier — no hashing, no padding.
// Emulator ANDROID_ID is also unique per AVD, so it works fine for testing.
@SuppressLint("HardwareIds")
private fun deviceId(context: android.content.Context): String {
    val androidId = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ANDROID_ID
    )
    // "9774d56d682e549c" is the known broken emulator value on very old Android
    if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
        return androidId  // e.g. "d4decfd27535d79d" — real 16-char hex
    }
    // Fallback: generate once and persist (covers rooted/broken devices)
    val store = context.getSharedPreferences("reader", 0)
    return store.getString("device_id_fallback", null)
        ?: java.util.UUID.randomUUID().toString().replace("-", "").take(16)
            .also { store.edit().putString("device_id_fallback", it).apply() }
}

// Derive stable email + password from the 16-char ANDROID_ID.
// email: androidId@wisdomone.app  — always valid, always unique per device
// password: androidId repeated to reach 16 chars minimum (already 16, fine)
private fun deviceEmail(id: String) = "${id}@wisdomone.app"
private fun devicePassword(id: String) = id  // 16 chars, meets backend min=8

// ── Auto-register or login with device identity, return Bearer token ──────────
// Called on every app start after health check. Caches JWT in SharedPrefs.
// On 401 from any API call, clear cached token and call this again to re-login.
private suspend fun ensureDeviceToken(context: android.content.Context, forceRefresh: Boolean = false): String? {
    val store = context.getSharedPreferences("reader", 0)
    if (!forceRefresh) {
        val cached = store.getString("bearer_token", null)
        if (cached != null) return cached
    }

    val id = deviceId(context)
    val email = deviceEmail(id)
    val password = devicePassword(id)
    val body = AuthInput(email, password)

    return try {
        // Always try LOGIN first — handles reinstall (device already in DB)
        val loginResp = withContext(Dispatchers.IO) { ApiClient.api.login(body) }
        val token = if (loginResp.isSuccessful) {
            loginResp.body()!!.accessToken
        } else {
            // 401 = not registered yet — register this device as a new user
            val regResp = withContext(Dispatchers.IO) { ApiClient.api.register(body) }
            if (regResp.isSuccessful) regResp.body()!!.accessToken else return null
        }
        store.edit().putString("bearer_token", token).apply()
        // Register device hardware details (idempotent on server)
        try {
                val model = "${Build.MANUFACTURER} ${Build.MODEL}".take(120)
                withContext(Dispatchers.IO) {
                    ApiClient.api.registerDevice(DeviceRegInput(id, "android", model))
                }
            } catch (_: Exception) {}
        token
    } catch (_: Exception) { null }
}

private suspend fun refreshToken(context: android.content.Context): String? {
    context.getSharedPreferences("reader", 0).edit().remove("bearer_token").apply()
    return ensureDeviceToken(context, forceRefresh = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptureApp(
    deepLinkVerseIdState: androidx.compose.runtime.MutableState<Int?>,
    shortcutTabState: androidx.compose.runtime.MutableState<String?>
) {
    val context = LocalContext.current
    val activity = context as Activity
    val deepLinkVerseId by deepLinkVerseIdState
    val shortcutTab by shortcutTabState
    val store = remember { context.getSharedPreferences("reader", 0) }
    val dao = remember { ScriptureDatabase.get(context).verses() }
    val catalogDao = remember { ScriptureDatabase.get(context).catalog() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Update prompts and maintenance mode are handled before this screen is ever composed (gate/AppGateScreens.kt).

    // ── Health check ──────────────────────────────────────────────────────────
    var apiOffline by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repeat(3) { attempt ->
            try {
                val resp = ApiClient.api.health()
                if (resp.isSuccessful) { apiOffline = false; return@LaunchedEffect }
            } catch (_: Exception) {}
            if (attempt < 2) kotlinx.coroutines.delay(2000L)
        }
        apiOffline = true
    }

    // ── State (declared before LaunchedEffect so they are accessible inside it) ────
    var religion by remember { mutableIntStateOf(store.getInt("religion", 0)) }
    var language by remember { mutableIntStateOf(store.getInt("language", 0)) }
    var scriptureId by remember { mutableIntStateOf(store.getInt("scripture", 0)) }
    var frequency by remember { mutableStateOf(store.getString("frequency", "Every 24 hours")?.let { if (it == "Daily") "Every 24 hours" else it } ?: "Every 24 hours") }
    var reminders by remember { mutableStateOf(store.getBoolean("reminders", false)) }
    var lockScreen by remember { mutableStateOf(store.getBoolean("lock_screen", false)) }
    var configured by remember { mutableStateOf(store.getBoolean("configured", false)) }
    var setupGuidePending by remember { mutableStateOf(store.getBoolean("setup_guide_pending", false)) }
    var splashDone by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("Home") }
    var showFeedback by remember { mutableStateOf(false) }
    var verse by remember { mutableStateOf<Verse?>(null) }
    var readingPrefsVersion by remember { mutableIntStateOf(0) }
    val showReadingScripture = remember(readingPrefsVersion) { store.getBoolean("reading_scripture", true) }
    val showReadingReference = remember(readingPrefsVersion) { store.getBoolean("reading_reference", true) }
    val showReadingTranslation = remember(readingPrefsVersion) { store.getBoolean("reading_translation", true) }
    val showReadingBranding = remember(readingPrefsVersion) { store.getBoolean("reading_branding", true) }
    DisposableEffect(store, dao) {
        val activeVerseListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in setOf("reading_scripture", "reading_reference", "reading_translation", "reading_branding")) {
                readingPrefsVersion++
            }
            if (key == "last_verse_id") {
                val activeId = store.getInt("last_verse_id", -1)
                if (activeId > 0) scope.launch {
                    val cached = withContext(Dispatchers.IO) { dao.find(activeId) }
                    if (cached != null) verse = cached.toVerse()
                }
            }
        }
        store.registerOnSharedPreferenceChangeListener(activeVerseListener)
        onDispose { store.unregisterOnSharedPreferenceChangeListener(activeVerseListener) }
    }
    val savedVerses by remember { dao.observeFavorites() }.collectAsState(initial = emptyList())
    val favoriteIds = remember(savedVerses) { savedVerses.map { it.id }.toSet() }
    var query by remember { mutableStateOf("") }
    var searchLanguageFilter by remember { mutableStateOf<Int?>(null) }
    var searchScriptureFilter by remember { mutableStateOf<Int?>(null) }
    var results by remember { mutableStateOf(listOf<Verse>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var religions by remember { mutableStateOf(listOf<ReligionItem>()) }
    var languages by remember { mutableStateOf(listOf<CatalogItem>()) }
    var scriptures by remember { mutableStateOf(listOf<CatalogItem>()) }
    var catalogLoading by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var historyVerses by remember { mutableStateOf(listOf<Verse>()) }
    var showHistory by remember { mutableStateOf(false) }
    var showChapterBrowser by remember { mutableStateOf(false) }
    var fontSize by remember { mutableIntStateOf(store.getInt("font_size", 22)) }
    // Local calendar day. Re-read on resume and at midnight so the mood card, verse theme and daily verse roll over correctly.
    var todayDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    var treeDays by remember { mutableStateOf(TreeStore.days(context)) }
    var treeEnabled by remember { mutableStateOf(TreeStore.enabled(context)) }
    var treeRemote by remember { mutableStateOf<com.scripturedaily.data.TreeDto?>(null) }
    val treeState: TreeState = remember(treeDays, todayDate) { TreeLogic.state(treeDays, todayDate) }
    var showJourney by remember { mutableStateOf(false) }
    var milestoneStage by remember { mutableStateOf<TreeStage?>(null) }
    val streak = treeState.currentStreak
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var moods by remember { mutableStateOf(DefaultMoods) }
    val todayKey = todayDate.toString()
    var mood by remember { mutableStateOf(if (store.getString("mood_day", null) == java.time.LocalDate.now().toString()) store.getString("mood", null) else null) }
    // "How are you feeling today?" is hidden for the rest of the local day once a feeling has been submitted (persisted).
    var moodCardHidden by remember { mutableStateOf(store.getString("mood_done_date", null) == java.time.LocalDate.now().toString()) }
    var moodMessage by remember { mutableStateOf(if (mood != null) store.getString("mood_message", null) else null) }
    var moodLoading by remember { mutableStateOf(false) }
    var moodError by remember { mutableStateOf<String?>(null) }
    var moodShuffle by remember { mutableIntStateOf(0) }
    var prefsSyncKey by remember { mutableIntStateOf(0) }
    var storyAvailable by remember { mutableStateOf(false) }
    var explainFor by remember { mutableStateOf<Verse?>(null) }
    var reflectFor by remember { mutableStateOf<Verse?>(null) }

    // ── Single sequential startup flow ─────────────────────────────────────────
    LaunchedEffect(Unit) {
        PaletteRefreshWorker.cancel(context)
        // Wallpaper behaviour: legacy values (change_verse / keep_same_verse / old labels) map to the three current modes.
        val storedMode = store.getString("wallpaper_mode", null)
        val normalizedMode = com.scripturedaily.lockscreen.WallpaperMode.normalize(storedMode)
        if (storedMode != normalizedMode) store.edit().putString("wallpaper_mode", normalizedMode).apply()
        val normalizedWallpaperTheme = normalizeWallpaperTheme(store.getString("wallpaper_theme", "MULTICOLOR"))
        if (store.getString("wallpaper_theme", null) != normalizedWallpaperTheme) store.edit().putString("wallpaper_theme", normalizedWallpaperTheme).apply()
        // 1. Clear stale tokens from old broken identity schemes
        if (store.getInt("auth_version", 0) < 4) {
            store.edit().remove("bearer_token").remove("device_id")
                .remove("device_id_fallback").putInt("auth_version", 4).apply()
        }
        // 2. Auth
        ensureDeviceToken(context)

        // 3. Catalog — Room cache with 24 h TTL
        catalogLoading = true
        val ttl = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        val cachedReligions = withContext(Dispatchers.IO) { catalogDao.byType("religion") }
        val religionsFresh = cachedReligions.isNotEmpty() && (now - (cachedReligions.minOfOrNull { it.fetchedAt } ?: 0L)) < ttl
        if (religionsFresh) {
            religions = cachedReligions.map { row ->
                val scriptures = row.scripturesJson?.let { json ->
                    try {
                        com.google.gson.Gson().fromJson(json, Array<ScriptureItem>::class.java).toList()
                    } catch (_: Exception) { emptyList() }
                } ?: emptyList()
                ReligionItem(row.itemId, row.name, row.code, scriptures)
            }
        } else {
            try {
                val r = ApiClient.api.religions()
                religions = r
                val gson = com.google.gson.Gson()
                val fetchTime = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    catalogDao.deleteByType("religion")
                    catalogDao.saveAll(r.map { item ->
                        CachedCatalogItem("religion_${item.id}", "religion", item.id, item.name, item.code, null,
                            gson.toJson(item.scriptures), fetchTime)
                    })
                }
            } catch (_: Exception) {
                if (cachedReligions.isNotEmpty()) religions = cachedReligions.map { row ->
                    val scriptures = row.scripturesJson?.let { json ->
                        try { com.google.gson.Gson().fromJson(json, Array<ScriptureItem>::class.java).toList() } catch (_: Exception) { emptyList() }
                    } ?: emptyList()
                    ReligionItem(row.itemId, row.name, row.code, scriptures)
                }
            }
        }
        if (religion == 0 && religions.isNotEmpty()) religion = religions.first().id
        // Load languages scoped to the current religion
        if (religion != 0) {
            try {
                languages = ApiClient.api.languages(religion)
            } catch (_: Exception) {}
        }
        if (language == 0 && languages.isNotEmpty()) language = languages.first().id
        catalogLoading = false

        // Also load scriptures for the current religion (for search filter + chapter browser)
        try {
            scriptures = ApiClient.api.scriptures(religion)
        } catch (_: Exception) {}

        // 4. Pull server preferences (source of truth) — overwrite local only if configured
        if (configured) {
            try {
                val resp = ApiClient.api.getPreferences()
                if (resp.isSuccessful) {
                    val p = resp.body()!!
                    val freqLocal = when (p.frequency) { "1h" -> "Every hour"; "6h" -> "Every 6 hours"; "12h" -> "Every 12 hours"; "24h", "daily" -> "Every 24 hours"; else -> "Every 24 hours" }
                    val firstScriptureId = p.scriptureIds.split(",").firstOrNull()?.trim()?.toIntOrNull() ?: scriptureId
                    if (p.religionId != religion || p.languageId != language) prefsSyncKey++   // changed elsewhere: drop stale verses below
                    if (p.religionId != religion) store.edit().putInt("wallpaper_image_index", 1).apply()   // each tradition has its own gallery: start at its first image
                    religion = p.religionId; language = p.languageId; scriptureId = firstScriptureId; frequency = freqLocal
                    store.edit().putInt("religion", p.religionId).putInt("language", p.languageId)
                        .putInt("scripture", firstScriptureId).putString("frequency", freqLocal)
                        .putString("wallpaper_mode", com.scripturedaily.lockscreen.WallpaperMode.normalize(p.wallpaperMode)).apply()
                }
            } catch (_: Exception) {}
        }

        // 5. Sync server favorites into Room
        if (configured) {
            try {
                val serverFavs = ApiClient.api.getFavorites()
                val serverIds = serverFavs.map { it.id }.toSet()
                withContext(Dispatchers.IO) {
                    dao.clearAllFavorites()
                    serverFavs.forEach { v ->
                        val existing = dao.find(v.id)
                        if (existing != null) dao.setFavorite(v.id, true)
                        else dao.save(v.toCached(scriptureId, language, favorite = true))
                    }
                    // Clear local favorites not on server
                    dao.favorites().filter { it.id !in serverIds }.forEach { dao.setFavorite(it.id, false) }
                }
            } catch (_: Exception) {}
        }
        if (configured) VerseSyncWorker.schedule(context, frequency)
    }

    // Sync server favorites into Room whenever the Saved tab is opened
    LaunchedEffect(page) {
        if (page == "Favorites" && configured) {
            try {
                val serverFavs = withContext(Dispatchers.IO) { ApiClient.api.getFavorites() }
                val serverIds = serverFavs.map { it.id }.toSet()
                withContext(Dispatchers.IO) {
                    // Mark server favorites as saved in Room
                    serverFavs.forEach { v ->
                        val existing = dao.find(v.id)
                        if (existing != null) dao.setFavorite(v.id, true)
                        else dao.save(v.toCached(scriptureId, language, favorite = true))
                    }
                    // Remove local favorites that are no longer on the server
                    dao.favorites().filter { it.id !in serverIds }
                        .forEach { dao.setFavorite(it.id, false) }
                }
            } catch (_: Exception) { /* offline — show cached */ }
            // Prompt review when user has built a meaningful collection
            if (savedVerses.size >= 10) {
                RatingManager.tryLaunchReview(activity)
            }
        }
    }

    // Deep-link: widget/notification tap with a specific verse_id
    LaunchedEffect(deepLinkVerseId) {
        val verseId = deepLinkVerseId
        if (verseId != null && configured) {
            val cached = withContext(Dispatchers.IO) { dao.find(verseId) }
            if (cached != null) {
                verse = cached.toVerse()
                page = "Home"
            } else {
                try {
                    val remote = withContext(Dispatchers.IO) { ApiClient.api.random(religion, language, scriptureId) }
                    verse = remote; page = "Home"
                } catch (_: Exception) {}
            }
        }
    }

    // App shortcut: navigate to the tapped tab
    LaunchedEffect(shortcutTab) {
        val tab = shortcutTab ?: return@LaunchedEffect
        if (!configured) return@LaunchedEffect
        when (tab) {
            "Home"      -> { page = "Home" }
            "Favorites" -> { page = "Favorites" }
            "Explore"   -> { page = "Explore" }
            "Share"     -> {
                page = "Home"
                verse?.let { v -> overlay = Overlay.Studio(v) }
            }
        }
        shortcutTabState.value = null
    }

    suspend fun applyCurrentLockVerse(forceWallpaper: Boolean = false): String? {
        val current = verse ?: withContext(Dispatchers.IO) {
            dao.matching(scriptureId, language)?.toVerse()
        } ?: return "No verse is available yet. Open a verse before enabling lock-screen notifications."
        if (verse == null) verse = current
        return withContext(Dispatchers.IO) {
            LockScreenVerse.applyIfEnabled(context, current.id, current.scripture, current.text, current.reference(), forceWallpaper)
        }
    }

    var permissionTarget by remember { mutableStateOf<String?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when (permissionTarget) {
            "reminders" -> {
                reminders = granted
                store.edit().putBoolean("reminders", granted).apply()
                if (granted) {
                    VerseSyncWorker.schedule(context, frequency)
                    scope.launch {
                        applyCurrentLockVerse()?.let { snackbarHostState.showSnackbar(it) }
                    }
                } else if (!lockScreen) VerseNotifier.cancel(context)
            }
            "setup" -> if (granted) {
                // Same behaviour as turning "Verse reminders" on in Settings.
                reminders = true
                store.edit().putBoolean("reminders", true).putString("setup_notifications", "enabled").apply()
                VerseSyncWorker.schedule(context, frequency)
                MetaAnalytics.track(context, MetaAnalytics.Event.DAILY_REMINDER_ENABLED, MetaAnalytics.params("source" to "setup"))
                scope.launch {
                    applyCurrentLockVerse()?.let { snackbarHostState.showSnackbar(it) }
                }
            } else {
                store.edit().putString("setup_notifications", "denied").apply()
                scope.launch {
                    snackbarHostState.showSnackbar("Notifications are off. You can turn them on anytime in Settings.")
                }
            }
            "lock" -> {
                if (granted) {
                    // User allowed notifications: enable all notification toggles and schedule.
                    store.edit()
                        .putBoolean("reminders", true)
                        .putBoolean("notify_verse_updates", true)
                        .putBoolean("notify_wallpaper_updates", true)
                        .apply()
                    reminders = true
                    VerseSyncWorker.schedule(context, frequency)
                    scope.launch {
                        applyCurrentLockVerse()?.let { snackbarHostState.showSnackbar(it) }
                    }
                } else {
                    // Denied: wallpaper is already active, just inform the user softly.
                    scope.launch {
                        snackbarHostState.showSnackbar("Verse wallpaper is active. Notifications were not enabled — you can turn them on in Settings.")
                    }
                }
            }
        }
        permissionTarget = null
    }

    val scripture = scriptureId
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) todayDate = java.time.LocalDate.now()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    suspend fun toggleFavorite(v: Verse) {
        val existing = dao.find(v.id)
        val nowFavorite = existing?.favorite != true
        if (existing != null) dao.setFavorite(v.id, nowFavorite)
        else dao.save(v.toCached(scripture, language, favorite = true, savedAt = 0L))
        try {
            if (nowFavorite) ApiClient.api.addFavorite(FavoriteInput(v.id))
            else ApiClient.api.removeFavorite(v.id)
        } catch (_: Exception) {}
        snackbarHostState.showSnackbar(if (nowFavorite) "Saved verse" else "Removed from saved verses")
        if (nowFavorite) MetaAnalytics.track(context, MetaAnalytics.Event.VERSE_SAVED)
        // Trigger in-app review at 3rd, 10th, 25th save
        if (nowFavorite) {
            val newCount = savedVerses.size + 1
            if (newCount == 3 || newCount == 10 || newCount == 25) {
                RatingManager.tryLaunchReview(activity)
            }
        }
    }

    suspend fun applyMood(code: String, shuffle: Int = 0) {
        moodLoading = true; moodError = null
        try {
            val result = withContext(Dispatchers.IO) { ApiClient.api.moodVerse(code, religion.takeIf { it > 0 }, language.takeIf { it > 0 }, shuffle) }
            val remote = result.verse
            val sid = scriptures.firstOrNull { it.name == remote.scripture }?.id ?: scriptureId
            val cached = withContext(Dispatchers.IO) {
                val old = dao.find(remote.id)
                remote.toCached(sid, language, old?.favorite == true).also { dao.save(it) }
            }
            ScriptureWidget.cacheVerseForWidget(context, cached)
            store.edit().putInt("last_verse_id", remote.id).putString("mood", code).putString("mood_day", todayKey)
                .putString("last_verse_day", todayKey).putString("mood_message", result.message).apply()
            verse = remote; mood = code; moodMessage = result.message
            ScriptureWidget().refreshAll(context)
            if (shuffle == 0) {
                store.edit().putString("mood_done_date", java.time.LocalDate.now().toString()).apply()  // survives restarts
                scope.launch { kotlinx.coroutines.delay(1800); moodCardHidden = true }                   // let the confirmation show, then animate away
                runCatching { withContext(Dispatchers.IO) { ApiClient.api.recordMood(MoodCheckinInput(code, remote.id)) } }
                MetaAnalytics.track(context, MetaAnalytics.Event.MOOD_CHECKED_IN)
            }
        } catch (_: Exception) {
            moodError = "Couldn't reach the server. Check your connection and try again."
        } finally { moodLoading = false }
    }

    suspend fun refreshHomeWithoutRotatingVerse() {
        loading = true; error = null
        try {
            // This endpoint returns the persisted selection unchanged; it creates one only for a first-time user.
            val remote = ApiClient.api.currentActiveVerse().data.verse
            val sid = remote.scriptureId ?: scriptureId
            val lang = remote.languageId ?: language
            val old = withContext(Dispatchers.IO) { dao.find(remote.id) }
            val cached = remote.toCached(sid, lang, old?.favorite == true)
            withContext(Dispatchers.IO) { dao.save(cached) }
            store.edit().putInt("last_verse_id", remote.id).putInt("current_verse_id", remote.id).apply()
            verse = remote
            ScriptureWidget.cacheVerseForWidget(context, cached)
            ScriptureWidget().refreshAll(context)
        } catch (_: Exception) {
            // Keep the currently displayed verse intact if the refresh request fails.
            error = "Couldn't refresh right now. Your current verse has been kept."
        } finally { loading = false }
    }

    suspend fun loadToday(random: Boolean = false, forceLanguage: Int? = null, forceScriptureId: Int? = null) {
        loading = true; error = null
        val langToUse = forceLanguage ?: language
        val scriptureToUse = forceScriptureId ?: scriptureId
        try {
            // Always use the persisted active-verse endpoint; only explicit Discover calls may rotate early.
            val remote = if (random) ApiClient.api.discoverVerse().data.verse
                         else ApiClient.api.activeVerse().data.verse
            val expectedReligion = religions.firstOrNull { it.id == religion }?.name
            if (expectedReligion != null && !remote.religion.equals(expectedReligion, ignoreCase = true)) {
                throw IllegalStateException("The server's active verse does not match the selected tradition")
            }
            val oldActiveId = store.getInt("last_verse_id", -1)
            val activeChanged = oldActiveId != remote.id
            val old = withContext(Dispatchers.IO) { dao.find(remote.id) }
            val cached = remote.toCached(remote.scriptureId ?: scriptureToUse, remote.languageId ?: langToUse, old?.favorite == true)
            withContext(Dispatchers.IO) { dao.save(cached) }
            ScriptureWidget.cacheVerseForWidget(context, cached)
            store.edit().putInt("last_verse_id", remote.id).putString("last_verse_day", java.time.LocalDate.now().toString())
                .putInt("current_verse_id", remote.id).putLong("active_verse_selected_at", System.currentTimeMillis()).apply()
            if (random) VerseSyncWorker.schedule(context, frequency, resetTimer = true)
            verse = remote
            ScriptureWidget().refreshAll(context)
            if (activeChanged || (lockScreen && !LockScreenVerse.isWallpaperAppliedForVerse(context, remote.id))) {
                withContext(Dispatchers.IO) {
                    LockScreenVerse.applyIfEnabled(context, remote.id, remote.scripture, remote.text, remote.reference())
                }
            }
        } catch (_: Exception) {
            val local = dao.matching(scriptureToUse, langToUse)
            if (local != null) {
                val oldActiveId = store.getInt("last_verse_id", -1)
                verse = local.toVerse()
                ScriptureWidget.cacheVerseForWidget(context, local)   // offline: widget/lock screen show the cached verse of THIS tradition
                ScriptureWidget().refreshAll(context)
                if (oldActiveId != local.id || (lockScreen && !LockScreenVerse.isWallpaperAppliedForVerse(context, local.id))) {
                    withContext(Dispatchers.IO) { LockScreenVerse.applyIfEnabled(context, local.id, local.scripture, local.text, local.reference()) }
                }
            } else {
                ScriptureWidget.clearCachedVerse(context)              // never leave another tradition's verse on the widget
                ScriptureWidget().refreshAll(context)
                error = "You're offline. Connect once to download your first verse."
            }
        } finally { loading = false }
    }

    /** Called after the tradition or language changed: drop everything derived from the old choice, then reload. */
    suspend fun refreshForPreferences(changed: Boolean) {
        if (changed) {
            store.edit().remove("last_verse_id").remove("current_verse_id").remove("last_verse_day")
                .remove("mood").remove("mood_day").remove("mood_message").apply()
            verse = null; mood = null; moodMessage = null; storyAvailable = false
            ScriptureWidget.clearCachedVerse(context)
            if (store.getBoolean("lock_screen", false)) withContext(Dispatchers.IO) { LockScreenVerse.clearLockScreenWallpaper(context) }
        }
        loadToday(random = false)
    }

    // Tradition/language changed on the server (e.g. saved from another session): reload so nothing stale is shown.
    LaunchedEffect(prefsSyncKey) { if (prefsSyncKey > 0) refreshForPreferences(changed = true) }

    /** Reports a Wisdom Tree activity (share, explain, story, studio, truths, wallpaper, widget, discover) to the backend. Fire-and-forget. */
    fun reportTreeActivity(event: String) {
        if (!treeEnabled || !configured) return
        scope.launch { runCatching { withContext(Dispatchers.IO) { ApiClient.api.treeActivity(com.scripturedaily.data.TreeActivityBody(event), java.time.LocalDate.now().toString()) } }.onSuccess { treeRemote = it } }
    }

    fun refreshTree() { if (treeEnabled) scope.launch { runCatching { withContext(Dispatchers.IO) { ApiClient.api.tree(java.time.LocalDate.now().toString()) } }.onSuccess { treeRemote = it } } }

    /** The daily reading is complete (verse viewed for a few seconds, or saved / shared / explained). One count per day. */
    fun markReading() {
        if (!configured || verse == null || !treeEnabled) return
        if (TreeStore.markRead(context, todayDate)) {
            val before = treeDays.size
            treeDays = TreeStore.days(context)
            val total = treeDays.size
            TreeStage.values().firstOrNull { it.first == total && total > before }?.let { milestoneStage = it }
            if (treeState.currentStreak + 1 == 7 || treeState.currentStreak + 1 == 30) scope.launch { RatingManager.tryLaunchReview(activity) }
            scope.launch { TreeStore.syncState(context)?.let { treeRemote = it; treeDays = TreeStore.days(context); treeEnabled = TreeStore.enabled(context) } }
        }
    }

    suspend fun getToken(): String? =
        store.getString("bearer_token", null)
            ?: withContext(Dispatchers.IO) { refreshToken(context) }

    fun enableLockScreen() {
        lockScreen = true
        store.edit().putBoolean("lock_screen", true).apply()
        scope.launch {
            // 1. Apply wallpaper immediately — independent of notification permission.
            val message = applyCurrentLockVerse(forceWallpaper = true)
            if (message?.startsWith("No verse") == true) {
                lockScreen = false
                store.edit().putBoolean("lock_screen", false).apply()
                snackbarHostState.showSnackbar(message)
                return@launch
            }
            message?.let { snackbarHostState.showSnackbar(it) }
            // 2. Ask for notification permission only if not yet granted and not yet asked.
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                val alreadyAsked = store.getBoolean("notification_permission_asked", false)
                if (!alreadyAsked) {
                    store.edit().putBoolean("notification_permission_asked", true).apply()
                    permissionTarget = "lock"
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                // If already asked and denied, wallpaper is still active — do nothing more.
            } else {
                // Permission already granted: enable all notification toggles.
                store.edit()
                    .putBoolean("reminders", true)
                    .putBoolean("notify_verse_updates", true)
                    .putBoolean("notify_wallpaper_updates", true)
                    .apply()
                reminders = true
                VerseSyncWorker.schedule(context, frequency)
            }
        }
    }

    fun disableLockScreen() {
        lockScreen = false
        store.edit().putBoolean("lock_screen", false).apply()
        scope.launch {
            withContext(Dispatchers.IO) { LockScreenVerse.clearLockScreenWallpaper(context) }
            VerseNotifier.cancelLockScreen(context)
            if (store.getBoolean("reminders", false)) applyCurrentLockVerse()
            else VerseNotifier.cancel(context)
            snackbarHostState.showSnackbar("Lock screen verse turned off")
        }
    }

    var homeLoadKey by remember { mutableIntStateOf(0) }

    // Midnight rollover: a new local day brings the next theme, the mood question and a fresh daily verse.
    LaunchedEffect(todayDate) {
        moodCardHidden = store.getString("mood_done_date", null) == todayDate.toString()
        if (store.getString("mood_day", null) != todayDate.toString()) { mood = null; moodMessage = null }
        val shown = store.getString("last_verse_day", null)
        if (configured && shown != null && shown != todayDate.toString()) homeLoadKey++
        val now = java.time.LocalDateTime.now()
        kotlinx.coroutines.delay(java.time.Duration.between(now, todayDate.plusDays(1).atStartOfDay()).toMillis().coerceAtLeast(1_000L) + 500L)
        todayDate = java.time.LocalDate.now()
    }

    // Meta first_verse_read: the first time a verse is on screen on Home on this install (fires at most once per install).
    LaunchedEffect(verse?.id, page, configured, setupGuidePending, overlay) {
        if (configured && !setupGuidePending && page == "Home" && verse != null && overlay == null) MetaAnalytics.trackFirstVerseRead(context)
    }
    // Reading counts once the daily verse has been on screen for a few seconds.
    LaunchedEffect(verse?.id, page, todayDate, configured, setupGuidePending) {
        if (configured && !setupGuidePending && page == "Home" && verse != null && overlay == null) {
            kotlinx.coroutines.delay(5_000)
            markReading()
        }
    }
    // Merge reading history with the server when the app opens (so progress follows the user between devices).
    LaunchedEffect(configured) { if (configured) TreeStore.syncState(context)?.let { treeRemote = it; treeDays = TreeStore.days(context); treeEnabled = TreeStore.enabled(context) } }

    // ── Unsaved settings guard ────────────────────────────────────────────────
    val savedReligion = store.getInt("religion", 0)
    val savedLanguage = store.getInt("language", 0)
    val savedFrequency = store.getString("frequency", "Every 24 hours")?.let { if (it == "Daily") "Every 24 hours" else it } ?: "Every 24 hours"
    val savedReminders = store.getBoolean("reminders", false)
    val settingsDirty = page == "Settings" && (
        religion != savedReligion || language != savedLanguage ||
        frequency != savedFrequency || reminders != savedReminders
    )
    var pendingNavRoute by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(homeLoadKey) {
        if (configured && page == "Home") {
            val lastId = store.getInt("last_verse_id", -1)
            // Reuse today's verse only if it belongs to the current tradition + language; otherwise fetch a fresh one.
            if (lastId > 0 && store.getString("last_verse_day", null) == todayDate.toString()) {
                val cached = withContext(Dispatchers.IO) { dao.find(lastId) }
                val religionName = religions.firstOrNull { it.id == religion }?.name
                if (cached != null && cached.languageId == language && (religionName == null || cached.religion == religionName)) {
                    verse = cached.toVerse()
                    return@LaunchedEffect
                }
            }
            loadToday()
        }
    }

    // Reload verse when user taps Home tab from another tab (not from Save)
    var prevPage by remember { mutableStateOf("Home") }
    LaunchedEffect(page) {
        if (page == "Home" && prevPage != "Home" && configured && verse == null && store.getInt("last_verse_id", -1) <= 0) loadToday()
        if (prevPage == "Explore" && page != "Explore") {
            query = ""
            results = emptyList()
            searchLanguageFilter = null
            searchScriptureFilter = null
        }
        prevPage = page
    }

    // ── Centralised config: fetched before Home. Bounded by AppConfigManager.STARTUP_TIMEOUT_MS, then the cached / default config applies. ──
    val configDone by AppConfigManager.startupDone.collectAsState()
    LaunchedEffect(Unit) { if (!AppConfigManager.startupDone.value) AppConfigManager.awaitStartup() }
    // First run: the splash is the very first thing drawn (no intermediate loading screen), so its background, logo,
    // copy and footer appear together. Returning users keep the compact loading screen.
    if (!configDone && !(!configured && !splashDone)) { ConfigLoadingScreen(); return }

    if (!configured) {
        ResponsiveContentFrame {
            androidx.compose.animation.Crossfade(targetState = splashDone, animationSpec = androidx.compose.animation.core.tween(220), label = "splash-to-onboarding") { showOnboarding ->
            if (!showOnboarding) {
                SplashScreen(onContinue = { splashDone = true })
            } else if (!configDone) {
                ConfigLoadingScreen()
            } else Onboarding(
            religion = religion,
            language = language,
            religions = religions,
            languages = languages,
            catalogLoading = catalogLoading,
            notice = error,
            onReligion = { religion = it },
            onLanguage = { language = it },
            onContinue = {
                error = null
                scope.launch {
                    val token = getToken()
                    val allScriptures = try { ApiClient.api.scriptures(religion) } catch (_: Exception) { emptyList() }
                    val selected = allScriptures.firstOrNull()
                    if (selected != null) {
                        scriptureId = selected.id
                        val scriptureIdsStr = allScriptures.joinToString(",") { it.id.toString() }
                        store.edit()
                            .putInt("religion", religion)
                            .putInt("language", language)
                            .putInt("scripture", selected.id)
                            .putBoolean("configured", true)
                            .putBoolean("setup_guide_pending", true)
                            .apply()
                        if (token != null) {
                            try {
                                ApiClient.api.savePreferences(
                                    PreferenceInput(religion, language, scriptureIdsStr, "24h", false)
                                )
                            } catch (_: Exception) {}
                        }
                        VerseSyncWorker.schedule(context, frequency)
                                        configured = true
                        setupGuidePending = true
                        homeLoadKey++
                        MetaAnalytics.track(context, MetaAnalytics.Event.ONBOARDING_COMPLETED, MetaAnalytics.params("language_id" to language.toString()))
                    } else error = "Connect to the internet to load the selected scripture catalog."
                }
            }
            )
            }
        }
        return
    }

    val navItems = listOf(
        Triple("Home", R.drawable.ic_nav_home, "Home"),
        Triple("Saved", R.drawable.ic_nav_saved, "Favorites"),
        Triple("Explore", R.drawable.ic_nav_explore, "Explore"),
        Triple("Discover", R.drawable.ic_nav_discover, "Discover"),
        Triple("Settings", R.drawable.ic_nav_settings, "Settings")
    )
    val useNavigationRail = LocalConfiguration.current.screenWidthDp >= 840

    // ── Setup guide: full-screen, no topBar / bottomBar ─────────────────────
    if (setupGuidePending) {
        /** Persists that the guide is over (completed or skipped) so it is never shown again. */
        fun finishSetup() {
            store.edit().putBoolean("setup_guide_pending", false).putBoolean("setup_guide_done", true).remove("setup_step").apply()
            setupGuidePending = false
        }
        Box(Modifier.fillMaxSize()) {
        FirstRunSetupScreen(
            initialStep = store.getInt("setup_step", 0),
            remindersOn = reminders,
            wallpaperOn = lockScreen,
            verseText = verse?.text,
            verseCitation = verse?.let { v -> "${v.scripture} · ${v.chapter}.${v.verseNumber}" },
            onStepChange = { store.edit().putInt("setup_step", it).apply() },
            // Step 1 — notifications: the same reminder pipeline as the Settings "Verse reminders" switch.
            onEnableNotifications = {
                val granted = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    reminders = true
                    store.edit().putBoolean("reminders", true).putString("setup_notifications", "enabled").apply()
                    VerseSyncWorker.schedule(context, frequency)
                    MetaAnalytics.track(context, MetaAnalytics.Event.DAILY_REMINDER_ENABLED, MetaAnalytics.params("source" to "setup"))
                    scope.launch { applyCurrentLockVerse()?.let { snackbarHostState.showSnackbar(it) } }
                } else {
                    val asked = store.getBoolean("notification_permission_asked", false)
                    val canAsk = !asked || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
                    if (canAsk) {
                        store.edit().putBoolean("notification_permission_asked", true).apply()
                        permissionTarget = "setup"
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Denied permanently: Android will not show the dialog again, so send the user to the system settings.
                        scope.launch { snackbarHostState.showSnackbar("Allow notifications for WisdomOne in Android settings to get verse reminders.") }
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        })
                    }
                }
            },
            // Step 2 — wallpaper: exactly what the Home screen "Lock screen verse" switch does.
            onEnableWallpaper = {
                if (!lockScreen) enableLockScreen()
            },
            // Step 3 — widget: Android's pin-widget flow where the launcher supports it.
            onAddWidget = {
                val manager = AppWidgetManager.getInstance(context)
                val provider = ComponentName(context, ScriptureWidgetReceiver::class.java)
                when {
                    manager.getAppWidgetIds(provider).isNotEmpty() ->
                        scope.launch { snackbarHostState.showSnackbar("The widget is already on your home screen.") }
                    manager.isRequestPinAppWidgetSupported -> {
                        val ok = runCatching { manager.requestPinAppWidget(provider, null, null) }.getOrDefault(false)
                        if (ok) store.edit().putBoolean("setup_widget_requested", true).apply()
                        else scope.launch { snackbarHostState.showSnackbar("Couldn't open the widget picker. Long-press your home screen, open Widgets, and add WisdomOne.") }
                    }
                    else -> scope.launch { snackbarHostState.showSnackbar("Your launcher can't add widgets from here. Long-press your home screen, open Widgets, and add WisdomOne.") }
                }
            },
            onFinish = { finishSetup() }
        )
        Box(Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars).padding(top = 52.dp)) { AppToastHost(snackbarHostState) }
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { AppToastHost(snackbarHostState) },
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.ic_wisdomone_ui_logo),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("WisdomOne", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Ink, letterSpacing = 0.3.sp)
                    }
                }
            }
        },
        bottomBar = {
            if (!useNavigationRail) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(BorderStroke(1.dp, Amber.copy(alpha = 0.15f)), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    navItems.forEach { (label, iconRes, route) ->
                        val selected = page == route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (settingsDirty && route != "Settings") {
                                    pendingNavRoute = route
                                    showUnsavedDialog = true
                                } else page = route
                            },
                            icon = { Icon(painter = painterResource(iconRes), contentDescription = label, modifier = Modifier.size(22.dp), tint = if (selected) Amber else InkSoft) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Amber else InkSoft) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Amber, selectedTextColor = Amber, unselectedIconColor = InkSoft, unselectedTextColor = InkSoft, indicatorColor = Amber.copy(alpha = 0.12f))
                        )
                    }
                }
            }
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (useNavigationRail) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                    navItems.forEach { (label, iconRes, route) ->
                        val selected = page == route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (settingsDirty && route != "Settings") {
                                    pendingNavRoute = route
                                    showUnsavedDialog = true
                                } else page = route
                            },
                            icon = { Icon(painter = painterResource(iconRes), contentDescription = label, modifier = Modifier.size(22.dp), tint = if (selected) Amber else InkSoft) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Amber else InkSoft) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth()) {
                    when (page) {
                    "Home" -> HomeScreen(
                    verse, loading, error, favoriteIds.contains(verse?.id), lockScreen,
                    showScripture = showReadingScripture,
                    showReference = showReadingReference,
                    showTranslation = showReadingTranslation,
                    showBranding = showReadingBranding,
                    moodSlot = {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !moodCardHidden,
                            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(450)) +
                                androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(450)) { -it / 3 } +
                                androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(450))
                        ) {
                            MoodCard(
                                moods = moods, selected = mood, loading = moodLoading, message = moodMessage, error = moodError,
                                onSelect = { code -> moodShuffle = 0; scope.launch { applyMood(code) } },
                                onAnother = { val m = mood; if (m != null) { moodShuffle++; scope.launch { applyMood(m, moodShuffle) } } },
                                onClear = {
                                    mood = null; moodMessage = null; moodError = null
                                    store.edit().remove("mood").remove("mood_day").remove("mood_message").remove("last_verse_id").apply()
                                    scope.launch { loadToday() }
                                },
                                onJournal = { overlay = Overlay.Journal }
                            )
                        }
                    },
                    // The five-level journey is complete at 150 distinct reading days.
                    // Keep the achievement/history in the journey data, but remove the tree card from Home.
                    treeSlot = if (treeState.journeyComplete || !treeEnabled) null else {
                        { WisdomTreeCard(treeState, onJourney = { showJourney = true }) }
                    },
                    actionsSlot = {
                        verse?.let { v ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { explainFor = v; markReading(); reportTreeActivity("explain"); MetaAnalytics.track(context, MetaAnalytics.Event.EXPLAIN_OPENED) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.5.dp, Amber.copy(alpha = 0.5f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
                                ) { Text("🧒  Explain this to me like I'm 5", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                                if (storyAvailable) OutlinedButton(onClick = { overlay = Overlay.StoryForVerse(v.id); markReading(); reportTreeActivity("story"); MetaAnalytics.track(context, MetaAnalytics.Event.STORY_OPENED) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.5.dp, Amber.copy(alpha = 0.5f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                                    contentPadding = PaddingValues(horizontal = 6.dp)) { Text("📖 Story", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
                            }
                        }
                    },
                    onRefresh = { scope.launch { refreshHomeWithoutRotatingVerse() } },
                    onRandom = {
                        mood = null; moodMessage = null
                        store.edit().remove("mood").remove("mood_day").remove("mood_message").apply()
                        scope.launch { loadToday(true) }
                    },
                    onFavorite = { verse?.let { v -> markReading(); scope.launch { toggleFavorite(v) } } },
                    onSpread = { verse?.let { v -> markReading(); overlay = Overlay.Studio(v); MetaAnalytics.track(context, MetaAnalytics.Event.SHARE_STUDIO_OPENED) } },
                    onShowLockScreen = { enableLockScreen() },
                    onHideLockScreen = { disableLockScreen() },
                    onHistory = {
                        scope.launch {
                            try { historyVerses = ApiClient.api.todayHistory(7, religion.takeIf { it > 0 }, language.takeIf { it > 0 }) } catch (_: Exception) {}
                            showHistory = true
                        }
                    },
                    onChapterBrowser = { showChapterBrowser = true },
                    fontSize = fontSize,
                    streak = streak, today = todayDate
                )
                "Favorites" -> FavoritesScreen(
                    savedVerses,
                    onOpen = { cached -> verse = cached.toVerse(); page = "Home" },
                    onRemove = { cached -> scope.launch { toggleFavorite(cached.toVerse()) } },
                    onMarkRead = { cached ->
                        scope.launch {
                            val today = java.time.LocalDate.now().toString()
                            val newDate = if (cached.readDate == today) null else today
                            withContext(Dispatchers.IO) { dao.setReadDate(cached.id, newDate) }
                        }
                    }
                )
                "Explore" -> ExploreScreen(
                    query, { query = it }, results, loading, favoriteIds,
                    languages, scriptures, searchLanguageFilter, searchScriptureFilter,
                    onSearch = { scope.launch {
                        loading = true
                        results = try { ApiClient.api.search(query, searchLanguageFilter, searchScriptureFilter) } catch (_: Exception) { emptyList() }
                        loading = false
                    }},
                    onFavorite = { v -> scope.launch { toggleFavorite(v) } },
                    onQueryChange = { query = it; if (it.isEmpty()) results = emptyList() },
                    onLanguageFilter = { searchLanguageFilter = it; results = emptyList() },
                    onScriptureFilter = { searchScriptureFilter = it; results = emptyList() }
                )
                "Discover" -> DiscoverScreen(onOpen = { item ->
                    when (item) {
                        DiscoverItem.TRUTHS -> overlay = Overlay.Truths
                        DiscoverItem.CHALLENGES -> overlay = Overlay.Challenges
                        DiscoverItem.STORIES -> overlay = Overlay.Stories
                        DiscoverItem.WIDGETS -> overlay = Overlay.Widgets
                        DiscoverItem.JOURNAL -> overlay = Overlay.Journal
                        DiscoverItem.STUDIO -> scope.launch {
                            val v = verse ?: withContext(Dispatchers.IO) { dao.find(store.getInt("last_verse_id", -1))?.toVerse() }
                            if (v == null) snackbarHostState.showSnackbar("Open a verse on Home first, then come back.")
                            else overlay = Overlay.Studio(v)
                        }
                    }
                })
                else -> SettingsExperienceScreen(
                    religion = religion,
                    language = language,
                    frequency = frequency,
                    reminders = reminders,
                    lockScreen = lockScreen,
                    religions = religions,
                    languages = languages,
                    fontSize = fontSize,
                    notice = error,
                    treeEnabled = treeEnabled,
                    onTreeEnabled = { enable, done ->
                        scope.launch {
                            val dto = withContext(Dispatchers.IO) { TreeStore.setEnabled(context, enable) }
                            if (dto != null) { treeEnabled = enable; treeRemote = dto; treeDays = TreeStore.days(context); if (enable) snackbarHostState.showSnackbar("Wisdom Tree restarted from Level 1") }
                            done(dto != null)
                        }
                    },
                    onWallpaperApplied = { reportTreeActivity("wallpaper") },
                    onAdsLab = if (BuildConfig.DEBUG) ({ overlay = Overlay.AdsLab }) else null,
                    onSavePreferences = { newReligion, newLanguage, newFrequency, newReminders, newWallpaperMode ->
                        religion = newReligion
                        language = newLanguage
                        frequency = newFrequency
                        reminders = newReminders
                        scope.launch {
                            val allScriptures = try { ApiClient.api.scriptures(newReligion) } catch (_: Exception) { emptyList() }
                            val selected = allScriptures.firstOrNull()
                            if (selected == null) {
                                error = "Couldn't reach the scripture catalog. Your saved settings were not changed."
                                snackbarHostState.showSnackbar("Couldn't save preferences. Please try again.")
                            } else {
                                scriptureId = selected.id
                                val ids = allScriptures.joinToString(",") { it.id.toString() }
                                val oldReligion = store.getInt("religion", 0)
                                val prefsChanged = newReligion != oldReligion || newLanguage != store.getInt("language", 0)
                                val frequencyChanged = newFrequency != (store.getString("frequency", "Every 24 hours") ?: "Every 24 hours")
                                val freqApi = when (newFrequency) {
                                    "Every hour" -> "1h"
                                    "Every 6 hours" -> "6h"
                                    "Every 12 hours" -> "12h"
                                    else -> "24h"
                                }
                                store.edit().putInt("religion", newReligion).putInt("language", newLanguage)
                                    .putInt("scripture", selected.id).putString("frequency", newFrequency)
                                    .putBoolean("reminders", newReminders).putString("wallpaper_mode", newWallpaperMode).apply()
                                if (newReligion != oldReligion) store.edit().putInt("wallpaper_image_index", 1).apply()
                                val token = getToken()
                                if (token != null) runCatching {
                                    ApiClient.api.savePreferences(PreferenceInput(newReligion, newLanguage, ids, freqApi, newReminders, newWallpaperMode))
                                }
                                VerseSyncWorker.schedule(context, newFrequency, resetTimer = prefsChanged || frequencyChanged)
                                if (!lockScreen && !newReminders) withContext(Dispatchers.IO) { VerseNotifier.cancel(context) }
                                snackbarHostState.showSnackbar("Preferences saved")
                                refreshForPreferences(changed = prefsChanged)
                            }
                        }
                    },
                    onReminders = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33) {
                            permissionTarget = "reminders"
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else reminders = enabled
                    },
                    onLockScreen = { enabled -> if (enabled) enableLockScreen() else disableLockScreen() },
                    onFontSize = { sz -> fontSize = sz; store.edit().putInt("font_size", sz).apply() },
                    onWidgetStudio = { overlay = Overlay.Widgets },
                    onRate = { RatingManager.openPlayStore(context) },
                    onFeedback = { showFeedback = true }
                )
                    }
                }
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            containerColor = Parchment,
            title = { Text("Unsaved changes", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink) },
            text = { Text("You've changed your preferences. Save before leaving?", fontSize = 14.sp, color = InkSoft, lineHeight = 21.sp) },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    // Discard: reset all fields back to stored values
                    religion = store.getInt("religion", 0)
                    language = store.getInt("language", 0)
                    frequency = store.getString("frequency", "Every 24 hours")?.let { if (it == "Daily") "Every 24 hours" else it } ?: "Every 24 hours"
                    reminders = store.getBoolean("reminders", false)
                    page = pendingNavRoute ?: "Home"
                    pendingNavRoute = null
                }) {
                    Text("Discard", color = InkSoft, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedDialog = false
                        val targetRoute = pendingNavRoute ?: "Home"
                        pendingNavRoute = null
                        scope.launch {
                            val allScriptures = try { ApiClient.api.scriptures(religion) } catch (_: Exception) { emptyList() }
                            val selected = allScriptures.firstOrNull()
                            if (selected != null) {
                                scriptureId = selected.id
                                val scriptureIdsStr = allScriptures.joinToString(",") { it.id.toString() }
                                val prefsChanged = religion != store.getInt("religion", 0) || language != store.getInt("language", 0)
                                val frequencyChanged = frequency != (store.getString("frequency", "Every 24 hours") ?: "Every 24 hours")
                                store.edit()
                                    .putInt("religion", religion).putInt("language", language)
                                    .putInt("scripture", selected.id).putString("frequency", frequency)
                                    .putBoolean("reminders", reminders).putBoolean("lock_screen", lockScreen)
                                    .apply()
                                val token = getToken()
                                if (token != null) {
                                    try {
                                        val freqApi = when (frequency) { "Every hour" -> "1h"; "Every 6 hours" -> "6h"; "Every 12 hours" -> "12h"; else -> "24h" }
                                        ApiClient.api.savePreferences(PreferenceInput(religion, language, scriptureIdsStr, freqApi, reminders, com.scripturedaily.lockscreen.WallpaperMode.normalize(store.getString("wallpaper_mode", null))))
                                    } catch (_: Exception) {}
                                }
                                VerseSyncWorker.schedule(context, frequency, resetTimer = prefsChanged || frequencyChanged)
                                snackbarHostState.showSnackbar("Preferences saved")
                                refreshForPreferences(changed = prefsChanged)
                                page = targetRoute
                            } else {
                                snackbarHostState.showSnackbar("Couldn't save preferences. Please try again.")
                                // Stay on Settings — do not navigate
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = W.InkButton),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        )
    }

    if (showFeedback) {
        ResponsiveContentFrame {
            FeedbackScreen(
                onDismiss = { showFeedback = false },
                onSubmit = { rating, comment, consent ->
                    showFeedback = false
                    scope.launch {
                        try {
                            ApiClient.api.submitFeedback(FeedbackInput(rating, comment.ifBlank { null }, consent))
                            snackbarHostState.showSnackbar("Thank you for your feedback!")
                        } catch (_: Exception) {
                            snackbarHostState.showSnackbar("Couldn't send feedback. Please try again.")
                        }
                    }
                }
            )
        }
    }

    if (showLogin) {
        LoginScreen(
            onDismiss = { showLogin = false },
            onSuccess = { token ->
                store.edit().putString("bearer_token", token).apply()
                showLogin = false
                scope.launch { snackbarHostState.showSnackbar("Signed in successfully.") }
            },
            onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
        )
    }

    if (showHistory) {
        HistoryScreen(
            verses = historyVerses,
            favoriteIds = favoriteIds,
            onOpen = { v -> verse = v; page = "Home"; showHistory = false },
            onFavorite = { v -> scope.launch { toggleFavorite(v) } },
            onDismiss = { showHistory = false }
        )
    }

    if (showChapterBrowser) {
        ChapterBrowserScreen(
            scriptures = scriptures,
            currentScriptureId = scriptureId,
            languageId = language,
            favoriteIds = favoriteIds,
            onOpen = { v -> verse = v; page = "Home"; showChapterBrowser = false },
            onFavorite = { v -> scope.launch { toggleFavorite(v) } },
            onDismiss = { showChapterBrowser = false }
        )
    }

    // ── Feature overlays ───────────────────────────────────────────────────────
    LaunchedEffect(configured) {
        if (configured) try { moods = ApiClient.api.moods().ifEmpty { moods } } catch (_: Exception) {}
    }
    LaunchedEffect(verse?.id, language) {
        storyAvailable = false
        val v = verse ?: return@LaunchedEffect
        try { storyAvailable = withContext(Dispatchers.IO) { ApiClient.api.storyForVerse(v.id, language.takeIf { it > 0 }).isSuccessful } } catch (_: Exception) {}
    }
    val closeOverlay = { overlay = null }
    when (val o = overlay) {
        null -> Unit
        Overlay.Truths -> UniversalTruthsScreen(language, religions.firstOrNull { it.id == religion }?.code, closeOverlay)
        Overlay.Challenges -> ChallengesScreen(language, religion, closeOverlay)
        Overlay.Journal -> JournalScreen(closeOverlay, onWriteFirstReflection = { overlay = null; verse?.let { reflectFor = it } })
        Overlay.Reflections -> JournalScreen(closeOverlay, onWriteFirstReflection = { overlay = null; verse?.let { reflectFor = it } }, initialTab = 1)
        Overlay.Widgets -> WidgetStudioScreen(closeOverlay)
        Overlay.AdsLab -> com.scripturedaily.ads.AdsLabScreen(closeOverlay)
        Overlay.Stories -> StoryLibraryScreen(language, religion, closeOverlay)
        is Overlay.Studio -> StudioScreen(o.verse, religion, religions.firstOrNull { it.id == religion }?.code, if (o.video) StudioTab.VIDEO else StudioTab.SHARE, closeOverlay, onActivity = { reportTreeActivity(it) })
        is Overlay.StoryForVerse -> StoryScreen(StorySource.ForVerse(o.verseId), language, closeOverlay)
    }
    explainFor?.let { v -> ExplainSheet(v, onDismiss = { explainFor = null }, onReflect = { explainFor = null; reflectFor = v }) }
    reflectFor?.let { v -> TodayReflectionSheet(v, onDismiss = { reflectFor = null }, onSaved = { refreshTree() }) }
    if (showJourney) TreeJourneyScreen(treeDays, treeRemote) { showJourney = false }
    // Level-completion interstitial: off by default on the server; only at the natural break after the celebration, never mid-reading.
    milestoneStage?.let { st ->
        LaunchedEffect(st) { if (st == TreeStage.FRUITS) com.scripturedaily.ads.AdManager.preloadInterstitial("tree_completion_interstitial") }
        MilestoneDialog(st) { milestoneStage = null; if (st == TreeStage.FRUITS) com.scripturedaily.ads.AdManager.showInterstitial(activity, "tree_completion_interstitial") }
    }
}
/** Shown for at most a few seconds while the app configuration is fetched (never indefinitely: see AppConfigManager). */
@Composable
private fun ConfigLoadingScreen() {
    Box(Modifier.fillMaxSize().background(Parchment), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Image(painter = painterResource(R.drawable.ic_wisdomone_ui_logo), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(84.dp).clip(CircleShape))
            Text("WisdomOne", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            CircularProgressIndicator(color = Amber, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Text("Getting things ready…", fontSize = 13.sp, color = InkSoft)
        }
    }
}

@Composable
private fun ResponsiveContentFrame(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier.fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun ApiHealthScreen(state: HealthState, onRetry: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Box(Modifier.fillMaxSize().background(Parchment), contentAlignment = Alignment.Center) {
        Box(Modifier.size(300.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Amber.copy(alpha = 0.15f), Color.Transparent))))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                modifier = Modifier.size(88.dp).graphicsLayer { alpha = if (state is HealthState.Failed) 1f else pulse }
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_wisdomone_ui_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
            Spacer(Modifier.height(24.dp))
            when (state) {
                is HealthState.Checking -> {
                    Text("Connecting…", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("Waking up the server, just a moment.", fontSize = 14.sp, color = InkSoft, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(28.dp))
                    CircularProgressIndicator(color = Amber, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                }
                is HealthState.Retrying -> {
                    Text("Still connecting…", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("Retry ${state.attempt} of 3 — the server may be starting up.", fontSize = 14.sp, color = InkSoft, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { i -> Box(Modifier.size(10.dp).clip(CircleShape).background(if (i < state.attempt) Amber else Amber.copy(alpha = 0.25f))) }
                    }
                }
                is HealthState.Failed -> {
                    Text("Can't reach the server", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("Check your internet connection\nor try again in a moment.", fontSize = 14.sp, color = InkSoft, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    Spacer(Modifier.height(28.dp))
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(0.65f).height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = W.InkButton)) {
                        Text("Try again", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun Onboarding(
    religion: Int,
    language: Int,
    religions: List<ReligionItem>,
    languages: List<CatalogItem>,
    catalogLoading: Boolean,
    notice: String?,
    onReligion: (Int) -> Unit,
    onLanguage: (Int) -> Unit,
    onContinue: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var localLanguages by remember(languages) { mutableStateOf(languages) }
    var langLoading by remember { mutableStateOf(false) }

    fun reloadLanguages(religionId: Int) {
        scope.launch {
            langLoading = true
            try {
                val fetched = withContext(Dispatchers.IO) { ApiClient.api.languages(religionId) }
                localLanguages = fetched
                if (fetched.isNotEmpty()) onLanguage(fetched.first().id)
            } catch (_: Exception) {}
            langLoading = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(Parchment).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).statusBarsPadding()
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Choose your\ntradition", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink, lineHeight = 40.sp)
        Spacer(Modifier.height(6.dp))
        Text("Pick the scripture you'd like to read daily.", fontSize = 15.sp, color = InkSoft)
        Spacer(Modifier.height(28.dp))

        Text("TRADITION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkSoft, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(10.dp))

        if (catalogLoading) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Amber, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        } else {
            religions.forEach { item ->
                val selected = religion == item.id
                val scriptureNames = item.scriptures.joinToString(", ") { it.name }
                val traditionIconRes = when (item.code.lowercase()) {
                    "hinduism"     -> R.drawable.ic_tradition_hinduism
                    "christianity" -> R.drawable.ic_tradition_christianity
                    "islam"        -> R.drawable.ic_tradition_islam
                    "buddhism"     -> R.drawable.ic_tradition_buddhism
                    else           -> R.drawable.ic_tradition_general_wisdom
                }
                val bannerRes = when (item.code.lowercase()) {
                    "hinduism"     -> R.drawable.banner_hinduism
                    "christianity" -> R.drawable.banner_christianity
                    "islam"        -> R.drawable.banner_islam
                    "buddhism"     -> R.drawable.banner_buddhism
                    else           -> R.drawable.banner_general_wisdom
                }
                val bg = if (selected) W.SurfaceAlt else W.Surface
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp).height(80.dp).clip(RoundedCornerShape(16.dp))
                        .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) W.PurpleMid else W.Gold.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
                        .clickable { onReligion(item.id); reloadLanguages(item.id) }
                ) {
                    // Banner artwork on the right — same technique as SettingsHeroHeader
                    Image(
                        painter = painterResource(bannerRes), contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = if (bannerRes == R.drawable.banner_general_wisdom) Alignment.CenterStart else Alignment.CenterEnd,
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(0.65f).align(Alignment.CenterEnd)
                    )
                    // Horizontal veil: surface → transparent
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to bg, 0.42f to bg, 0.68f to bg.copy(alpha = 0.4f), 1f to Color.Transparent)))
                    // Top + bottom edge veils
                    Box(Modifier.fillMaxWidth().height(22.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(bg, Color.Transparent))))
                    Box(Modifier.fillMaxWidth().height(22.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, bg))))
                    // Content row on top of the veil
                    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                .background(W.Gold.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(traditionIconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 15.sp, color = Ink)
                            if (scriptureNames.isNotEmpty()) Text(scriptureNames, fontSize = 12.sp, color = InkMid, maxLines = 1)
                        }
                        Box(
                            Modifier.size(22.dp).clip(CircleShape)
                                .border(BorderStroke(1.8.dp, if (selected) W.PurpleMid else InkSoft.copy(alpha = 0.6f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(W.PurpleMid))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("LANGUAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(10.dp))

        if (catalogLoading || langLoading) {
            Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Amber, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              localLanguages.chunked(3).forEach { chunk ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEach { item ->
                    val selected = language == item.id
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Amber.copy(alpha = 0.08f) else W.Surface)
                            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Amber else W.Gold.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                            .clickable { onLanguage(item.id) }
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(18.dp).clip(CircleShape)
                                    .background(if (selected) Amber else Color.Transparent)
                                    .border(BorderStroke(1.5.dp, if (selected) Amber else InkSoft.copy(alpha = 0.6f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(item.name, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp, color = if (selected) Ink else InkMid)
                                Text(item.nativeScript.ifEmpty { languageNativeScript(item.name) }, fontSize = 11.sp, color = if (selected) Amber else InkSoft)
                            }
                        }
                    }
                }
                repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
              }
            }
        }

        if (notice != null) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(W.Error.copy(alpha = 0.08f)).padding(14.dp)) {
                Text(notice, color = W.Error, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            enabled = !catalogLoading && !langLoading && religion != 0 && language != 0,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = W.InkButton)
        ) {
            Text("Start reading", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("You can change your tradition and language anytime in Settings.", fontSize = 12.sp, color = InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
        Spacer(Modifier.navigationBarsPadding())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeScreen(
    verse: Verse?, loading: Boolean, error: String?, favorite: Boolean, lockScreen: Boolean,
    fontSize: Int, streak: Int, today: java.time.LocalDate,
    showScripture: Boolean = true, showReference: Boolean = true, showTranslation: Boolean = true, showBranding: Boolean = true,
    moodSlot: @Composable () -> Unit, treeSlot: (@Composable () -> Unit)?, actionsSlot: @Composable () -> Unit,
    onRefresh: () -> Unit, onRandom: () -> Unit, onFavorite: () -> Unit, onSpread: () -> Unit,
    onShowLockScreen: () -> Unit, onHideLockScreen: () -> Unit,
    onHistory: () -> Unit, onChapterBrowser: () -> Unit
) {
    val scrollState = rememberScrollState()
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            val todayLabel = remember(today) { today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.getDefault())) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clip(RoundedCornerShape(50)).background(Amber.copy(alpha = 0.1f)).padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Amber, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(todayLabel, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.weight(1f))
                if (streak > 0) Row(Modifier.clip(RoundedCornerShape(50)).background(Amber.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 13.sp)
                    Spacer(Modifier.width(5.dp))
                    Text("$streak day streak", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.SemiBold)
                }
            }
            AdSlot("home_top_banner")
            moodSlot()
            AdSlot("home_after_mood")
            // A completed five-level journey passes null: emit no layout node and no spacer.
            // All following Home content naturally moves up in the scroll column.
            treeSlot?.invoke()
            if (loading && verse == null) {
                Box(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(28.dp)).background(Amber.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = Amber, modifier = Modifier.size(36.dp))
                        Text("Loading today's verse…", color = InkSoft, fontSize = 13.sp)
                    }
                }
            }
            if (error != null && verse == null) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(W.Error.copy(alpha = 0.1f)).padding(20.dp)) {
                    Text(error, color = W.Error, fontSize = 14.sp)
                }
            }
            if (verse != null) {
                ThemedVerseCard(verse, VerseThemes.forDate(today), fontSize,
                    showScripture = showScripture, showReference = showReference,
                    showTranslation = showTranslation, showBranding = showBranding)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val favBg by animateColorAsState(if (favorite) Amber else Color.Transparent, tween(300), label = "favBg")
                    val favContent by animateColorAsState(if (favorite) Color.White else Ink, tween(300), label = "favContent")
                    Button(onClick = onFavorite, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = favBg, contentColor = favContent), border = BorderStroke(1.5.dp, Amber.copy(alpha = 0.5f)), contentPadding = PaddingValues(start = 10.dp, end = 4.dp, top = 0.dp, bottom = 0.dp)) {
                        Icon(if (favorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (favorite) "Saved" else "Save", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    }
                    // One primary sharing action: opens Share Studio (Share + WhatsApp Status live inside it).
                    Button(
                        onClick = onSpread, modifier = Modifier.weight(1.9f).height(52.dp), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = W.OnPurple),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share with the World", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false)
                    }
                }
                actionsSlot()
                AdSlot("home_after_daily_verse")
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (lockScreen) Amber.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface).border(1.dp, if (lockScreen) Amber.copy(alpha = 0.4f) else Amber.copy(alpha = 0.1f), RoundedCornerShape(20.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(if (lockScreen) Amber.copy(alpha = 0.2f) else Amber.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                            Icon(if (lockScreen) Icons.Outlined.Lock else Icons.Outlined.LockOpen, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (lockScreen) "Verse wallpaper & notification on" else "Lock screen verse", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (lockScreen) Amber else Ink)
                            Text(if (lockScreen) "Tap to turn off and clear the lock-screen image" else "Show this verse on your lock-screen wallpaper and in notifications", fontSize = 12.sp, color = InkSoft)
                        }
                        Switch(checked = lockScreen, onCheckedChange = { if (it) onShowLockScreen() else onHideLockScreen() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Amber))
                    }
                }
            }
            Button(onClick = onRandom, enabled = !loading, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = W.InkButton)) {
                if (loading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Finding a verse…" else "Discover another verse", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            AdSlot("home_inline_banner")
            AdSlot("home_bottom_banner", screenOverride = "home_bottom")
            Text("Take a breath. Carry the thought with you.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = InkSoft, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FavoritesScreen(
    items: List<CachedVerse>,
    onOpen: (CachedVerse) -> Unit,
    onRemove: (CachedVerse) -> Unit,
    onMarkRead: (CachedVerse) -> Unit
) {
    var expandedItems by remember { mutableStateOf(emptySet<Int>()) }
    val adCfg by AdManager.config.collectAsState()
    val adPositions = remember(items.size, adCfg) { if (adCfg.enabled) savedVerseAdPositions(items.size, adCfg.saved) else emptySet() }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp, vertical = 20.dp)) {
            Column {
                Text("Saved Verses", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${items.size} verse${if (items.size != 1) "s" else ""} saved", fontSize = 13.sp, color = InkSoft)
            }
        }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(Amber.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Amber.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                    }
                    Text("No saved verses yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Save a verse from the home screen\nto keep it close.", fontSize = 13.sp, color = InkSoft, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val expanded = item.id in expandedItems
                    val displayReference = if (item.book?.equals(item.scripture, ignoreCase = true) == true)
                        item.reference().removePrefix("${item.book} · ") else item.reference()
                    val today2 = remember { java.time.LocalDate.now().toString() }
                    val isReadToday = item.readDate == today2 || item.savedAt.let { saved ->
                        saved > 0L && java.time.Instant.ofEpochMilli(saved)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString() == today2
                    }
                    // Shimmer animation for collapsed state
                    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
                    val shimmerX by shimmerTransition.animateFloat(
                        initialValue = -300f, targetValue = 300f,
                        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
                        label = "shimmerX"
                    )
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (isReadToday) W.Success.copy(alpha = 0.4f) else Amber.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    ) {
                        Column {
                            // Top accent bar — green if read, gold if not
                            Box(Modifier.fillMaxWidth().height(4.dp).background(
                                if (isReadToday) Brush.horizontalGradient(listOf(W.Success, Color(0xFF6DBF82)))
                                else Brush.horizontalGradient(listOf(Amber, AmberLight))
                            ))
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Top row
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Amber.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text(item.scripture.uppercase(), color = Amber, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (isReadToday) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(W.Success.copy(alpha = 0.12f)).border(BorderStroke(1.dp, W.Success.copy(alpha = 0.4f)), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                                            Text("✓ Today", fontSize = 10.sp, color = W.Success, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    // Remove button only in collapsed (unread) state
                                    if (!isReadToday) {
                                        Box(
                                            Modifier.size(26.dp).clip(RoundedCornerShape(7.dp))
                                                .background(W.Error.copy(alpha = 0.08f))
                                                .border(BorderStroke(1.dp, W.Error.copy(alpha = 0.3f)), RoundedCornerShape(7.dp))
                                                .clickable { onRemove(item) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("\u00d7", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = W.Error, lineHeight = 15.sp)
                                        }
                                    }
                                }
                                if (!isReadToday) {
                                    // Collapsed state: shimmer placeholder lines
                                    val shimmerBrush = Brush.horizontalGradient(
                                        colors = listOf(InkSoft.copy(alpha = 0.08f), InkSoft.copy(alpha = 0.18f), InkSoft.copy(alpha = 0.08f)),
                                        startX = shimmerX, endX = shimmerX + 300f
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(shimmerBrush))
                                        Box(Modifier.fillMaxWidth(0.65f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(shimmerBrush))
                                        Box(Modifier.fillMaxWidth(0.80f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(shimmerBrush))
                                    }
                                    // Reveal button
                                    OutlinedButton(
                                        onClick = { onMarkRead(item) },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, Amber.copy(alpha = 0.6f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber)
                                    ) {
                                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Reveal today's verse", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    // Revealed state: show verse text
                                    val isLong = item.text.length > 160
                                    Text(
                                        "\u201c${item.text}\u201d",
                                        fontSize = 14.sp, lineHeight = 22.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                                        overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
                                    )
                                    if (isLong) {
                                        Text(
                                            if (expanded) "Show less" else "Read full verse",
                                            color = Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable {
                                                expandedItems = if (expanded) expandedItems - item.id else expandedItems + item.id
                                            }
                                        )
                                    }
                                }
                                // Reference always visible at bottom
                                Text(displayReference, color = InkSoft, fontSize = 11.sp)
                            }
                        }
                    }
                    // Ads follow saved verse #1, #3, #5 ... (backend-configurable); an ad row never counts as a saved verse.
                    if (index in adPositions) {
                        if (index == adCfg.saved.firstAdAfterItems - 1) AdSlot("saved_first_verse_native", slotKey = "saved_$index")
                        else AdSlot("saved_every_two_banner", slotKey = "saved_$index", formatOverride = adCfg.saved.repeatFormat)
                    }
                  }
                }
                item { AdSlot("saved_bottom_banner") }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun ExploreScreen(query: String, onQuery: (String) -> Unit, results: List<Verse>, loading: Boolean, favoriteIds: Set<Int>, languages: List<CatalogItem>, scriptures: List<CatalogItem>, languageFilter: Int?, scriptureFilter: Int?, onSearch: () -> Unit, onFavorite: (Verse) -> Unit, onQueryChange: (String) -> Unit, onLanguageFilter: (Int?) -> Unit, onScriptureFilter: (Int?) -> Unit) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var searched by remember { mutableStateOf(false) }

    // Reset searched state when query is cleared externally
    LaunchedEffect(query) { if (query.isEmpty()) searched = false }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp, vertical = 20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Explore", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Search across all scriptures", fontSize = 13.sp, color = InkSoft)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.5.dp, if (query.isNotEmpty()) Amber.copy(alpha = 0.6f) else Amber.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = Amber.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            TextField(
                value = query, onValueChange = { onQuery(it); onQueryChange(it) }, modifier = Modifier.weight(1f),
                placeholder = { Text("Search a word or idea…", color = InkSoft, fontSize = 14.sp) },
                singleLine = true,
                enabled = !searched,
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent, disabledIndicatorColor = Color.Transparent, disabledTextColor = Ink)
            )
            if (searched) {
                // Show clear button after search
                IconButton(onClick = { onQueryChange(""); searched = false }, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(InkSoft.copy(alpha = 0.15f))) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = InkMid, modifier = Modifier.size(16.dp))
                }
            } else if (query.length >= 2) {
                // Show search button while typing
                IconButton(onClick = {
                    keyboardController?.hide()
                    searched = true
                    onSearch()
                }, enabled = !loading, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Amber)) {
                    if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (results.isEmpty() && !loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(Amber.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = Amber.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                    }
                    Text(if (query.isEmpty()) "Search for wisdom" else "No results found", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(if (query.isEmpty()) "Type a word, theme, or idea\nto explore the scriptures." else "Try a different word or phrase.", fontSize = 13.sp, color = InkSoft, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(results) { v ->
                    val saved = favoriteIds.contains(v.id)
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, Amber.copy(alpha = 0.12f), RoundedCornerShape(20.dp))) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(listOf(Amber, AmberLight))))
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Top row: badge + save button only
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Amber.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text(v.scripture.uppercase(), color = Amber, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    val saveBg by animateColorAsState(if (saved) Amber else Color.Transparent, tween(250), label = "saveBg")
                                    val saveContent by animateColorAsState(if (saved) Color.White else Ink, tween(250), label = "saveContent")
                                    Button(onClick = { onFavorite(v) }, modifier = Modifier.height(34.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = saveBg, contentColor = saveContent), border = BorderStroke(1.dp, Amber.copy(alpha = 0.5f)), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)) {
                                        Icon(if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (saved) "Saved" else "Save", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Text("\u201c${v.text}\u201d", fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                // Reference at bottom
                                Text(v.reference(), color = InkSoft, fontSize = 11.sp)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun FeedbackScreen(onDismiss: () -> Unit, onSubmit: (Int, String, Boolean) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    val maxChars = 1000
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Parchment).clickable(enabled = false) {}
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Leave us a feedback", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Box(Modifier.size(32.dp).clip(CircleShape).background(Ink.copy(alpha = 0.08f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Text("×", fontSize = 20.sp, color = Ink, fontWeight = FontWeight.Bold)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How do you feel about WisdomOne?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("Slide to rate your experience", fontSize = 12.sp, color = InkSoft)
                Slider(
                    value = rating.toFloat(), onValueChange = { rating = it.toInt() },
                    valueRange = 1f..10f, steps = 8, modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber, inactiveTrackColor = Amber.copy(alpha = 0.2f))
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bad", fontSize = 12.sp, color = W.Error)
                    Text("Average", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.SemiBold)
                    Text("Good", fontSize = 12.sp, color = W.Success)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (rating <= 3) "😞" else "😐", fontSize = 28.sp)
                    Text(if (rating in 4..6) "😊" else "😐", fontSize = 28.sp)
                    Text(if (rating >= 7) "😄" else "😐", fontSize = 28.sp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Anything we could do better?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("Every bit of feedback helps!", fontSize = 12.sp, color = InkSoft)
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, Amber.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    TextField(
                        value = comment, onValueChange = { if (it.length <= maxChars) comment = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        placeholder = { Text("Share your thoughts…", color = InkSoft, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        maxLines = 5
                    )
                    Text("${comment.length}/$maxChars", fontSize = 11.sp, color = InkSoft, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Amber.copy(alpha = 0.06f))
                    .clickable { consent = !consent }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = consent, onCheckedChange = { consent = it }, colors = CheckboxDefaults.colors(checkedColor = Amber, uncheckedColor = InkSoft))
                Spacer(Modifier.width(8.dp))
                Text("I consent to this feedback being used to improve the app.", fontSize = 12.sp, color = InkMid, lineHeight = 18.sp)
            }
            Button(
                onClick = { onSubmit(rating, comment, consent) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber)
            ) {
                Text("Submit Feedback", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun languageNativeScript(name: String): String = when (name.lowercase()) {
    "hindi"   -> "हिन्दी"
    "english" -> "English"
    "bengali" -> "বাংলা"
    "arabic"  -> "عربي"
    "tamil"   -> "தமிழ்"
    "telugu"  -> "తెలుగు"
    "marathi" -> "मराठी"
    else      -> name
}


@Composable
private fun LoginScreen(
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Parchment).clickable(enabled = false) {}
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isRegister) "Create account" else "Sign in", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Box(Modifier.size(32.dp).clip(CircleShape).background(Ink.copy(alpha = 0.08f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Text("×", fontSize = 20.sp, color = Ink, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None
                    else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null, tint = InkSoft)
                    }
                }
            )
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        try {
                            val body = AuthInput(email.trim(), password)
                            val resp = if (isRegister) ApiClient.api.register(body) else ApiClient.api.login(body)
                            if (resp.isSuccessful) onSuccess(resp.body()!!.accessToken)
                            else onError(if (isRegister) "Registration failed. Try a different email." else "Incorrect email or password.")
                        } catch (_: Exception) { onError("Network error. Please try again.") }
                        loading = false
                    }
                },
                enabled = email.isNotBlank() && password.length >= 8 && !loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = W.InkButton)
            ) {
                if (loading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                Text(if (isRegister) "Create account" else "Sign in", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { isRegister = !isRegister }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (isRegister) "Already have an account? Sign in" else "No account? Register", color = Amber, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── History screen ────────────────────────────────────────────────────────────
@Composable
private fun HistoryScreen(
    verses: List<Verse>,
    favoriteIds: Set<Int>,
    onOpen: (Verse) -> Unit,
    onFavorite: (Verse) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Parchment).clickable(enabled = false) {}
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Recent Verses", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Box(Modifier.size(32.dp).clip(CircleShape).background(Ink.copy(alpha = 0.08f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Text("×", fontSize = 20.sp, color = Ink, fontWeight = FontWeight.Bold)
                }
            }
            if (verses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No history available. Connect to load.", color = InkSoft, fontSize = 14.sp)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { AdSlot("history_top_banner") }
                    itemsIndexed(verses) { vIndex, v ->
                        if (vIndex == 3) AdSlot("history_inline_native")
                        val saved = favoriteIds.contains(v.id)
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, Amber.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                                .clickable { onOpen(v) }
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Amber.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text(v.scripture.uppercase(), color = Amber, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { onFavorite(v) }, modifier = Modifier.size(32.dp)) {
                                        Icon(if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text("\u201c${v.text}\u201d", fontSize = 14.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Text(v.reference(), fontSize = 11.sp, color = InkSoft)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

// ── Chapter browser screen ────────────────────────────────────────────────────
@Composable
private fun ChapterBrowserScreen(
    scriptures: List<CatalogItem>,
    currentScriptureId: Int,
    languageId: Int,
    favoriteIds: Set<Int>,
    onOpen: (Verse) -> Unit,
    onFavorite: (Verse) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedScripture by remember { mutableIntStateOf(currentScriptureId) }
    var chapters by remember { mutableStateOf(listOf<Int>()) }
    var selectedChapter by remember { mutableStateOf<Int?>(null) }
    var verses by remember { mutableStateOf(listOf<Verse>()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedScripture) {
        loading = true
        selectedChapter = null; verses = emptyList()
        try { chapters = ApiClient.api.scriptureChapters(selectedScripture, languageId) } catch (_: Exception) {}
        loading = false
    }

    LaunchedEffect(selectedChapter) {
        val ch = selectedChapter ?: return@LaunchedEffect
        loading = true; verses = emptyList()
        try { verses = ApiClient.api.chapterVerses(selectedScripture, ch, languageId) } catch (_: Exception) {}
        loading = false
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Parchment).clickable(enabled = false) {}
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Browse Chapters", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Box(Modifier.size(32.dp).clip(CircleShape).background(Ink.copy(alpha = 0.08f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Text("×", fontSize = 20.sp, color = Ink, fontWeight = FontWeight.Bold)
                }
            }
            // Scripture picker
            if (scriptures.size > 1) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    scriptures.forEach { sc ->
                        val sel = selectedScripture == sc.id
                        FilterChip(selected = sel, onClick = { selectedScripture = sc.id },
                            label = { Text(sc.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Amber, selectedLabelColor = Color.White))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            // Chapter picker
            if (loading && chapters.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Amber, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                }
            } else if (chapters.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chapters) { ch ->
                        val sel = selectedChapter == ch
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(if (sel) Amber else MaterialTheme.colorScheme.surface)
                                .border(1.5.dp, if (sel) Amber else Amber.copy(alpha = 0.25f), CircleShape)
                                .clickable { selectedChapter = ch },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$ch", fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Color.White else InkMid)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            // Verses list
            if (loading && selectedChapter != null) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Amber, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                }
            } else if (verses.isNotEmpty()) {
                LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(verses) { v ->
                        val saved = favoriteIds.contains(v.id)
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, Amber.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .clickable { onOpen(v) }
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Verse ${v.verseNumber}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Amber)
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { onFavorite(v) }, modifier = Modifier.size(32.dp)) {
                                        Icon(if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = Amber, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(v.text, fontSize = 14.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (v.translation != null) Text(v.translation, fontSize = 12.sp, color = InkSoft, lineHeight = 18.sp)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            } else if (selectedChapter != null && !loading) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No verses found for this chapter.", color = InkSoft, fontSize = 14.sp)
                }
            } else if (chapters.isNotEmpty() && selectedChapter == null) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Select a chapter above.", color = InkSoft, fontSize = 14.sp)
                }
            }
        }
    }
}
