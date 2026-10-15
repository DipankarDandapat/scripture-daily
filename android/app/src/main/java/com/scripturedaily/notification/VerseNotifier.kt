package com.scripturedaily.notification

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

/** Delivers lock-screen verses and verse reminders with separate lock-screen visibility. */
object VerseNotifier {
    private const val LOCK_CHANNEL_ID = "scripture_verse_lock_screen_v2"
    private const val REMINDER_CHANNEL_ID = "scripture_verse_reminders_v1"
    private const val LEGACY_CHANNEL_ID = "scripture_verse_lock_screen_v1"
    private const val LOCK_NOTIFICATION_ID = 1001
    private const val REMINDER_NOTIFICATION_ID = 1002

    fun show(
        context: Context,
        verseId: Int,
        scripture: String,
        text: String,
        reference: String
    ): Boolean {
        val prefs = context.getSharedPreferences("reader", Context.MODE_PRIVATE)
        val showOnLockScreen = prefs.getBoolean("lock_screen", false)
        val remindersEnabled = prefs.getBoolean("reminders", false)
        if (!showOnLockScreen && !remindersEnabled) {
            cancel(context)
            return false
        }

        val compatManager = NotificationManagerCompat.from(context)
        val notificationId = if (showOnLockScreen) LOCK_NOTIFICATION_ID else REMINDER_NOTIFICATION_ID
        if (showOnLockScreen) compatManager.cancel(REMINDER_NOTIFICATION_ID)
        else compatManager.cancel(LOCK_NOTIFICATION_ID)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            cancel(context)
            return false
        }
        if (!compatManager.areNotificationsEnabled()) {
            cancel(context)
            return false
        }

        val channelId = if (showOnLockScreen) LOCK_CHANNEL_ID else REMINDER_CHANNEL_ID
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            val channel = if (showOnLockScreen) {
                NotificationChannel(
                    LOCK_CHANNEL_ID,
                    "Scripture verse on lock screen",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Shows the selected scripture verse on the lock screen"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(false)
                }
            } else {
                NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Verse reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Verse reminders stay out of the lock screen"
                    lockscreenVisibility = Notification.VISIBILITY_SECRET
                    setShowBadge(false)
                }
            }
            manager.createNotificationChannel(channel)
            if (manager.getNotificationChannel(channelId)?.importance == NotificationManager.IMPORTANCE_NONE) {
                compatManager.cancel(notificationId)
                return false
            }
        }

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("verse_id", verseId)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = "Today’s $scripture verse"
        val body = "$text\n$reference"
        val visibility = if (showOnLockScreen) NotificationCompat.VISIBILITY_PUBLIC
            else NotificationCompat.VISIBILITY_SECRET
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText("$text · $reference")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(visibility)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setColor(0xFF2D1B4E.toInt())

        if (showOnLockScreen) {
            val publicVersion = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(title)
                .setContentText("$text · $reference")
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .build()
            builder.setPublicVersion(publicVersion)
        }

        return try {
            compatManager.notify(notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancelLockScreen(context: Context) {
        NotificationManagerCompat.from(context).cancel(LOCK_NOTIFICATION_ID)
    }

    fun cancel(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(LOCK_NOTIFICATION_ID)
        manager.cancel(REMINDER_NOTIFICATION_ID)
    }
}
