package com.scripturedaily.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.scripturedaily.lockscreen.LockScreenVerse
import com.scripturedaily.widget.ScriptureWidget
import com.scripturedaily.notification.VerseNotifier
import java.util.concurrent.TimeUnit

class VerseSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ApiClient.ensureInit(applicationContext)
        val prefs = applicationContext.getSharedPreferences("reader", Context.MODE_PRIVATE)
        val religion  = prefs.getInt("religion", 1)
        val language  = prefs.getInt("language", 0).takeIf { it > 0 } ?: 1
        val scripture = prefs.getInt("scripture", 0)  // cache label only; verses come from the whole tradition
        return try {
            val db = ScriptureDatabase.get(applicationContext).verses()
            val response = ApiClient.api.refreshActiveVerse().data
            val verse = response.verse
            // The server owns the schedule: always arm the next alarm for its next_update_at (even when nothing changed this time).
            VerseScheduler.recordServerSchedule(applicationContext, response.nextUpdateAt)
            val previousActiveId = prefs.getInt("last_verse_id", -1)
            val lockWallpaperStale = prefs.getBoolean("lock_screen", false) && !LockScreenVerse.isWallpaperAppliedForVerse(applicationContext, verse.id)
            if (previousActiveId == verse.id && db.find(verse.id) != null && !lockWallpaperStale) return Result.success()
            val previous = db.find(verse.id)
            val cached = verse.toCached(verse.scriptureId ?: scripture, verse.languageId ?: language)
            db.save(cached)
            prefs.edit().putInt("last_verse_id", verse.id).putInt("current_verse_id", verse.id)
                .putString("last_verse_day", java.time.LocalDate.now().toString())
                .putLong("active_verse_selected_at", System.currentTimeMillis()).apply()
            ScriptureWidget.cacheVerseForWidget(applicationContext, cached)
            ScriptureWidget().refreshAll(applicationContext)
            LockScreenVerse.applyIfEnabled(applicationContext, verse.id, verse.scripture, verse.text, verse.reference())
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
    }

    companion object {
        /** Kept for existing callers; see [VerseScheduler] for how updates are scheduled. */
        fun schedule(context: Context, frequency: String, resetTimer: Boolean = false) = VerseScheduler.schedule(context, frequency, resetTimer)
    }
}
