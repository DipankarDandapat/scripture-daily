package com.scripturedaily.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.scripturedaily.MainActivity
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.FcmTokenInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WisdomOneMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM"
        const val CHANNEL_ID = "wisdomone_fcm_v1"
        private const val PREFS = "reader"
        private const val KEY_FCM_TOKEN = "fcm_token"

        fun savedToken(context: Context): String? =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, null)

        /** Call this from Application or MainActivity onCreate so the channel exists
         *  before any background notification arrives. */
        fun createChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "WisdomOne messages", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Verse updates and announcements from WisdomOne" }
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Ensure the channel exists as soon as the service process starts
        createChannel(this)
    }

    // Called when Firebase rotates the token (first install, reinstall, token expiry)
    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed: $token")
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_FCM_TOKEN, token).apply()
        ApiClient.ensureInit(this)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { ApiClient.api.updateFcmToken(FcmTokenInput(token)) }
        }
    }

    // Called ONLY when the app is in the FOREGROUND.
    // When the app is BACKGROUND or KILLED, FCM shows the notification automatically
    // using the channel declared in the manifest (wisdomone_fcm_v1) — no code needed.
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "WisdomOne"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        val verseId = message.data["verse_id"]?.toIntOrNull() ?: -1
        showNotification(title, body, verseId)
    }

    private fun showNotification(title: String, body: String, verseId: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (verseId > 0) putExtra("verse_id", verseId)
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF2D1B4E.toInt())
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
