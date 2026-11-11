package com.scripturedaily.ambient

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.time.LocalTime

/**
 * WisdomOne live wallpaper: the current verse on a slowly animated, time-of-day sky (see [LiveWallpaperRenderer]).
 *
 * Battery: draws only while visible, at ~12 fps with hardware canvas; in "Still" motion or Battery Saver it draws a single
 * frame and only redraws when the verse, theme or hour-of-day mood changes. It never touches the network — the verse comes
 * from the same local cache the widget uses.
 */
class VerseLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VerseEngine()

    private inner class VerseEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val handler = Handler(Looper.getMainLooper())
        private val renderer = LiveWallpaperRenderer()
        private val prefs = AmbientWisdom.prefs(this@VerseLiveWallpaperService)
        private val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        private var visible = false
        private var width = 0
        private var height = 0
        private var xOffset = 0.5f
        private val startedAt = SystemClock.uptimeMillis()
        private val stillClock = 40_000L   // a pleasant fixed frame for "Still" / Battery Saver

        private val frame = object : Runnable {
            override fun run() {
                drawFrame()
                handler.removeCallbacks(this)
                if (!visible) return
                if (animating()) handler.postDelayed(this, FRAME_MS)
                else handler.postDelayed(this, STILL_REFRESH_MS)   // pick up the next time-of-day mood
            }
        }

        private fun animating() = AmbientWisdom.Motion.of(prefs.getString(AmbientWisdom.KEY_LIVE_MOTION, null)) == AmbientWisdom.Motion.SUBTLE &&
            !power.isPowerSaveMode

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setOffsetNotificationsEnabled(true)
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            handler.removeCallbacks(frame)
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            handler.removeCallbacks(frame)
            if (visible) handler.post(frame)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width; this.height = height   // also covers fold / unfold and rotation
            handler.removeCallbacks(frame); handler.post(frame)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xStep: Float, yStep: Float, xPixels: Int, yPixels: Int) {
            this.xOffset = xOffset
            if (!animating() && visible) drawFrame()
        }

        override fun onSharedPreferenceChanged(p: SharedPreferences?, key: String?) {
            if (key in WATCHED && visible) { handler.removeCallbacks(frame); handler.post(frame) }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            if (width <= 0 || height <= 0) return
            val canvas = runCatching { holder.lockHardwareCanvas() }.getOrNull() ?: runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                val (text, reference) = currentVerse(this@VerseLiveWallpaperService)
                val theme = AmbientWisdom.resolveTheme(AmbientWisdom.LiveTheme.of(prefs.getString(AmbientWisdom.KEY_LIVE_THEME, null)), LocalTime.now().hour)
                val clock = if (animating()) SystemClock.uptimeMillis() - startedAt else stillClock
                renderer.draw(canvas, width, height, clock, theme, text, reference, xOffset)
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }
    }

    companion object {
        private const val FRAME_MS = 83L                       // ≈12 fps: smooth for slow motion, light on the GPU
        private const val STILL_REFRESH_MS = 15 * 60 * 1000L
        private val WATCHED = setOf("widget_text", "widget_reference", AmbientWisdom.KEY_LIVE_THEME, AmbientWisdom.KEY_LIVE_MOTION)
        const val FALLBACK_TEXT = "Open WisdomOne to receive your daily verse."

        /** The verse shown everywhere else (Home, widget, lock screen) — read from the local cache only. */
        fun currentVerse(context: Context): Pair<String, String> {
            val p = AmbientWisdom.prefs(context)
            val text = p.getString("widget_text", null)?.takeIf { it.isNotBlank() } ?: FALLBACK_TEXT
            val ref = p.getString("widget_reference", null).orEmpty()
            return text to ref
        }

        fun component(context: Context) = ComponentName(context, VerseLiveWallpaperService::class.java)

        /** True when WisdomOne is the current home-screen live wallpaper. */
        fun isActive(context: Context): Boolean = runCatching {
            WallpaperManager.getInstance(context).wallpaperInfo?.component == component(context)
        }.getOrDefault(false)
    }
}
