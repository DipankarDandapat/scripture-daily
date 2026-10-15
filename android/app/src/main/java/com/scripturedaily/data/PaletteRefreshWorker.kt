package com.scripturedaily.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.scripturedaily.lockscreen.LockScreenVerse
import com.scripturedaily.data.ScriptureDatabase
import com.scripturedaily.widget.ScriptureWidget
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Refreshes presentation only: offline, with no verse changes or notifications. */
class PaletteRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val prefs = applicationContext.getSharedPreferences("reader", Context.MODE_PRIVATE)
        val intervalHours = prefs.getInt("palette_interval_hours", 6).takeIf { it in setOf(1, 6, 12, 24) } ?: 6
        ScriptureWidget().refreshAll(applicationContext)
        if (prefs.getBoolean("lock_screen", false)) {
            val cached = ScriptureDatabase.get(applicationContext).verses().matching(
                prefs.getInt("scripture", 1),
                prefs.getInt("language", 1)
            )
            if (cached != null) {
                LockScreenVerse.refreshWallpaperPalette(
                    applicationContext,
                    cached.id,
                    cached.scripture,
                    cached.text,
                    cached.reference()
                )
            }
        }
        enqueueNextBoundary(applicationContext, intervalHours)
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }

    companion object {
        private const val WORK_NAME = "scripture-daily-palette-boundary"
        private const val LEGACY_PERIODIC_WORK_NAME = "scripture-daily-palette-refresh"

        /** Run once immediately, then chain refreshes at the next selected local-time boundary. */
        fun schedule(context: Context) {
            val manager = WorkManager.getInstance(context)
            manager.cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)
            manager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PaletteRefreshWorker>().build()
            )
        }

        private fun enqueueNextBoundary(context: Context, intervalHours: Int) {
            val now = ZonedDateTime.now()
            val boundaryHour = ((now.hour / intervalHours) + 1) * intervalHours
            val next = if (boundaryHour >= 24) {
                now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            } else {
                now.toLocalDate().atTime(boundaryHour, 0).atZone(now.zone)
            }
            val delayMs = Duration.between(now, next).toMillis().coerceAtLeast(1L)
            val request = OneTimeWorkRequestBuilder<PaletteRefreshWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request
            )
        }
    }
}
