package com.scripturedaily.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Keeps the verse (app, lock screen and widgets) rotating on the user's chosen frequency without the app being open.
 *
 * Two cooperating mechanisms, because neither is reliable on its own:
 *  1. **AlarmManager.setAndAllowWhileIdle** is armed for the server's `next_update_at`. It is allowed to fire in Doze and needs no
 *     special permission. When it fires, [VerseAlarmReceiver] enqueues a one-time [VerseSyncWorker] (network-constrained, with backoff)
 *     that fetches the new verse and re-arms the next alarm. It is deliberately *inexact* (no SCHEDULE_EXACT_ALARM): Android may delay
 *     it by minutes under Doze / App Standby, which is fine for hour-scale rotation and keeps battery use low.
 *  2. **A unique periodic WorkManager job** at the same interval is the safety net: it survives reboots by itself and catches up if an
 *     alarm was dropped (force-stop, aggressive OEM task killers). Periodic work is itself deferred under Doze, so it is never relied on alone.
 *
 * Alarms are cleared by a reboot or a force-stop: [com.scripturedaily.lockscreen.BootReceiver] and the Application start re-arm them.
 * Known platform limits: after a user force-stop nothing runs until the app is opened again; some OEMs (Xiaomi, Oppo, Vivo, Huawei, Samsung
 * "sleeping apps") block background work unless the app is exempted from battery optimisation.
 */
object VerseScheduler {
    const val PERIODIC_WORK = "scripture-daily-sync"
    const val ONE_SHOT_WORK = "scripture-daily-sync-now"
    private const val PREFS = "reader"
    private const val KEY_NEXT = "next_verse_update_ms"
    private const val REQUEST_CODE = 7101
    private const val MIN_DELAY_MS = 5_000L

    fun intervalHours(frequency: String): Long = when (frequency) { "Every hour" -> 1L; "Every 6 hours" -> 6L; "Every 12 hours" -> 12L; else -> 24L }

    /** (Re)schedules everything for [frequency]. [resetTimer] = the active verse was just changed, so the next change is a full interval away. */
    fun schedule(context: Context, frequency: String, resetTimer: Boolean = false) {
        val app = context.applicationContext
        val hours = intervalHours(frequency)
        val periodic = PeriodicWorkRequestBuilder<VerseSyncWorker>(hours, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES).build()
        // A changed interval REPLACEs (cancels the old job — no duplicates); an unchanged one keeps its timer.
        val policy = if (resetTimer) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.UPDATE
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(PERIODIC_WORK, policy, periodic)

        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val interval = TimeUnit.HOURS.toMillis(hours)
        val stored = prefs.getLong(KEY_NEXT, 0L)
        val next = when {
            resetTimer || stored <= 0L -> now + interval
            stored - now > interval -> now + interval      // frequency was shortened: never wait longer than the new interval
            else -> stored
        }
        if (next != stored) prefs.edit().putLong(KEY_NEXT, next).apply()
        arm(app, next)
    }

    /** Re-arms the alarm from persisted state (after boot, time/zone change or process start). Cheap and idempotent. */
    fun ensureArmed(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("configured", false)) return
        val frequency = prefs.getString("frequency", "Every 24 hours")?.let { if (it == "Daily") "Every 24 hours" else it } ?: "Every 24 hours"
        schedule(app, frequency)
    }

    /** Stores the server's schedule for the active verse and arms the alarm for it. */
    fun recordServerSchedule(context: Context, nextUpdateAtIso: String?) {
        val ms = runCatching { java.time.Instant.parse(nextUpdateAtIso).toEpochMilli() }
            .recoverCatching { java.time.OffsetDateTime.parse(nextUpdateAtIso).toInstant().toEpochMilli() }.getOrNull() ?: return
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_NEXT, ms).apply()
        arm(app, ms)
    }

    /** Runs a sync as soon as the network allows. Unique, so a burst of alarms/triggers never creates parallel syncs. */
    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<VerseSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(ONE_SHOT_WORK, ExistingWorkPolicy.KEEP, request)
    }

    fun cancelAll(context: Context) {
        val app = context.applicationContext
        WorkManager.getInstance(app).cancelUniqueWork(PERIODIC_WORK)
        WorkManager.getInstance(app).cancelUniqueWork(ONE_SHOT_WORK)
        (app.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent(app))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(context, REQUEST_CODE, Intent(context, VerseAlarmReceiver::class.java).setAction("com.scripturedaily.VERSE_DUE"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun arm(context: Context, atMs: Long) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = maxOf(atMs, System.currentTimeMillis() + MIN_DELAY_MS)
        // Same PendingIntent => this replaces any previous alarm, so there is never more than one.
        try { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context)) } catch (_: SecurityException) { /* the periodic job still covers us */ }
    }
}

/** Fired by AlarmManager when the verse is due. Does no work itself; hands off to WorkManager. */
class VerseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) { VerseScheduler.syncNow(context) }
}
