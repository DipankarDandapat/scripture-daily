package com.scripturedaily.analytics

import android.content.Context
import com.facebook.appevents.AppEventsLogger
import com.scripturedaily.config.AppConfigManager

/**
 * Thin wrapper around the Meta (Facebook) AppEventsLogger.
 *
 * All calls are no-ops when the remote-config key `meta_ads_enabled` is false
 * (or absent). Set it to true in the backend /config response to start sending
 * events to Meta Events Manager.
 *
 * Usage:
 *   MetaAnalytics.track(context, MetaAnalytics.Event.ONBOARDING_COMPLETED)
 */
object MetaAnalytics {

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

    /** Returns true only when the backend config explicitly enables Meta tracking. */
    private fun isEnabled(): Boolean =
        AppConfigManager.current.bool("meta_ads_enabled", false)

    /**
     * Log a named event to Meta Events Manager.
     * Safe to call from any thread; the SDK dispatches on its own executor.
     * Does nothing when [isEnabled] returns false.
     */
    fun track(context: Context, eventName: String) {
        if (!isEnabled()) return
        AppEventsLogger.newLogger(context).logEvent(eventName)
    }
}
