package com.scripturedaily.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.scripturedaily.notification.VerseNotifier
import com.scripturedaily.widget.ScriptureWidget
import java.util.concurrent.TimeUnit

class VerseSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("reader", Context.MODE_PRIVATE)
        val religion = prefs.getInt("religion", 1)
        val language = prefs.getInt("language", 1)
        val scripture = prefs.getInt("scripture", religion)
        return try {
            val verse = if (prefs.getString("frequency", "Daily") == "Daily") ApiClient.api.today(religion, language, scripture)
                else ApiClient.api.random(religion, language, scripture)
            val previous = ScriptureDatabase.get(applicationContext).verses().find(verse.id)
            ScriptureDatabase.get(applicationContext).verses().save(verse.toCached(scripture, language, previous?.favorite == true))
            ScriptureWidget().refreshAll(applicationContext)
            if (prefs.getBoolean("reminders", false)) VerseNotifier.show(applicationContext, verse.scripture, verse.text, verse.reference())
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }

    companion object {
        fun schedule(context: Context, frequency: String) {
            val hours = when (frequency) { "Every hour" -> 1L; "Every 6 hours" -> 6L; else -> 24L }
            val request = PeriodicWorkRequestBuilder<VerseSyncWorker>(hours, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("scripture-daily-sync", ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
