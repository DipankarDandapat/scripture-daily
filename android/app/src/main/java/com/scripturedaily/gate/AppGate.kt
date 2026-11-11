package com.scripturedaily.gate

import com.google.gson.JsonObject
import com.scripturedaily.config.ConfigSource
import com.scripturedaily.config.ConfigSnapshot
import java.time.Instant

/**
 * Startup gate driven by `GET /api/v1/app/config` (replaces Firebase Remote Config `latestAppVersion`).
 *
 * Priority — identical to backend `app.services.app_config.evaluate_gate`:
 *   1. mandatory update   (installed < minimum_supported_version, or force_update and installed < latest_version)
 *   2. maintenance mode   (maintenance_mode.enabled)
 *   3. optional update    (installed < latest_version) — shown as a dismissible prompt over the app
 *   4. the app
 *
 * Offline rules (see docs/APP_CONFIG_AND_MAINTENANCE.md):
 *   - Never *assume* maintenance: only a config that actually said `enabled = true` can block the app.
 *   - A cached "maintenance on" keeps blocking for at most `enforce_cached_for_hours` after it was fetched, and not past its
 *     `estimated_end_time`; after that the app opens on its offline cache instead of locking the user out.
 *   - First launch with no config ever fetched and no network → a connection screen with Retry *and* Continue.
 */
object AppGate {

    data class UpdateConfig(
        val enabled: Boolean = true,
        val latestVersion: String = "",
        val minimumSupportedVersion: String = "",
        val forceUpdate: Boolean = false,
        val title: String = "A new version is available",
        val message: String = "Update WisdomOne to get the latest improvements and fixes.",
        val mandatoryTitle: String = "Update required",
        val mandatoryMessage: String = "This version of WisdomOne is no longer supported. Please update to continue.",
        val playStoreUrl: String = "",
        val optionalReminderHours: Int = 24,
    )

    data class MaintenanceConfig(
        val enabled: Boolean = false,
        val title: String = "We'll Be Back Soon!",
        val message: String = "We're currently upgrading WisdomOne to bring you a better experience. Our team is working to restore the service as quickly as possible.",
        val estimatedEnd: Instant? = null,
        val allowRetry: Boolean = true,
        val enforceCachedForHours: Int = 12,
    )

    sealed interface Decision {
        data class MandatoryUpdate(val update: UpdateConfig) : Decision
        /** [fromCache] = the server could not be reached; this is the last status it reported. */
        data class Maintenance(val maintenance: MaintenanceConfig, val fromCache: Boolean) : Decision
        data class OptionalUpdate(val update: UpdateConfig) : Decision
        /** First launch, nothing cached and the server is unreachable. */
        data object Unverified : Decision
        data object Open : Decision
    }

    val Decision.blocksApp: Boolean get() = this is Decision.MandatoryUpdate || this is Decision.Maintenance

    // ── Parsing (tolerant: any missing / malformed field falls back to its safe default) ──
    private fun JsonObject.str(k: String, d: String) = runCatching { get(k).takeUnless { it.isJsonNull }?.asString }.getOrNull() ?: d
    private fun JsonObject.bool(k: String, d: Boolean) = runCatching { get(k).asBoolean }.getOrDefault(d)
    private fun JsonObject.int(k: String, d: Int) = runCatching { get(k).asInt }.getOrDefault(d)

    fun updateConfig(o: JsonObject?): UpdateConfig {
        val d = UpdateConfig()
        if (o == null) return d.copy(enabled = false)
        return UpdateConfig(
            enabled = o.bool("enabled", true),
            latestVersion = o.str("latest_version", ""),
            minimumSupportedVersion = o.str("minimum_supported_version", ""),
            forceUpdate = o.bool("force_update", false),
            title = o.str("title", d.title).ifBlank { d.title },
            message = o.str("message", d.message).ifBlank { d.message },
            mandatoryTitle = o.str("mandatory_title", d.mandatoryTitle).ifBlank { d.mandatoryTitle },
            mandatoryMessage = o.str("mandatory_message", d.mandatoryMessage).ifBlank { d.mandatoryMessage },
            playStoreUrl = o.str("play_store_url", ""),
            optionalReminderHours = o.int("optional_reminder_hours", d.optionalReminderHours).coerceIn(0, 720),
        )
    }

    fun maintenanceConfig(o: JsonObject?): MaintenanceConfig {
        val d = MaintenanceConfig()
        if (o == null) return d
        return MaintenanceConfig(
            enabled = o.bool("enabled", false),
            title = o.str("title", d.title).ifBlank { d.title },
            message = o.str("message", d.message).ifBlank { d.message },
            estimatedEnd = runCatching { Instant.parse(o.str("estimated_end_time", "")) }
                .recoverCatching { java.time.OffsetDateTime.parse(o.str("estimated_end_time", "")).toInstant() }.getOrNull(),
            allowRetry = o.bool("allow_retry", true),
            enforceCachedForHours = o.int("enforce_cached_for_hours", d.enforceCachedForHours).coerceIn(0, 168),
        )
    }

    // ── Versions ──
    private fun parts(v: String): List<Int> = v.trim().split('.', '-', ' ').takeWhile { it.isNotEmpty() && it.all(Char::isDigit) }
        .map { it.toInt() }.dropLastWhile { it == 0 }

    /** True when [installed] is strictly older than [other]. Blank [other] never counts as newer. */
    fun isOlder(installed: String, other: String): Boolean {
        if (other.isBlank()) return false
        val a = parts(installed); val b = parts(other)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x < y
        }
        return false
    }

    enum class UpdateStatus { DISABLED, UP_TO_DATE, OPTIONAL, MANDATORY }

    fun updateStatus(update: UpdateConfig, installed: String): UpdateStatus = when {
        !update.enabled -> UpdateStatus.DISABLED
        isOlder(installed, update.minimumSupportedVersion) -> UpdateStatus.MANDATORY
        update.forceUpdate && isOlder(installed, update.latestVersion) -> UpdateStatus.MANDATORY
        isOlder(installed, update.latestVersion) -> UpdateStatus.OPTIONAL
        else -> UpdateStatus.UP_TO_DATE
    }

    /**
     * @param lastFetchOk whether the most recent attempt to reach the config API succeeded (null = not attempted yet).
     */
    fun decide(snapshot: ConfigSnapshot, installedVersion: String, lastFetchOk: Boolean?, nowMs: Long = System.currentTimeMillis()): Decision {
        val update = updateConfig(snapshot.obj("app_update"))
        val maintenance = maintenanceConfig(snapshot.obj("maintenance_mode"))
        val status = updateStatus(update, installedVersion)
        if (status == UpdateStatus.MANDATORY) return Decision.MandatoryUpdate(update)

        val offline = lastFetchOk == false
        if (snapshot.source == ConfigSource.DEFAULTS) return if (offline) Decision.Unverified else Decision.Open

        if (maintenance.enabled) {
            if (!offline) return Decision.Maintenance(maintenance, fromCache = false)
            // Server unreachable: trust the last known "on" only for a bounded time, so a network problem can't lock users out.
            val ageMs = nowMs - snapshot.fetchedAtMs
            val withinWindow = ageMs in 0..(maintenance.enforceCachedForHours * 3_600_000L)
            val beforeEnd = maintenance.estimatedEnd?.let { nowMs < it.toEpochMilli() } ?: true
            if (withinWindow && beforeEnd) return Decision.Maintenance(maintenance, fromCache = true)
        }
        return if (status == UpdateStatus.OPTIONAL) Decision.OptionalUpdate(update) else Decision.Open
    }

    /** Whether the optional prompt should show again, given when (and for which version) the user last tapped "Later". */
    fun shouldPromptOptional(update: UpdateConfig, dismissedVersion: String?, dismissedAtMs: Long, nowMs: Long): Boolean {
        if (dismissedVersion != update.latestVersion) return true
        return nowMs - dismissedAtMs >= update.optionalReminderHours * 3_600_000L
    }
}
