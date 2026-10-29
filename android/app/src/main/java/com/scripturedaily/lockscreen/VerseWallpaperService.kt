package com.scripturedaily.lockscreen

import android.content.SharedPreferences
import android.graphics.Rect
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.scripturedaily.data.ScriptureDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Draws today's verse as a live wallpaper. Unlike a static wallpaper set with WallpaperManager.setBitmap,
 * Android removes a live wallpaper automatically when its app is uninstalled, so the lock screen goes back
 * to the default without the app having to run any cleanup code.
 */
class VerseWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VerseEngine()

    private inner class VerseEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val prefs = getSharedPreferences("reader", MODE_PRIVATE)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var job: Job? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            scope.cancel()
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) redraw()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            redraw()
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String?) {
            if (key == LockScreenVerse.KEY_VERSE_ID || key == LockScreenVerse.KEY_LIVE_REV) redraw()
        }

        private fun redraw() {
            job?.cancel()
            job = scope.launch { draw() }
        }

        private suspend fun draw() {
            val holder = surfaceHolder
            val frame = holder.surfaceFrame
            if (frame.isEmpty) return
            val app = applicationContext
            val verse = prefs.getInt(LockScreenVerse.KEY_VERSE_ID, -1).takeIf { it > 0 }
                ?.let { ScriptureDatabase.get(app).verses().find(it) }
            val bitmap = if (verse != null) {
                LockScreenVerse.render(app, verse.scripture, verse.text, verse.reference())
            } else {
                LockScreenVerse.render(app, "WisdomOne", "Open the app to load today's verse.", "")
            }
            val canvas = holder.lockCanvas()
            if (canvas == null) {
                bitmap.recycle()
                return
            }
            try {
                canvas.drawBitmap(bitmap, null, Rect(0, 0, frame.width(), frame.height()), null)
            } finally {
                holder.unlockCanvasAndPost(canvas)
                bitmap.recycle()
            }
        }
    }
}
