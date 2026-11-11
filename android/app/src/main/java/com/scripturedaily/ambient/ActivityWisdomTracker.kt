package com.scripturedaily.ambient

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * Optional activity-aware verses, using Google Play services' **Activity Recognition Transition API**.
 *
 * Why this API: it is event-driven (the system batches sensor data and only wakes us on a transition such as
 * "started walking"), so there is no polling, no location, no foreground service and negligible battery cost.
 * Privacy: only the latest activity type and its time are stored, in app-private preferences on this phone. Nothing is
 * uploaded. Turning the feature off unregisters immediately.
 */
object ActivityWisdomTracker {
    private const val REQUEST_CODE = 7310

    val permission: String get() = if (Build.VERSION.SDK_INT >= 29) Manifest.permission.ACTIVITY_RECOGNITION else "com.google.android.gms.permission.ACTIVITY_RECOGNITION"

    fun hasPermission(context: Context) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(context: Context): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, Intent(context, ActivityTransitionReceiver::class.java), flags)
    }

    private val TYPES = listOf(DetectedActivity.WALKING, DetectedActivity.RUNNING, DetectedActivity.ON_BICYCLE, DetectedActivity.IN_VEHICLE, DetectedActivity.STILL)

    /** Starts listening for transitions. No-op (returns false) without permission. */
    @SuppressLint("MissingPermission")
    fun start(context: Context): Boolean {
        if (!hasPermission(context)) return false
        val transitions = TYPES.map { ActivityTransition.Builder().setActivityType(it).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build() }
        return runCatching {
            ActivityRecognition.getClient(context).requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent(context))
            true
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun stop(context: Context) {
        if (hasPermission(context)) runCatching { ActivityRecognition.getClient(context).removeActivityTransitionUpdates(pendingIntent(context)) }
        AmbientWisdom.prefs(context).edit().remove(AmbientWisdom.KEY_LAST_ACTIVITY).remove(AmbientWisdom.KEY_LAST_ACTIVITY_AT).apply()
    }

    /** Re-registers after reboot / app update when the user chose activity mode (registrations do not survive a reboot). */
    fun restoreIfEnabled(context: Context) {
        val mode = AmbientWisdom.ActivityMode.of(AmbientWisdom.prefs(context).getString(AmbientWisdom.KEY_ACTIVITY_MODE, null))
        if (mode == AmbientWisdom.ActivityMode.ACTIVITY) start(context)
    }

    fun mapType(type: Int): AmbientWisdom.Activity = when (type) {
        DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> AmbientWisdom.Activity.WALKING
        DetectedActivity.RUNNING -> AmbientWisdom.Activity.RUNNING
        DetectedActivity.ON_BICYCLE -> AmbientWisdom.Activity.CYCLING
        DetectedActivity.IN_VEHICLE -> AmbientWisdom.Activity.IN_VEHICLE
        DetectedActivity.STILL -> AmbientWisdom.Activity.STILL
        else -> AmbientWisdom.Activity.UNKNOWN
    }

    /** Current recommendation for this phone, combining the chosen mode, the last transition and the hour. */
    fun recommendation(context: Context, hour: Int = java.time.LocalTime.now().hour): AmbientWisdom.Category? {
        val p = AmbientWisdom.prefs(context)
        val mode = AmbientWisdom.ActivityMode.of(p.getString(AmbientWisdom.KEY_ACTIVITY_MODE, null))
        val activity = AmbientWisdom.Activity.of(p.getString(AmbientWisdom.KEY_LAST_ACTIVITY, null))
        val age = System.currentTimeMillis() - p.getLong(AmbientWisdom.KEY_LAST_ACTIVITY_AT, 0L)
        return AmbientWisdom.recommend(mode, activity, age, hour, AmbientWisdom.Category.of(p.getString(AmbientWisdom.KEY_MANUAL_CATEGORY, null)))
    }
}

/** Receives activity transitions and stores only the latest one locally. */
class ActivityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val last = ActivityTransitionResult.extractResult(intent)?.transitionEvents
            ?.lastOrNull { it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER } ?: return
        AmbientWisdom.prefs(context).edit()
            .putString(AmbientWisdom.KEY_LAST_ACTIVITY, ActivityWisdomTracker.mapType(last.activityType).key)
            .putLong(AmbientWisdom.KEY_LAST_ACTIVITY_AT, System.currentTimeMillis())
            .apply()
    }
}
