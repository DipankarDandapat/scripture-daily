package com.scripturedaily

import android.content.Intent
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.CachedVerse
import com.scripturedaily.data.ScriptureDatabase
import com.scripturedaily.data.Verse
import com.scripturedaily.data.toCached
import com.scripturedaily.data.VerseSyncWorker
import com.scripturedaily.notification.VerseNotifier
import com.scripturedaily.widget.ScriptureWidget
import kotlinx.coroutines.launch

private val Forest = Color(0xFF183D32)
private val Gold = Color(0xFFD8AC68)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            val colors = if (dark) darkColorScheme(primary = Color(0xFFA7CDB8), secondary = Gold, background = Color(0xFF101916), surface = Color(0xFF1A2521))
                else lightColorScheme(primary = Forest, secondary = Gold, background = Color(0xFFF7F5EF), surface = Color.White)
            MaterialTheme(colorScheme = colors) { ScriptureApp() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptureApp() {
    val context = LocalContext.current
    val store = remember { context.getSharedPreferences("reader", 0) }
    val dao = remember { ScriptureDatabase.get(context).verses() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var religion by remember { mutableIntStateOf(store.getInt("religion", 1)) }
    var language by remember { mutableIntStateOf(store.getInt("language", 1)) }
    var scriptureId by remember { mutableIntStateOf(store.getInt("scripture", 1)) }
    var frequency by remember { mutableStateOf(store.getString("frequency", "Daily") ?: "Daily") }
    var reminders by remember { mutableStateOf(store.getBoolean("reminders", false)) }
    var configured by remember { mutableStateOf(store.getBoolean("configured", false)) }
    var page by remember { mutableStateOf("Home") }
    var verse by remember { mutableStateOf<Verse?>(null) }
    val savedVerses by remember { dao.observeFavorites() }.collectAsState(initial = emptyList())
    val favoriteIds = remember(savedVerses) { savedVerses.map { it.id }.toSet() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Verse>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> reminders = granted }
    val scripture = scriptureId

    suspend fun loadToday(random: Boolean = false) {
        loading = true; error = null
        try {
            val remote = if (random) ApiClient.api.random(religion, language, scripture) else ApiClient.api.today(religion, language, scripture)
            val old = dao.find(remote.id)
            dao.save(remote.toCached(scripture, language, old?.favorite == true))
            verse = remote
            if (store.getBoolean("reminders", false)) VerseNotifier.show(context, remote.scripture, remote.text, remote.reference())
            ScriptureWidget().refreshAll(context)
        } catch (_: Exception) {
            val local = dao.matching(scripture, language)
            if (local != null) {
                verse = local.toVerse()
                if (store.getBoolean("reminders", false)) VerseNotifier.show(context, local.scripture, local.text, local.reference())
            } else error = "You're offline. Connect once to download your first verse."
        } finally { loading = false }
    }

    LaunchedEffect(configured, religion, language, scriptureId, page) {
        if (configured && page == "Home") loadToday()
    }

    if (!configured) {
        Onboarding(religion, language, error, onReligion = { religion = it }, onLanguage = { language = it }, onContinue = {
            error = null
            scope.launch {
                val selected = try { ApiClient.api.scriptures(religion).firstOrNull() } catch (_: Exception) { null }
                if (selected != null) {
                    scriptureId = selected.id
                    store.edit().putInt("religion", religion).putInt("language", language).putInt("scripture", selected.id).putBoolean("configured", true).apply()
                    VerseSyncWorker.schedule(context, frequency)
                    configured = true
                } else error = "Connect to the internet to load the selected scripture catalog."
            }
        })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { CenterAlignedTopAppBar(title = { Text("Scripture Daily", fontWeight = FontWeight.SemiBold) }, actions = { IconButton(onClick = { page = "Settings" }) { Icon(Icons.Outlined.Settings, "Settings") } }) },
        bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            listOf(Triple("Home", Icons.Outlined.Home, "Home"), Triple("Favorites", Icons.Outlined.Favorite, "Favorites"), Triple("Explore", Icons.Outlined.Explore, "Explore"), Triple("Settings", Icons.Outlined.Settings, "Settings")).forEach { (label, icon, route) ->
                NavigationBarItem(selected = page == route, onClick = { page = route }, icon = { Icon(icon, contentDescription = label) }, label = { Text(label) })
            }
        } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (page) {
                "Home" -> HomeScreen(verse, loading, error, favoriteIds.contains(verse?.id), onRandom = { scope.launch { loadToday(true) } }, onFavorite = {
                    verse?.let { v -> scope.launch {
                        val isFav = dao.find(v.id)?.favorite == true
                        dao.save(v.toCached(scripture, language, !isFav))
                        snackbarHostState.showSnackbar(if (isFav) "Removed from Favorites" else "Saved to Favorites")
                    } }
                }, onShare = { verse?.let { v -> val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${v.text}\n\n${v.scripture} · ${v.reference()}"); context.startActivity(Intent.createChooser(send, "Share verse")) } })
                "Favorites" -> FavoritesScreen(savedVerses, onOpen = { cached -> verse = cached.toVerse(); page = "Home" }, onRemove = { cached -> scope.launch { dao.setFavorite(cached.id, false) } })
                "Explore" -> ExploreScreen(query, { query = it }, results, loading, onSearch = { scope.launch { loading = true; results = try { ApiClient.api.search(query, language) } catch (_: Exception) { emptyList() }; loading = false } })
                else -> SettingsScreen(religion, language, frequency, reminders, error, onReligion = { religion = it }, onLanguage = { language = it }, onFrequency = { frequency = it }, onReminders = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else reminders = enabled
                }, onSave = {
                    scope.launch {
                        val selected = try { ApiClient.api.scriptures(religion).firstOrNull() } catch (_: Exception) { null }
                        if (selected != null) {
                            scriptureId = selected.id
                            store.edit().putInt("religion", religion).putInt("language", language).putInt("scripture", selected.id).putString("frequency", frequency).putBoolean("reminders", reminders).apply()
                            VerseSyncWorker.schedule(context, frequency)
                            if (reminders) verse?.let { VerseNotifier.show(context, it.scripture, it.text, it.reference()) } else VerseNotifier.cancel(context)
                            page = "Home"
                        } else error = "Couldn't reach the scripture catalog. Your saved settings were not changed."
                    }
                })
            }
        }
    }
}

@Composable
private fun Onboarding(religion: Int, language: Int, notice: String?, onReligion: (Int) -> Unit, onLanguage: (Int) -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 26.dp), verticalArrangement = Arrangement.Center) {
        Text("A quieter moment,\nwhenever you need it.", fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp)); Text("Wisdom for every day.", color = Color(0xFF66746D), fontSize = 16.sp)
        Spacer(Modifier.height(34.dp)); Text("YOUR TRADITION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(Modifier.height(10.dp))
        listOf(1 to "Hinduism · Bhagavad Gita", 2 to "Christianity · Bible", 3 to "Islam · Quran").forEach { (id, title) ->
            ChoiceCard(title, religion == id) { onReligion(id) }
        }
        Spacer(Modifier.height(24.dp)); Text("LANGUAGE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { ChoiceChip("English", language == 1) { onLanguage(1) }; ChoiceChip("हिन्दी", language == 2) { onLanguage(2) } }
        if (notice != null) { Spacer(Modifier.height(8.dp)); Text(notice, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        Spacer(Modifier.height(30.dp)); Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Begin your daily reading") }
        Spacer(Modifier.height(12.dp)); Text("Choose what feels right to you. You can change this anytime.", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun ChoiceCard(title: String, selected: Boolean, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.elevatedCardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected, onClick); Spacer(Modifier.width(8.dp)); Text(title, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun HomeScreen(verse: Verse?, loading: Boolean, error: String?, favorite: Boolean, onRandom: () -> Unit, onFavorite: () -> Unit, onShare: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("YOUR MOMENT OF WISDOM", fontSize = 11.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("A verse for today", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
            Text("✦", fontSize = 26.sp, color = Gold)
        }
        if (loading && verse == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error != null && verse == null) Text(error, color = Color(0xFF9C463D))
        if (verse != null) {
            ElevatedCard(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), shape = RoundedCornerShape(28.dp), colors = CardDefaults.elevatedCardColors(containerColor = Forest), elevation = CardDefaults.elevatedCardElevation(4.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(verse.scripture.uppercase(), color = Gold, fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                    Text("“${verse.text}”", color = Color(0xFFF8F5EC), fontSize = 25.sp, lineHeight = 35.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(3.dp)); Text(verse.reference(), color = Color(0xFFD0DAD3), fontSize = 13.sp)
                    if (verse.isDemo) Text("DEMO PARAPHRASE · NOT A CANONICAL TRANSLATION", color = Color(0xFFE5C78E), fontSize = 9.sp, letterSpacing = 0.4.sp)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onFavorite) { Icon(if (favorite) Icons.Outlined.Bookmark else Icons.Outlined.Favorite, null, tint = Gold); Spacer(Modifier.width(7.dp)); Text(if (favorite) "Saved" else "Save", color = Color.White) }
                        TextButton(onClick = onShare) { Icon(Icons.Outlined.Share, null, tint = Gold); Spacer(Modifier.width(7.dp)); Text("Share", color = Color.White) }
                    }
                }
            }
        }
        Button(onClick = onRandom, enabled = !loading, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text(if (loading) "Finding a verse…" else "Discover another verse") }
        Text("Take a breath. Carry the thought with you.", modifier = Modifier.align(Alignment.CenterHorizontally), color = Color(0xFF748078), fontSize = 13.sp)
    }
}

@Composable
private fun FavoritesScreen(items: List<CachedVerse>, onOpen: (CachedVerse) -> Unit, onRemove: (CachedVerse) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Saved verses", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(14.dp))
        if (items.isEmpty()) Text("Your saved verses will appear here. Save a verse to keep it close.", color = Color.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(items) { item ->
            ElevatedCard(onClick = { onOpen(item) }, colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(item.scripture, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text("“${item.text}”"); TextButton(onClick = { onRemove(item) }, modifier = Modifier.align(Alignment.End)) { Text("Remove") } }
            }
        } }
    }
}

@Composable
private fun ExploreScreen(query: String, onQuery: (String) -> Unit, results: List<Verse>, loading: Boolean, onSearch: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Explore", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp))
        OutlinedTextField(query, onQuery, modifier = Modifier.fillMaxWidth(), label = { Text("Search a word or idea") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true)
        Spacer(Modifier.height(8.dp)); Button(onClick = onSearch, enabled = query.length >= 2 && !loading, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Searching…" else "Search verses") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(results) { v -> ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp)) { Text(v.scripture, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text("“${v.text}”"); Spacer(Modifier.height(6.dp)); Text(v.reference(), color = Color.Gray, fontSize = 12.sp) } } } }
    }
}

@Composable
private fun SettingsScreen(religion: Int, language: Int, frequency: String, reminders: Boolean, notice: String?, onReligion: (Int) -> Unit, onLanguage: (Int) -> Unit, onFrequency: (String) -> Unit, onReminders: (Boolean) -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Your preferences", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text("Tradition", fontWeight = FontWeight.SemiBold)
        listOf(1 to "Hinduism · Bhagavad Gita", 2 to "Christianity · Bible", 3 to "Islam · Quran").forEach { (id, label) -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(religion == id, onClick = { onReligion(id) }); Text(label) } }
        Text("Language", fontWeight = FontWeight.SemiBold)
        Row { ChoiceChip("English", language == 1) { onLanguage(1) }; Spacer(Modifier.width(8.dp)); ChoiceChip("हिन्दी", language == 2) { onLanguage(2) } }
        Text("Verse frequency", fontWeight = FontWeight.SemiBold)
        listOf("Daily", "Every 6 hours", "Every hour").forEach { label -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(frequency == label, onClick = { onFrequency(label) }); Text(label) } }
        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Verse reminders"); Text("Keeps the verse in notifications. To show its text while locked, allow notifications and sensitive content on your lock screen in Android Settings.", fontSize = 12.sp, color = Color.Gray) }; Switch(reminders, onReminders) }
        Text("Widget: add it from your home-screen picker. Lock-screen widget placement depends on Android version and device support.", fontSize = 12.sp, color = Color.Gray)
        if (notice != null) Text(notice, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        Spacer(Modifier.weight(1f)); Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Save preferences") }
    }
}
