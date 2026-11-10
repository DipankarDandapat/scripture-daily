package com.scripturedaily.ads

import android.content.Context
import android.graphics.Color as AColor
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.scripturedaily.ui.W

/**
 * Drop-in ad placement. Renders nothing (zero height, no request) unless the backend config enables [placementId] and the
 * global switch is on; collapses again if the ad fails so a failed request never leaves a blank gap.
 */
@Composable
fun AdSlot(placementId: String, modifier: Modifier = Modifier, formatOverride: AdFormat? = null, slotKey: String = "", screenOverride: String? = null) {
    val cfg by AdManager.config.collectAsState()
    val ready by AdManager.ready.collectAsState()
    val resolved = remember(cfg, placementId, formatOverride) { cfg.resolve(placementId, formatOverride) }
    if (!ready || resolved == null || resolved.format.isFullScreen) return
    val reserveKey = resolved.id + slotKey
    val screen = screenOverride ?: resolved.screen
    val allowed = remember(screen, reserveKey) { AdManager.reserve(screen, reserveKey) }
    DisposableEffect(screen, reserveKey) { onDispose { AdManager.release(screen, reserveKey) } }
    if (!allowed) return
    when {
        resolved.format.isBanner -> BannerSlot(resolved, modifier)
        resolved.format.isNative -> NativeSlot(resolved, modifier)
    }
}

/** Renders an already-resolved placement (used by the debug lab). */
@Composable
fun AdSlotFor(r: ResolvedPlacement) {
    if (r.format.isBanner) BannerSlot(r, Modifier) else if (r.format.isNative) NativeSlot(r, Modifier)
}

private fun bannerSize(context: Context, format: AdFormat, widthDp: Int): AdSize = when (format) {
    AdFormat.BANNER_ADAPTIVE_INLINE -> AdSize.getInlineAdaptiveBannerAdSize(widthDp, 250)
    AdFormat.BANNER_STANDARD_320_50 -> AdSize.BANNER
    AdFormat.BANNER_STANDARD_300_50 -> AdSize(300, 50)
    AdFormat.BANNER_STANDARD_320_100 -> AdSize.LARGE_BANNER
    AdFormat.BANNER_MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
    AdFormat.BANNER_FIXED -> AdSize.BANNER
    else -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)   // anchored + large adaptive
}

@Composable
private fun BannerSlot(r: ResolvedPlacement, modifier: Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        var loaded by remember(r.unitId, widthDp) { mutableStateOf(false) }
        var failed by remember(r.unitId, widthDp) { mutableStateOf(false) }
        val size = remember(r.format, widthDp) { bannerSize(context, r.format, widthDp) }
        val adView = remember(r.unitId, widthDp, r.format) {
            AdView(context).apply {
                adUnitId = r.unitId
                setAdSize(size)
                adListener = object : AdListener() {
                    override fun onAdLoaded() { loaded = true; failed = false; AdManager.note("Banner loaded (${r.id})") }
                    override fun onAdFailedToLoad(e: LoadAdError) { failed = true; loaded = false; AdManager.note("Banner failed (${r.id}): ${e.message}") }
                }
                loadAd(AdRequest.Builder().build())     // once per (unit, width): recomposition does not reload
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        val height = when { failed -> 0.dp; loaded -> size.height.coerceAtLeast(50).dp; else -> 1.dp }
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(height).clipToBounds(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            if (!failed) AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NativeSlot(r: ResolvedPlacement, modifier: Modifier) {
    val context = LocalContext.current
    var nativeAd by remember(r.unitId) { mutableStateOf<NativeAd?>(null) }
    var failed by remember(r.unitId) { mutableStateOf(false) }
    DisposableEffect(r.unitId) {
        val loader = AdLoader.Builder(context, r.unitId)
            .forNativeAd { ad -> nativeAd?.destroy(); nativeAd = ad; AdManager.note("Native loaded (${r.id})") }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(e: LoadAdError) { failed = true; AdManager.note("Native failed (${r.id}): ${e.message}") }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().setVideoOptions(VideoOptions.Builder().setStartMuted(true).build()).build())
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { nativeAd?.destroy(); nativeAd = null }
    }
    val ad = nativeAd
    if (failed || ad == null) return            // nothing reserved while loading or after failure
    val bg = W.Surface.toArgb(); val fg = W.Ink.toArgb(); val soft = W.InkSoft.toArgb(); val accent = W.Purple.toArgb(); val border = W.Gold.toArgb()
    androidx.compose.foundation.layout.Box(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        AndroidView(
            factory = { ctx -> buildNativeView(ctx, bg, fg, soft, accent, border) },
            update = { view -> bindNative(view, ad) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

/** A rectangular native-advanced card: "Ad" attribution, headline, body, media, call-to-action. Built in code so it follows the app theme. */
private fun buildNativeView(ctx: Context, bg: Int, fg: Int, soft: Int, accent: Int, border: Int): NativeAdView {
    val root = NativeAdView(ctx)
    val column = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 14))
        background = android.graphics.drawable.GradientDrawable().apply { setColor(bg); cornerRadius = dp(ctx, 18).toFloat(); setStroke(dp(ctx, 1), (border and 0x00FFFFFF) or 0x40000000) }
    }
    val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val badge = TextView(ctx).apply {
        text = "Ad"; setTextColor(AColor.WHITE); textSize = 11f; typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(ctx, 7), dp(ctx, 2), dp(ctx, 7), dp(ctx, 2))
        background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFF8A6A3A.toInt()); cornerRadius = dp(ctx, 6).toFloat() }
    }
    val icon = ImageView(ctx).apply { layoutParams = LinearLayout.LayoutParams(dp(ctx, 36), dp(ctx, 36)).apply { marginStart = dp(ctx, 10) } }
    val headline = TextView(ctx).apply {
        setTextColor(fg); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; maxLines = 2
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(ctx, 10) }
    }
    top.addView(badge); top.addView(icon); top.addView(headline)
    val body = TextView(ctx).apply { setTextColor(soft); textSize = 13f; maxLines = 3; setPadding(0, dp(ctx, 8), 0, dp(ctx, 8)) }
    val media = MediaView(ctx).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 170)) }
    val cta = Button(ctx).apply {
        setTextColor(AColor.WHITE); isAllCaps = false; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
        background = android.graphics.drawable.GradientDrawable().apply { setColor(accent); cornerRadius = dp(ctx, 14).toFloat() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 46)).apply { topMargin = dp(ctx, 10) }
    }
    column.addView(top); column.addView(body); column.addView(media); column.addView(cta)
    root.addView(column)
    root.headlineView = headline; root.bodyView = body; root.mediaView = media; root.callToActionView = cta; root.iconView = icon
    return root
}

private fun bindNative(view: NativeAdView, ad: NativeAd) {
    (view.headlineView as TextView).text = ad.headline
    (view.bodyView as TextView).apply { text = ad.body; visibility = if (ad.body == null) View.GONE else View.VISIBLE }
    (view.callToActionView as Button).apply { text = ad.callToAction ?: "Learn more"; visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE }
    (view.iconView as ImageView).apply { if (ad.icon != null) { setImageDrawable(ad.icon!!.drawable); visibility = View.VISIBLE } else visibility = View.GONE }
    (view.mediaView as MediaView).apply { ad.mediaContent?.let { mediaContent = it }; visibility = if (ad.mediaContent == null) View.GONE else View.VISIBLE }
    view.setNativeAd(ad)
}
