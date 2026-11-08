package com.scripturedaily

import android.app.Application
import com.scripturedaily.ads.AdManager
import com.scripturedaily.config.AppConfigManager

class WisdomOneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppConfigManager.init(this)
        AppConfigManager.observeForeground(this)
        AdManager.attach(this)
    }
}
