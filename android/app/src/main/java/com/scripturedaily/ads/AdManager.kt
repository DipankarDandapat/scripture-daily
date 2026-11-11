package com.scripturedaily.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.scripturedaily.config.AppConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Single entry point for advertising.
 *
 *  - [config] follows the backend `ads_*` keys (refreshed on startup and whenever the app returns to the foreground).
 *  - Nothing is requested and the SDK/consent flow is not even started while `ads_enabled` is false.
 *  - Consent (Google UMP) is handled before the Mobile Ads SDK is initialised; [ready] flips true once ads may be requested.
 *  - Full-screen cooldowns and the per-screen cap live here so screens never duplicate that logic.
 */
object AdManager {
    private const val TAG = "AdManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var app: Application
    private var activityRef: WeakReference<Activity>? = null
    @Volatile private var consentStarted = false
    @Volatile private var sdkStarted = false

    val config: StateFlow<AdConfig> by lazy { AppConfigManager.state.map { AdConfig.from(it) }.stateIn(scope, SharingStarted.Eagerly, AdConfig.from(AppConfigManager.current)) }
    private val _ready = MutableStateFlow(false)
    /** True once consent has been resolved and the Mobile Ads SDK is initialised. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** Human-readable event log shown by the debug "Ads test lab". */
    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()
    fun note(message: String) { Log.d(TAG, message); _log.value = (_log.value + message).takeLast(60) }

    private var lastFullScreenMs = 0L
    private var lastAppOpenMs = 0L
    private val perScreen = HashMap<String, MutableSet<String>>()

    fun attach(application: Application) {
        app = application
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) { activityRef = WeakReference(activity); startIfEnabled(activity) }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
        // If ads are switched on later (backend change picked up on foreground), start then.
        scope.launch { config.collect { cfg -> if (cfg.enabled) activityRef?.get()?.let { startIfEnabled(it) } } }
        scope.launch { AppConfigManager.startupDone.collect { done -> if (done) activityRef?.get()?.let { startIfEnabled(it) } } }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            private var first = true
            override fun onStart(owner: LifecycleOwner) { if (first) { first = false; return }; AppOpenAdManager.onForeground() }
        })
    }

    private fun startIfEnabled(activity: Activity) {
        val cfg = config.value
        if (!cfg.enabled || !AppConfigManager.startupDone.value || consentStarted) return
        consentStarted = true
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(cfg.privacy.underAge).build()
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { err ->
                if (err != null) note("Consent form: ${err.message}")
                if (info.canRequestAds() || !cfg.privacy.requireConsent) startSdk(cfg)
            }
        }, { err ->
            note("Consent info failed: ${err.message}")
            if (info.canRequestAds()) startSdk(cfg)
            else consentStarted = false     // retry on the next resume/foreground
        })
        if (info.canRequestAds()) startSdk(cfg)   // consent already given in an earlier session
    }

    private fun startSdk(cfg: AdConfig) {
        if (sdkStarted) return
        sdkStarted = true
        val rating = when (cfg.privacy.maxRating) { "G" -> RequestConfiguration.MAX_AD_CONTENT_RATING_G; "T" -> RequestConfiguration.MAX_AD_CONTENT_RATING_T; "MA" -> RequestConfiguration.MAX_AD_CONTENT_RATING_MA; else -> RequestConfiguration.MAX_AD_CONTENT_RATING_PG }
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder()
            .setMaxAdContentRating(rating)
            .setTagForChildDirectedTreatment(if (cfg.privacy.childDirected) RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE else RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
            .setTagForUnderAgeOfConsent(if (cfg.privacy.underAge) RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE else RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
            .build())
        MobileAds.initialize(app) { _ready.value = true; note("Mobile Ads SDK ready (test units: ${cfg.usesTestUnits})") }
    }

    /** True only when ads are enabled, consent is done and the SDK is up. */
    fun canRequest(): Boolean = _ready.value && config.value.enabled

    // ── Per-screen cap ───────────────────────────────────────────────────────
    fun reserve(screen: String, placementId: String): Boolean = synchronized(perScreen) {
        val used = perScreen.getOrPut(screen) { mutableSetOf() }
        if (placementId in used) return true
        if (used.size >= config.value.maxAdsPerScreen) return false
        used += placementId; true
    }
    fun release(screen: String, placementId: String) = synchronized(perScreen) { perScreen[screen]?.remove(placementId); Unit }

    // ── Full-screen frequency rules ──────────────────────────────────────────
    fun fullScreenCooldownPassed(now: Long = System.currentTimeMillis()): Boolean =
        now - lastFullScreenMs >= config.value.minSecondsBetweenFullscreen * 1000L
    internal fun markFullScreenShown() { lastFullScreenMs = System.currentTimeMillis() }
    internal fun appOpenCooldownPassed(now: Long = System.currentTimeMillis()) = now - lastAppOpenMs >= config.value.minSecondsBetweenAppOpen * 1000L
    internal fun markAppOpenShown() { lastAppOpenMs = System.currentTimeMillis() }
    internal fun currentActivity(): Activity? = activityRef?.get()

    // ── Interstitial / rewarded / rewarded interstitial ──────────────────────
    private var interstitial: Pair<String, InterstitialAd>? = null

    /** Preloads the placement's interstitial (no-op unless the placement resolves). */
    fun preloadInterstitial(placementId: String) {
        val r = config.value.resolve(placementId, AdFormat.INTERSTITIAL) ?: return
        if (!canRequest() || interstitial?.first == placementId) return
        InterstitialAd.load(app, r.unitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = placementId to ad; note("Interstitial loaded ($placementId)") }
            override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null; note("Interstitial failed: ${e.message}") }
        })
    }

    /** Shows a preloaded interstitial at a natural break. Honours the cooldown; never blocks navigation if nothing is ready. */
    fun showInterstitial(activity: Activity, placementId: String, onDone: () -> Unit = {}): Boolean {
        val ad = interstitial?.takeIf { it.first == placementId }?.second
        if (ad == null || !canRequest() || !fullScreenCooldownPassed()) { onDone(); return false }
        interstitial = null
        ad.fullScreenContentCallback = fullScreenCallback("Interstitial", onDone)
        ad.show(activity)
        return true
    }

    fun loadAndShowRewarded(activity: Activity, placementId: String, onReward: () -> Unit, onUnavailable: (String) -> Unit) {
        val r = config.value.resolve(placementId, AdFormat.REWARDED)
        if (r == null || !canRequest()) { onUnavailable("Rewards are not available right now."); return }
        RewardedAd.load(activity, r.unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                note("Rewarded loaded")
                var earned = false
                ad.fullScreenContentCallback = fullScreenCallback("Rewarded") { if (earned) onReward() }
                ad.show(activity) { earned = true }       // the reward is granted only after the SDK confirms it was earned
            }
            override fun onAdFailedToLoad(e: LoadAdError) { note("Rewarded failed: ${e.message}"); onUnavailable("No reward is available right now. Please try again later.") }
        })
    }

    fun loadAndShowRewardedInterstitial(activity: Activity, placementId: String, onReward: () -> Unit, onUnavailable: (String) -> Unit) {
        val r = config.value.resolve(placementId, AdFormat.REWARDED_INTERSTITIAL)
        if (r == null || !canRequest()) { onUnavailable("Not available right now."); return }
        RewardedInterstitialAd.load(activity, r.unitId, AdRequest.Builder().build(), object : RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                var earned = false
                ad.fullScreenContentCallback = fullScreenCallback("RewardedInterstitial") { if (earned) onReward() }
                ad.show(activity) { earned = true }
            }
            override fun onAdFailedToLoad(e: LoadAdError) { note("RewardedInterstitial failed: ${e.message}"); onUnavailable("Not available right now.") }
        })
    }

    private fun fullScreenCallback(name: String, onDone: () -> Unit) = object : FullScreenContentCallback() {
        override fun onAdShowedFullScreenContent() { markFullScreenShown(); note("$name shown") }
        override fun onAdDismissedFullScreenContent() { note("$name dismissed"); onDone() }
        override fun onAdFailedToShowFullScreenContent(e: AdError) { note("$name failed to show: ${e.message}"); onDone() }
    }
}

/** App-open ads: optional, off by default on the server, shown only after a cooldown and never over another full-screen ad. */
object AppOpenAdManager {
    private var ad: AppOpenAd? = null
    private var loading = false
    private var showing = false
    const val PLACEMENT = "app_open_ad"

    fun preload(context: Context) {
        val r = AdManager.config.value.resolve(PLACEMENT, AdFormat.APP_OPEN) ?: return
        if (!AdManager.canRequest() || ad != null || loading) return
        loading = true
        AppOpenAd.load(context, r.unitId, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(loaded: AppOpenAd) { ad = loaded; loading = false; AdManager.note("App open loaded") }
            override fun onAdFailedToLoad(e: LoadAdError) { loading = false; AdManager.note("App open failed: ${e.message}") }
        })
    }

    fun onForeground() {
        val activity = AdManager.currentActivity() ?: return
        val cfg = AdManager.config.value
        if (cfg.resolve(PLACEMENT, AdFormat.APP_OPEN) == null || !AdManager.canRequest()) return
        if (com.scripturedaily.gate.GateState.blocking) return   // never over the update / maintenance screens
        val loaded = ad
        if (loaded == null) { preload(activity); return }
        if (showing || !AdManager.appOpenCooldownPassed() || !AdManager.fullScreenCooldownPassed()) return
        ad = null; showing = true
        loaded.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { AdManager.markAppOpenShown(); AdManager.markFullScreenShown() }
            override fun onAdDismissedFullScreenContent() { showing = false; preload(activity) }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { showing = false }
        }
        loaded.show(activity)
    }
}
