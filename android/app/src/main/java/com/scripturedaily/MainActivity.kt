package com.scripturedaily

import android.Manifest
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.AuthInput
import com.scripturedaily.data.CachedVerse
import com.scripturedaily.data.CatalogItem
import com.scripturedaily.data.DeviceRegInput
import com.scripturedaily.data.FeedbackInput
import com.scripturedaily.data.PreferenceInput
import com.scripturedaily.data.ReligionItem
import com.scripturedaily.data.ScriptureDatabase
import com.scripturedaily.data.Verse
import com.scripturedaily.data.VerseSyncWorker
import com.scripturedaily.data.toCached
import com.scripturedaily.lockscreen.LockScreenVerse
import com.scripturedaily.notification.VerseNotifier
import com.scripturedaily.widget.ScriptureWidget
import com.scripturedaily.widget.ScriptureWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Purple = Color(0xFF3D2060)
private val PurpleDark = Color(0xFF2D1B4E)
private val PurpleMid = Color(0xFF5C3D8F)
private val Gold = Color(0xFFD4A574)
private val GoldLight = Color(0xFFE8C49A)
private val Parchment = Color(0xFFF7F2EE)
private val Ink = Color(0xFF1A1025)
private val InkMid = Color(0xFF4A3D5C)
private val InkSoft = Color(0xFF8A7A9A)
private val Amber = Gold
private val AmberLight = GoldLight
private val Forest = PurpleDark

// ── Custom Toast ─────────────────────────────────────────────────────────────
enum class ToastType { SUCCESS, ERROR, INFO }

data class ToastData(val message: String, val type: ToastType = ToastType.INFO)

@Composable
fun AppToastHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        val isError = data.visuals.message.lowercase().let {
            it.contains("couldn't") || it.contains("error") || it.contains("failed") || it.contains("not")
        }
        val isSuccess = data.visuals.message.lowercase().let {
            it.contains("saved") || it.contains("thank") || it.contains("removed") || it.contains("success")
        }
        val bgColor = when {
            isError   -> Color(0xFF9C463D)
            isSuccess -> Color(0xFF3D6B4A)
            else      -> Color(0xFF2D1B4E)
        }
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
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(
                    data.visuals.message,
                    fontSize = 14.sp,
                    color = Color.White,
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ApiClient.init(this) {
            getSharedPreferences("reader", 0).getString("bearer_token", null)
        }
        setContent {
            val dark = isSystemInDarkTheme()
            val colors = if (dark)
                darkColorScheme(
                    primary = Color(0xFFD4A574), secondary = Color(0xFFE8C49A),
                    background = Color(0xFF120D1E), surface = Color(0xFF1E1530),
                    onSurface = Color(0xFFF0EAF8), onBackground = Color(0xFFF0EAF8)
                )
            else
                lightColorScheme(
                    primary = Color(0xFF3D2060), secondary = Color(0xFFD4A574),
                    background = Color(0xFFF7F2EE), surface = Color(0xFFFFFFFF),
                    onSurface = Color(0xFF1A1025), onBackground = Color(0xFF1A1025)
                )
            MaterialTheme(colorScheme = colors) { ScriptureApp() }
        }
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
private fun ScriptureApp() {
    val context = LocalContext.current
    val store = remember { context.getSharedPreferences("reader", 0) }
    val dao = remember { ScriptureDatabase.get(context).verses() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Health check ──────────────────────────────────────────────────────────
    var healthState by remember { mutableStateOf<HealthState>(HealthState.Checking) }

    suspend fun runHealthCheck() {
        healthState = HealthState.Checking
        repeat(3) { attempt ->
            if (healthState == HealthState.Ready) return
            if (attempt > 0) healthState = HealthState.Retrying(attempt)
            try {
                val resp = ApiClient.api.health()
                if (resp.isSuccessful) { healthState = HealthState.Ready; return }
            } catch (_: Exception) {}
            if (attempt < 2) kotlinx.coroutines.delay(2000L)
        }
        if (healthState != HealthState.Ready) healthState = HealthState.Failed
    }

    LaunchedEffect(Unit) { runHealthCheck() }

    if (healthState != HealthState.Ready) {
        ApiHealthScreen(healthState, onRetry = { scope.launch { runHealthCheck() } })
        return
    }

    // ── State (declared before LaunchedEffect so they are accessible inside it) ────
    var religion by remember { mutableIntStateOf(store.getInt("religion", 0)) }
    var language by remember { mutableIntStateOf(store.getInt("language", 0)) }
    var scriptureId by remember { mutableIntStateOf(store.getInt("scripture", 0)) }
    var frequency by remember { mutableStateOf(store.getString("frequency", "Daily") ?: "Daily") }
    var reminders by remember { mutableStateOf(store.getBoolean("reminders", false)) }
    var lockScreen by remember { mutableStateOf(store.getBoolean("lock_screen", false)) }
    var configured by remember { mutableStateOf(store.getBoolean("configured", false)) }
    var splashDone by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("Home") }
    var showFeedback by remember { mutableStateOf(false) }
    var verse by remember { mutableStateOf<Verse?>(null) }
    val savedVerses by remember { dao.observeFavorites() }.collectAsState(initial = emptyList())
    val favoriteIds = remember(savedVerses) { savedVerses.map { it.id }.toSet() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Verse>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var religions by remember { mutableStateOf(listOf<ReligionItem>()) }
    var languages by remember { mutableStateOf(listOf<CatalogItem>()) }
    var catalogLoading by remember { mutableStateOf(false) }

    // ── Single sequential startup flow ─────────────────────────────────────────
    // Order: health → login/register → POST /devices → GET religions + languages
    LaunchedEffect(Unit) {
        // 1. Clear stale tokens from old broken identity schemes
        if (store.getInt("auth_version", 0) < 4) {
            store.edit().remove("bearer_token").remove("device_id")
                .remove("device_id_fallback").putInt("auth_version", 4).apply()
        }
        // 2. Auth: login first (handles reinstall), register if new device
        ensureDeviceToken(context)
        // 3. Load catalog only after token is stored
        catalogLoading = true
        try {
            val r = ApiClient.api.religions()
            val l = ApiClient.api.languages()
            religions = r
            languages = l
            if (religion == 0 && r.isNotEmpty()) religion = r.first().id
            if (language == 0 && l.isNotEmpty()) language = l.first().id
        } catch (_: Exception) {}
        catalogLoading = false
    }

    var permissionTarget by remember { mutableStateOf<String?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when (permissionTarget) {
            "reminders" -> reminders = granted
            "lock" -> if (granted) {
                verse?.let { current ->
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            VerseNotifier.show(context, current.scripture, current.text, current.reference(), current.isDemo, launchCard = true)
                        }
                    }
                }
            } else scope.launch { snackbarHostState.showSnackbar("Allow notifications to also list the verse on the lock screen.") }
        }
        permissionTarget = null
    }

    val scripture = scriptureId
    val lifecycleOwner = LocalLifecycleOwner.current
    var settingsResume by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) settingsResume++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    suspend fun toggleFavorite(v: Verse) {
        val existing = dao.find(v.id)
        val nowFavorite = existing?.favorite != true
        if (existing != null) dao.setFavorite(v.id, nowFavorite)
        else dao.save(v.toCached(scripture, language, favorite = true, savedAt = 0L))
        snackbarHostState.showSnackbar(if (nowFavorite) "Saved verse" else "Removed from saved verses")
    }

    fun publishLockScreen(v: Verse, forceWallpaper: Boolean, launchCard: Boolean, announce: Boolean) {
        scope.launch {
            val message = withContext(Dispatchers.IO) {
                LockScreenVerse.applyIfEnabled(context, v.id, v.scripture, v.text, v.reference(), v.isDemo, forceWallpaper, launchCard)
            }
            if (announce && message != null) snackbarHostState.showSnackbar(message)
        }
    }

    suspend fun loadToday(random: Boolean = false) {
        loading = true; error = null
        try {
            val remote = if (random) ApiClient.api.random(religion, language, scriptureId)
                         else ApiClient.api.today()
            val old = dao.find(remote.id)
            dao.save(remote.toCached(scriptureId, language, old?.favorite == true))
            verse = remote
            ScriptureWidget().refreshAll(context)
            withContext(Dispatchers.IO) {
                LockScreenVerse.applyIfEnabled(context, remote.id, remote.scripture, remote.text, remote.reference(), remote.isDemo, forceWallpaper = false, launchCard = false)
            }
        } catch (_: Exception) {
            val local = dao.matching(scriptureId, language)
            if (local != null) verse = local.toVerse()
            else error = "You're offline. Connect once to download your first verse."
        } finally { loading = false }
    }

    suspend fun getToken(): String? =
        store.getString("bearer_token", null)
            ?: withContext(Dispatchers.IO) { refreshToken(context) }

    fun enableLockScreen() {
        lockScreen = true
        store.edit().putBoolean("lock_screen", true).apply()
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionTarget = "lock"
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        verse?.let { publishLockScreen(it, forceWallpaper = true, launchCard = !needsPermission, announce = true) }
            ?: scope.launch { snackbarHostState.showSnackbar("Open today's verse first, then show it on the lock screen.") }
    }

    fun disableLockScreen() {
        lockScreen = false
        store.edit().putBoolean("lock_screen", false).apply()
        scope.launch(Dispatchers.IO) {
            LockScreenVerse.clearWallpaper(context)
            if (!store.getBoolean("reminders", false)) VerseNotifier.cancel(context)
        }
    }

    LaunchedEffect(configured, religion, language, scriptureId, page) {
        if (configured && page == "Home") loadToday()
    }

    if (!configured) {
        if (!splashDone) {
            SplashScreen(onContinue = { splashDone = true })
            return
        }
        Onboarding(
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
                            .apply()
                        if (token != null) {
                            try {
                                ApiClient.api.savePreferences(
                                    PreferenceInput(religion, language, scriptureIdsStr, "daily", false)
                                )
                            } catch (_: Exception) {}
                        }
                        VerseSyncWorker.schedule(context, frequency)
                        configured = true
                    } else error = "Connect to the internet to load the selected scripture catalog."
                }
            }
        )
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
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_lumora_logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(22.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("WisdomOne", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Ink, letterSpacing = 0.3.sp)
                        Text("A little light, every day.", fontSize = 11.sp, color = InkSoft)
                    }
                }
                HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter), color = Amber.copy(alpha = 0.2f), thickness = 1.dp)
            }
        },
        bottomBar = {
            val navItems = listOf(
                Triple("Home", R.drawable.ic_nav_home, "Home"),
                Triple("Saved", R.drawable.ic_nav_saved, "Favorites"),
                Triple("Explore", R.drawable.ic_nav_explore, "Explore"),
                Triple("Settings", R.drawable.ic_nav_settings, "Settings")
            )
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.border(BorderStroke(1.dp, Amber.copy(alpha = 0.15f)), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                navItems.forEach { (label, iconRes, route) ->
                    val selected = page == route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { page = route },
                        icon = { Icon(painter = painterResource(iconRes), contentDescription = label, modifier = Modifier.size(22.dp), tint = if (selected) Amber else InkSoft) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Amber else InkSoft) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Amber, selectedTextColor = Amber, unselectedIconColor = InkSoft, unselectedTextColor = InkSoft, indicatorColor = Amber.copy(alpha = 0.12f))
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (page) {
                "Home" -> HomeScreen(
                    verse, loading, error, favoriteIds.contains(verse?.id), lockScreen,
                    onRandom = { scope.launch { loadToday(true) } },
                    onFavorite = { verse?.let { v -> scope.launch { toggleFavorite(v) } } },
                    onShare = { verse?.let { v -> val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${v.text}\n\n${v.scripture} · ${v.reference()}"); context.startActivity(Intent.createChooser(send, "Share verse")) } },
                    onShowLockScreen = { enableLockScreen() },
                    onHideLockScreen = { disableLockScreen() }
                )
                "Favorites" -> FavoritesScreen(
                    savedVerses,
                    onOpen = { cached -> verse = cached.toVerse(); page = "Home" },
                    onRemove = { cached -> scope.launch { dao.setFavorite(cached.id, false) } }
                )
                "Explore" -> ExploreScreen(
                    query, { query = it }, results, loading, favoriteIds,
                    onSearch = { scope.launch { loading = true; results = try { ApiClient.api.search(query) } catch (_: Exception) { emptyList() }; loading = false } },
                    onFavorite = { v -> scope.launch { toggleFavorite(v) } },
                    onQueryChange = { query = it; if (it.isEmpty()) results = emptyList() }
                )
                else -> SettingsScreen(
                    religion, language, frequency, reminders, lockScreen,
                    religions, languages, settingsResume, error,
                    onReligion = { religion = it },
                    onLanguage = { language = it },
                    onFrequency = { frequency = it },
                    onReminders = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33) {
                            permissionTarget = "reminders"
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else reminders = enabled
                    },
                    onLockScreen = { enabled -> if (enabled) enableLockScreen() else disableLockScreen() },
                    onSave = {
                        scope.launch {
                            val allScriptures = try { ApiClient.api.scriptures(religion) } catch (_: Exception) { emptyList() }
                            val selected = allScriptures.firstOrNull()
                            if (selected != null) {
                                scriptureId = selected.id
                                val scriptureIdsStr = allScriptures.joinToString(",") { it.id.toString() }
                                store.edit()
                                    .putInt("religion", religion).putInt("language", language)
                                    .putInt("scripture", selected.id).putString("frequency", frequency)
                                    .putBoolean("reminders", reminders).putBoolean("lock_screen", lockScreen)
                                    .apply()
                                // Sync to server best-effort
                        val token = getToken()
                                if (token != null) {
                                    try {
                                        val freqApi = when (frequency) { "Every 6 hours" -> "6h"; "Every hour" -> "1h"; else -> "daily" }
                                        ApiClient.api.savePreferences(PreferenceInput(religion, language, scriptureIdsStr, freqApi, reminders))
                                    } catch (_: Exception) {}
                                }
                                VerseSyncWorker.schedule(context, frequency)
                                val current = verse
                                withContext(Dispatchers.IO) {
                                    if (!lockScreen) LockScreenVerse.clearWallpaper(context)
                                    if (current != null && (lockScreen || reminders)) {
                                        LockScreenVerse.applyIfEnabled(context, current.id, current.scripture, current.text, current.reference(), current.isDemo, forceWallpaper = lockScreen, launchCard = false)
                                    } else VerseNotifier.cancel(context)
                                }
                                page = "Home"
                            } else error = "Couldn't reach the scripture catalog. Your saved settings were not changed."
                        }
                    },
                    onFeedback = { showFeedback = true }
                )
            }
        }
    }

    if (showFeedback) {
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
                    painter = painterResource(R.drawable.ic_lumora_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(44.dp))
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
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(0.65f).height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink)) {
                        Text("Try again", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun SplashScreen(onContinue: () -> Unit) {
    val logoAlpha = remember { Animatable(0f) }
    val logoOffset = remember { Animatable(40f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffset = remember { Animatable(24f) }
    val btnAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        kotlinx.coroutines.coroutineScope {
            launch { logoAlpha.animateTo(1f, tween(700, easing = EaseOutCubic)) }
            launch { logoOffset.animateTo(0f, tween(700, easing = EaseOutCubic)) }
        }
        kotlinx.coroutines.delay(200)
        kotlinx.coroutines.coroutineScope {
            launch { textAlpha.animateTo(1f, tween(600, easing = EaseOutCubic)) }
            launch { textOffset.animateTo(0f, tween(600, easing = EaseOutCubic)) }
        }
        kotlinx.coroutines.delay(400)
        btnAlpha.animateTo(1f, tween(500, easing = EaseOutCubic))
    }

    Box(Modifier.fillMaxSize().background(Parchment)) {
        Box(Modifier.size(320.dp).offset(x = 120.dp, y = (-80).dp).clip(CircleShape).background(Brush.radialGradient(listOf(Amber.copy(alpha = 0.18f), Color.Transparent))))
        Box(Modifier.size(260.dp).align(Alignment.BottomStart).offset(x = (-60).dp, y = 60.dp).clip(CircleShape).background(Brush.radialGradient(listOf(AmberLight.copy(alpha = 0.14f), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(96.dp).graphicsLayer { alpha = logoAlpha.value; translationY = logoOffset.value }
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_lumora_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(96.dp).clip(RoundedCornerShape(48.dp))
                )
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.graphicsLayer { alpha = textAlpha.value; translationY = textOffset.value }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("A quieter moment,", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center, lineHeight = 42.sp)
                Text("whenever you need it.", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Purple, textAlign = TextAlign.Center, lineHeight = 42.sp)
                Spacer(Modifier.height(16.dp))
                Text("Daily wisdom from the world's\ngreatest scriptures.", fontSize = 16.sp, color = InkSoft, textAlign = TextAlign.Center, lineHeight = 24.sp)
            }
            Spacer(Modifier.height(56.dp))
            Box(Modifier.graphicsLayer { alpha = btnAlpha.value }) {
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink)) {
                    Text("Begin your journey", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.graphicsLayer { alpha = btnAlpha.value }) {
                Text("Free · No account needed", fontSize = 12.sp, color = InkSoft, textAlign = TextAlign.Center)
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
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (selected) Brush.linearGradient(listOf(Amber.copy(alpha = 0.18f), AmberLight.copy(alpha = 0.08f))) else Brush.linearGradient(listOf(Color.White, Color.White)))
                        .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Amber else Amber.copy(alpha = 0.15f)), RoundedCornerShape(18.dp))
                        .clickable { onReligion(item.id) }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = if (selected) Ink else InkMid)
                            if (scriptureNames.isNotEmpty()) Text(scriptureNames, fontSize = 13.sp, color = if (selected) Amber else InkSoft)
                        }
                        Box(
                            Modifier.size(22.dp).clip(CircleShape)
                                .background(if (selected) Amber else Color.Transparent)
                                .border(BorderStroke(1.5.dp, if (selected) Amber else Amber.copy(alpha = 0.3f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("LANGUAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkSoft, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(10.dp))

        if (catalogLoading) {
            Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Amber, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                languages.forEach { item ->
                    val selected = language == item.id
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Amber else Color.White)
                            .border(BorderStroke(1.5.dp, if (selected) Amber else Amber.copy(alpha = 0.2f)), RoundedCornerShape(14.dp))
                            .clickable { onLanguage(item.id) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.name, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 15.sp, color = if (selected) Color.White else InkMid)
                    }
                }
            }
        }

        if (notice != null) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF9C463D).copy(alpha = 0.08f)).padding(14.dp)) {
                Text(notice, color = Color(0xFF9C463D), fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            enabled = !catalogLoading && religion != 0 && language != 0,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink)
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
private fun HomeScreen(
    verse: Verse?, loading: Boolean, error: String?, favorite: Boolean, lockScreen: Boolean,
    onRandom: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit,
    onShowLockScreen: () -> Unit, onHideLockScreen: () -> Unit
) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(260.dp).background(Brush.verticalGradient(listOf(Amber.copy(alpha = 0.07f), Color.Transparent))))
        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            val today = remember { java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault()).format(java.util.Date()) }
            Row(Modifier.clip(RoundedCornerShape(50)).background(Amber.copy(alpha = 0.1f)).padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Amber, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(today, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium)
            }
            if (loading && verse == null) {
                Box(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(28.dp)).background(Amber.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = Amber, modifier = Modifier.size(36.dp))
                        Text("Loading today's verse…", color = InkSoft, fontSize = 13.sp)
                    }
                }
            }
            if (error != null && verse == null) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF9C463D).copy(alpha = 0.1f)).padding(20.dp)) {
                    Text(error, color = Color(0xFF9C463D), fontSize = 14.sp)
                }
            }
            if (verse != null) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
                    Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color(0xFF1A1025), Color(0xFF2D1B4E), Color(0xFF1A1025)))))
                    Box(Modifier.size(280.dp).align(Alignment.BottomStart).offset(x = (-60).dp, y = 60.dp).background(Brush.radialGradient(listOf(Gold.copy(alpha = 0.22f), Color.Transparent))))
                    Box(Modifier.size(180.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp).background(Brush.radialGradient(listOf(PurpleMid.copy(alpha = 0.3f), Color.Transparent))))
                    Box(Modifier.size(120.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-30).dp).clip(CircleShape).border(BorderStroke(1.dp, Amber.copy(alpha = 0.12f)), CircleShape))
                    Box(Modifier.size(80.dp).align(Alignment.BottomStart).offset(x = (-20).dp, y = 20.dp).clip(CircleShape).border(BorderStroke(1.dp, Amber.copy(alpha = 0.1f)), CircleShape))
                    Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter).offset(y = 56.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Amber.copy(alpha = 0.25f), Amber.copy(alpha = 0.25f), Color.Transparent))))
                    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Amber.copy(alpha = 0.18f)).border(BorderStroke(1.dp, Amber.copy(alpha = 0.3f)), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text(verse.scripture.uppercase(), color = Amber, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Amber.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
                        Text("\u201c${verse.text}\u201d", color = Color(0xFFF5F0E8), fontSize = 22.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal)
                        Box(Modifier.fillMaxWidth(0.3f).height(1.dp).background(Brush.horizontalGradient(listOf(Amber.copy(alpha = 0.5f), Color.Transparent))))
                        Text(verse.reference(), color = Color(0xFF8A8078), fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val favBg by animateColorAsState(if (favorite) Amber else Color.Transparent, tween(300), label = "favBg")
                    val favContent by animateColorAsState(if (favorite) Color.White else Ink, tween(300), label = "favContent")
                    Button(onClick = onFavorite, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = favBg, contentColor = favContent), border = BorderStroke(1.5.dp, Amber.copy(alpha = 0.5f))) {
                        Icon(if (favorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (favorite) "Saved" else "Save", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, Amber.copy(alpha = 0.5f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (lockScreen) Amber.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface).border(1.dp, if (lockScreen) Amber.copy(alpha = 0.4f) else Amber.copy(alpha = 0.1f), RoundedCornerShape(20.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(if (lockScreen) Amber.copy(alpha = 0.2f) else Amber.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                            Icon(if (lockScreen) Icons.Outlined.Lock else Icons.Outlined.LockOpen, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (lockScreen) "Showing on lock screen" else "Lock screen verse", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (lockScreen) Amber else Ink)
                            Text(if (lockScreen) "Tap to remove" else "Display this verse on your lock screen", fontSize = 12.sp, color = InkSoft)
                        }
                        Switch(checked = lockScreen, onCheckedChange = { if (it) onShowLockScreen() else onHideLockScreen() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Amber))
                    }
                }
            }
            Button(onClick = onRandom, enabled = !loading, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink)) {
                if (loading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Finding a verse…" else "Discover another verse", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Text("Take a breath. Carry the thought with you.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = InkSoft, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FavoritesScreen(items: List<CachedVerse>, onOpen: (CachedVerse) -> Unit, onRemove: (CachedVerse) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Amber.copy(alpha = 0.06f), Color.Transparent))).padding(horizontal = 20.dp, vertical = 20.dp)) {
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
                items(items) { item ->
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, Amber.copy(alpha = 0.12f), RoundedCornerShape(20.dp))) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(4.dp).background(Brush.horizontalGradient(listOf(Amber, AmberLight))))
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Amber.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text(item.scripture.uppercase(), color = Amber, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(item.reference(), color = InkSoft, fontSize = 11.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier.size(26.dp).clip(RoundedCornerShape(7.dp))
                                            .background(Color(0xFF9C463D).copy(alpha = 0.08f))
                                            .border(BorderStroke(1.dp, Color(0xFF9C463D).copy(alpha = 0.3f)), RoundedCornerShape(7.dp))
                                            .clickable { onRemove(item) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("\u00d7", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9C463D), lineHeight = 15.sp)
                                    }
                                }
                                Text("\u201c${item.text}\u201d", fontSize = 15.sp, lineHeight = 23.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 4, overflow = TextOverflow.Ellipsis)
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
private fun ExploreScreen(query: String, onQuery: (String) -> Unit, results: List<Verse>, loading: Boolean, favoriteIds: Set<Int>, onSearch: () -> Unit, onFavorite: (Verse) -> Unit, onQueryChange: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Amber.copy(alpha = 0.06f), Color.Transparent))).padding(horizontal = 20.dp, vertical = 20.dp)) {
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
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )
            if (query.length >= 2) {
                IconButton(onClick = onSearch, enabled = !loading, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Amber)) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Amber.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text(v.scripture.uppercase(), color = Amber, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(v.reference(), color = InkSoft, fontSize = 11.sp)
                                }
                                Text("\u201c${v.text}\u201d", fontSize = 15.sp, lineHeight = 23.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    val saveBg by animateColorAsState(if (saved) Amber else Color.Transparent, tween(250), label = "saveBg")
                                    val saveContent by animateColorAsState(if (saved) Color.White else Ink, tween(250), label = "saveContent")
                                    Button(onClick = { onFavorite(v) }, modifier = Modifier.height(34.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = saveBg, contentColor = saveContent), border = BorderStroke(1.dp, Amber.copy(alpha = 0.5f)), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)) {
                                        Icon(if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (saved) "Saved" else "Save", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
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
private fun SettingsScreen(
    religion: Int, language: Int, frequency: String, reminders: Boolean, lockScreen: Boolean,
    religions: List<ReligionItem>, languages: List<CatalogItem>,
    resumeTick: Int, notice: String?,
    onReligion: (Int) -> Unit, onLanguage: (Int) -> Unit, onFrequency: (String) -> Unit,
    onReminders: (Boolean) -> Unit, onLockScreen: (Boolean) -> Unit, onSave: () -> Unit,
    onFeedback: () -> Unit
) {
    val context = LocalContext.current
    val wallpaperAllowed = remember(resumeTick) { LockScreenVerse.canSetLockWallpaper(context) }
    val lockCardAllowed = remember(resumeTick) { LockScreenVerse.canPostLockCard(context) }
    val pinWidgetSupported = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Amber.copy(alpha = 0.06f), Color.Transparent))).padding(horizontal = 20.dp, vertical = 20.dp)) {
            Column {
                Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Personalise your experience", fontSize = 13.sp, color = InkSoft)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSectionLabel("YOUR TRADITION")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                religions.forEach { item ->
                    val selected = religion == item.id
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Amber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Amber else Amber.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                            .clickable { onReligion(item.id) }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = { onReligion(item.id) }, colors = RadioButtonDefaults.colors(selectedColor = Amber))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Ink else InkMid)
                                val scriptureNames = item.scriptures.joinToString(", ") { it.name }
                                if (scriptureNames.isNotEmpty()) Text(scriptureNames, fontSize = 12.sp, color = InkSoft)
                            }
                        }
                    }
                }
            }

            SettingsSectionLabel("LANGUAGE")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                languages.forEach { item ->
                    val selected = language == item.id
                    Button(
                        onClick = { onLanguage(item.id) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (selected) Amber else MaterialTheme.colorScheme.surface, contentColor = if (selected) Color.White else InkMid),
                        border = BorderStroke(1.5.dp, if (selected) Amber else Amber.copy(alpha = 0.2f))
                    ) { Text(item.name, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 15.sp) }
                }
            }

            SettingsSectionLabel("VERSE FREQUENCY")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Daily", "Every 6 hours", "Every hour").forEach { label ->
                    val selected = frequency == label
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Amber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Amber else Amber.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                            .clickable { onFrequency(label) }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = { onFrequency(label) }, colors = RadioButtonDefaults.colors(selectedColor = Amber))
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Ink else InkMid)
                        }
                    }
                }
            }

            SettingsSectionLabel("NOTIFICATIONS & WIDGET")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, Amber.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                ) {
                    SettingsToggleRow("Verse reminders", "Posts the verse in lock screen notifications.", reminders, onReminders)
                }
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, Amber.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                ) {
                    SettingsToggleRow("Lock screen verse", "Places today's verse on the lock screen wallpaper.", lockScreen, onLockScreen)
                }
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, Amber.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Home screen widget", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
                            Text(
                                if (pinWidgetSupported) "Tap to add the verse widget to your home screen."
                                else "Long-press your home screen → Widgets to add manually.",
                                fontSize = 12.sp, color = InkSoft
                            )
                        }
                        if (pinWidgetSupported) {
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    val provider = ComponentName(context, ScriptureWidgetReceiver::class.java)
                                    AppWidgetManager.getInstance(context).requestPinAppWidget(provider, null, null)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }


            if (!wallpaperAllowed) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = 0.08f)).padding(14.dp)) {
                    Text("This phone is blocking lock screen wallpaper changes. The verse can still appear as a lock screen notification.", fontSize = 12.sp, color = Color(0xFF8B6914))
                }
            }

            SettingsSectionLabel("SYSTEM")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, Amber.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                ) {
                    SettingsLinkRow("Notification settings") { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) }) }
                }
                if (Build.VERSION.SDK_INT >= 34 && !lockCardAllowed) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(1.dp, Amber.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                    ) {
                        SettingsLinkRow("Allow verse card on lock screen") { context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply { data = Uri.parse("package:${context.packageName}") }) }
                    }
                }
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, Amber.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                ) {
                    SettingsLinkRow("Battery settings") { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                }
            }

            if (notice != null) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF9C463D).copy(alpha = 0.08f)).padding(14.dp)) {
                    Text(notice, color = Color(0xFF9C463D), fontSize = 13.sp)
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink)
            ) {
                Text("Save preferences", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = Amber.copy(alpha = 0.12f)
            )

            OutlinedButton(
                onClick = onFeedback,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, Amber.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber)
            ) {
                Text("Leave us a feedback", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(16.dp))
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
                    Text("Bad", fontSize = 12.sp, color = Color(0xFFB85C38))
                    Text("Average", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.SemiBold)
                    Text("Good", fontSize = 12.sp, color = Color(0xFF4A8C5C))
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

@Composable
private fun SettingsSectionLabel(label: String) {
    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkSoft, letterSpacing = 1.4.sp)
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
            Text(subtitle, fontSize = 12.sp, color = InkSoft)
        }
        Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Amber))
    }
}

@Composable
private fun SettingsLinkRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = Amber)
        Icon(Icons.Outlined.Share, contentDescription = null, tint = Amber.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}
