package com.scripturedaily

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

object ForceUpdateManager {

    private const val KEY = "latestAppVersion"
    // Fetch fresh values every 1 hour in production (0 for debug)
    private const val FETCH_INTERVAL_SECONDS = 3600L

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().also { rc ->
            rc.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
                    .build()
            )
            // Default: same as current build so no false-positive on first launch
            rc.setDefaultsAsync(mapOf(KEY to BuildConfig.VERSION_NAME))
        }
    }

    /** Returns the latest required version string from Remote Config, e.g. "1.4.0" */
    suspend fun fetchLatestVersion(): String {
        return try {
            remoteConfig.fetchAndActivate().await()
            remoteConfig.getString(KEY).trim()
        } catch (_: Exception) {
            // On any failure fall back to the cached/default value — never block the user
            remoteConfig.getString(KEY).trim().ifEmpty { BuildConfig.VERSION_NAME }
        }
    }

    /**
     * Returns true when [latestVersion] is strictly newer than [currentVersion].
     * Compares dot-separated numeric segments, e.g. "1.4.0" > "1.3.2".
     */
    fun isUpdateRequired(currentVersion: String, latestVersion: String): Boolean {
        val current = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val latest  = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(current.size, latest.size)
        for (i in 0 until len) {
            val c = current.getOrElse(i) { 0 }
            val l = latest.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
