package com.scripturedaily.lockscreen

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.scripturedaily.lockscreentime.TimePalette
import com.scripturedaily.notification.VerseNotifier
import java.io.IOException

/** Applies a static verse image to the lock-screen wallpaper and posts a regular notification. */
object LockScreenVerse {
    private const val PREFS = "reader"
    private const val KEY_ENABLED = "lock_screen"
    internal const val KEY_VERSE_ID = "lock_screen_verse_id"
    /** True once the user chose the live wallpaper, which Android removes automatically on uninstall. */
    const val KEY_LIVE = "lock_live_mode"
    /** Bumped to tell the live wallpaper to redraw (palette change, boot). */
    internal const val KEY_LIVE_REV = "lock_live_rev"

    fun liveWallpaperSupported(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LIVE_WALLPAPER)

    fun isLiveWallpaperActive(context: Context): Boolean = try {
        val manager = WallpaperManager.getInstance(context)
        val info = if (Build.VERSION.SDK_INT >= 34) manager.getWallpaperInfo(WallpaperManager.FLAG_LOCK) else manager.wallpaperInfo
        info?.packageName == context.packageName
    } catch (_: RuntimeException) {
        false
    }

    /** Opens Android's live-wallpaper preview for the verse wallpaper; the user taps "Set wallpaper" there. */
    fun liveWallpaperChooserIntent(context: Context): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, VerseWallpaperService::class.java))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun canSetLockWallpaper(context: Context): Boolean = try {
        val manager = WallpaperManager.getInstance(context)
        manager.isWallpaperSupported && manager.isSetWallpaperAllowed
    } catch (_: RuntimeException) {
        false
    }

    /** Blocking; call from a background thread. */
    fun applyIfEnabled(
        context: Context,
        verseId: Int,
        scripture: String,
        text: String,
        reference: String,
        forceWallpaper: Boolean = false
    ): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wallpaperEnabled = prefs.getBoolean(KEY_ENABLED, false)
        val notify = wallpaperEnabled || prefs.getBoolean("reminders", false)
        if (!wallpaperEnabled && !notify) return null

        val livePending = wallpaperEnabled && prefs.getBoolean(KEY_LIVE, false) && !isLiveWallpaperActive(context)
        val shouldSetWallpaper = wallpaperEnabled &&
            (forceWallpaper || prefs.getInt(KEY_VERSE_ID, -1) != verseId)
        // Live mode: the wallpaper service reads this id, so record it even before the user finishes picking it.
        if (shouldSetWallpaper && prefs.getBoolean(KEY_LIVE, false)) prefs.edit().putInt(KEY_VERSE_ID, verseId).apply()
        val wallpaperSet = if (shouldSetWallpaper) {
            setLockWallpaper(context, scripture, text, reference).also { success ->
                if (success) prefs.edit().putInt(KEY_VERSE_ID, verseId).apply()
            }
        } else false
        val notified = notify && VerseNotifier.show(context, verseId, scripture, text, reference)
        val message = when {
            livePending -> "Tap Set wallpaper, then choose Lock screen. Android removes this wallpaper automatically if you uninstall the app."
            wallpaperSet && notified -> "Today's verse wallpaper and lock-screen notification are active."
            wallpaperSet -> "Today's verse is on your lock screen. Notifications are off; enable them in Android Settings if you also want alerts."
            shouldSetWallpaper && !wallpaperSet && notified -> "Android could not set the lock-screen wallpaper. The verse is available in notifications."
            shouldSetWallpaper && !wallpaperSet -> "Android blocked the lock-screen wallpaper and notifications are unavailable. Check Android settings."
            wallpaperEnabled && !notified -> "The verse wallpaper is active. Notifications are unavailable; check Android notification settings."
            wallpaperEnabled -> "The verse wallpaper and lock-screen notification are active."
            notified -> "Verse reminder updated. It is shown in the notification shade, not on the lock screen."
            shouldSetWallpaper && !canSetLockWallpaper(context) -> "This phone blocked lock-screen wallpaper changes. Check Android wallpaper settings."
            else -> "Allow notifications for WisdomOne to show the verse."
        }
        return message
    }

    /** Refreshes only the palette/background on the existing verse wallpaper; never posts a notification. */
    fun refreshWallpaperPalette(
        context: Context,
        verseId: Int,
        scripture: String,
        text: String,
        reference: String
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return
        if (setLockWallpaper(context, scripture, text, reference)) {
            prefs.edit().putInt(KEY_VERSE_ID, verseId).apply()
        }
    }

    /** Clears only the lock-screen wallpaper; the home-screen wallpaper is untouched. */
    fun clearLockScreenWallpaper(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_VERSE_ID).remove(KEY_LIVE).apply()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            WallpaperManager.getInstance(context).clear(WallpaperManager.FLAG_LOCK)
        } catch (_: IOException) {
            // Some devices do not have a separately configurable lock-screen image.
        } catch (_: SecurityException) {
            // A device policy may forbid wallpaper changes.
        } catch (_: RuntimeException) {
            // Leave the existing system wallpaper untouched if the service is unavailable.
        }
    }

    private fun setLockWallpaper(context: Context, scripture: String, text: String, reference: String): Boolean {
        val livePrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (livePrefs.getBoolean(KEY_LIVE, false)) {
            // Live mode: the wallpaper service draws the verse itself. Never overwrite it with a static image.
            if (!isLiveWallpaperActive(context)) return false
            livePrefs.edit().putLong(KEY_LIVE_REV, System.currentTimeMillis()).apply()
            return true
        }
        val bitmap = render(context, scripture, text, reference)
        return try {
            val manager = WallpaperManager.getInstance(context)
            if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) return false
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: RuntimeException) {
            false
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    internal fun render(context: Context, scripture: String, text: String, reference: String): Bitmap {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels.coerceIn(480, 1440)
        val height = metrics.heightPixels.coerceIn(800, 3200)
        val interval = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("palette_interval_hours", 6)
        val palette = TimePalette.now(interval)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.background)

        val contentWidth = (width * 0.84f).toInt()
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            if (scripture.all { it.code < 128 }) letterSpacing = 0.08f
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.primaryText
            typeface = Typeface.DEFAULT
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.secondaryText
            typeface = Typeface.DEFAULT
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            textSize = width * 0.028f
        }

        val title = layout(scripture.uppercase(), titlePaint, width * 0.034f, contentWidth, 3)
        val referenceLayout = layout(reference, metaPaint, width * 0.032f, contentWidth, 3)
        val bandTop = height * 0.34f
        val bandBottom = height * 0.80f
        val gap = height * 0.028f
        val fixedHeight = title.height + referenceLayout.height + gap * 2
        val maxBodyHeight = (bandBottom - bandTop - fixedHeight).coerceAtLeast(height * 0.16f)
        var bodySize = width * 0.058f
        val minBodySize = width * 0.034f
        var body = layout("“$text”", bodyPaint, bodySize, contentWidth, 18)
        while (body.height > maxBodyHeight && bodySize > minBodySize) {
            bodySize -= width * 0.002f
            body = layout("“$text”", bodyPaint, bodySize, contentWidth, 18)
        }

        val blockHeight = title.height + gap + body.height + gap + referenceLayout.height
        var y = bandTop + ((bandBottom - bandTop - blockHeight).coerceAtLeast(0f) / 2f)
        val left = (width - contentWidth) / 2f
        canvas.save()
        canvas.translate(left, y)
        title.draw(canvas)
        y = title.height + gap
        canvas.translate(0f, y)
        body.draw(canvas)
        canvas.translate(0f, body.height + gap)
        referenceLayout.draw(canvas)
        canvas.restore()
        canvas.drawText("WisdomOne", width / 2f, height * 0.92f, footerPaint)
        return bitmap
    }

    private fun layout(text: String, paint: TextPaint, textSizePx: Float, width: Int, maxLines: Int): StaticLayout {
        paint.textSize = textSizePx
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(0f, 1.15f)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .build()
    }
}
