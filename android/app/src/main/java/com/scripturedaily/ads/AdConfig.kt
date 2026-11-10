package com.scripturedaily.ads

import com.google.gson.JsonObject
import com.scripturedaily.BuildConfig
import com.scripturedaily.config.ConfigSnapshot

/** Supported ad formats (names match the backend `ads_placements[*].format`). */
enum class AdFormat(val unitKey: String) {
    APP_OPEN("APP_OPEN"),
    BANNER_ADAPTIVE_ANCHORED("BANNER_ADAPTIVE_ANCHORED"), BANNER_ADAPTIVE_INLINE("BANNER_ADAPTIVE_INLINE"),
    BANNER_STANDARD_320_50("BANNER_FIXED"), BANNER_STANDARD_300_50("BANNER_FIXED"), BANNER_STANDARD_320_100("BANNER_FIXED"),
    BANNER_MEDIUM_RECTANGLE("BANNER_FIXED"), BANNER_LARGE_ADAPTIVE("BANNER_ADAPTIVE_ANCHORED"), BANNER_FIXED("BANNER_FIXED"),
    NATIVE_ADVANCED("NATIVE_ADVANCED"), NATIVE_VIDEO("NATIVE_VIDEO"),
    INTERSTITIAL("INTERSTITIAL"), REWARDED("REWARDED"), REWARDED_INTERSTITIAL("REWARDED_INTERSTITIAL");

    val isBanner get() = name.startsWith("BANNER")
    val isNative get() = this == NATIVE_ADVANCED || this == NATIVE_VIDEO
    val isFullScreen get() = this == INTERSTITIAL || this == REWARDED || this == REWARDED_INTERSTITIAL || this == APP_OPEN

    companion object { fun parse(raw: String?): AdFormat? = values().firstOrNull { it.name == raw } }
}

data class AdPlacement(val id: String, val enabled: Boolean, val screen: String, val format: AdFormat?, val adUnitId: String)
data class AdSavedVerses(val firstAdAfterItems: Int = 1, val repeatIntervalItems: Int = 2, val maxInlineAds: Int = 3, val repeatFormat: AdFormat = AdFormat.BANNER_ADAPTIVE_INLINE)
data class AdPrivacy(val requireConsent: Boolean = true, val childDirected: Boolean = false, val underAge: Boolean = false, val maxRating: String = "PG")

/** A placement that is allowed to load right now, with the exact unit id to request. */
data class ResolvedPlacement(val id: String, val screen: String, val format: AdFormat, val unitId: String, val usingTestUnit: Boolean)

/** Parsed `ads_*` configuration. Immutable; replaced whenever the backend config changes. */
data class AdConfig(
    val enabled: Boolean = false,
    val useTestIds: Boolean = true,
    val appId: String = "",
    val testUnits: Map<String, String> = emptyMap(),
    val productionUnits: Map<String, String> = emptyMap(),
    val maxAdsPerScreen: Int = 3,
    val minSecondsBetweenFullscreen: Int = 180,
    val minSecondsBetweenAppOpen: Int = 14_400,
    val placements: Map<String, AdPlacement> = emptyMap(),
    val saved: AdSavedVerses = AdSavedVerses(),
    val blockedScreens: Set<String> = emptySet(),
    val privacy: AdPrivacy = AdPrivacy()
) {
    /** Debug builds ALWAYS use Google test units, whatever the server says: live ads can never be requested during development. */
    val usesTestUnits get() = BuildConfig.DEBUG || useTestIds

    /** Returns null (=> nothing is requested) when ads are globally off, the placement is off/unknown/blocked, or no valid unit id exists. */
    fun resolve(placementId: String, formatOverride: AdFormat? = null): ResolvedPlacement? {
        if (!enabled) return null                        // global kill switch
        val p = placements[placementId] ?: return null
        if (!p.enabled || p.screen in blockedScreens) return null
        val format = formatOverride ?: p.format ?: return null
        val unit = if (usesTestUnits) testUnits[format.unitKey]
                   else p.adUnitId.ifBlank { productionUnits[format.unitKey].orEmpty() }
        if (unit.isNullOrBlank() || !UNIT_ID.matches(unit)) return null   // never request with an empty or malformed id
        return ResolvedPlacement(p.id, p.screen, format, unit, usesTestUnits)
    }

    companion object {
        private val UNIT_ID = Regex("^ca-app-pub-\\d{16}/\\d{10}$")
        val DISABLED = AdConfig()

        private fun JsonObject.strMap(key: String): Map<String, String> =
            runCatching { getAsJsonObject(key).entrySet().associate { it.key to it.value.asString } }.getOrDefault(emptyMap())

        fun from(snapshot: ConfigSnapshot): AdConfig = runCatching {
            val g = snapshot.obj("ads_global")
            val sv = snapshot.obj("ads_saved_verses")
            val pr = snapshot.obj("ads_privacy")
            val placements = snapshot.obj("ads_placements")?.entrySet()?.associate { (id, node) ->
                val o = node.asJsonObject
                id to AdPlacement(id, o.get("enabled")?.asBoolean ?: false, o.get("screen")?.asString.orEmpty(),
                    AdFormat.parse(o.get("format")?.asString), o.get("ad_unit_id")?.takeIf { !it.isJsonNull }?.asString.orEmpty())
            }.orEmpty()
            AdConfig(
                enabled = snapshot.bool("ads_enabled", false),
                useTestIds = snapshot.bool("ads_use_test_ids", true),
                appId = snapshot.string("ads_app_id", ""),
                testUnits = snapshot.values.strMap("ads_test_units"),
                productionUnits = snapshot.values.strMap("ads_production_units"),
                maxAdsPerScreen = g?.get("max_ads_per_screen")?.asInt ?: 3,
                minSecondsBetweenFullscreen = g?.get("min_seconds_between_fullscreen_ads")?.asInt ?: 180,
                minSecondsBetweenAppOpen = g?.get("min_seconds_between_app_open_ads")?.asInt ?: 14_400,
                placements = placements,
                saved = AdSavedVerses(sv?.get("first_ad_after_items")?.asInt ?: 1, sv?.get("repeat_interval_items")?.asInt ?: 2,
                    sv?.get("max_inline_ads")?.asInt ?: 3, AdFormat.parse(sv?.get("repeat_format")?.asString) ?: AdFormat.BANNER_ADAPTIVE_INLINE),
                blockedScreens = runCatching { snapshot.values.getAsJsonArray("ads_blocked_screens").map { it.asString }.toSet() }.getOrDefault(emptySet()),
                privacy = AdPrivacy(pr?.get("require_consent")?.asBoolean ?: true, pr?.get("tag_for_child_directed_treatment")?.asBoolean ?: false,
                    pr?.get("tag_for_under_age_of_consent")?.asBoolean ?: false, pr?.get("max_ad_content_rating")?.asString ?: "PG")
            )
        }.getOrDefault(DISABLED)   // malformed config => no ads, never a crash
    }
}

/**
 * Where ads go in the Saved Verses list. [savedVerseIndex] counts only real saved verses (0-based), never ad rows.
 * Defaults give ads after verses 1, 3, 5, 7 ...; the interval and first position come from the backend.
 */
fun shouldInsertSavedVerseAd(savedVerseIndex: Int, firstAdAfterItems: Int, repeatIntervalItems: Int): Boolean {
    if (firstAdAfterItems <= 0) return false
    val itemCount = savedVerseIndex + 1
    if (itemCount == firstAdAfterItems) return true
    return itemCount > firstAdAfterItems && repeatIntervalItems > 0 && (itemCount - firstAdAfterItems) % repeatIntervalItems == 0
}

/** Indexes (0-based saved-verse positions) that get an ad after them, honouring [AdSavedVerses.maxInlineAds]. */
fun savedVerseAdPositions(count: Int, cfg: AdSavedVerses): Set<Int> {
    val out = LinkedHashSet<Int>()
    for (i in 0 until count) {
        if (out.size >= cfg.maxInlineAds) break
        if (shouldInsertSavedVerseAd(i, cfg.firstAdAfterItems, cfg.repeatIntervalItems)) out += i
    }
    return out
}
