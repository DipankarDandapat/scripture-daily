package com.scripturedaily.ambient

import com.scripturedaily.ambient.AmbientWisdom.Activity
import com.scripturedaily.ambient.AmbientWisdom.ActivityMode
import com.scripturedaily.ambient.AmbientWisdom.Category
import com.scripturedaily.ambient.AmbientWisdom.ImageOrder
import com.scripturedaily.ambient.AmbientWisdom.LiveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientWisdomTest {
    private val count = 20

    @Test fun `sequential rotation starts at the selected image and shows all before repeating`() {
        val shown = (0 until count).map { AmbientWisdom.imageFor(ImageOrder.SEQUENTIAL, 5, it, count, 1L) }
        assertEquals(5, shown.first()); assertEquals((1..count).toSet(), shown.toSet())
        assertEquals(5, AmbientWisdom.imageFor(ImageOrder.SEQUENTIAL, 5, count, count, 1L))
    }

    @Test fun `shuffle shows every image once per cycle and never repeats across a cycle boundary`() {
        for (seed in listOf(1L, 42L, 987654321L)) {
            val seq = (0 until count * 6).map { AmbientWisdom.imageFor(ImageOrder.SHUFFLE, 1, it, count, seed) }
            seq.chunked(count).forEach { cycle -> assertEquals((1..count).toSet(), cycle.toSet()) }
            seq.zipWithNext().forEach { (a, b) -> assertNotEquals("back-to-back repeat for seed $seed", a, b) }
        }
    }

    @Test fun `shuffle is stable for the same seed and differs between seeds`() {
        val a = (0 until count).map { AmbientWisdom.imageFor(ImageOrder.SHUFFLE, 1, it, count, 7L) }
        assertEquals(a, (0 until count).map { AmbientWisdom.imageFor(ImageOrder.SHUFFLE, 1, it, count, 7L) })
        assertNotEquals(a, (0 until count).map { AmbientWisdom.imageFor(ImageOrder.SHUFFLE, 1, it, count, 8L) })
    }

    @Test fun `time of day themes`() {
        assertEquals(LiveTheme.MORNING, AmbientWisdom.themeForHour(6))
        assertEquals(LiveTheme.AFTERNOON, AmbientWisdom.themeForHour(14))
        assertEquals(LiveTheme.EVENING, AmbientWisdom.themeForHour(18))
        assertEquals(LiveTheme.NIGHT, AmbientWisdom.themeForHour(23)); assertEquals(LiveTheme.NIGHT, AmbientWisdom.themeForHour(2))
        assertEquals(LiveTheme.EVENING, AmbientWisdom.resolveTheme(LiveTheme.EVENING, 9))
    }

    @Test fun `activity recommendations follow the brief`() {
        val fresh = 60_000L
        assertEquals(Category.ENERGY, AmbientWisdom.recommend(ActivityMode.ACTIVITY, Activity.WALKING, fresh, 12, null))
        assertEquals(Category.STRENGTH, AmbientWisdom.recommend(ActivityMode.ACTIVITY, Activity.RUNNING, fresh, 12, null))
        assertEquals(Category.FOCUS, AmbientWisdom.recommend(ActivityMode.ACTIVITY, Activity.STILL, fresh, 11, null))
        assertEquals(Category.PEACE, AmbientWisdom.recommend(ActivityMode.ACTIVITY, Activity.STILL, fresh, 23, null))
    }

    @Test fun `stale or missing activity falls back to time of day, off means no suggestion, manual is respected`() {
        assertEquals(Category.PEACE, AmbientWisdom.recommend(ActivityMode.ACTIVITY, Activity.RUNNING, AmbientWisdom.ACTIVITY_FRESH_MS + 1, 23, null))
        assertEquals(Category.ENERGY, AmbientWisdom.recommend(ActivityMode.TIME_OF_DAY, Activity.UNKNOWN, 0, 7, null))
        assertNull(AmbientWisdom.recommend(ActivityMode.OFF, Activity.RUNNING, 0, 7, null))
        assertEquals(Category.STRENGTH, AmbientWisdom.recommend(ActivityMode.MANUAL, Activity.STILL, 0, 23, Category.STRENGTH))
    }

    @Test fun `every category maps to an existing Universal Truths topic`() {
        val topics = setOf("love", "patience", "forgiveness", "gratitude", "compassion", "peace", "truth", "humility", "hope", "courage", "charity", "wisdom", "faith", "self-control")
        Category.entries.forEach { assertTrue(it.topic in topics) }
    }

    @Test fun `excerpt is a single short line cut on a word boundary`() {
        val long = "For one who is not at peace within there is no steady wisdom, and without peace how can there be happiness?"
        val e = AmbientWisdom.excerpt(long)
        assertTrue(e.length <= 60); assertTrue(e.endsWith("…")); assertTrue(!e.contains("\n"))
        assertEquals("Be still.", AmbientWisdom.excerpt("  Be\nstill. "))
        assertEquals("Love is patient, love is kind.", AmbientWisdom.excerpt("Love is patient, love is kind. It does not envy, it does not boast, it is not proud."))
    }
}
