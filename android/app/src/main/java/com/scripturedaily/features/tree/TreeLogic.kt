package com.scripturedaily.features.tree

import java.time.LocalDate

/** Wisdom Tree rules (kept identical to backend/app/services/tree.py). Pure Kotlin so it is unit-testable. */
enum class TreeStage(val label: String, val first: Int, val last: Int, val emoji: String) {
    SPROUT("Sprout", 1, 3, "🌱"), LEAVES("Leaves", 4, 7, "🌿"), BRANCHES("Branches", 8, 14, "🌳"),
    FLOWERS("Flowers", 15, 21, "🌸"), FRUITS("Fruits", 22, 30, "🍎");

    val level get() = ordinal + 1
    val range get() = if (this == FRUITS) "Day $first–$last" else if (first == last) "Day $first" else "Day $first–$last"
}

data class TreeState(
    val totalDays: Int, val stage: TreeStage, val status: String, val currentStreak: Int, val longestStreak: Int,
    val readToday: Boolean, val lastRead: LocalDate?, val next: TreeStage?, val daysToNext: Int,
    val journeyLevel: Int, val journeyDay: Int, val journeyComplete: Boolean, val daysToJourneyComplete: Int
) {
    /** Five 30-reading-day journeys; each journey uses the same five visual growth milestones. */
    val level get() = journeyLevel
    fun unlocked(s: TreeStage) = journeyDay >= s.first
}

object TreeLogic {
    const val DAYS_PER_LEVEL = 30
    const val MAX_LEVELS = 5
    const val TOTAL_JOURNEY_DAYS = DAYS_PER_LEVEL * MAX_LEVELS

    /** Growth stage within the current 30-reading-day level. */
    fun stageFor(total: Int): TreeStage {
        if (total >= TOTAL_JOURNEY_DAYS) return TreeStage.FRUITS
        val dayInLevel = if (total <= 0) 0 else ((total - 1) % DAYS_PER_LEVEL) + 1
        return TreeStage.values().firstOrNull { dayInLevel <= it.last } ?: TreeStage.FRUITS
    }

    fun streaks(days: Set<LocalDate>, today: LocalDate): Pair<Int, Int> {
        if (days.isEmpty()) return 0 to 0
        val ordered = days.sorted()
        var longest = 1; var run = 1
        for (i in 1 until ordered.size) { run = if (ordered[i].toEpochDay() - ordered[i - 1].toEpochDay() == 1L) run + 1 else 1; longest = maxOf(longest, run) }
        val last = ordered.last()
        var current = 0
        if (today.toEpochDay() - last.toEpochDay() <= 1) {
            current = 1; var cursor = last
            while (days.contains(cursor.minusDays(1))) { cursor = cursor.minusDays(1); current++ }
        }
        return current to maxOf(longest, current)
    }

    fun state(days: Set<LocalDate>, today: LocalDate): TreeState {
        val total = days.size
        val journeyComplete = total >= TOTAL_JOURNEY_DAYS
        val journeyLevel = if (total == 0) 1 else minOf(MAX_LEVELS, ((total - 1) / DAYS_PER_LEVEL) + 1)
        val journeyDay = if (total == 0) 0 else ((total - 1) % DAYS_PER_LEVEL) + 1
        val stage = stageFor(total)
        val (current, longest) = streaks(days, today)
        val last = days.maxOrNull()
        val status = when {
            total == 0 -> "Ready to sprout"
            journeyComplete -> "Journey complete"
            days.contains(today) || (last != null && today.toEpochDay() - last.toEpochDay() <= 1) -> "Growing"
            else -> "Resting"            // growth is paused; nothing already earned is ever removed
        }
        val next = if (journeyComplete) null else TreeStage.values().firstOrNull { journeyDay < it.first }
        val daysToJourneyComplete = if (journeyComplete) 0 else TOTAL_JOURNEY_DAYS - total
        return TreeState(total, stage, status, current, longest, days.contains(today), last, next,
            next?.let { it.first - journeyDay } ?: 0, journeyLevel, journeyDay, journeyComplete, daysToJourneyComplete)
    }

    /** Encouraging, never guilt-based, copy for the card. */
    fun message(s: TreeState): String = when {
        s.totalDays == 0 -> "Read today's verse to plant your first seed."
        s.status == "Resting" -> "Your tree is resting and keeps everything it has grown. Read today's verse whenever you're ready."
        s.journeyComplete -> "You completed all five Wisdom Tree levels. Your achievement is saved permanently."
        s.readToday && s.next != null -> "Today's reading is done. ${s.daysToNext} more ${if (s.daysToNext == 1) "day" else "days"} to ${s.next.label}."
        s.readToday && s.next == null -> "Today's reading is done. Level ${s.level} is complete — your next level has begun."
        s.readToday -> "Today's reading is done. Your tree is flourishing."
        else -> "Keep reading daily to grow your tree and unlock beautiful milestones."
    }
}
