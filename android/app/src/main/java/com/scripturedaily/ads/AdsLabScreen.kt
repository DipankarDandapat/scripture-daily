package com.scripturedaily.ads

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.W

/**
 * Debug-only QA screen (Settings → Help & Support). Shows the effective ad configuration and lets a tester load every format
 * with Google's official test units. Reached only from DEBUG builds, which always use test units regardless of server config.
 */
@Composable
fun AdsLabScreen(onBack: () -> Unit) {
    val activity = LocalContext.current as Activity
    val cfg by AdManager.config.collectAsState()
    val ready by AdManager.ready.collectAsState()
    val log by AdManager.log.collectAsState()
    var banner by remember { mutableStateOf<String?>(null) }
    var native by remember { mutableStateOf<String?>(null) }
    OverlayScreen("Ads test lab", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ads_enabled=${cfg.enabled} · test units=${cfg.usesTestUnits} · SDK ready=$ready", color = W.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Placements enabled: " + cfg.placements.values.filter { it.enabled }.joinToString { it.id }.ifBlank { "none" }, color = W.InkMid, fontSize = 12.sp)
            Text("Every format below uses its placement from the backend config; with ads_enabled=false or a disabled placement, nothing loads.", color = W.InkMid, fontSize = 12.sp)
            Button(onClick = { banner = "home_bottom_banner" }) { Text("Load anchored adaptive banner") }
            banner?.let { AdSlotForced("lab_anchored", AdFormat.BANNER_ADAPTIVE_ANCHORED) }
            Button(onClick = { native = "home_after_daily_verse" }) { Text("Load native advanced") }
            native?.let { AdSlotForced("lab_native", AdFormat.NATIVE_ADVANCED) }
            Button(onClick = { AdManager.preloadInterstitial("tree_completion_interstitial"); AdManager.note("Preload requested") }) { Text("Preload interstitial") }
            Button(onClick = { AdManager.showInterstitial(activity, "tree_completion_interstitial") }) { Text("Show interstitial") }
            Button(onClick = { AdManager.loadAndShowRewarded(activity, "wallpaper_download_rewarded", { AdManager.note("REWARD GRANTED") }, { AdManager.note(it) }) }) { Text("Rewarded") }
            Button(onClick = { AdManager.loadAndShowRewardedInterstitial(activity, "wallpaper_download_rewarded", { AdManager.note("REWARD GRANTED") }, { AdManager.note(it) }) }) { Text("Rewarded interstitial") }
            Text("Event log", fontWeight = FontWeight.Bold, color = W.Ink)
            log.reversed().forEach { Text(it, fontSize = 11.sp, color = W.InkMid) }
        }
    }
}

/** Lab-only slot that ignores per-placement enablement but still honours the global switch, consent and test units. */
@Composable
private fun AdSlotForced(key: String, format: AdFormat) {
    val cfg by AdManager.config.collectAsState()
    val ready by AdManager.ready.collectAsState()
    if (!ready || !cfg.enabled) { Text("Ads are off (global switch or consent).", color = W.InkMid, fontSize = 12.sp); return }
    val unit = cfg.testUnits[format.unitKey] ?: return
    val forced = cfg.copy(placements = cfg.placements + (key to AdPlacement(key, true, "lab", format, unit)))
    // Reuse AdSlot by temporarily resolving through a private config is not possible; render via the shared slot with a registered placement.
    AdSlotFor(forced.resolve(key) ?: return)
}
