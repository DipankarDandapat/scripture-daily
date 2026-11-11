package com.scripturedaily.analytics

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.scripturedaily.config.AppConfigManager

/**
 * Meta (Facebook) App Events, used only for app-install campaign attribution.
 *
 * Privacy rules enforced here:
 *  - The SDK is NOT auto-initialised by the manifest (`AutoInitEnabled` / `AutoLogAppEventsEnabled` are false). It is initialised,
 *    and automatic install / activation logging switched on, only by [init] when the backend flag `meta_ads_enabled` is true.
 *  - Event parameters must never describe religion, scripture, verse content, mood or any other sensitive attribute. Keys that
 *    match [SENSITIVE_KEY_STEMS] are dropped before anything reaches the SDK, whoever builds the [Bundle].
 */
object MetaAnalytics {

    private const val TAG = "MetaAnalytics"
    private const val PREFS = "meta_analytics"
    internal const val FIRST_VERSE_READ_KEY = "first_verse_read_sent"

    object Event {
        const val ONBOARDING_COMPLETED   = "onboarding_completed"
        const val FIRST_VERSE_READ       = "first_verse_read"
        const val DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
        const val VERSE_SAVED            = "verse_saved"
        const val MOOD_CHECKED_IN        = "mood_checked_in"
        const val CHALLENGE_JOINED       = "challenge_joined"
        const val CHALLENGE_DAY_DONE     = "challenge_day_completed"
        const val STORY_OPENED           = "story_opened"
        const val SHARE_STUDIO_OPENED    = "share_studio_opened"
        const val EXPLAIN_OPENED         = "explain_opened"
    }

    /**
     * A parameter key is refused when, lower-cased, it contains any of these stems. Religion, beliefs, health / mental state
     * and similar are sensitive categories under Meta's terms and must never be sent, even indirectly (a scripture name or a
     * verse reveals a religion; a mood reveals mental state).
     */
    internal val SENSITIVE_KEY_STEMS: Set<String> = setOf(
        "religion", "religious", "tradition", "faith", "belief", "denomination", "church", "worship", "prayer",
        "scripture", "verse", "chapter", "book", "story",
        "mood", "emotion", "feeling", "mental", "health", "anxiety", "depress",
        "sexual", "gender", "ethnic", "race", "politic", "union",
    )

    @Volatile private var logger: AppEventsLogger? = null
    @Volatile private var sdkActive = false

    /** True when [key] must not be sent to Meta. */
    internal fun isSensitiveKey(key: String): Boolean {
        val k = key.lowercase()
        return SENSITIVE_KEY_STEMS.any { k.contains(it) }
    }

    /** Drops every pair whose key is sensitive. Pure, so it is unit-tested directly. */
    internal fun safePairs(pairs: List<Pair<String, String>>): List<Pair<String, String>> =
        pairs.filterNot { isSensitiveKey(it.first) }

    /**
     * Applies the current `meta_ads_enabled` value. Called after the startup config has loaded and again whenever the flag
     * changes (see WisdomOneApplication). Safe to call repeatedly.
     *
     * Enabled  → initialise the SDK, switch on automatic app-event logging (install / activation, needed for attribution) and
     *            activate the app.
     * Disabled → switch automatic logging and auto-init back off. Those SDK settings are persisted, so this also stops the SDK
     *            from initialising itself on the next cold start after the flag has been turned off.
     */
    @Synchronized
    fun init(application: Application) {
        val enabled = isEnabled()
        Log.d(TAG, "init() — meta_ads_enabled=$enabled | configSource=${AppConfigManager.current.source}")
        try {
            if (enabled) {
                if (sdkActive) return
                FacebookSdk.setAutoInitEnabled(true)
                FacebookSdk.fullyInitialize()
                FacebookSdk.setAutoLogAppEventsEnabled(true)
                AppEventsLogger.activateApp(application)
                logger = AppEventsLogger.newLogger(application)
                sdkActive = true
                Log.d(TAG, "init() — SDK activated, logger ready")
            } else {
                FacebookSdk.setAutoLogAppEventsEnabled(false)
                FacebookSdk.setAutoInitEnabled(false)
                logger = null
                sdkActive = false
                Log.d(TAG, "init() — tracking disabled, SDK auto-init and auto-logging off")
            }
        } catch (e: Exception) {
            Log.e(TAG, "init() — failed: ${e.message}", e)
        }
    }

    fun isEnabled(): Boolean =
        AppConfigManager.current.bool("meta_ads_enabled", false)

    fun track(context: Context, eventName: String, params: Bundle? = null) {
        if (!isEnabled()) {
            Log.d(TAG, "track($eventName) — dropped: meta_ads_enabled is false")
            return
        }
        val log = logger ?: run {
            // Flag is on but the startup init hasn't run yet (or the flag flipped on mid-session).
            (context.applicationContext as? Application)?.let { init(it) }
            logger
        } ?: run {
            Log.w(TAG, "track($eventName) — dropped: SDK not ready")
            return
        }
        val safe = params?.let { stripSensitive(eventName, it) }
        try {
            if (safe != null && !safe.isEmpty) log.logEvent(eventName, safe) else log.logEvent(eventName)
            Log.i(TAG, "track($eventName) params=${safe?.keySet()} — sent")
        } catch (e: Exception) {
            Log.e(TAG, "track($eventName) — logEvent failed: ${e.message}", e)
        }
    }

    /**
     * Fires [Event.FIRST_VERSE_READ] the first time a verse is shown on Home on this install. The moment is claimed even while
     * tracking is disabled, so turning Meta ads on later never reports an old user's "first" verse.
     */
    fun trackFirstVerseRead(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val store = object : OnceStore {
            override fun isSet(key: String) = prefs.getBoolean(key, false)
            override fun set(key: String) { prefs.edit().putBoolean(key, true).apply() }
        }
        if (claimOnce(store, FIRST_VERSE_READ_KEY)) track(context, Event.FIRST_VERSE_READ)
    }

    /** Minimal persistent flag store so the once-per-install logic can be unit-tested without Android. */
    internal interface OnceStore {
        fun isSet(key: String): Boolean
        fun set(key: String)
    }

    /** Returns true exactly once per [key] for a given [store]. */
    @Synchronized
    internal fun claimOnce(store: OnceStore, key: String): Boolean {
        if (store.isSet(key)) return false
        store.set(key)
        return true
    }

    private fun stripSensitive(eventName: String, params: Bundle): Bundle {
        val blocked = params.keySet().filter(::isSensitiveKey)
        if (blocked.isEmpty()) return params
        Log.w(TAG, "track($eventName) — removed sensitive parameter(s) $blocked")
        return Bundle(params).apply { blocked.forEach(::remove) }
    }

    /** Convenience builder so call sites stay readable. Sensitive keys are dropped here as well as in [track]. */
    fun params(vararg pairs: Pair<String, String>): Bundle =
        Bundle().apply { safePairs(pairs.toList()).forEach { (k, v) -> putString(k, v) } }
}
