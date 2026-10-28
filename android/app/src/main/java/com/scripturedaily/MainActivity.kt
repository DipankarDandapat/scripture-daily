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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.aspectRatio
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
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.AuthInput
import com.scripturedaily.data.CachedCatalogItem
import com.scripturedaily.data.CachedVerse
import com.scripturedaily.data.CatalogItem
import com.scripturedaily.data.DeviceRegInput
import com.scripturedaily.data.FavoriteInput
import com.scripturedaily.data.FeedbackInput
import com.scripturedaily.data.PreferenceInput
import com.scripturedaily.data.PaletteRefreshWorker
import com.scripturedaily.data.ReligionItem
import com.scripturedaily.data.ScriptureDatabase
import com.scripturedaily.data.ScriptureItem
import com.scripturedaily.data.Verse
import com.scripturedaily.data.VerseSyncWorker
import com.scripturedaily.data.toCached
import com.scripturedaily.lockscreen.LockScreenVerse
import com.scripturedaily.lockscreentime.TimePalette
import com.scripturedaily.notification.VerseNotifier
import com.scripturedaily.widget.ScriptureWidget
import com.scripturedaily.widget.ScriptureWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
// ── Force-update screen ──────────────────────────────────────────────────────
@Composable
private fun ForceUpdateScreen() {
    val context = LocalContext.current
    Box(
        Modifier.fillMaxSize().background(Parchment),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(320.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Amber.copy(alpha = 0.15f), Color.Transparent))))
        Column(
            Modifier.padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                Modifier.size(88.dp).clip(CircleShape)
                    .background(Amber.copy(alpha = 0.12f))
                    .border(BorderStroke(1.5.dp, Amber.copy(alpha = 0.3f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("↑", fontSize = 38.sp, color = Amber, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Update Required",
                fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = Ink, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "A new version of WisdomOne is available. Please update to continue.",
                fontSize = 15.sp, color = InkSoft,
                textAlign = TextAlign.Center, lineHeight = 23.sp
            )
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = {
                    val uri = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink)
            ) {
                Text("Update Now", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
        val deepLinkVerseId = intent?.getIntExtra("verse_id", -1)?.takeIf { it > 0 }
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
            MaterialTheme(colorScheme = colors) { ScriptureApp(deepLinkVerseId) }
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

// ── Share image: render a styled verse card bitmap and share it ──────────────
private fun shareVerseImage(context: android.content.Context, verse: Verse) {
    val w = 1080; val h = 1080
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    // Background gradient (dark purple)
    val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#1A1025") }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
    // Accent bar
    val barPaint = Paint().apply { color = android.graphics.Color.parseColor("#D4A574"); strokeWidth = 6f; style = Paint.Style.FILL }
    canvas.drawRect(80f, 120f, 86f, 280f, barPaint)
    // Scripture label
    val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#D4A574")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 36f; letterSpacing = 0.12f
    }
    canvas.drawText(verse.scripture.uppercase(), 110f, 175f, labelPaint)
    // Verse text
    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F5F0E8")
        typeface = Typeface.DEFAULT; textSize = 52f
    }
    val bodyLayout = StaticLayout.Builder.obtain("“${verse.text}”", 0, verse.text.length + 2, bodyPaint, w - 160)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.2f)
        .setMaxLines(12).setEllipsize(TextUtils.TruncateAt.END).setIncludePad(false).build()
    canvas.save(); canvas.translate(80f, 320f); bodyLayout.draw(canvas); canvas.restore()
    // Reference
    val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#8A8078"); typeface = Typeface.DEFAULT; textSize = 36f
    }
    canvas.drawText(verse.reference(), 80f, h - 140f, refPaint)
    // Watermark
    val wmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#D4A574"); typeface = Typeface.DEFAULT; textSize = 30f
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("WisdomOne", w - 80f, h - 80f, wmPaint)

    val file = File(context.cacheDir, "verse_share.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
    bitmap.recycle()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "${verse.text}\n\n${verse.scripture} · ${verse.reference()}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share verse"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptureApp(deepLinkVerseId: Int? = null) {
    val context = LocalContext.current
    val store = remember { context.getSharedPreferences("reader", 0) }
    val dao = remember { ScriptureDatabase.get(context).verses() }
    val catalogDao = remember { ScriptureDatabase.get(context).catalog() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Force-update check ───────────────────────────────────────────────────
    var forceUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val latest = ForceUpdateManager.fetchLatestVersion()
        forceUpdate = ForceUpdateManager.isUpdateRequired(BuildConfig.VERSION_NAME, latest)
    }

    if (forceUpdate) {
        ForceUpdateScreen()
        return
    }

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
    var paletteIntervalHours by remember {
        mutableIntStateOf(store.getInt("palette_interval_hours", 6).takeIf { it in setOf(1, 6, 12, 24) } ?: 6)
    }
    var configured by remember { mutableStateOf(store.getBoolean("configured", false)) }
    var setupGuidePending by remember { mutableStateOf(store.getBoolean("setup_guide_pending", false)) }
    var splashDone by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("Home") }
    var showFeedback by remember { mutableStateOf(false) }
    var verse by remember { mutableStateOf<Verse?>(null) }
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
    var streak by remember { mutableIntStateOf(store.getInt("streak", 0)) }

    // ── Single sequential startup flow ─────────────────────────────────────────
    LaunchedEffect(Unit) {
        PaletteRefreshWorker.schedule(context)
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
                    val freqLocal = when (p.frequency) { "6h" -> "Every 6 hours"; "1h" -> "Every hour"; else -> "Daily" }
                    val firstScriptureId = p.scriptureIds.split(",").firstOrNull()?.trim()?.toIntOrNull() ?: scriptureId
                    religion = p.religionId; language = p.languageId; scriptureId = firstScriptureId; frequency = freqLocal
                    store.edit().putInt("religion", p.religionId).putInt("language", p.languageId)
                        .putInt("scripture", firstScriptureId).putString("frequency", freqLocal).apply()
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
        }
    }

    // Deep-link: widget/notification tap with a specific verse_id
    LaunchedEffect(deepLinkVerseId) {
        if (deepLinkVerseId != null && configured) {
            val cached = withContext(Dispatchers.IO) { dao.find(deepLinkVerseId) }
            if (cached != null) {
                verse = cached.toVerse()
                page = "Home"
            } else {
                try {
                    val remote = withContext(Dispatchers.IO) { ApiClient.api.random(religion, language, scriptureId) }
                    // fetch specific verse via search isn't available, use the cached or skip
                    verse = remote; page = "Home"
                } catch (_: Exception) {}
            }
        }
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
                reminders = true
                store.edit().putBoolean("reminders", true).apply()
                VerseSyncWorker.schedule(context, frequency)
            }
            "lock" -> {
                if (granted) {
                    scope.launch {
                        applyCurrentLockVerse()?.let { snackbarHostState.showSnackbar(it) }
                    }
                } else {
                    scope.launch {
                        VerseNotifier.cancel(context)
                        snackbarHostState.showSnackbar("The lock-screen wallpaper is still active. Allow notifications to also receive verse notifications.")
                    }
                }
            }
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
        // Optimistic local update
        if (existing != null) dao.setFavorite(v.id, nowFavorite)
        else dao.save(v.toCached(scripture, language, favorite = true, savedAt = 0L))
        // Sync to server best-effort
        try {
            if (nowFavorite) ApiClient.api.addFavorite(FavoriteInput(v.id))
            else ApiClient.api.removeFavorite(v.id)
        } catch (_: Exception) {}
        snackbarHostState.showSnackbar(if (nowFavorite) "Saved verse" else "Removed from saved verses")
    }

    suspend fun loadToday(random: Boolean = false) {
        loading = true; error = null
        try {
            val remote = if (random) ApiClient.api.random(religion, language, scriptureId)
                         else ApiClient.api.today()
            val old = dao.find(remote.id)
            dao.save(remote.toCached(scriptureId, language, old?.favorite == true))
            verse = remote
            if (!random) {
                val today = java.time.LocalDate.now().toString()
                val last = store.getString("last_streak_date", "")
                val yesterday = java.time.LocalDate.now().minusDays(1).toString()
                val newStreak = when (last) {
                    today -> store.getInt("streak", 1)
                    yesterday -> store.getInt("streak", 0) + 1
                    else -> 1
                }
                store.edit().putString("last_streak_date", today).putInt("streak", newStreak)
                    .putBoolean("read_$today", true).apply()
                streak = newStreak
            }
            ScriptureWidget().refreshAll(context)
            withContext(Dispatchers.IO) {
                LockScreenVerse.applyIfEnabled(context, remote.id, remote.scripture, remote.text, remote.reference())
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
        scope.launch {
            val message = applyCurrentLockVerse(forceWallpaper = true)
            if (message?.startsWith("No verse") == true) {
                snackbarHostState.showSnackbar(message)
                return@launch
            }
            if (needsPermission) {
                permissionTarget = "lock"
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else message?.let { snackbarHostState.showSnackbar(it) }
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

    LaunchedEffect(homeLoadKey) {
        if (configured && page == "Home") loadToday()
    }

    // Reload verse when user taps Home tab from another tab (not from Save)
    var prevPage by remember { mutableStateOf("Home") }
    LaunchedEffect(page) {
        if (page == "Home" && prevPage != "Home" && configured) loadToday()
        prevPage = page
    }

    if (!configured) {
        ResponsiveContentFrame {
            if (!splashDone) {
                SplashScreen(onContinue = { splashDone = true })
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
                                    PreferenceInput(religion, language, scriptureIdsStr, "daily", false)
                                )
                            } catch (_: Exception) {}
                        }
                        VerseSyncWorker.schedule(context, frequency)
                        PaletteRefreshWorker.schedule(context)
                        configured = true
                        setupGuidePending = true
                        homeLoadKey++
                    } else error = "Connect to the internet to load the selected scripture catalog."
                }
            }
            )
        }
        return
    }

    val navItems = listOf(
        Triple("Home", R.drawable.ic_nav_home, "Home"),
        Triple("Saved", R.drawable.ic_nav_saved, "Favorites"),
        Triple("Explore", R.drawable.ic_nav_explore, "Explore"),
        Triple("Settings", R.drawable.ic_nav_settings, "Settings")
    )
    val useNavigationRail = LocalConfiguration.current.screenWidthDp >= 840

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
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(3.dp).clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("WisdomOne", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Ink, letterSpacing = 0.3.sp)
                    }
                }
                HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter), color = Amber.copy(alpha = 0.2f), thickness = 1.dp)
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
                            onClick = { page = route },
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
                            onClick = { page = route },
                            icon = { Icon(painter = painterResource(iconRes), contentDescription = label, modifier = Modifier.size(22.dp), tint = if (selected) Amber else InkSoft) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Amber else InkSoft) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth()) {
                    if (setupGuidePending) {
                        FirstRunSetupScreen(
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionTarget = "setup"
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            reminders = true
                            store.edit().putBoolean("reminders", true).apply()
                            VerseSyncWorker.schedule(context, frequency)
                            scope.launch { snackbarHostState.showSnackbar("Verse reminders are enabled.") }
                        }
                    },
                    onOpenNotificationSettings = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        })
                    },
                    onAddWidget = {
                        val manager = AppWidgetManager.getInstance(context)
                        if (manager.isRequestPinAppWidgetSupported) {
                            manager.requestPinAppWidget(ComponentName(context, ScriptureWidgetReceiver::class.java), null, null)
                        } else scope.launch { snackbarHostState.showSnackbar("Long-press your home screen, open Widgets, and add Scripture Daily.") }
                    },
                    onFinish = {
                        store.edit().putBoolean("setup_guide_pending", false).apply()
                        setupGuidePending = false
                    }
                        )
                    } else when (page) {
                "Home" -> HomeScreen(
                    verse, loading, error, favoriteIds.contains(verse?.id), lockScreen,
                    onRandom = { scope.launch { loadToday(true) } },
                    onFavorite = { verse?.let { v -> scope.launch { toggleFavorite(v) } } },
                    onShare = { verse?.let { v -> shareVerseImage(context, v) } },
                    onShareText = { verse?.let { v ->
                        val formatted = "❝ ${v.text} ❞\n\n— ${v.scripture} · ${v.reference()}\n\nvia WisdomOne"
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, formatted)
                        context.startActivity(Intent.createChooser(send, "Share verse"))
                    }},
                    onShowLockScreen = { enableLockScreen() },
                    onHideLockScreen = { disableLockScreen() },
                    onHistory = {
                        scope.launch {
                            try { historyVerses = ApiClient.api.todayHistory() } catch (_: Exception) {}
                            showHistory = true
                        }
                    },
                    onChapterBrowser = { showChapterBrowser = true },
                    fontSize = fontSize,
                    streak = streak,
                    onCopy = { verse?.let { v ->
                        val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("verse", v.text))
                        scope.launch { snackbarHostState.showSnackbar("Verse copied to clipboard") }
                    }},
                    onTheme = { theme ->
                        scope.launch {
                            loading = true
                            try {
                                val hits = ApiClient.api.search(theme, language, scriptureId)
                                val pick = hits.randomOrNull()
                                if (pick != null) {
                                    val old = dao.find(pick.id)
                                    dao.save(pick.toCached(scriptureId, language, old?.favorite == true))
                                    verse = pick
                                }
                            } catch (_: Exception) {}
                            loading = false
                        }
                    }
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
                else -> SettingsScreen(
                    religion, language, frequency, reminders, lockScreen, paletteIntervalHours, fontSize,
                    religions, languages, settingsResume, error,
                    onReligion = { religion = it },
                    onLanguage = { language = it },
                    onFrequency = { frequency = it },
                    onPaletteInterval = { hours ->
                        paletteIntervalHours = hours
                        store.edit().putInt("palette_interval_hours", hours).apply()
                        PaletteRefreshWorker.schedule(context)
                    },
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
                                    .putInt("palette_interval_hours", paletteIntervalHours)
                                    .putBoolean("reminders", reminders).putBoolean("lock_screen", lockScreen)
                                    .apply()
                                val token = getToken()
                                if (token != null) {
                                    try {
                                        val freqApi = when (frequency) { "Every 6 hours" -> "6h"; "Every hour" -> "1h"; else -> "daily" }
                                        ApiClient.api.savePreferences(PreferenceInput(religion, language, scriptureIdsStr, freqApi, reminders))
                                    } catch (_: Exception) {}
                                }
                                VerseSyncWorker.schedule(context, frequency)
                                PaletteRefreshWorker.schedule(context)
                                val current = verse
                                withContext(Dispatchers.IO) {
                                    if (current != null && (lockScreen || reminders)) {
                                        LockScreenVerse.applyIfEnabled(
                                            context, current.id, current.scripture, current.text, current.reference(), forceWallpaper = lockScreen
                                        )
                                    } else VerseNotifier.cancel(context)
                                }
                                snackbarHostState.showSnackbar("Preferences saved")
                                page = "Home"
                                homeLoadKey++
                            } else {
                                error = "Couldn't reach the scripture catalog. Your saved settings were not changed."
                                snackbarHostState.showSnackbar("Couldn't save preferences. Please try again.")
                            }
                        }
                    },
                        onFontSize = { sz -> fontSize = sz; store.edit().putInt("font_size", sz).apply() },
                        onFeedback = { showFeedback = true }
                    )
                    }
                }
            }
        }
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
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp).clip(CircleShape)
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
                    painter = painterResource(R.drawable.ic_wisdomone_ui_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp).clip(CircleShape)
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
        }
    }
}

@Composable
private fun FirstRunSetupScreen(
    onRequestNotifications: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onAddWidget: () -> Unit,
    onFinish: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val titles = listOf("Stay in the moment", "Choose what appears", "Keep a verse close")
    val descriptions = listOf(
        "Allow notifications if you want a verse reminder. You can skip this and enable it later in Settings.",
        "Enabling Lock screen verse sets a static verse image and posts a notification. Android controls notification privacy, and its static wallpaper may remain after uninstall—turn the feature off first to clear it. You can skip this and change it later.",
        "Add the Scripture Daily home-screen widget from here, or skip and add it later by long-pressing an empty area of your home screen. Lock-screen widgets are available only on supported Android devices."
    )
    Column(
        Modifier.fillMaxSize().background(Parchment).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Optional setup · ${step + 1} of 3", fontSize = 12.sp, color = InkSoft, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(3) { index ->
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(4.dp)).background(if (index <= step) Amber else Amber.copy(alpha = 0.2f)))
            }
        }
        Spacer(Modifier.height(30.dp))
        Text(titles[step], fontSize = 29.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(12.dp))
        Text(descriptions[step], fontSize = 15.sp, lineHeight = 23.sp, color = InkSoft)
        Spacer(Modifier.height(26.dp))
        when (step) {
            0 -> OutlinedButton(onClick = onRequestNotifications, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) {
                Text("Allow notifications", color = Purple, fontWeight = FontWeight.SemiBold)
            }
            1 -> OutlinedButton(onClick = onOpenNotificationSettings, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) {
                Text("Open notification settings", color = Purple, fontWeight = FontWeight.SemiBold)
            }
            else -> OutlinedButton(onClick = onAddWidget, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) {
                Text("Add home-screen widget", color = Purple, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { if (step < 2) step++ else onFinish() },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink)
        ) { Text(if (step < 2) "Next" else "Finish", fontWeight = FontWeight.SemiBold) }
        TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Skip setup", color = InkSoft)
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
                    "hinduism"       -> R.drawable.tradition_hinduism
                    "christianity"   -> R.drawable.tradition_christianity
                    "islam"          -> R.drawable.tradition_islam
                    "buddhism"       -> R.drawable.tradition_buddhism
                    else             -> R.drawable.tradition_general_wisdom
                }
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (selected) Amber.copy(alpha = 0.07f) else Color.White)
                        .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Amber else Color(0xFFE8E0D8)), RoundedCornerShape(16.dp))
                        .clickable { onReligion(item.id); reloadLanguages(item.id) }
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Amber.copy(alpha = 0.15f) else Color(0xFFF0EBE3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(traditionIconRes), contentDescription = null, tint = if (selected) Amber else Color(0xFF8A7A6A), modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 15.sp, color = if (selected) Ink else InkMid)
                            if (scriptureNames.isNotEmpty()) Text(scriptureNames, fontSize = 12.sp, color = InkSoft)
                        }
                        Box(
                            Modifier.size(22.dp).clip(CircleShape)
                                .background(if (selected) Amber else Color.Transparent)
                                .border(BorderStroke(1.5.dp, if (selected) Amber else Color(0xFFCCC4D8)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(R.drawable.chevron_right),
                            contentDescription = null,
                            tint = if (selected) Amber else InkSoft.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = if (selected) 90f else 0f }
                        )
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                localLanguages.forEach { item ->
                    val selected = language == item.id
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Amber.copy(alpha = 0.08f) else Color.White)
                            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Amber else Color(0xFFE8E0D8)), RoundedCornerShape(14.dp))
                            .clickable { onLanguage(item.id) }
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(18.dp).clip(CircleShape)
                                    .background(if (selected) Amber else Color.Transparent)
                                    .border(BorderStroke(1.5.dp, if (selected) Amber else Color(0xFFCCC4D8)), CircleShape),
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
            enabled = !catalogLoading && !langLoading && religion != 0 && language != 0,
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
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeScreen(
    verse: Verse?, loading: Boolean, error: String?, favorite: Boolean, lockScreen: Boolean,
    fontSize: Int, streak: Int,
    onRandom: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit, onShareText: () -> Unit,
    onCopy: () -> Unit, onShowLockScreen: () -> Unit, onHideLockScreen: () -> Unit,
    onHistory: () -> Unit, onChapterBrowser: () -> Unit, onTheme: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = onRandom,
        state = pullState,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxWidth().height(260.dp).background(Brush.verticalGradient(listOf(Amber.copy(alpha = 0.07f), Color.Transparent))))
        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            val today = remember { java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault()).format(java.util.Date()) }
            Row(Modifier.clip(RoundedCornerShape(50)).background(Amber.copy(alpha = 0.1f)).padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Amber, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(today, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium)
            }
            if (streak > 0) {
                Row(Modifier.clip(RoundedCornerShape(50)).background(Amber.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 13.sp)
                    Spacer(Modifier.width(5.dp))
                    Text("$streak day streak", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.SemiBold)
                }
                // ── Week streak card ─────────────────────────────────────
                val context2 = LocalContext.current
                val store2 = remember { context2.getSharedPreferences("reader", 0) }
                val activeDays = remember {
                    val set = mutableSetOf<Int>()
                    val today = java.time.LocalDate.now()
                    for (i in 0..6) {
                        val day = today.minusDays(i.toLong())
                        val key = "read_${day}"
                        if (store2.getBoolean(key, false)) set.add(day.dayOfWeek.value % 7) // 0=Sun
                    }
                    set
                }
                val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                val todayDow = remember { java.time.LocalDate.now().dayOfWeek.value % 7 }
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Amber.copy(alpha = 0.07f))
                        .border(BorderStroke(1.dp, Amber.copy(alpha = 0.18f)), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("This week", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Amber, letterSpacing = 0.8.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            dayLabels.forEachIndexed { idx, label ->
                                val isToday = idx == todayDow
                                val isActive = idx in activeDays
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        Modifier.size(32.dp).clip(CircleShape)
                                            .background(
                                                when {
                                                    isActive -> Amber
                                                    isToday  -> Amber.copy(alpha = 0.2f)
                                                    else     -> Amber.copy(alpha = 0.06f)
                                                }
                                            )
                                            .border(BorderStroke(if (isToday) 1.5.dp else 0.dp, Amber.copy(alpha = 0.5f)), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isActive) Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Text(label, fontSize = 10.sp, color = if (isToday) Amber else InkSoft, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
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
                        Text("\u201c${verse.text}\u201d", color = Color(0xFFF5F0E8), fontSize = fontSize.sp, lineHeight = (fontSize + 12).sp, fontWeight = FontWeight.Normal)
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
                    OutlinedButton(onClick = onCopy, modifier = Modifier.size(52.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.5.dp, Amber.copy(alpha = 0.5f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink), contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    }
                }
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
            Button(onClick = onRandom, enabled = !loading, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink)) {
                if (loading) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Finding a verse…" else "Discover another verse", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            // ── Theme shortcut chips ─────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("🕊 Peace", "💪 Strength", "🙏 Gratitude", "❤ Love").forEach { chip ->
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(1.dp, Amber.copy(alpha = 0.3f)), RoundedCornerShape(50))
                            .clickable(enabled = !loading) { onTheme(chip.substringAfter(" ").lowercase()) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(chip, fontSize = 11.sp, color = InkMid, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    }
                }
            }
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
                    val expanded = item.id in expandedItems
                    val displayReference = if (item.book?.equals(item.scripture, ignoreCase = true) == true)
                        item.reference().removePrefix("${item.book} · ") else item.reference()
                    val today2 = remember { java.time.LocalDate.now().toString() }
                    val isReadToday = item.readDate == today2
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, Amber.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).clickable {
                        expandedItems = if (expanded) expandedItems - item.id else expandedItems + item.id
                    }) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(4.dp).background(
                                if (isReadToday) Brush.horizontalGradient(listOf(Color(0xFF4A8C5C), Color(0xFF6DBF82)))
                                else Brush.horizontalGradient(listOf(Amber, AmberLight))
                            ))
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Amber.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text(item.scripture.uppercase(), color = Amber, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (isReadToday) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4A8C5C)))
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(displayReference, color = InkSoft, fontSize = 11.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        Modifier.size(26.dp).clip(RoundedCornerShape(7.dp))
                                            .background(if (isReadToday) Color(0xFF4A8C5C).copy(alpha = 0.12f) else Amber.copy(alpha = 0.08f))
                                            .border(BorderStroke(1.dp, if (isReadToday) Color(0xFF4A8C5C).copy(alpha = 0.5f) else Amber.copy(alpha = 0.3f)), RoundedCornerShape(7.dp))
                                            .clickable { onMarkRead(item) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("\u2713", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                            color = if (isReadToday) Color(0xFF4A8C5C) else InkSoft)
                                    }
                                    Spacer(Modifier.width(6.dp))
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
                                Text(
                                    "\u201c${item.text}\u201d",
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                                    overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
                                )
                                Text(if (expanded) "Show less" else "Read full verse", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
private fun ExploreScreen(query: String, onQuery: (String) -> Unit, results: List<Verse>, loading: Boolean, favoriteIds: Set<Int>, languages: List<CatalogItem>, scriptures: List<CatalogItem>, languageFilter: Int?, scriptureFilter: Int?, onSearch: () -> Unit, onFavorite: (Verse) -> Unit, onQueryChange: (String) -> Unit, onLanguageFilter: (Int?) -> Unit, onScriptureFilter: (Int?) -> Unit) {
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
    religion: Int, language: Int, frequency: String, reminders: Boolean, lockScreen: Boolean, paletteIntervalHours: Int, fontSize: Int,
    religions: List<ReligionItem>, languages: List<CatalogItem>,
    resumeTick: Int, notice: String?,
    onReligion: (Int) -> Unit, onLanguage: (Int) -> Unit, onFrequency: (String) -> Unit, onPaletteInterval: (Int) -> Unit,
    onReminders: (Boolean) -> Unit, onLockScreen: (Boolean) -> Unit, onSave: () -> Unit,
    onFontSize: (Int) -> Unit, onFeedback: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinWidgetSupported = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }
    val homeWidgetAdded = remember(resumeTick) {
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, ScriptureWidgetReceiver::class.java))
            .isNotEmpty()
    }

    var localLanguages by remember(languages) { mutableStateOf(languages) }
    var langLoading by remember { mutableStateOf(false) }

    fun reloadLanguages(religionId: Int) {
        scope.launch {
            langLoading = true
            try {
                val fetched = withContext(Dispatchers.IO) { ApiClient.api.languages(religionId) }
                localLanguages = fetched
                // Always auto-select first language of the new tradition
                if (fetched.isNotEmpty()) onLanguage(fetched.first().id)
            } catch (_: Exception) {}
            langLoading = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header
        Box(
            Modifier.fillMaxWidth()
                .background(Color(0xFFFAF6F0))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Text("Personalise Your Experience", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text("Choose your tradition, language and preferences for daily inspiration", fontSize = 13.sp, color = InkSoft, lineHeight = 19.sp)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(4.dp))
            SettingsSectionLabel("YOUR TRADITION")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                religions.forEach { item ->
                    val selected = religion == item.id
                    val traditionIconRes = when (item.code.lowercase()) {
                        "hinduism"       -> R.drawable.tradition_hinduism
                        "christianity"   -> R.drawable.tradition_christianity
                        "islam"          -> R.drawable.tradition_islam
                        "buddhism"       -> R.drawable.tradition_buddhism
                        else             -> R.drawable.tradition_general_wisdom
                    }
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Amber.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Amber else Color(0xFFE8E0D8)), RoundedCornerShape(16.dp))
                            .clickable { onReligion(item.id); reloadLanguages(item.id) }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) Amber.copy(alpha = 0.15f) else Color(0xFFF0EBE3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painter = painterResource(traditionIconRes), contentDescription = null, tint = if (selected) Amber else Color(0xFF8A7A6A), modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Ink else InkMid)
                                val scriptureNames = item.scriptures.joinToString(", ") { it.name }
                                if (scriptureNames.isNotEmpty()) Text(scriptureNames, fontSize = 12.sp, color = InkSoft)
                            }
                            Box(
                                Modifier.size(22.dp).clip(CircleShape)
                                    .background(if (selected) Amber else Color.Transparent)
                                    .border(BorderStroke(1.5.dp, if (selected) Amber else Color(0xFFCCC4D8)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(R.drawable.chevron_right),
                                contentDescription = null,
                                tint = if (selected) Amber else InkSoft.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = if (selected) 90f else 0f }
                            )
                        }
                    }
                }
            }

            SettingsSectionLabel("SELECT LANGUAGE")
            if (langLoading) {
                Box(Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Amber, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    localLanguages.forEach { item ->
                        val selected = language == item.id
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Amber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                                .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Amber else Color(0xFFE8E0D8)), RoundedCornerShape(14.dp))
                                .clickable { onLanguage(item.id) }
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(18.dp).clip(CircleShape)
                                        .background(if (selected) Amber else Color.Transparent)
                                        .border(BorderStroke(1.5.dp, if (selected) Amber else Color(0xFFCCC4D8)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White))
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(item.name, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp, color = if (selected) Ink else InkMid)
                                    Text(item.nativeScript, fontSize = 11.sp, color = if (selected) Amber else InkSoft)
                                }
                            }
                        }
                    }
                }
            }

            SettingsSectionLabel("VERSE FREQUENCY")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("Daily", "Once a day", "📅"),
                    Triple("Every 6 hours", "3× a day", "⏰"),
                    Triple("Every hour", "24× a day", "⏰")
                ).forEach { (label, sub, icon) ->
                    val selected = frequency == label
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Amber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Amber else Color(0xFFE8E0D8)), RoundedCornerShape(14.dp))
                            .clickable { onFrequency(label) }
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(icon, fontSize = 18.sp)
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Ink else InkMid,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                            Text(sub, fontSize = 10.sp, color = InkSoft, textAlign = TextAlign.Center)
                            Box(
                                Modifier.size(16.dp).clip(CircleShape)
                                    .background(if (selected) Amber else Color.Transparent)
                                    .border(BorderStroke(1.5.dp, if (selected) Amber else Color(0xFFCCC4D8)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                            }
                        }
                    }
                }
            }

            SettingsSectionLabel("NOTIFICATIONS & WIDGET")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, Color(0xFFE8E0D8)), RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                SettingsToggleRow(
                    "Verse reminders",
                    "Notify me each time a new verse arrives (cadence set by Verse Frequency above)",
                    icon = { Icon(painter = painterResource(R.drawable.notification_bell), contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp)) },
                    checked = reminders, onChecked = onReminders
                )
                HorizontalDivider(color = Color(0xFFE8E0D8), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsToggleRow(
                    "Lock screen verse",
                    "Shows today's verse on your lock screen",
                    icon = { Icon(painter = painterResource(R.drawable.lock_screen), contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp)) },
                    checked = lockScreen, onChecked = onLockScreen
                )
                if (lockScreen || homeWidgetAdded) {
                    HorizontalDivider(color = Color(0xFFE8E0D8), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Lock screen & widget colour frequency", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("How often the wallpaper and widget palette changes.", fontSize = 11.sp, color = InkSoft)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 6, 12, 24).forEach { hours ->
                                val sel = paletteIntervalHours == hours
                                Box(
                                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) Amber.copy(alpha = 0.12f) else Color(0xFFF5F0EA))
                                        .border(BorderStroke(if (sel) 1.5.dp else 1.dp, if (sel) Amber else Color(0xFFE8E0D8)), RoundedCornerShape(10.dp))
                                        .clickable { onPaletteInterval(hours) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${hours}h", fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Amber else InkMid)
                                }
                            }
                        }
                        Text("Current: ${TimePalette.now(paletteIntervalHours).period} · every ${paletteIntervalHours}h", fontSize = 11.sp, color = InkSoft)
                    }
                }
                HorizontalDivider(color = Color(0xFFE8E0D8), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Amber.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(painter = painterResource(R.drawable.home_widget), contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Home screen widget", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
                        Text(
                            if (homeWidgetAdded) "Widget is on your home screen"
                            else "Tap to add the verse widget to your home screen",
                            fontSize = 12.sp, color = InkSoft
                        )
                    }
                    if (pinWidgetSupported) {
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                val provider = ComponentName(context, ScriptureWidgetReceiver::class.java)
                                AppWidgetManager.getInstance(context).requestPinAppWidget(provider, null, null)
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.5.dp, Amber),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(if (homeWidgetAdded) "Add another" else "Add Widget", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            SettingsSectionLabel("READING PREFERENCES")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, Color(0xFFE8E0D8)), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Amber.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(painter = painterResource(R.drawable.reading_font_size), contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Verse font size", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
                        Text("Adjust the text size for better readability", fontSize = 12.sp, color = InkSoft)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("A", fontSize = 13.sp, color = InkSoft, fontWeight = FontWeight.Normal)
                    Spacer(Modifier.width(4.dp))
                    Slider(
                        value = fontSize.toFloat(),
                        onValueChange = { onFontSize(it.toInt()) },
                        valueRange = 16f..32f,
                        steps = 7,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber, inactiveTrackColor = Amber.copy(alpha = 0.2f))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("A", fontSize = 22.sp, color = InkSoft, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Small", fontSize = 11.sp, color = InkSoft)
                    Text("Medium", fontSize = 11.sp, color = Amber, fontWeight = FontWeight.SemiBold)
                    Text("Large", fontSize = 11.sp, color = InkSoft)
                }
            }
            SettingsSectionLabel("SYSTEM")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, Color(0xFFE8E0D8)), RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                SettingsLinkRow(
                    "Notification settings",
                    "Open Android notification settings",
                    icon = { Icon(painter = painterResource(R.drawable.system_settings), contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp)) }
                ) { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) }) }
                HorizontalDivider(color = Color(0xFFE8E0D8), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsLinkRow(
                    "Battery settings",
                    "Optimise for uninterrupted verse delivery",
                    icon = { Icon(painter = painterResource(R.drawable.battery_settings), contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp)) }
                ) { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1B4E))
            ) {
                Icon(painter = painterResource(R.drawable.save_preferences), contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Save preferences", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
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

private fun traditionIcon(code: String): String = when (code.lowercase()) {
    "hinduism"       -> "🛕"
    "christianity"   -> "✝"
    "islam"          -> "☽"
    "buddhism"       -> "❁"
    "general wisdom" -> "✿"
    else             -> "✶"
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
private fun SettingsSectionLabel(label: String, showInfo: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber, letterSpacing = 1.4.sp, modifier = Modifier.weight(1f))
        if (showInfo) {
            Icon(painter = painterResource(R.drawable.info), contentDescription = null, tint = InkSoft.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, icon: @Composable () -> Unit, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Amber.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
            Text(subtitle, fontSize = 12.sp, color = InkSoft)
        }
        Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Amber))
    }
}

@Composable
private fun SettingsLinkRow(label: String, subtitle: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Amber.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink)
            Text(subtitle, fontSize = 12.sp, color = InkSoft)
        }
        Icon(painter = painterResource(R.drawable.chevron_right), contentDescription = null, tint = InkSoft.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}

// ── Login / Register screen ───────────────────────────────────────────────────
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
                colors = ButtonDefaults.buttonColors(containerColor = Ink)
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
                    items(verses) { v ->
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
