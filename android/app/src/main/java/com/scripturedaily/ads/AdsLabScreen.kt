package com.scripturedaily.ads

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.W

// Google's official test unit IDs — safe to click, never charge real money.
// https://developers.google.com/admob/android/test-ads
private const val TEST_BANNER          = "ca-app-pub-3940256099942544/9214589741"
private const val TEST_NATIVE          = "ca-app-pub-3940256099942544/2247696110"
private const val TEST_INTERSTITIAL    = "ca-app-pub-3940256099942544/1033173712"
private const val TEST_REWARDED        = "ca-app-pub-3940256099942544/5224354917"
private const val TEST_REWARDED_INTER  = "ca-app-pub-3940256099942544/5354046379"
private const val TEST_APP_OPEN        = "ca-app-pub-3940256099942544/9257395921"

/**
 * Debug-only QA screen — Settings → (debug) Ads test lab.
 *
 * Every button uses Google's official test unit IDs directly so they work
 * regardless of whether the placement is enabled in the backend DB.
 * This lets you verify every ad format loads and shows correctly before
 * flipping placements on in production.
 */
@Composable
fun AdsLabScreen(onBack: () -> Unit) {
    val activity = LocalContext.current as Activity
    val cfg by AdManager.config.collectAsState()
    val ready by AdManager.ready.collectAsState()
    val log by AdManager.log.collectAsState()

    OverlayScreen("Ads test lab", onBack) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Status ──────────────────────────────────────────────────────
            StatusRow("ads_enabled", cfg.enabled.toString())
            StatusRow("ads_use_test_ids", cfg.usesTestUnits.toString())
            StatusRow("SDK ready", ready.toString())
            StatusRow("Placements ON", cfg.placements.values.count { it.enabled }.toString())

            if (!ready) {
                Text(
                    "SDK not ready. Make sure ads_enabled=true in the backend DB and consent has been granted.",
                    color = W.Error, fontSize = 12.sp
                )
            }

            SectionHeader("Banner & Native")
            LabBannerButton("Anchored adaptive banner", AdFormat.BANNER_ADAPTIVE_ANCHORED, TEST_BANNER)
            LabBannerButton("Inline adaptive banner",   AdFormat.BANNER_ADAPTIVE_INLINE,   TEST_BANNER)
            LabBannerButton("Native advanced",          AdFormat.NATIVE_ADVANCED,           TEST_NATIVE)

            SectionHeader("Interstitial")
            Button(
                onClick = {
                    AdManager.note("Preloading interstitial…")
                    forcePreloadInterstitial(activity, TEST_INTERSTITIAL)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Preload interstitial (tree_completion)") }
            Button(
                onClick = { forceShowInterstitial(activity, TEST_INTERSTITIAL) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Show interstitial") }

            SectionHeader("Studio — Make another (interstitial)")
            Button(
                onClick = { forceShowInterstitial(activity, TEST_INTERSTITIAL) { AdManager.note("Make another — done") } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("studio_make_another_interstitial") }

            SectionHeader("Rewarded")
            Button(
                onClick = {
                    forceShowRewarded(activity, TEST_REWARDED,
                        onReward = { AdManager.note("✓ REWARD GRANTED — wallpaper_download_rewarded") },
                        onUnavailable = { AdManager.note("Rewarded unavailable: $it") })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("wallpaper_download_rewarded") }
            Button(
                onClick = {
                    forceShowRewarded(activity, TEST_REWARDED,
                        onReward = { AdManager.note("✓ REWARD GRANTED — studio_share_rewarded") },
                        onUnavailable = { AdManager.note("Rewarded unavailable: $it") })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("studio_share_rewarded") }
            Button(
                onClick = {
                    forceShowRewarded(activity, TEST_REWARDED,
                        onReward = { AdManager.note("✓ REWARD GRANTED — studio_whatsapp_rewarded") },
                        onUnavailable = { AdManager.note("Rewarded unavailable: $it") })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("studio_whatsapp_rewarded") }
            Button(
                onClick = {
                    forceShowRewarded(activity, TEST_REWARDED,
                        onReward = { AdManager.note("✓ REWARD GRANTED — studio_save_image_rewarded") },
                        onUnavailable = { AdManager.note("Rewarded unavailable: $it") })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("studio_save_image_rewarded") }
            Button(
                onClick = {
                    forceShowRewarded(activity, TEST_REWARDED,
                        onReward = { AdManager.note("✓ REWARD GRANTED — studio_share_video_rewarded") },
                        onUnavailable = { AdManager.note("Rewarded unavailable: $it") })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("studio_share_video_rewarded") }
            Button(
                onClick = {
                    forceShowRewarded(activity, TEST_REWARDED,
                        onReward = { AdManager.note("✓ REWARD GRANTED — studio_whatsapp_video_rewarded") },
                        onUnavailable = { AdManager.note("Rewarded unavailable: $it") })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("studio_whatsapp_video_rewarded") }
            Button(
                onClick = {
                    forceShowRewarded(activity, TEST_REWARDED,
                        onReward = { AdManager.note("✓ REWARD GRANTED — studio_save_video_rewarded") },
                        onUnavailable = { AdManager.note("Rewarded unavailable: $it") })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("studio_save_video_rewarded") }
            Button(
                onClick = {
                    forceShowRewarded(activity, TEST_REWARDED,
                        onReward = { AdManager.note("✓ REWARD GRANTED — tree_rewarded_bonus") },
                        onUnavailable = { AdManager.note("Rewarded unavailable: $it") })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("tree_rewarded_bonus") }

            SectionHeader("Rewarded Interstitial")
            Button(
                onClick = {
                    forceShowRewardedInterstitial(activity, TEST_REWARDED_INTER,
                        onReward = { AdManager.note("✓ REWARD GRANTED — studio_generate_video_rewarded") },
                        onUnavailable = { AdManager.note("Rewarded interstitial unavailable: $it") })
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("studio_generate_video_rewarded") }

            SectionHeader("App Open")
            Button(
                onClick = { AppOpenAdManager.onForeground(); AdManager.note("App open triggered") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Trigger app_open_ad (needs cooldown = 0)") }

            SectionHeader("Event log")
            log.reversed().forEach {
                Text(it, fontSize = 11.sp, color = if (it.startsWith("✓")) W.Success else W.InkMid)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Force-load helpers using hardcoded test unit IDs ─────────────────────────
// These bypass placement enablement so the lab works even when all placements
// are off in the backend DB.

private fun forcePreloadInterstitial(activity: Activity, unitId: String) {
    com.google.android.gms.ads.interstitial.InterstitialAd.load(
        activity, unitId,
        com.google.android.gms.ads.AdRequest.Builder().build(),
        object : com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: com.google.android.gms.ads.interstitial.InterstitialAd) {
                AdManager.note("Interstitial loaded ✓ — tap Show to display")
                _labInterstitial = ad
            }
            override fun onAdFailedToLoad(e: com.google.android.gms.ads.LoadAdError) {
                AdManager.note("Interstitial failed: ${e.message}")
            }
        }
    )
}

private var _labInterstitial: com.google.android.gms.ads.interstitial.InterstitialAd? = null

private fun forceShowInterstitial(activity: Activity, unitId: String, onDone: () -> Unit = {}) {
    val ad = _labInterstitial
    if (ad == null) { forcePreloadInterstitial(activity, unitId); AdManager.note("Loading… tap Show again in a moment"); return }
    _labInterstitial = null
    ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() { AdManager.note("Interstitial dismissed"); onDone() }
        override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { AdManager.note("Interstitial show failed: ${e.message}"); onDone() }
    }
    ad.show(activity)
}

private fun forceShowRewarded(activity: Activity, unitId: String, onReward: () -> Unit, onUnavailable: (String) -> Unit) {
    com.google.android.gms.ads.rewarded.RewardedAd.load(
        activity, unitId,
        com.google.android.gms.ads.AdRequest.Builder().build(),
        object : com.google.android.gms.ads.rewarded.RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: com.google.android.gms.ads.rewarded.RewardedAd) {
                AdManager.note("Rewarded loaded ✓")
                var earned = false
                ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() { if (earned) onReward() else AdManager.note("Rewarded dismissed without earning") }
                    override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { onUnavailable(e.message) }
                }
                ad.show(activity) { earned = true; AdManager.note("Reward item earned") }
            }
            override fun onAdFailedToLoad(e: com.google.android.gms.ads.LoadAdError) { onUnavailable(e.message) }
        }
    )
}

private fun forceShowRewardedInterstitial(activity: Activity, unitId: String, onReward: () -> Unit, onUnavailable: (String) -> Unit) {
    com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd.load(
        activity, unitId,
        com.google.android.gms.ads.AdRequest.Builder().build(),
        object : com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd) {
                AdManager.note("Rewarded interstitial loaded ✓")
                var earned = false
                ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() { if (earned) onReward() else AdManager.note("Rewarded interstitial dismissed without earning") }
                    override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) { onUnavailable(e.message) }
                }
                ad.show(activity) { earned = true; AdManager.note("Reward item earned") }
            }
            override fun onAdFailedToLoad(e: com.google.android.gms.ads.LoadAdError) { onUnavailable(e.message) }
        }
    )
}

// ── Banner/Native lab slot ────────────────────────────────────────────────────
@Composable
private fun LabBannerButton(label: String, format: AdFormat, unitId: String) {
    val cfg by AdManager.config.collectAsState()
    val ready by AdManager.ready.collectAsState()
    var show by remember { mutableStateOf(false) }
    Button(onClick = { show = !show }, modifier = Modifier.fillMaxWidth()) {
        Text(if (show) "Hide $label" else "Load $label")
    }
    if (!show) return
    if (!ready || !cfg.enabled) {
        Text("SDK not ready or ads_enabled=false", color = W.InkMid, fontSize = 12.sp)
        return
    }
    val labKey = "lab_${format.name.lowercase()}"
    val forced = cfg.copy(
        placements = cfg.placements + (labKey to AdPlacement(labKey, true, "lab", format, unitId))
    )
    val resolved = forced.resolve(labKey) ?: return
    AdSlotFor(resolved)
}

// ── Small UI helpers ──────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(4.dp))
    Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
}

@Composable
private fun StatusRow(key: String, value: String) {
    val ok = value == "true" || (value.toIntOrNull() ?: 0) > 0
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (ok) W.Success.copy(alpha = 0.08f) else W.Error.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, fontSize = 12.sp, color = W.InkMid)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (ok) W.Success else W.Error)
    }
}
