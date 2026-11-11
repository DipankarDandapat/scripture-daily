package com.scripturedaily.ambient

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scripturedaily.MainActivity

/**
 * "Ambient verse" — a silent, ongoing, single-line notification with public lock-screen visibility.
 *
 * This is WisdomOne's officially supported alternative to Always-On Display and to a "Dynamic Island": Android has no
 * third-party API to draw on the AOD, but devices whose AOD / lock screen shows notifications will show this one-line
 * excerpt. It is low importance (no sound, no vibration, no heads-up), minimal (one line, no image) and is only
 * re-posted when the verse changes — no background work of its own.
 */
object AmbientNotification {
    private const val CHANNEL_ID = "wisdomone_ambient_verse_v1"
    private const val NOTIFICATION_ID = 4107

    fun enabled(context: Context) = AmbientWisdom.prefs(context).getBoolean(AmbientWisdom.KEY_AMBIENT_NOTIFICATION, false)

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Posts / refreshes the ambient verse when enabled, cancels it otherwise. Returns true when it is showing. */
    fun update(context: Context): Boolean {
        if (!enabled(context)) { cancel(context); return false }
        if (!canPost(context)) return false
        val (text, reference) = VerseLiveWallpaperService.currentVerse(context)
        if (text == VerseLiveWallpaperService.FALLBACK_TEXT) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Ambient verse", NotificationManager.IMPORTANCE_LOW).apply {
                description = "A quiet one-line verse for your lock screen and always-on display (where your phone supports it)."
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false); enableVibration(false); setSound(null, null)
            })
        }
        val open = PendingIntent.getActivity(context, 41, Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val line = AmbientWisdom.excerpt(text)
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(line)
            .setContentText(reference)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setColor(0xFF2D1B4E.toInt())
            .setContentIntent(open)
            .build()
        return runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n); true }.getOrDefault(false)
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
}
