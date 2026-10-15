package com.scripturedaily.lockscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scripturedaily.data.ScriptureDatabase
import com.scripturedaily.data.PaletteRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Restores the selected verse wallpaper and regular notification after reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) return
        val pending = goAsync()
        val app = context.applicationContext
        PaletteRefreshWorker.schedule(app)
        if (action != Intent.ACTION_BOOT_COMPLETED) {
            pending.finish()
            return
        }
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
                    cached.reference()
                )
            } finally {
                pending.finish()
            }
        }
    }
}
