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
import com.scripturedaily.lockscreen.LockScreenVerseActivity

/** Posts the current verse where the lock screen can show it. */
object VerseNotifier {
    private const val CHANNEL_ID = "scripture_verse_lock_screen_v2"
    private const val LEGACY_CHANNEL_ID = "scripture_verse_lock_screen_v1"
    private const val NOTIFICATION_ID = 1001

    fun canUseFullScreenIntent(context: Context): Boolean =
        NotificationManagerCompat.from(context).canUseFullScreenIntent()

    fun show(
        context: Context,
        scripture: String,
        text: String,
        reference: String,
        isDemo: Boolean = false,
        launchCard: Boolean = false
    ): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scripture verse on lock screen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows the selected scripture verse on the lock screen"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = "Today’s $scripture verse"
        val body = "$text\n$reference"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText("$text · $reference")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setColor(0xFF183D32.toInt())

        if (launchCard && canUseFullScreenIntent(context)) {
            val card = Intent(context, LockScreenVerseActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(LockScreenVerseActivity.EXTRA_SCRIPTURE, scripture)
                putExtra(LockScreenVerseActivity.EXTRA_TEXT, text)
                putExtra(LockScreenVerseActivity.EXTRA_REFERENCE, reference)
                putExtra(LockScreenVerseActivity.EXTRA_DEMO, isDemo)
            }
            val fullScreen = PendingIntent.getActivity(
                context,
                1,
                card,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreen, true)
        }

        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText("$text · $reference")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .build()
        builder.setPublicVersion(publicVersion)

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
