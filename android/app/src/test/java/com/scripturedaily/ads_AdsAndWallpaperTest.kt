package com.scripturedaily

import com.google.gson.JsonParser
import com.scripturedaily.ads.AdConfig
import com.scripturedaily.ads.AdFormat
import com.scripturedaily.ads.AdSavedVerses
import com.scripturedaily.ads.savedVerseAdPositions
import com.scripturedaily.ads.shouldInsertSavedVerseAd
import com.scripturedaily.config.AppConfigManager
import com.scripturedaily.config.ConfigSnapshot
import com.scripturedaily.config.ConfigSource
import com.scripturedaily.lockscreen.WallpaperMode
import com.scripturedaily.lockscreen.WallpaperPacks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"

private fun snapshot(json: String) = ConfigSnapshot("v", JsonParser.parseString(json).asJsonObject, ConfigSource.NETWORK, 0L)

private fun adsJson(enabled: Boolean = true, homeEnabled: Boolean = true, extra: String = "") = """{
  "ads_enabled": $enabled, "ads_use_test_ids": true,
  "ads_test_units": {"NATIVE_ADVANCED": "$TEST_NATIVE", "BANNER_ADAPTIVE_ANCHORED": "$TEST_BANNER", "BANNER_ADAPTIVE_INLINE": "$TEST_BANNER"},
  "ads_production_units": {"NATIVE_ADVANCED": "", "BANNER_ADAPTIVE_ANCHORED": ""},
  "ads_global": {"max_ads_per_screen": 3, "min_seconds_between_fullscreen_ads": 180, "min_seconds_between_app_open_ads": 14400},
  "ads_placements": {
    "home_after_daily_verse": {"enabled": $homeEnabled, "screen": "home", "format": "NATIVE_ADVANCED", "ad_unit_id": ""},
    "home_bottom_banner": {"enabled": false, "screen": "home", "format": "BANNER_ADAPTIVE_ANCHORED", "ad_unit_id": ""},
    "reflection_ad": {"enabled": true, "screen": "reflection_editor", "format": "NATIVE_ADVANCED", "ad_unit_id": ""}
  },
  "ads_blocked_screens": ["reflection_editor"] $extra }"""

class AdConfigTest {
    @Test fun `global switch off disables every placement`() {
        val cfg = AdConfig.from(snapshot(adsJson(enabled = false)))
        assertNull(cfg.resolve("home_after_daily_verse"))
    }

    @Test fun `enabled placement resolves to the official test unit`() {
        val r = AdConfig.from(snapshot(adsJson())).resolve("home_after_daily_verse")
        assertNotNull(r); assertEquals(TEST_NATIVE, r!!.unitId); assertEquals(AdFormat.NATIVE_ADVANCED, r.format); assertTrue(r.usingTestUnit)
    }

    @Test fun `disabled or unknown placements never resolve`() {
        val cfg = AdConfig.from(snapshot(adsJson()))
        assertNull(cfg.resolve("home_bottom_banner")); assertNull(cfg.resolve("does_not_exist"))
        assertNull(AdConfig.from(snapshot(adsJson(homeEnabled = false))).resolve("home_after_daily_verse"))
    }

    @Test fun `blocked screens such as the reflection editor never show ads`() {
        assertNull(AdConfig.from(snapshot(adsJson())).resolve("reflection_ad"))
    }

    @Test fun `format override picks the matching unit and a missing unit disables the slot`() {
        val cfg = AdConfig.from(snapshot(adsJson()))
        assertEquals(TEST_BANNER, cfg.resolve("home_after_daily_verse", AdFormat.BANNER_ADAPTIVE_INLINE)!!.unitId)
        assertNull(cfg.resolve("home_after_daily_verse", AdFormat.REWARDED))   // no unit configured for that format
    }

    @Test fun `malformed or empty unit ids are never requested`() {
        val bad = adsJson().replace(TEST_NATIVE, "not-a-unit")
        assertNull(AdConfig.from(snapshot(bad)).resolve("home_after_daily_verse"))
    }

    @Test fun `malformed config degrades to no ads instead of crashing`() {
        assertFalse(AdConfig.from(snapshot("""{"ads_enabled": true, "ads_placements": 5}""")).enabled && AdConfig.from(snapshot("""{"ads_enabled": true, "ads_placements": 5}""")).placements.isNotEmpty())
        assertNull(AdConfig.from(snapshot("{}")).resolve("home_after_daily_verse"))
    }

    @Test fun `built-in fallback config keeps ads off`() {
        assertFalse(AdConfig.from(snapshot(AppConfigManager.SAFE_DEFAULTS.toString())).enabled)
    }

    @Test fun `debug builds use test units even if the server says production`() {
        val cfg = AdConfig.from(snapshot(adsJson().replace("\"ads_use_test_ids\": true", "\"ads_use_test_ids\": false")))
        assertTrue(cfg.usesTestUnits)   // BuildConfig.DEBUG in unit tests
    }
}

class SavedVerseAdsTest {
    private val positions = { n: Int, first: Int, every: Int, max: Int -> savedVerseAdPositions(n, AdSavedVerses(first, every, max)).toList() }

    @Test fun `ads follow saved verses 1 3 5 7`() {
        assertEquals(listOf(0, 2, 4), positions(8, 1, 2, 3))     // 0-based indexes of saved verses 1, 3, 5
        assertEquals(listOf(0, 2, 4, 6), positions(8, 1, 2, 10))
    }
    @Test fun `zero saved verses has no ad and one saved verse has the first ad`() {
        assertEquals(emptyList<Int>(), positions(0, 1, 2, 3)); assertEquals(listOf(0), positions(1, 1, 2, 3)); assertEquals(listOf(0), positions(2, 1, 2, 3))
    }
    @Test fun `interval and first position are configurable`() {
        assertEquals(listOf(2, 5, 8), positions(10, 3, 3, 10))
        assertTrue(shouldInsertSavedVerseAd(2, 3, 3)); assertFalse(shouldInsertSavedVerseAd(3, 3, 3))
        assertEquals(listOf(1), positions(10, 2, 0, 10))         // interval 0 => only the first ad
        assertEquals(emptyList<Int>(), positions(10, 0, 2, 10))  // first = 0 => disabled
    }
    @Test fun `max inline ads is respected`() { assertEquals(2, positions(20, 1, 2, 2).size) }
}

class WallpaperModeTest {
    @Test fun `legacy and unknown values map to change verse and image together`() {
        listOf(null, "", "change_verse", "keep_same_verse", "Change verse + wallpaper", "garbage").forEach { assertEquals(it, WallpaperMode.VERSE_AND_IMAGE, WallpaperMode.normalize(it)) }
        assertEquals(WallpaperMode.KEEP_IMAGE, WallpaperMode.normalize("keep_image")); assertEquals(WallpaperMode.COLORS, WallpaperMode.normalize("COLORS"))
    }
    @Test fun `image rotation walks the whole pack and wraps`() {
        assertEquals(1, WallpaperPacks.rotatedIndex(1, 0)); assertEquals(2, WallpaperPacks.rotatedIndex(1, 1)); assertEquals(1, WallpaperPacks.rotatedIndex(1, 20))
        assertEquals(20, WallpaperPacks.rotatedIndex(1, 19)); assertEquals(5, WallpaperPacks.rotatedIndex(20, 5))
        assertEquals(20, (0 until 20).map { WallpaperPacks.rotatedIndex(7, it) }.toSet().size)
    }
    @Test fun `each tradition has its own pack and out-of-range indexes are clamped`() {
        assertEquals(listOf("hinduism", "christianity", "islam", "buddhism", "general"), (1..5).map { WallpaperPacks.slug(it) })
        assertEquals(1, WallpaperPacks.clampIndex(0)); assertEquals(20, WallpaperPacks.clampIndex(99))
    }
}
