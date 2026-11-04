package com.scripturedaily.features.tree

import android.content.Context
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.TreeReadBody
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

    fun days(context: Context): Set<LocalDate> {
        val p = prefs(context)
        migrateLegacy(context)
        return p.getStringSet(KEY, emptySet()).orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
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
    } catch (_: Exception) { null }
}
