package com.scripturedaily.gate

import com.google.gson.JsonParser
import com.scripturedaily.config.ConfigSnapshot
import com.scripturedaily.config.ConfigSource
import com.scripturedaily.gate.AppGate.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppGateTest {
    private val hour = 3_600_000L
    private val now = 1_800_000_000_000L

    private fun snap(
        latest: String = "1.4.2", minimum: String = "1.0.0", force: Boolean = false, updateEnabled: Boolean = true,
        maintenance: Boolean = false, end: String? = null, cacheHours: Int = 12,
        source: ConfigSource = ConfigSource.NETWORK, fetchedAt: Long = now,
    ): ConfigSnapshot {
        val endJson = end?.let { "\"$it\"" } ?: "null"
        val json = """{"app_update":{"enabled":$updateEnabled,"latest_version":"$latest","minimum_supported_version":"$minimum","force_update":$force,
            "play_store_url":"https://play.google.com/store/apps/details?id=com.scripturedaily"},
            "maintenance_mode":{"enabled":$maintenance,"title":"We'll Be Back Soon!","estimated_end_time":$endJson,"enforce_cached_for_hours":$cacheHours}}"""
        return ConfigSnapshot("v", JsonParser.parseString(json).asJsonObject, source, fetchedAt)
    }

    // ── version compare (ported from the removed ForceUpdateManagerTest) ──
    @Test fun `numeric version compare`() {
        assertTrue(AppGate.isOlder("1.3.2", "1.3.3")); assertTrue(AppGate.isOlder("1.3.2", "1.4.0")); assertTrue(AppGate.isOlder("1.3.2", "2.0.0"))
        assertTrue(AppGate.isOlder("1.9.0", "1.10.0"))
        assertFalse(AppGate.isOlder("1.3.2", "1.3.2")); assertFalse(AppGate.isOlder("1.3.2", "1.3.1"))
        assertFalse(AppGate.isOlder("1.3.0", "1.3")); assertFalse(AppGate.isOlder("1.3.2", ""))
    }

    // ── the four update scenarios ──
    @Test fun `scenario 1 and 4 - supported and latest version opens normally`() {
        assertEquals(Decision.Open, AppGate.decide(snap(latest = "1.4.2"), "1.4.2", true, now))
        assertEquals(Decision.Open, AppGate.decide(snap(latest = "1.4.0"), "1.4.2", true, now))   // installed newer than latest
    }

    @Test fun `scenario 2 - newer version gives an optional prompt`() {
        assertTrue(AppGate.decide(snap(latest = "1.5.0"), "1.4.2", true, now) is Decision.OptionalUpdate)
    }

    @Test fun `scenario 3 - below minimum or forced is mandatory`() {
        assertTrue(AppGate.decide(snap(latest = "1.5.0", minimum = "1.5.0"), "1.4.2", true, now) is Decision.MandatoryUpdate)
        assertTrue(AppGate.decide(snap(latest = "1.5.0", force = true), "1.4.2", true, now) is Decision.MandatoryUpdate)
        assertEquals(Decision.Open, AppGate.decide(snap(latest = "1.4.2", force = true), "1.4.2", true, now))
    }

    @Test fun `update checks can be switched off from the backend`() {
        assertEquals(Decision.Open, AppGate.decide(snap(latest = "9.0", minimum = "9.0", updateEnabled = false), "1.4.2", true, now))
    }

    // ── priority ──
    @Test fun `mandatory update wins over maintenance, maintenance wins over optional update`() {
        assertTrue(AppGate.decide(snap(latest = "2.0", minimum = "2.0", maintenance = true), "1.4.2", true, now) is Decision.MandatoryUpdate)
        assertTrue(AppGate.decide(snap(latest = "2.0", maintenance = true), "1.4.2", true, now) is Decision.Maintenance)
        assertTrue(AppGate.decide(snap(maintenance = true), "1.4.2", true, now) is Decision.Maintenance)
    }

    // ── offline behaviour ──
    @Test fun `never assumes maintenance - first launch offline asks to retry or continue`() {
        val defaults = ConfigSnapshot("defaults", JsonParser.parseString("{}").asJsonObject, ConfigSource.DEFAULTS, 0L)
        assertEquals(Decision.Unverified, AppGate.decide(defaults, "1.4.2", false, now))
    }

    @Test fun `cached maintenance blocks only within its window and before its end time`() {
        val recent = snap(maintenance = true, source = ConfigSource.CACHE, fetchedAt = now - 2 * hour)
        val d = AppGate.decide(recent, "1.4.2", false, now)
        assertTrue(d is Decision.Maintenance && d.fromCache)
        assertEquals(Decision.Open, AppGate.decide(snap(maintenance = true, source = ConfigSource.CACHE, fetchedAt = now - 13 * hour), "1.4.2", false, now))
        val ended = snap(maintenance = true, end = "2020-01-01T00:00:00Z", source = ConfigSource.CACHE, fetchedAt = now - hour)
        assertEquals(Decision.Open, AppGate.decide(ended, "1.4.2", false, now))
        // a reachable server is always obeyed, end time or not
        assertTrue(AppGate.decide(snap(maintenance = true, end = "2020-01-01T00:00:00Z"), "1.4.2", true, now) is Decision.Maintenance)
    }

    @Test fun `cached normal config keeps working offline`() {
        assertEquals(Decision.Open, AppGate.decide(snap(source = ConfigSource.CACHE, fetchedAt = now - 100 * hour), "1.4.2", false, now))
    }

    @Test fun `malformed values fall back to safe defaults`() {
        val bad = ConfigSnapshot("v", JsonParser.parseString("""{"app_update":"oops","maintenance_mode":{"enabled":"yes?"}}""").asJsonObject, ConfigSource.NETWORK, now)
        assertEquals(Decision.Open, AppGate.decide(bad, "1.4.2", true, now))
        val m = AppGate.maintenanceConfig(JsonParser.parseString("""{"enabled":true,"estimated_end_time":"2026-11-12T06:30:00+05:30","title":""}""").asJsonObject)
        assertEquals("We'll Be Back Soon!", m.title); assertEquals(1794445200000L, m.estimatedEnd!!.toEpochMilli())
    }

    @Test fun `optional prompt comes back after the reminder interval or for a newer version`() {
        val u = AppGate.UpdateConfig(latestVersion = "1.5.0", optionalReminderHours = 24)
        assertFalse(AppGate.shouldPromptOptional(u, "1.5.0", now - hour, now))
        assertTrue(AppGate.shouldPromptOptional(u, "1.5.0", now - 25 * hour, now))
        assertTrue(AppGate.shouldPromptOptional(u, "1.4.9", now - hour, now))
    }
}
