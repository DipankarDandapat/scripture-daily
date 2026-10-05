package com.scripturedaily.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scripturedaily.data.ScriptureDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Restores the lock-screen verse after reboot. The wallpaper itself already survives reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = app.getSharedPreferences("reader", Context.MODE_PRIVATE)
                if (!prefs.getBoolean("lock_screen", false) && !prefs.getBoolean("reminders", false)) return@launch
                val cached = ScriptureDatabase.get(app).verses().matching(
                    prefs.getInt("scripture", 1),
                    prefs.getInt("language", 1)
                ) ?: return@launch
                LockScreenVerse.applyIfEnabled(
                    app,
                    cached.id,
                    cached.scripture,
                    cached.text,
                    cached.reference(),
                    cached.isDemo,
                    forceWallpaper = true,
                    launchCard = false
                )
            } finally {
                pending.finish()
            }
        }
    }
}
