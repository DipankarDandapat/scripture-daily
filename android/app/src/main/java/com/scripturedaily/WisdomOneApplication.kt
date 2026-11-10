package com.scripturedaily

import android.app.Application
import android.util.Log
import com.scripturedaily.ads.AdManager
import com.scripturedaily.analytics.MetaAnalytics
import com.scripturedaily.config.AppConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WisdomOneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppConfigManager.init(this)
        AppConfigManager.observeForeground(this)
        AdManager.attach(this)
        // Fetch config then init Meta SDK so meta_ads_enabled is already resolved
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            AppConfigManager.awaitStartup()
            Log.d("WisdomOneApp", "Config ready — source=${AppConfigManager.current.source} meta_ads_enabled=${AppConfigManager.current.bool("meta_ads_enabled", false)}")
            MetaAnalytics.init(this@WisdomOneApplication)
        }
    }
}
