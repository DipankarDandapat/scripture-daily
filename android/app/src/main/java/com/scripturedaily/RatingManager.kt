package com.scripturedaily

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object RatingManager {

    private const val PREFS = "rating_prefs"
    private const val KEY_RATED        = "has_rated"
    private const val KEY_PROMPTED     = "prompt_count"
    private const val KEY_LAST_PROMPT  = "last_prompt_ms"
    private const val MIN_GAP_MS       = 3 * 24 * 60 * 60 * 1000L  // 3 days between prompts
    private const val MAX_PROMPTS      = 5

    /** Returns true if we should show the in-app review flow right now. */
    fun shouldPrompt(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_RATED, false)) return false
        if (prefs.getInt(KEY_PROMPTED, 0) >= MAX_PROMPTS) return false
        val last = prefs.getLong(KEY_LAST_PROMPT, 0L)
        return System.currentTimeMillis() - last >= MIN_GAP_MS
    }

    /** Call after a positive engagement event (streak milestone, save milestone). */
    suspend fun tryLaunchReview(activity: Activity): Boolean {
        if (!shouldPrompt(activity)) return false
        val launched = launchInAppReview(activity)
        if (launched) {
            val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_PROMPTED, prefs.getInt(KEY_PROMPTED, 0) + 1)
                .putLong(KEY_LAST_PROMPT, System.currentTimeMillis())
                .apply()
        }
        return launched
    }

    /** Mark that the user has rated (called when they tap "Rate" from Settings/snackbar). */
    fun markRated(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_RATED, true).apply()
    }

    /** Open Play Store listing directly. */
    fun openPlayStore(context: Context) {
        val uri = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private suspend fun launchInAppReview(activity: Activity): Boolean =
        suspendCancellableCoroutine { cont ->
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { request ->
                if (request.isSuccessful) {
                    manager.launchReviewFlow(activity, request.result)
                        .addOnCompleteListener { cont.resume(true) }
                } else {
                    cont.resume(false)
                }
            }
        }
}
