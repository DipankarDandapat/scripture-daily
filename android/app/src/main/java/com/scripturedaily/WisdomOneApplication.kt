package com.scripturedaily

import android.app.Application
import android.util.Log
import com.scripturedaily.ads.AdManager
import com.scripturedaily.analytics.MetaAnalytics
import com.scripturedaily.config.AppConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WisdomOneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppConfigManager.init(this)
        AppConfigManager.observeForeground(this)
        AdManager.attach(this)
        runCatching { com.scripturedaily.data.VerseScheduler.ensureArmed(this) }
        // The Meta SDK never auto-initialises (see AndroidManifest). Wait for the startup config, then apply meta_ads_enabled —
        // and re-apply it whenever a later refresh changes it, so turning the flag off also stops the SDK.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            AppConfigManager.awaitStartup()
            Log.d("WisdomOneApp", "Config ready — source=${AppConfigManager.current.source} meta_ads_enabled=${AppConfigManager.current.bool("meta_ads_enabled", false)}")
            AppConfigManager.state.map { it.bool("meta_ads_enabled", false) }.distinctUntilChanged().collect {
                MetaAnalytics.init(this@WisdomOneApplication)
            }
        }
    }
}
