package com.scripturedaily.data

import android.content.Context
<<<<<<< HEAD
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
=======
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
>>>>>>> settings_redesign
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.scripturedaily.lockscreen.LockScreenVerse
import com.scripturedaily.widget.ScriptureWidget
<<<<<<< HEAD
=======
import com.scripturedaily.notification.VerseNotifier
>>>>>>> settings_redesign
import java.util.concurrent.TimeUnit

class VerseSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ApiClient.ensureInit(applicationContext)
        val prefs = applicationContext.getSharedPreferences("reader", Context.MODE_PRIVATE)
        val religion  = prefs.getInt("religion", 1)
        val language  = prefs.getInt("language", 0).takeIf { it > 0 } ?: 1
        val scripture = prefs.getInt("scripture", 0)  // cache label only; verses come from the whole tradition
        return try {
<<<<<<< HEAD
            val verse = if (prefs.getString("frequency", "Daily") == "Daily") ApiClient.api.today(religion, language, java.time.LocalDate.now().toString())
                else ApiClient.api.random(religion, language, null)
            val previous = ScriptureDatabase.get(applicationContext).verses().find(verse.id)
            val cached = verse.toCached(scripture, language, previous?.favorite == true)
            ScriptureDatabase.get(applicationContext).verses().save(cached)
            ScriptureWidget.cacheVerseForWidget(applicationContext, cached)
            ScriptureWidget().refreshAll(applicationContext)
            LockScreenVerse.applyIfEnabled(
                applicationContext,
                verse.id,
                verse.scripture,
                verse.text,
                verse.reference()
            )
=======
            val db = ScriptureDatabase.get(applicationContext).verses()
            val verse = ApiClient.api.refreshActiveVerse().data.verse
            val previousActiveId = prefs.getInt("last_verse_id", -1)
            if (previousActiveId == verse.id && db.find(verse.id) != null) return Result.success()
            val previous = db.find(verse.id)
            val cached = verse.toCached(verse.scriptureId ?: scripture, verse.languageId ?: language, previous?.favorite == true)
            db.save(cached)
            prefs.edit().putInt("last_verse_id", verse.id).putInt("current_verse_id", verse.id)
                .putString("last_verse_day", java.time.LocalDate.now().toString())
                .putLong("active_verse_selected_at", System.currentTimeMillis()).apply()
            ScriptureWidget.cacheVerseForWidget(applicationContext, cached)
            ScriptureWidget().refreshAll(applicationContext)
            LockScreenVerse.applyIfEnabled(applicationContext, verse.id, verse.scripture, verse.text, verse.reference())
>>>>>>> settings_redesign
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }

    companion object {
<<<<<<< HEAD
        fun schedule(context: Context, frequency: String) {
            val hours = when (frequency) { "Every hour" -> 1L; "Every 6 hours" -> 6L; else -> 24L }
            val request = PeriodicWorkRequestBuilder<VerseSyncWorker>(hours, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("scripture-daily-sync", ExistingPeriodicWorkPolicy.UPDATE, request)
=======
        fun schedule(context: Context, frequency: String, resetTimer: Boolean = false) {
            val hours = when (frequency) { "Every hour" -> 1L; "Every 6 hours" -> 6L; "Every 12 hours" -> 12L; else -> 24L }
            // Keep-same-verse wallpaper refresh works offline; normal verse updates retry when network is unavailable.
            val request = PeriodicWorkRequestBuilder<VerseSyncWorker>(hours, TimeUnit.HOURS).build()
            val policy = if (resetTimer) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.UPDATE
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("scripture-daily-sync", policy, request)
>>>>>>> settings_redesign
        }
    }
}
