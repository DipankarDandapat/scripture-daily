package com.scripturedaily.features

import com.scripturedaily.features.home.VerseThemes
import com.scripturedaily.features.share.StudioTemplates
import com.scripturedaily.features.tree.TreeLogic
import com.scripturedaily.features.tree.TreeStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VerseThemesTest {
    @Test fun `there are ten distinct themes`() {
        assertEquals(10, VerseThemes.ALL.size)
        assertEquals(10, VerseThemes.ALL.map { it.name }.toSet().size)
        assertEquals((1..10).toList(), VerseThemes.ALL.map { it.index })
    }

    @Test fun `theme advances one per day and loops after ten`() {
        val start = LocalDate.of(2026, 11, 4)
        val seq = (0 until 25).map { VerseThemes.indexFor(start.plusDays(it.toLong())) }
        for (i in 1 until seq.size) assertEquals((seq[i - 1] + 1) % 10, seq[i])
        assertEquals(seq[0], seq[10]); assertEquals(seq[0], seq[20])
    }

    @Test fun `theme is stable within a day and deterministic`() {
        val d = LocalDate.of(2027, 1, 15)
        repeat(5) { assertEquals(VerseThemes.forDate(d), VerseThemes.forDate(LocalDate.of(2027, 1, 15))) }
        assertNotEquals(VerseThemes.forDate(d), VerseThemes.forDate(d.plusDays(1)))
    }

    @Test fun `every theme keeps verse text readable (WCAG AA)`() {
        VerseThemes.ALL.forEach { t ->
            for (stop in listOf(t.top, t.bottom)) {
                assertTrue("${t.name} text vs $stop", VerseThemes.contrast(t.text, stop) >= 4.5)
                assertTrue("${t.name} reference vs $stop", VerseThemes.contrast(t.reference, stop) >= 4.5)
            }
        }
    }
}

class TreeLogicTest {
    private val today = LocalDate.of(2026, 11, 4)
    private fun days(vararg offsets: Int) = offsets.map { today.minusDays(it.toLong()) }.toSet()

    @Test fun `stage thresholds match the brief`() {
        assertEquals(TreeStage.SPROUT, TreeLogic.stageFor(1)); assertEquals(TreeStage.SPROUT, TreeLogic.stageFor(3))
        assertEquals(TreeStage.LEAVES, TreeLogic.stageFor(4)); assertEquals(TreeStage.LEAVES, TreeLogic.stageFor(7))
        assertEquals(TreeStage.BRANCHES, TreeLogic.stageFor(8)); assertEquals(TreeStage.BRANCHES, TreeLogic.stageFor(14))
        assertEquals(TreeStage.FLOWERS, TreeLogic.stageFor(15)); assertEquals(TreeStage.FLOWERS, TreeLogic.stageFor(21))
        assertEquals(TreeStage.FRUITS, TreeLogic.stageFor(22)); assertEquals(TreeStage.FRUITS, TreeLogic.stageFor(30))
<<<<<<< HEAD
        assertEquals(TreeStage.FRUITS, TreeLogic.stageFor(90))
=======
        assertEquals(TreeStage.SPROUT, TreeLogic.stageFor(31))
        assertEquals(TreeStage.FRUITS, TreeLogic.stageFor(60))
        assertEquals(TreeStage.SPROUT, TreeLogic.stageFor(61))
        assertEquals(TreeStage.FRUITS, TreeLogic.stageFor(150))
>>>>>>> settings_redesign
    }

    @Test fun `a missed day pauses growth but never removes progress`() {
        val grown = (10..20).map { today.minusDays(it.toLong()) }.toSet()   // 11 days read, then a long break
        val s = TreeLogic.state(grown, today)
        assertEquals(11, s.totalDays); assertEquals(TreeStage.BRANCHES, s.stage); assertEquals("Resting", s.status)
<<<<<<< HEAD
=======
        assertEquals(1, s.level); assertEquals(11, s.journeyDay); assertFalse(s.journeyComplete)
>>>>>>> settings_redesign
        assertEquals(0, s.currentStreak); assertEquals(11, s.longestStreak)
        assertTrue(s.unlocked(TreeStage.LEAVES) && s.unlocked(TreeStage.BRANCHES) && !s.unlocked(TreeStage.FLOWERS))
        val back = TreeLogic.state(grown + today, today)
        assertEquals(12, back.totalDays); assertEquals("Growing", back.status)
    }

    @Test fun `current streak and longest streak are tracked separately`() {
        val s = TreeLogic.state(days(0, 1, 2, 10, 11, 12, 13, 14), today)
        assertEquals(3, s.currentStreak); assertEquals(5, s.longestStreak); assertEquals(8, s.totalDays)
        assertEquals(1, TreeLogic.state(days(1), today).currentStreak)   // yesterday still counts until today ends
        assertEquals(0, TreeLogic.state(days(2), today).currentStreak)
    }

    @Test fun `zero days is ready to sprout and messages are never guilt based`() {
        val s = TreeLogic.state(emptySet(), today)
        assertEquals("Ready to sprout", s.status); assertFalse(TreeLogic.message(s).contains("lost", true))
        val resting = TreeLogic.state(days(30), today)
        assertTrue(TreeLogic.message(resting).contains("keeps everything"))
    }

<<<<<<< HEAD
=======
    @Test fun `each 30 reading days starts the next level on the following reading day`() {
        val levelOneComplete = TreeLogic.state((1..30).map { today.minusDays(it.toLong()) }.toSet(), today)
        assertEquals(1, levelOneComplete.level)
        assertEquals(30, levelOneComplete.journeyDay)
        assertEquals(TreeStage.FRUITS, levelOneComplete.stage)
        assertFalse(levelOneComplete.journeyComplete)

        val levelTwoStarted = TreeLogic.state((1..31).map { today.minusDays(it.toLong()) }.toSet(), today)
        assertEquals(2, levelTwoStarted.level)
        assertEquals(1, levelTwoStarted.journeyDay)
        assertEquals(TreeStage.SPROUT, levelTwoStarted.stage)
    }

    @Test fun `after five levels home journey is complete and all progress remains counted`() {
        val completed = TreeLogic.state((1..150).map { today.minusDays(it.toLong()) }.toSet(), today)
        assertEquals(5, completed.level)
        assertEquals(150, completed.totalDays)
        assertTrue(completed.journeyComplete)
        assertEquals(0, completed.daysToJourneyComplete)
        assertEquals(155, TreeLogic.state((1..155).map { today.minusDays(it.toLong()) }.toSet(), today).totalDays)
    }

>>>>>>> settings_redesign
    @Test fun `same day counted once because progress is a set of dates`() {
        assertEquals(1, (setOf(today) + today).size)
    }
}

class StudioTemplateVisibilityTest {
    @Test fun `common templates are visible to every tradition`() {
        for (code in listOf("hinduism", "christianity", "islam", "buddhism", "general wisdom", null)) {
            val common = StudioTemplates.BUILT_IN.filter { it.isCommon }
            assertTrue(StudioTemplates.visible(StudioTemplates.BUILT_IN, code).containsAll(common))
        }
    }

    @Test fun `tradition templates only appear for the matching tradition`() {
        for (code in listOf("hinduism", "christianity", "islam", "buddhism", "general wisdom")) {
            val visible = StudioTemplates.visible(StudioTemplates.BUILT_IN, code)
            assertTrue(visible.any { it.tradition == code })
            assertTrue(visible.all { it.tradition == null || it.tradition == code })
        }
        assertTrue(StudioTemplates.visible(StudioTemplates.BUILT_IN, null).all { it.isCommon })
        // the user's own tradition is listed before the common templates
        assertFalse(StudioTemplates.visible(StudioTemplates.BUILT_IN, "islam").first().isCommon)
    }

    @Test fun `general users never see religious templates`() {
        val general = StudioTemplates.visible(StudioTemplates.BUILT_IN, "general wisdom")
        assertTrue(general.none { it.tradition in listOf("hinduism", "christianity", "islam", "buddhism") })
        assertTrue(general.none { it.motif in listOf("om", "cross", "crescent", "dharma", "lotus", "mosque") })
    }
}
