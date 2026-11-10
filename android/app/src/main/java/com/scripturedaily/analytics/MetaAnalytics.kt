package com.scripturedaily.analytics

import android.app.Application
import android.content.Context
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.scripturedaily.config.AppConfigManager

object MetaAnalytics {

    private const val TAG = "MetaAnalytics"

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

    private var logger: AppEventsLogger? = null

    /** Call once after AppConfigManager has loaded (e.g. in WisdomOneApplication). */
    fun init(application: Application) {
        val enabled = AppConfigManager.current.bool("meta_ads_enabled", false)
        Log.d(TAG, "init() — meta_ads_enabled=$enabled | configSource=${AppConfigManager.current.source}")
        if (!enabled) {
            Log.d(TAG, "init() — tracking disabled, SDK not activated")
            return
        }
        try {
            AppEventsLogger.activateApp(application)
            logger = AppEventsLogger.newLogger(application)
            Log.d(TAG, "init() — SDK activated, logger ready | appId=${FacebookSdk.getApplicationId()}")
        } catch (e: Exception) {
            Log.e(TAG, "init() — SDK activation failed: ${e.message}", e)
        }
    }

    fun isEnabled(): Boolean =
        AppConfigManager.current.bool("meta_ads_enabled", false)

    fun track(context: Context, eventName: String, params: android.os.Bundle? = null) {
        val enabled = isEnabled()
        Log.d(TAG, "track($eventName) — meta_ads_enabled=$enabled | configSource=${AppConfigManager.current.source} | loggerReady=${logger != null}")
        if (!enabled) {
            Log.w(TAG, "track($eventName) — DROPPED: meta_ads_enabled is false")
            return
        }
        val log = logger ?: run {
            Log.w(TAG, "track($eventName) — logger was null, initialising now")
            try {
                AppEventsLogger.activateApp(context.applicationContext as Application)
                AppEventsLogger.newLogger(context.applicationContext).also { logger = it }
            } catch (e: Exception) {
                Log.e(TAG, "track($eventName) — lazy init failed: ${e.message}", e)
                return
            }
        }
        try {
            if (params != null) log.logEvent(eventName, params) else log.logEvent(eventName)
            Log.i(TAG, "track($eventName) params=${params?.keySet()} — SENT to Meta ✓")
        } catch (e: Exception) {
            Log.e(TAG, "track($eventName) — logEvent failed: ${e.message}", e)
        }
    }

    /** Convenience builder so call sites stay readable. */
    fun params(vararg pairs: Pair<String, String>): android.os.Bundle =
        android.os.Bundle().apply { pairs.forEach { (k, v) -> putString(k, v) } }
}
