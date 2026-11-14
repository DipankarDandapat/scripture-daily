package com.scripturedaily.ambient

import android.content.Context
import android.content.SharedPreferences
import kotlin.random.Random

/**
 * Ambient Wisdom Layer — shared preferences and the pure (unit-tested) rules behind it.
 *
 * Everything here stays on the device. The verse itself always comes from the existing active-verse pipeline
 * (VerseSyncWorker → Room cache → `widget_*` prefs), so the lock screen, live wallpaper, widget, notification and Home
 * show the same verse and no extra API call is made per wallpaper frame or refresh.
 */
object AmbientWisdom {
    const val PREFS = "reader"   // same store as the rest of the app's reading preferences

    // Keys (existing keys are reused: "lock_screen" = lock-screen verse on/off, "frequency" = refresh interval)
    const val KEY_LOCK_SCREEN = "lock_screen"
    const val KEY_FREQUENCY = "frequency"
    const val KEY_IMAGE_ORDER = "wallpaper_image_order"
    const val KEY_LIVE_THEME = "live_wallpaper_theme"
    const val KEY_LIVE_MOTION = "live_wallpaper_motion"
    const val KEY_ACTIVITY_MODE = "activity_wisdom_mode"
    const val KEY_MANUAL_CATEGORY = "activity_wisdom_manual"
    const val KEY_LAST_ACTIVITY = "activity_wisdom_last"
    const val KEY_LAST_ACTIVITY_AT = "activity_wisdom_last_at"
    const val KEY_AMBIENT_NOTIFICATION = "ambient_notification"
    const val KEY_LIVE_WALLPAPER_ENABLED = "live_wallpaper_user_enabled"

    fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── Refresh frequency (values already used by VerseSyncWorker) ──
    val FREQUENCIES = listOf("Every hour" to "1 hour", "Every 6 hours" to "6 hours", "Every 12 hours" to "12 hours", "Every 24 hours" to "24 hours")

    // ── Background image rotation ────────────────────────────────────────────
    enum class ImageOrder(val key: String, val label: String, val detail: String) {
        SEQUENTIAL("sequential", "In order", "Walk through the collection from your selected image."),
        SHUFFLE("shuffle", "Shuffle", "Random order — no image repeats until all ${com.scripturedaily.lockscreen.WallpaperPacks.COUNT} have been shown.");
        companion object { fun of(raw: String?) = entries.firstOrNull { it.key == raw } ?: SEQUENTIAL }
    }

    /**
     * Image (1..[count]) for the [rotation]-th refresh. Both orders show every image once per cycle before any repeats.
     * Shuffle uses a fresh permutation per cycle (seeded, so it is stable across process restarts) and never shows the same
     * image twice in a row across a cycle boundary.
     */
    fun imageFor(order: ImageOrder, selected: Int, rotation: Int, count: Int, seed: Long): Int {
        require(count > 0)
        val r = rotation.coerceAtLeast(0)
        if (order == ImageOrder.SEQUENTIAL) return Math.floorMod(selected.coerceIn(1, count) - 1 + r, count) + 1
        val cycle = r / count
        val perm = permutation(cycle, count, seed)
        return perm[r % count]
    }

    private fun permutation(cycle: Int, count: Int, seed: Long): List<Int> {
        var previousLast = -1
        var p: MutableList<Int> = mutableListOf()
        for (c in 0..cycle) {
            p = (1..count).shuffled(Random(seed * 31 + c)).toMutableList()
            if (c > 0 && count > 1 && p.first() == previousLast) { val t = p[0]; p[0] = p[1]; p[1] = t }
            previousLast = p.last()
        }
        return p
    }

    // ── Live wallpaper themes ────────────────────────────────────────────────
    enum class LiveTheme(val key: String, val label: String, val emoji: String) {
        // Time-of-day (original)
        AUTO("auto", "Time of day", "🕐"),
        MORNING("morning", "Morning", "🌅"),
        AFTERNOON("afternoon", "Afternoon", "☀️"),
        EVENING("evening", "Evening", "🌇"),
        NIGHT("night", "Night", "🌙"),
        // New nature themes
        GOLDEN_HOUR("golden_hour", "Golden Hour", "✨"),
        MONSOON("monsoon", "Monsoon", "🌧️"),
        DESERT_DAWN("desert_dawn", "Desert Dawn", "🏜️"),
        FOREST("forest", "Forest", "🌿"),
        OCEAN("ocean", "Ocean", "🌊"),
        SNOW("snow", "Snow", "❄️"),
        LOTUS("lotus", "Lotus Pond", "🪷"),
        STARFIELD("starfield", "Starfield", "⭐"),
        CANDLELIGHT("candlelight", "Candlelight", "🕯️"),
        CHERRY_BLOSSOM("cherry_blossom", "Cherry Blossom", "🌸");

        companion object { fun of(raw: String?) = entries.firstOrNull { it.key == raw } ?: AUTO }
    }

    /** Morning 05–11, afternoon 11–17, evening 17–20, night 20–05 (local time). */
    fun themeForHour(hour: Int): LiveTheme = when (Math.floorMod(hour, 24)) {
        in 5..10 -> LiveTheme.MORNING
        in 11..16 -> LiveTheme.AFTERNOON
        in 17..19 -> LiveTheme.EVENING
        else -> LiveTheme.NIGHT
    }

    fun resolveTheme(choice: LiveTheme, hour: Int): LiveTheme = if (choice == LiveTheme.AUTO) themeForHour(hour) else choice

    /** Suggested default theme for a tradition (used when user hasn't picked one yet). */
    fun defaultThemeForReligion(religionId: Int): LiveTheme = when (religionId) {
        1 -> LiveTheme.LOTUS          // Hinduism
        2 -> LiveTheme.MORNING        // Christianity
        3 -> LiveTheme.DESERT_DAWN    // Islam
        4 -> LiveTheme.CHERRY_BLOSSOM // Buddhism
        else -> LiveTheme.AUTO
    }

    enum class Motion(val key: String, val label: String) {
        SUBTLE("subtle", "Gentle motion"), STILL("still", "Still");
        companion object { fun of(raw: String?) = entries.firstOrNull { it.key == raw } ?: SUBTLE }
    }

    // ── Activity-aware wisdom ────────────────────────────────────────────────
    enum class Category(val key: String, val label: String, val topic: String, val emoji: String) {
        ENERGY("energy", "Energy & motivation", "hope", "🌅"),
        STRENGTH("strength", "Strength & perseverance", "courage", "💪"),
        FOCUS("focus", "Focus & wisdom", "wisdom", "🎯"),
        PEACE("peace", "Peace & calm", "peace", "🌙"),
        PATIENCE("patience", "Patience on the way", "patience", "🧭");
        companion object { fun of(raw: String?) = entries.firstOrNull { it.key == raw } }
    }

    enum class Activity(val key: String, val label: String) {
        WALKING("walking", "Walking"), RUNNING("running", "Running"), STILL("still", "Sitting or working"),
        IN_VEHICLE("in_vehicle", "On the move"), CYCLING("cycling", "Cycling"), UNKNOWN("unknown", "Not detected yet");
        companion object { fun of(raw: String?) = entries.firstOrNull { it.key == raw } ?: UNKNOWN }
    }

    enum class ActivityMode(val key: String, val label: String, val detail: String) {
        OFF("off", "Off", "Verses are not tailored to what you're doing."),
        TIME_OF_DAY("time", "Time of day", "Calm at night, focus by day — no sensors or permissions."),
        ACTIVITY("activity", "Activity detection", "Uses Android activity recognition (walking, running, still). Stays on your phone."),
        MANUAL("manual", "Manual", "You choose the kind of verse you want.");
        companion object { fun of(raw: String?) = entries.firstOrNull { it.key == raw } ?: OFF }
    }

    const val ACTIVITY_FRESH_MS = 3 * 60 * 60 * 1000L

    fun recommend(mode: ActivityMode, activity: Activity, activityAgeMs: Long, hour: Int, manual: Category?): Category? = when (mode) {
        ActivityMode.OFF -> null
        ActivityMode.MANUAL -> manual ?: Category.PEACE
        ActivityMode.TIME_OF_DAY -> byTime(hour)
        ActivityMode.ACTIVITY -> {
            val a = if (activityAgeMs in 0..ACTIVITY_FRESH_MS) activity else Activity.UNKNOWN
            when (a) {
                Activity.WALKING -> Category.ENERGY
                Activity.RUNNING, Activity.CYCLING -> Category.STRENGTH
                Activity.IN_VEHICLE -> Category.PATIENCE
                Activity.STILL -> if (isNight(hour)) Category.PEACE else Category.FOCUS
                Activity.UNKNOWN -> byTime(hour)
            }
        }
    }

    private fun isNight(hour: Int) = Math.floorMod(hour, 24).let { it >= 21 || it < 5 }

    private fun byTime(hour: Int): Category = when (Math.floorMod(hour, 24)) {
        in 5..10 -> Category.ENERGY
        in 11..17 -> Category.FOCUS
        in 18..20 -> Category.PATIENCE
        else -> Category.PEACE
    }

    // ── Always-on / single-line excerpt ──────────────────────────────────────
    fun excerpt(text: String, max: Int = 60): String {
        val clean = text.replace(Regex("\\s+"), " ").trim().trim('\u201c', '\u201d', '"')
        if (clean.length <= max) return clean
        val firstSentence = clean.split(Regex("(?<=[.;!?।])\\s")).first()
        if (firstSentence.length in 20..max) return firstSentence.trimEnd()
        val cut = clean.take(max - 1)
        val space = cut.lastIndexOf(' ')
        return (if (space > max / 2) cut.take(space) else cut).trimEnd(',', ';', ':', ' ') + "…"
    }
}
