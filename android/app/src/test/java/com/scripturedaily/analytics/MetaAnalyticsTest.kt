package com.scripturedaily.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaAnalyticsTest {

    // ── sensitive-key blocklist ──
    @Test fun `keys that were sent before this fix are blocked`() {
        listOf("tradition", "scripture", "mood").forEach { assertTrue(it, MetaAnalytics.isSensitiveKey(it)) }
    }

    @Test fun `variants and other sensitive attributes are blocked regardless of case`() {
        listOf(
            "religion", "Religion_ID", "faith", "belief", "denomination", "scripture_name", "SCRIPTURE", "verse_id", "verse_text",
            "story_id", "book", "chapter", "prayer", "mood_code", "emotion", "mental_state", "health", "sexual_orientation",
            "ethnicity", "political_view",
        ).forEach { assertTrue(it, MetaAnalytics.isSensitiveKey(it)) }
    }

    @Test fun `neutral keys are allowed`() {
        listOf("source", "language_id", "fb_content_type", "step", "screen").forEach { assertFalse(it, MetaAnalytics.isSensitiveKey(it)) }
    }

    @Test fun `safePairs drops only sensitive pairs and keeps order`() {
        val input = listOf("tradition" to "Christianity", "language_id" to "1", "scripture" to "Bible", "mood" to "anxious", "source" to "reminders")
        assertEquals(listOf("language_id" to "1", "source" to "reminders"), MetaAnalytics.safePairs(input))
    }

    // ── first_verse_read fires once per install ──
    private class FakeStore : MetaAnalytics.OnceStore {
        val keys = mutableSetOf<String>()
        override fun isSet(key: String) = key in keys
        override fun set(key: String) { keys += key }
    }

    @Test fun `first verse read is claimed exactly once`() {
        val store = FakeStore()
        assertTrue(MetaAnalytics.claimOnce(store, MetaAnalytics.FIRST_VERSE_READ_KEY))
        assertFalse(MetaAnalytics.claimOnce(store, MetaAnalytics.FIRST_VERSE_READ_KEY))
        assertFalse(MetaAnalytics.claimOnce(store, MetaAnalytics.FIRST_VERSE_READ_KEY))
        assertTrue(MetaAnalytics.FIRST_VERSE_READ_KEY in store.keys)
    }

    @Test fun `a persisted flag (later app launch) never fires again`() {
        val store = FakeStore().apply { set(MetaAnalytics.FIRST_VERSE_READ_KEY) }
        assertFalse(MetaAnalytics.claimOnce(store, MetaAnalytics.FIRST_VERSE_READ_KEY))
    }

    @Test fun `a fresh install fires`() {
        assertTrue(MetaAnalytics.claimOnce(FakeStore(), MetaAnalytics.FIRST_VERSE_READ_KEY))
    }
}
