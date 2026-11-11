package com.scripturedaily.config

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.scripturedaily.data.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Where the configuration currently in use came from. */
enum class ConfigSource { DEFAULTS, CACHE, NETWORK }

data class ConfigSnapshot(val version: String, val values: JsonObject, val source: ConfigSource, val fetchedAtMs: Long) {
    fun bool(key: String, default: Boolean): Boolean = runCatching { values.get(key).asBoolean }.getOrDefault(default)
    fun int(key: String, default: Int): Int = runCatching { values.get(key).asInt }.getOrDefault(default)
    fun string(key: String, default: String): String = runCatching { values.get(key).asString }.getOrDefault(default)
    fun obj(key: String): JsonObject? = runCatching { values.get(key).asJsonObject }.getOrNull()
}

/**
 * Centralised key-value configuration (GET /api/v1/app/config) — the only remote configuration source of the app
 * (Firebase Remote Config was removed). The startup gate (com.scripturedaily.gate) reads update / maintenance values from here.
 *
 *  - Startup: [awaitStartup] runs before Home is shown, but never longer than [STARTUP_TIMEOUT_MS]; if the API is slow or down the
 *    last good copy (or the safe built-in defaults) is applied so the user is never stuck on a loading screen.
 *  - Foreground: [observeForeground] refreshes (conditional GET with ETag, so an unchanged config costs one tiny 304) each time the
 *    app returns from the background, at most once per `config_refresh_min_interval_seconds`.
 *  - The last successfully fetched config is persisted and used when the API is unavailable.
 */
object AppConfigManager {
    const val STARTUP_TIMEOUT_MS = 4_000L
    private const val PREFS = "app_config_cache"
    private const val DEFAULT_MIN_INTERVAL_SECONDS = 60

    /** Built-in fallback used until a config has ever been fetched. Ads stay OFF so nothing is requested without a server decision. */
    val SAFE_DEFAULTS: JsonObject = JsonParser.parseString(
        """{"config_schema_version":1,"config_refresh_min_interval_seconds":$DEFAULT_MIN_INTERVAL_SECONDS,"ads_enabled":false,"ads_use_test_ids":true,"wisdom_tree_default_enabled":true,"meta_ads_enabled":false}"""
    ).asJsonObject

    private val _state = MutableStateFlow(ConfigSnapshot("defaults", SAFE_DEFAULTS, ConfigSource.DEFAULTS, 0L))
    val state: StateFlow<ConfigSnapshot> = _state.asStateFlow()
    private val _lastFetchOk = MutableStateFlow<Boolean?>(null)
    /** Result of the most recent attempt to reach the server: true (200/304), false (network/server error), null (not tried). */
    val lastFetchOk: StateFlow<Boolean?> = _lastFetchOk.asStateFlow()
    private val _startupDone = MutableStateFlow(false)
    val startupDone: StateFlow<Boolean> = _startupDone.asStateFlow()

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var etag: String? = null
    private var lastAttemptMs = 0L
    private var initialised = false
    private lateinit var app: Context

    val current: ConfigSnapshot get() = _state.value

    /** Loads the persisted copy (if any). Safe to call repeatedly. */
    fun init(context: Context) {
        if (initialised) return
        app = context.applicationContext
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = p.getString("json", null)
        val version = p.getString("version", null)
        if (json != null && version != null) runCatching {
            _state.value = ConfigSnapshot(version, JsonParser.parseString(json).asJsonObject, ConfigSource.CACHE, p.getLong("fetched_at", 0L))
            etag = p.getString("etag", null)
        }
        initialised = true
    }

    private fun persist(snapshot: ConfigSnapshot, etagValue: String?) {
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("json", snapshot.values.toString()).putString("version", snapshot.version)
            .putString("etag", etagValue).putLong("fetched_at", snapshot.fetchedAtMs).apply()
    }

    /**
     * Fetches the config. Returns true when the server answered (200 or 304). [force] ignores the minimum interval.
     * Network or parse failures leave the last good snapshot untouched.
     */
    suspend fun refresh(force: Boolean = false): Boolean = mutex.withLock {
        if (!initialised) return false
        val now = System.currentTimeMillis()
        val minMs = current.int("config_refresh_min_interval_seconds", DEFAULT_MIN_INTERVAL_SECONDS) * 1000L
        if (!force && now - lastAttemptMs < minMs) return true
        lastAttemptMs = now
        val ok = runCatching {
            ApiClient.ensureInit(app)
            val response = withContext(Dispatchers.IO) { ApiClient.api.appConfig(etag) }
            when {
                response.code() == 304 -> { _state.value = _state.value.copy(fetchedAtMs = now); true }
                response.isSuccessful -> {
                    val body = response.body() ?: return@runCatching false
                    val snap = ConfigSnapshot(body.version, body.config, ConfigSource.NETWORK, now)
                    etag = response.headers()["ETag"]
                    persist(snap, etag)
                    _state.value = snap
                    true
                }
                else -> false
            }
        }.getOrDefault(false)
        _lastFetchOk.value = ok
        ok
    }

    /** Called once before Home appears. Returns whether fresh data was obtained; either way a usable config is in place afterwards. */
    suspend fun awaitStartup(): Boolean {
        val fresh = withTimeoutOrNull(STARTUP_TIMEOUT_MS) { refresh(force = true) } ?: false
        if (!fresh) _lastFetchOk.value = false   // a timeout counts as "could not verify"
        _startupDone.value = true
        return fresh
    }

    /** Refresh whenever the app comes back to the foreground. */
    fun observeForeground(application: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            private var first = true
            override fun onStart(owner: LifecycleOwner) {
                if (first) { first = false; return }  // the cold start is handled by awaitStartup()
                scope.launch { refresh(force = false) }
            }
        })
    }
}
