package com.scripturedaily

import com.scripturedaily.data.VerseScheduler
import com.scripturedaily.features.media.MusicStyle
import com.scripturedaily.features.media.MusicSynth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class MusicAndScheduleTest {
    @Test fun everyTrackIsAlwaysAvailableAndTraditionComesFirst() {
        val hindu = MusicStyle.ordered(1)
        assertEquals(MusicStyle.values().toSet(), hindu.toSet())
        assertTrue(hindu.take(3).all { it.tradition == 1 })
        assertEquals(MusicStyle.NONE, hindu.last())
        assertEquals(MusicStyle.values().toSet(), MusicStyle.ordered(4).toSet())
    }

    @Test fun tracksAreAudibleAndDoNotClip() {
        for (style in MusicStyle.values().filter { it != MusicStyle.NONE }) {
            val pcm = MusicSynth.generate(style, 8, 0.8f)
            val rms = sqrt(pcm.sumOf { it.toDouble() * it } / pcm.size) / 32768.0
            val peak = pcm.maxOf { abs(it.toInt()) } / 32768.0
            assertTrue("$style too quiet: rms=$rms", rms > 0.05)
            assertTrue("$style clips: peak=$peak", peak < 0.98)
        }
    }

    @Test fun frequencyMapsToHours() {
        assertEquals(1L, VerseScheduler.intervalHours("Every hour"))
        assertEquals(6L, VerseScheduler.intervalHours("Every 6 hours"))
        assertEquals(12L, VerseScheduler.intervalHours("Every 12 hours"))
        assertEquals(24L, VerseScheduler.intervalHours("Every 24 hours"))
    }
}
