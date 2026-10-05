package com.scripturedaily.lockscreen

import android.app.WallpaperManager
import android.content.Context
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
import com.scripturedaily.notification.VerseNotifier
import java.io.IOException

/**
 * Publishes a verse onto the lock screen surfaces Android exposes to apps:
 * the lock-screen wallpaper, and a public lock-screen notification that can
 * open a card over the keyguard.
 */
object LockScreenVerse {
    private const val PREFS = "reader"
    private const val KEY_ENABLED = "lock_screen"
    private const val KEY_VERSE_ID = "lock_screen_verse_id"

    fun canSetLockWallpaper(context: Context): Boolean = try {
        val manager = WallpaperManager.getInstance(context)
        manager.isWallpaperSupported && manager.isSetWallpaperAllowed
    } catch (_: RuntimeException) {
        false
    }

    fun canPostLockCard(context: Context): Boolean =
        VerseNotifier.canUseFullScreenIntent(context)

    /**
     * Blocking. Call from a background thread.
     * Returns a short message when the user should hear the result, or null when nothing was requested.
     */
    fun applyIfEnabled(
        context: Context,
        verseId: Int,
        scripture: String,
        text: String,
        reference: String,
        isDemo: Boolean,
        forceWallpaper: Boolean,
        launchCard: Boolean
    ): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wallpaperEnabled = prefs.getBoolean(KEY_ENABLED, false)
        val notify = wallpaperEnabled || prefs.getBoolean("reminders", false)
        if (!wallpaperEnabled && !notify) return null
        val wallpaper = wallpaperEnabled && (forceWallpaper || prefs.getInt(KEY_VERSE_ID, -1) != verseId)
        val result = apply(context, scripture, text, reference, isDemo, wallpaper, notify, launchCard && wallpaperEnabled)
        if (result.wallpaperSet) prefs.edit().putInt(KEY_VERSE_ID, verseId).apply()
        return result.message
    }

    private data class ApplyResult(val message: String, val wallpaperSet: Boolean)

    fun clearWallpaper(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_VERSE_ID).apply()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            WallpaperManager.getInstance(context).clear(WallpaperManager.FLAG_LOCK)
        } catch (_: IOException) {
            // The device may have no separate lock wallpaper to clear.
        } catch (_: SecurityException) {
            // A device policy can forbid wallpaper changes.
        }
    }

    private fun apply(
        context: Context,
        scripture: String,
        text: String,
        reference: String,
        isDemo: Boolean,
        wallpaper: Boolean,
        notify: Boolean,
        launchCard: Boolean
    ): ApplyResult {
        var wallpaperSet = false
        var wallpaperBlocked = false
        var notified = false
        if (wallpaper) {
            val bitmap = render(context, scripture, text, reference, isDemo)
            try {
                val manager = WallpaperManager.getInstance(context)
                if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) {
                    wallpaperBlocked = true
                } else {
                    manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    wallpaperSet = true
                }
            } catch (_: IOException) {
                wallpaperBlocked = true
            } catch (_: SecurityException) {
                wallpaperBlocked = true
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
        if (notify) {
            notified = VerseNotifier.show(context, scripture, text, reference, isDemo, launchCard)
        }
        val message = when {
            wallpaperSet -> "Today's verse is on your lock screen. Lock the phone to see it."
            wallpaperBlocked && notified -> "This phone blocked the lock screen wallpaper. The verse is in your lock screen notifications."
            notified -> "The verse was added to your lock screen notifications. Allow notification content on the lock screen."
            wallpaperBlocked -> "This phone blocked the lock screen wallpaper. Allow notifications for Scripture Daily, then try again."
            else -> "Allow notifications so the verse can appear on the lock screen."
        }
        return ApplyResult(message, wallpaperSet)
    }

    private fun render(context: Context, scripture: String, text: String, reference: String, isDemo: Boolean): Bitmap {
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels.coerceIn(480, 1440)
        val height = metrics.heightPixels.coerceIn(800, 3200)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#183D32"))

        val contentWidth = (width * 0.84f).toInt()
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8AC68")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            if (scripture.all { it.code < 128 }) letterSpacing = 0.08f
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8F5EC")
            typeface = Typeface.DEFAULT
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D0DAD3")
            typeface = Typeface.DEFAULT
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A7CDB8")
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            textSize = width * 0.028f
        }

        val title = layout(scripture.uppercase(), titlePaint, width * 0.034f, contentWidth, 3)
        val referenceLayout = layout(reference, metaPaint, width * 0.032f, contentWidth, 3)
        val demoLayout = null
        val bandTop = height * 0.36f
        val bandBottom = height * 0.78f
        val gap = height * 0.028f
        val fixed = title.height + referenceLayout.height + gap * 2
        val maxBodyHeight = (bandBottom - bandTop - fixed).coerceAtLeast(height * 0.16f)
        var bodySize = width * 0.058f
        val minBodySize = width * 0.034f
        var body = layout("“$text”", bodyPaint, bodySize, contentWidth, 16)
        while (body.height > maxBodyHeight && bodySize > minBodySize) {
            bodySize -= width * 0.002f
            body = layout("“$text”", bodyPaint, bodySize, contentWidth, 16)
        }

        val block = title.height + gap + body.height + gap + referenceLayout.height
        var y = bandTop + ((bandBottom - bandTop - block).coerceAtLeast(0f) / 2f)
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
        canvas.drawText("Scripture Daily", width / 2f, height * 0.92f, footerPaint)
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
