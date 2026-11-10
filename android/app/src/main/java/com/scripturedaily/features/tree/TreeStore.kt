package com.scripturedaily.features.tree

import android.content.Context
import com.scripturedaily.data.ApiClient
<<<<<<< HEAD
import com.scripturedaily.data.TreeReadBody
=======
import com.scripturedaily.data.TreeDto
import com.scripturedaily.data.TreeReadBody
import com.scripturedaily.data.TreeSettingsBody
>>>>>>> settings_redesign
import java.time.LocalDate

/**
 * Local-first persistence for the Wisdom Tree. Reading days live in SharedPreferences (so progress survives restarts
 * and works offline) and are merged with the backend's history whenever the network is available, which is what
 * lets progress follow the user between devices.
 */
object TreeStore {
    private const val FILE = "reader"
    private const val KEY = "tree_days"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

<<<<<<< HEAD
    fun days(context: Context): Set<LocalDate> {
        val p = prefs(context)
        migrateLegacy(context)
        return p.getStringSet(KEY, emptySet()).orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
=======
    /** Reading days that count toward the *current* journey: nothing before the last restart (re-enable) is included. */
    fun days(context: Context): Set<LocalDate> {
        val p = prefs(context)
        migrateLegacy(context)
        val start = startedOn(context)
        return p.getStringSet(KEY, emptySet()).orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .filter { start == null || !it.isBefore(start) }.toSet()
    }

    /** Wisdom Tree switch (Settings → Reading Preferences). Default on. */
    fun enabled(context: Context): Boolean = prefs(context).getBoolean("tree_enabled", true)
    fun startedOn(context: Context): LocalDate? = prefs(context).getString("tree_started_on", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** Applies the switch locally. Turning it back ON after it was OFF restarts the journey today (history is kept, just not counted). */
    fun applyEnabled(context: Context, enabled: Boolean, today: LocalDate = LocalDate.now()) {
        val p = prefs(context)
        val was = p.getBoolean("tree_enabled", true)
        val e = p.edit().putBoolean("tree_enabled", enabled)
        if (enabled && !was) e.putString("tree_started_on", today.toString())
        e.apply()
>>>>>>> settings_redesign
    }

    /** One-time import of the old streak system's `read_YYYY-MM-DD` flags and streak counter so nobody loses progress. */
    private fun migrateLegacy(context: Context) {
        val p = prefs(context)
        if (p.getBoolean("tree_migrated", false)) return
        val set = p.getStringSet(KEY, emptySet()).orEmpty().toMutableSet()
        p.all.forEach { (k, v) -> if (k.startsWith("read_") && v == true) runCatching { LocalDate.parse(k.removePrefix("read_")) }.getOrNull()?.let { set.add(it.toString()) } }
        val last = p.getString("last_streak_date", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val streak = p.getInt("streak", 0)
        if (last != null) for (i in 0 until streak) set.add(last.minusDays(i.toLong()).toString())
        p.edit().putStringSet(KEY, set).putBoolean("tree_migrated", true).apply()
    }

    /** Records [date] as read. Returns true if this is a new day (repeat reads on the same day never count twice). */
    fun markRead(context: Context, date: LocalDate = LocalDate.now()): Boolean {
<<<<<<< HEAD
        val current = days(context)
        if (date in current) return false
        prefs(context).edit().putStringSet(KEY, (current + date).map { it.toString() }.toSet()).apply()
        return true
    }

    /** Pushes local days to the server and merges back whatever the server knows (union). Safe to call offline. */
    suspend fun sync(context: Context): Set<LocalDate>? = try {
        val local = days(context)
        val recent = local.sortedDescending().take(400).map { it.toString() }
        val tree = ApiClient.api.recordReading(LocalDate.now().toString(), TreeReadBody(recent, markToday = LocalDate.now() in local))
        val merged = local + tree.history.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        prefs(context).edit().putStringSet(KEY, merged.map { it.toString() }.toSet()).apply()
        merged
=======
        if (!enabled(context)) return false
        val current = days(context)
        if (date in current) return false
        // Keep every day ever read (full history); only the days on/after the restart date count (see days()).
        val all = prefs(context).getStringSet(KEY, emptySet()).orEmpty()
        prefs(context).edit().putStringSet(KEY, all + date.toString()).apply()
        return true
    }

    /**
     * Pushes local days to the server and merges back whatever the server knows (union). Safe to call offline.
     * Also adopts the server's Wisdom Tree switch / restart date, so a second device or a reinstall stays consistent.
     */
    suspend fun syncState(context: Context): TreeDto? = try {
        val p = prefs(context)
        val allLocal = p.getStringSet(KEY, emptySet()).orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        val recent = allLocal.sortedDescending().take(400).map { it.toString() }
        val today = LocalDate.now()
        val tree = ApiClient.api.recordReading(today.toString(), TreeReadBody(recent, markToday = today in days(context) && enabled(context)))
        tree.enabled?.let { en -> p.edit().putBoolean("tree_enabled", en).apply() }
        tree.startedOn?.let { sd -> p.edit().putString("tree_started_on", sd).apply() }
        val merged = allLocal + tree.history.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        p.edit().putStringSet(KEY, merged.map { it.toString() }.toSet()).apply()
        tree
    } catch (_: Exception) { null }

    /** Back-compat wrapper: the merged set of reading days that count right now, or null when offline. */
    suspend fun sync(context: Context): Set<LocalDate>? = syncState(context)?.let { days(context) }

    /** Switch the tree on/off on the server first; the local state only changes when the server accepted it. */
    suspend fun setEnabled(context: Context, enabled: Boolean): TreeDto? = try {
        val dto = ApiClient.api.setTreeEnabled(TreeSettingsBody(enabled), LocalDate.now().toString())
        applyEnabled(context, enabled)
        dto.startedOn?.let { prefs(context).edit().putString("tree_started_on", it).apply() }
        dto
>>>>>>> settings_redesign
    } catch (_: Exception) { null }
}
