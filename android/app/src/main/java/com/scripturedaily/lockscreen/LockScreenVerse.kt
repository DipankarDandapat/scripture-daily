package com.scripturedaily.lockscreen

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.Shader
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.scripturedaily.notification.VerseNotifier
import java.io.IOException

/** Applies a static verse image to the lock-screen wallpaper and posts a regular notification. */
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
        val notify = wallpaperEnabled || prefs.getBoolean("reminders", false) || prefs.getBoolean("notify_verse_updates", false)
        if (!wallpaperEnabled && !notify) return null

        val shouldSetWallpaper = wallpaperEnabled &&
            (forceWallpaper || prefs.getInt(KEY_VERSE_ID, -1) != verseId)
        val wallpaperSet = if (shouldSetWallpaper) {
            setLockWallpaper(context, scripture, text, reference).also { success ->
                if (success) prefs.edit().putInt(KEY_VERSE_ID, verseId).apply()
            }
        } else false
        if (wallpaperSet) VerseNotifier.showWallpaperUpdated(context)
        val notified = notify && VerseNotifier.show(context, verseId, scripture, text, reference)
        val message = when {
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

    /** Applies the currently configured wallpaper immediately, even when automatic updates are disabled. */
    fun applyWallpaperNow(
        context: Context,
        verseId: Int,
        scripture: String,
        text: String,
        reference: String
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val applied = setLockWallpaper(context, scripture, text, reference)
        if (applied) prefs.edit().putInt(KEY_VERSE_ID, verseId).apply()
        return applied
    }

    /** Result of an explicit "Save & Apply" request. Success is reported only when Android returns a valid wallpaper id. */
    sealed interface ApplyResult {
        data object Success : ApplyResult
        data class Failed(val reason: String) : ApplyResult
    }

    /**
     * Applies the current wallpaper configuration to the home screen, lock screen or both.
     * [which] is a WallpaperManager.FLAG_SYSTEM / FLAG_LOCK mask. Blocking; call off the main thread.
     */
    fun applyTo(context: Context, which: Int, verseId: Int, scripture: String, text: String, reference: String): ApplyResult {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val manager = WallpaperManager.getInstance(context)
        if (!manager.isWallpaperSupported) return ApplyResult.Failed("This device doesn't support changing the wallpaper.")
        if (!manager.isSetWallpaperAllowed) return ApplyResult.Failed("Wallpaper changes are blocked by this device (a policy or restriction). Check Android settings.")
        val rotation = prefs.getInt("wallpaper_rotation", 0)
        val bitmap = render(context, scripture, text, reference, rotation, null, null)
        return try {
            val id = manager.setBitmap(bitmap, null, true, which)
            if (id > 0) {
                if (which and WallpaperManager.FLAG_LOCK != 0) prefs.edit().putInt(KEY_VERSE_ID, verseId).apply()
                ApplyResult.Success
            } else ApplyResult.Failed("Android did not accept the wallpaper. Some phones restrict this; try Home Screen or Lock Screen separately.")
        } catch (e: SecurityException) {
            ApplyResult.Failed("Android denied the wallpaper permission.")
        } catch (e: IOException) {
            ApplyResult.Failed("Couldn't write the wallpaper: ${e.message ?: "storage error"}.")
        } catch (e: RuntimeException) {
            ApplyResult.Failed("This device couldn't apply the wallpaper (${e.javaClass.simpleName}).")
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    /** Refreshes only the palette/background on the existing verse wallpaper; never posts a notification. */
    fun refreshWallpaperPalette(
        context: Context,
        verseId: Int,
        scripture: String,
        text: String,
        reference: String
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return false
        val applied = setLockWallpaper(context, scripture, text, reference)
        if (applied) prefs.edit().putInt(KEY_VERSE_ID, verseId).apply()
        return applied
    }

    fun isWallpaperAppliedForVerse(context: Context, verseId: Int): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_VERSE_ID, -1) == verseId

    /** Clears only the lock-screen wallpaper; the home-screen wallpaper is untouched. */
    fun clearLockScreenWallpaper(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_VERSE_ID).apply()
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

    private data class WallpaperPalette(val top: Int, val middle: Int, val bottom: Int, val accent: Int, val primaryText: Int, val secondaryText: Int)

    private fun setLockWallpaper(context: Context, scripture: String, text: String, reference: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val rotation = prefs.getInt("wallpaper_rotation", 0)
        val bitmap = render(context, scripture, text, reference, rotation, null, null)
        return try {
            val manager = WallpaperManager.getInstance(context)
            if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) return false
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            // Every refresh advances the scene; Multicolor also advances the palette, fixed themes do not.
            prefs.edit().putInt("wallpaper_rotation", rotation + 1).apply()
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

    private fun paletteFor(theme: String, rotation: Int): WallpaperPalette {
        val palettes = listOf(
            WallpaperPalette(Color.rgb(255, 207, 150), Color.rgb(150, 93, 151), Color.rgb(39, 24, 65), Color.rgb(255, 224, 155), Color.WHITE, Color.rgb(247, 222, 211)),
            WallpaperPalette(Color.rgb(185, 220, 245), Color.rgb(72, 125, 174), Color.rgb(17, 44, 79), Color.rgb(255, 226, 155), Color.WHITE, Color.rgb(221, 234, 247)),
            WallpaperPalette(Color.rgb(255, 222, 167), Color.rgb(183, 121, 65), Color.rgb(70, 39, 50), Color.rgb(255, 238, 190), Color.WHITE, Color.rgb(250, 229, 206)),
            WallpaperPalette(Color.rgb(180, 220, 174), Color.rgb(60, 112, 84), Color.rgb(17, 48, 43), Color.rgb(234, 220, 153), Color.WHITE, Color.rgb(220, 238, 221)),
            WallpaperPalette(Color.rgb(224, 190, 235), Color.rgb(110, 72, 143), Color.rgb(32, 19, 56), Color.rgb(245, 215, 169), Color.WHITE, Color.rgb(232, 218, 244)),
            WallpaperPalette(Color.rgb(255, 231, 201), Color.rgb(191, 150, 112), Color.rgb(82, 58, 58), Color.rgb(255, 246, 220), Color.WHITE, Color.rgb(247, 232, 215)),
            WallpaperPalette(Color.rgb(113, 119, 155), Color.rgb(47, 47, 82), Color.rgb(12, 11, 28), Color.rgb(203, 190, 247), Color.WHITE, Color.rgb(220, 216, 240))
        )
        return when (theme) {
            "GOLDEN" -> WallpaperPalette(Color.rgb(255, 224, 160), Color.rgb(188, 117, 58), Color.rgb(66, 37, 48), Color.rgb(255, 232, 173), Color.WHITE, Color.rgb(250, 229, 205))
            "ROYAL_PURPLE" -> WallpaperPalette(Color.rgb(192, 151, 222), Color.rgb(91, 49, 130), Color.rgb(27, 15, 48), Color.rgb(240, 204, 153), Color.WHITE, Color.rgb(229, 215, 241))
            "FOREST_GREEN" -> WallpaperPalette(Color.rgb(182, 213, 166), Color.rgb(58, 113, 78), Color.rgb(15, 45, 37), Color.rgb(242, 222, 151), Color.WHITE, Color.rgb(220, 238, 218))
            "OCEAN_BLUE" -> WallpaperPalette(Color.rgb(158, 213, 239), Color.rgb(47, 111, 158), Color.rgb(13, 40, 74), Color.rgb(255, 224, 158), Color.WHITE, Color.rgb(216, 235, 246))
            "SOFT_SAND" -> WallpaperPalette(Color.rgb(255, 237, 211), Color.rgb(200, 163, 126), Color.rgb(91, 68, 62), Color.rgb(255, 247, 220), Color.WHITE, Color.rgb(248, 231, 212))
            "MIDNIGHT" -> WallpaperPalette(Color.rgb(78, 77, 110), Color.rgb(35, 31, 61), Color.rgb(8, 8, 21), Color.rgb(202, 185, 248), Color.WHITE, Color.rgb(220, 216, 240))
            else -> palettes[Math.floorMod(rotation, palettes.size)]
        }
    }

    fun previewBitmap(
        context: Context, scripture: String, text: String, reference: String,
        themeOverride: String? = null, categoryOverride: String? = null,
        textSizeOverride: String? = null, textPositionOverride: String? = null,
        imageIndexOverride: Int? = null, religionOverride: Int? = null, modeOverride: String? = null, showVerseOverride: Boolean? = null,
        showReferenceOverride: Boolean? = null, widthPx: Int = 0, heightPx: Int = 0
    ): Bitmap {
        val rotation = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("wallpaper_rotation", 0)
        return render(context, scripture, text, reference, rotation, themeOverride, categoryOverride, textSizeOverride, textPositionOverride, imageIndexOverride, religionOverride, modeOverride,
            previewSelected = true, showVerseOverride = showVerseOverride, showReferenceOverride = showReferenceOverride, widthPx = widthPx, heightPx = heightPx)
    }

    /**
     * Output size in pixels. Uses the real display (including system bars) so the wallpaper has exactly the screen's aspect ratio;
     * an over-large size is scaled down *proportionally* (never clamped per-axis, which used to stretch tall phones).
     */
    private fun targetSize(context: Context, widthPx: Int, heightPx: Int): Pair<Int, Int> {
        var w: Int; var h: Int
        if (widthPx > 0 && heightPx > 0) { w = widthPx; h = heightPx } else {
            val real = realScreenSize(context)
            w = real.first; h = real.second
        }
        val maxSide = 3200f
        val down = minOf(1f, maxSide / maxOf(w, h))
        val minSide = 240f
        val up = maxOf(1f, minSide / minOf(w, h))
        val k = if (down < 1f) down else up
        return Pair(maxOf(1, (w * k).toInt()), maxOf(1, (h * k).toInt()))
    }

    private fun realScreenSize(context: Context): Pair<Int, Int> = try {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            Pair(minOf(b.width(), b.height()), maxOf(b.width(), b.height()))
        } else {
            val m = android.util.DisplayMetrics()
            @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(m)
            Pair(minOf(m.widthPixels, m.heightPixels), maxOf(m.widthPixels, m.heightPixels))
        }
    } catch (_: Exception) {
        val m = context.resources.displayMetrics
        Pair(m.widthPixels, m.heightPixels)
    }

    /**
     * Draws artwork without stretching. When "cover" scaling would throw away more than ~22% of the image (artwork is 2:3 but phones
     * are ~9:20), the whole picture is fitted to the width instead and the remaining top/bottom is filled with a soft, darkened
     * extension of the same image, feathered into the artwork so there is no visible seam.
     */
    private fun drawArtwork(canvas: Canvas, src: Bitmap, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val cover = maxOf(width.toFloat() / src.width, height.toFloat() / src.height)
        val fitW = width.toFloat() / src.width
        val keptFraction = fitW / cover          // share of the source that stays visible under cover scaling
        if (keptFraction >= 0.78f || src.width.toFloat() / src.height >= width.toFloat() / height) {
            val sw = src.width * cover; val sh = src.height * cover
            canvas.drawBitmap(src, null, RectF((width - sw) / 2f, (height - sh) / 2f, (width + sw) / 2f, (height + sh) / 2f), paint)
            return
        }
        // 1) soft backdrop: cover-scaled via a tiny intermediate bitmap (cheap blur), darkened a little
        val tinyW = 32; val tinyH = maxOf(1, (tinyW * height.toFloat() / width).toInt())
        val tiny = Bitmap.createScaledBitmap(src, tinyW, tinyH, true)
        canvas.drawBitmap(tiny, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        tiny.recycle()
        canvas.drawColor(Color.argb(70, 0, 0, 0))
        // 2) the full artwork, fitted to the width, centred
        val ph = src.height * fitW
        val top = (height - ph) / 2f
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawBitmap(src, null, RectF(0f, top, width.toFloat(), top + ph), paint)
        // 3) feather the top/bottom edges of the artwork into the backdrop
        val feather = minOf(ph * 0.18f, height * 0.12f)
        val mask = Paint().apply { xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT) }
        mask.shader = LinearGradient(0f, top, 0f, top + feather, Color.argb(255, 0, 0, 0), Color.argb(0, 0, 0, 0), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, top, width.toFloat(), top + feather, mask)
        mask.shader = LinearGradient(0f, top + ph - feather, 0f, top + ph, Color.argb(0, 0, 0, 0), Color.argb(255, 0, 0, 0), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, top + ph - feather, width.toFloat(), top + ph, mask)
        canvas.restoreToCount(layer)
    }

    private fun render(
        context: Context, scripture: String, text: String, reference: String, rotation: Int,
        themeOverride: String?, categoryOverride: String?, textSizeOverride: String? = null,
        textPositionOverride: String? = null, imageIndexOverride: Int? = null,
        religionOverride: Int? = null, modeOverride: String? = null, previewSelected: Boolean = false,
        showVerseOverride: Boolean? = null, showReferenceOverride: Boolean? = null, widthPx: Int = 0, heightPx: Int = 0
    ): Bitmap {
        val (width, height) = targetSize(context, widthPx, heightPx)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val theme = themeOverride ?: (prefs.getString("wallpaper_theme", "MULTICOLOR") ?: "MULTICOLOR")
        val palette = paletteFor(theme, rotation)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val category = categoryOverride ?: (prefs.getString("wallpaper_category", "") ?: "")
        val tradition = religionOverride ?: prefs.getInt("religion", 0)
        val selectedImageIndex = imageIndexOverride ?: prefs.getInt("wallpaper_image_index", 1)
        val mode = WallpaperMode.normalize(modeOverride ?: prefs.getString("wallpaper_mode", WallpaperMode.VERSE_AND_IMAGE))
        // colors: no artwork (multi-theme backgrounds). keep_image: always the selected image. verse_and_image: walk through the pack,
        // starting at the selected image, once per applied refresh (the preview shows the selected image itself).
        // The user's rotation preference (in order / shuffle) decides the next image; both show every image once before repeating.
        val imageIndex = if (mode == WallpaperMode.VERSE_AND_IMAGE && !previewSelected) com.scripturedaily.ambient.AmbientWisdom.imageFor(
            com.scripturedaily.ambient.AmbientWisdom.ImageOrder.of(prefs.getString(com.scripturedaily.ambient.AmbientWisdom.KEY_IMAGE_ORDER, null)),
            selectedImageIndex, rotation, WallpaperPacks.COUNT, shuffleSeed(prefs)
        ) else WallpaperPacks.clampIndex(selectedImageIndex)
        val sourceImage = if (mode == WallpaperMode.COLORS) null else WallpaperPacks.load(context, tradition, imageIndex)
        val imageResource = if (sourceImage != null) 1 else 0
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (sourceImage != null) {
            drawArtwork(canvas, sourceImage, width, height)
            sourceImage.recycle()
            val tint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.argb(22, 15, 10, 30), Color.argb(18, 15, 10, 30), Color.argb(115, 10, 8, 20)),
                    floatArrayOf(0f, .48f, 1f), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), tint)
            bgPaint.color = palette.accent
            bgPaint.alpha = 38
        } else {
            bgPaint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), palette.top, palette.bottom, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            bgPaint.shader = null
            bgPaint.color = palette.middle
            bgPaint.alpha = 95
            canvas.drawCircle(width * .52f, height * .48f, width * .47f, bgPaint)
            bgPaint.color = palette.accent
            bgPaint.alpha = 38
        }
        val sceneVariant = Math.floorMod(rotation, 4)
        val sunX = floatArrayOf(.82f, .22f, .68f, .43f)[sceneVariant]
        val sunY = floatArrayOf(.20f, .23f, .16f, .27f)[sceneVariant]
        if (imageResource == 0) canvas.drawCircle(width * sunX, height * sunY, width * (.20f + sceneVariant * .012f), bgPaint)
        // Soft mountain/landscape silhouettes make every generated wallpaper feel like artwork, not a flat colour swap.
        val landscape = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.bottom; alpha = 105; style = Paint.Style.FILL }
        val ridge = Path().apply {
            val peaks = when (sceneVariant) {
                1 -> floatArrayOf(.58f, .40f, .65f, .47f, .57f, .39f)
                2 -> floatArrayOf(.61f, .50f, .63f, .42f, .76f, .52f)
                3 -> floatArrayOf(.55f, .43f, .34f, .55f, .70f, .41f)
                else -> floatArrayOf(.62f, .48f, .36f, .60f, .58f, .43f)
            }
            moveTo(0f, height * peaks[0]); lineTo(width * .19f, height * peaks[1]);
            lineTo(width * .36f, height * peaks[2]); lineTo(width * .58f, height * peaks[3]);
            lineTo(width * .78f, height * peaks[4]); lineTo(width.toFloat(), height * peaks[5]);
            lineTo(width.toFloat(), height.toFloat()); lineTo(0f, height.toFloat()); close()
        }
        if (imageResource == 0) canvas.drawPath(ridge, landscape)
        val sparkle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent; alpha = 155; strokeWidth = width * .004f }
        if (imageResource == 0) for (i in 0..11) {
            val x = width * ((i * 37 % 97) / 100f)
            val y = height * (.12f + ((i * 19 % 70) / 100f))
            canvas.drawCircle(x, y, width * .004f, sparkle)
        }

        val contentWidth = (width * 0.84f).toInt()
        val showVerse = showVerseOverride ?: prefs.getBoolean("wallpaper_show_verse", true)
        val showReference = showReferenceOverride ?: prefs.getBoolean("wallpaper_show_reference", true)
        // Dynamic text colour: sample the image in the text region and pick black or white for contrast.
        val textColor = if (imageResource == 0) palette.primaryText
            else contrastTextColor(bitmap, width, height, textPositionOverride ?: prefs.getString("wallpaper_text_position", "Bottom"))
        val accentColor = if (imageResource == 0) palette.accent
            else if (Color.luminance(textColor) > 0.5f) Color.argb(220, 30, 20, 10) else Color.argb(220, 255, 230, 160)
        val shadowColor = if (Color.luminance(textColor) > 0.5f) Color.argb(180, 255, 255, 255) else Color.argb(200, 0, 0, 0)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            if (scripture.all { it.code < 128 }) letterSpacing = 0.08f
            setShadowLayer(width * .012f, 0f, width * .004f, shadowColor)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(width * .016f, 0f, width * .005f, shadowColor)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor; alpha = 210; typeface = Typeface.DEFAULT
            setShadowLayer(width * .012f, 0f, width * .004f, shadowColor)
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor; typeface = Typeface.DEFAULT; textAlign = Paint.Align.CENTER; textSize = width * .028f
            setShadowLayer(width * .010f, 0f, width * .003f, shadowColor)
        }
        val title = layout(if (prefs.getBoolean("reading_scripture", true)) scripture.uppercase() else "", titlePaint, width * .034f, contentWidth, 2)
        val referenceLayout = layout(if (showReference && prefs.getBoolean("reading_reference", true)) reference else "", metaPaint, width * .032f, contentWidth, 3)
        val preferredSize = when (textSizeOverride ?: prefs.getString("wallpaper_text_size", "Medium")) {
            "Small" -> width * .040f
            "Large" -> width * .064f
            else -> width * .052f
        }
        val body = if (showVerse) {
            var size = preferredSize
            val maxHeight = height * .31f
            var current = layout("“$text”", bodyPaint, size, contentWidth, 120)
            // Shrink the preferred size until the entire verse fits. Never silently ellipsize it.
            while (current.height > maxHeight && size > width * .014f) {
                size *= .94f
                current = layout("“$text”", bodyPaint, size, contentWidth, 120)
            }
            current
        } else layout("", bodyPaint, width * .05f, contentWidth, 1)
        val gap = height * .018f
        val blockHeight = title.height + body.height + referenceLayout.height + gap * 2
        val position = textPositionOverride ?: prefs.getString("wallpaper_text_position", "Bottom")
        val blockTop = when (position) {
            "Top" -> height * .27f
            "Middle" -> (height * .50f - blockHeight / 2f).coerceAtLeast(height * .25f)
            else -> (height * .72f - blockHeight).coerceAtLeast(height * .52f)
        }
        var y = blockTop
        canvas.save(); canvas.translate((width - contentWidth) / 2f, y)
        if (title.height > 0) title.draw(canvas)
        y = title.height + gap; canvas.translate(0f, y); body.draw(canvas)
        canvas.translate(0f, body.height + gap); if (referenceLayout.height > 0) referenceLayout.draw(canvas)
        canvas.restore()
        if (prefs.getBoolean("reading_branding", true)) canvas.drawText("WisdomOne", width / 2f, height * .93f, footerPaint)
        return bitmap
    }

    private fun drawTraditionArtwork(canvas: Canvas, category: String, tradition: Int, palette: WallpaperPalette, width: Int, height: Int, sceneVariant: Int) {
        val cx = width * floatArrayOf(.50f, .46f, .54f, .49f)[sceneVariant]
        val cy = height * (.245f + sceneVariant * .008f)
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent; alpha = 170; style = Paint.Style.STROKE; strokeWidth = width * .009f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent; alpha = 105; style = Paint.Style.FILL }
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 85; style = Paint.Style.STROKE; strokeWidth = width * .006f }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = canvas.drawLine(x1, y1, x2, y2, accent)
        fun petal(x: Float, y: Float, rx: Float, ry: Float, angle: Float) {
            canvas.save(); canvas.rotate(angle, x, y); canvas.drawOval(x - rx, y - ry, x + rx, y + ry, accent); canvas.restore()
        }
        when (category) {
            "Krishna", "Radha Krishna" -> {
                // Peacock feather and flute: recognisable devotional artwork without a text glyph.
                line(cx, cy + height * .08f, cx + width * .08f, cy - height * .08f)
                canvas.drawOval(cx - width * .065f, cy - height * .14f, cx + width * .065f, cy + height * .02f, accent)
                canvas.drawCircle(cx, cy - height * .06f, width * .035f, fill)
                canvas.drawCircle(cx, cy - height * .06f, width * .018f, white)
                line(cx - width * .19f, cy + height * .11f, cx + width * .19f, cy + height * .11f)
                for (i in -2..2) canvas.drawCircle(cx + i * width * .055f, cy + height * .11f, width * .008f, fill)
                for (i in 0..5) petal(cx, cy + height * .19f, width * .045f, height * .055f, i * 60f)
            }
            "Shiva" -> {
                line(cx, cy - height * .16f, cx, cy + height * .19f)
                line(cx - width * .13f, cy - height * .07f, cx + width * .13f, cy - height * .07f)
                line(cx - width * .13f, cy - height * .07f, cx - width * .13f, cy + height * .05f)
                line(cx + width * .13f, cy - height * .07f, cx + width * .13f, cy + height * .05f)
                line(cx - width * .13f, cy + height * .05f, cx, cy + height * .13f)
                line(cx + width * .13f, cy + height * .05f, cx, cy + height * .13f)
                canvas.drawArc(cx + width * .02f, cy - height * .19f, cx + width * .16f, cy - height * .04f, 70f, 220f, false, accent)
            }
            "Ganesha" -> {
                canvas.drawCircle(cx, cy, width * .09f, accent)
                canvas.drawOval(cx - width * .18f, cy - height * .035f, cx - width * .06f, cy + height * .10f, accent)
                canvas.drawOval(cx + width * .06f, cy - height * .035f, cx + width * .18f, cy + height * .10f, accent)
                val trunk = Path().apply { moveTo(cx, cy + height * .02f); cubicTo(cx + width * .04f, cy + height * .12f, cx + width * .12f, cy + height * .11f, cx + width * .08f, cy + height * .20f) }
                canvas.drawPath(trunk, accent)
                canvas.drawCircle(cx - width * .03f, cy - height * .02f, width * .009f, fill)
                canvas.drawCircle(cx + width * .03f, cy - height * .02f, width * .009f, fill)
            }
            "Bhagavad Gita" -> {
                val book = Path().apply { moveTo(cx, cy + height * .12f); lineTo(cx - width * .16f, cy + height * .04f); lineTo(cx - width * .16f, cy - height * .13f); lineTo(cx, cy - height * .06f); lineTo(cx + width * .16f, cy - height * .13f); lineTo(cx + width * .16f, cy + height * .04f); close() }
                canvas.drawPath(book, accent); line(cx, cy - height * .06f, cx, cy + height * .12f)
            }
            "Jesus", "Cross", "Biblical Scenes" -> {
                line(cx, cy - height * .18f, cx, cy + height * .20f)
                line(cx - width * .12f, cy - height * .06f, cx + width * .12f, cy - height * .06f)
                canvas.drawCircle(cx, cy - height * .17f, width * .08f, white)
                val hill = Path().apply { moveTo(cx - width * .22f, cy + height * .22f); quadTo(cx, cy + height * .11f, cx + width * .22f, cy + height * .22f) }
                canvas.drawPath(hill, accent)
            }
            "Bible" -> {
                val book = Path().apply { moveTo(cx, cy + height * .16f); lineTo(cx - width * .16f, cy + height * .09f); lineTo(cx - width * .16f, cy - height * .14f); lineTo(cx, cy - height * .07f); lineTo(cx + width * .16f, cy - height * .14f); lineTo(cx + width * .16f, cy + height * .09f); close() }
                canvas.drawPath(book, accent); line(cx, cy - height * .07f, cx, cy + height * .16f)
                line(cx - width * .07f, cy - height * .03f, cx - width * .02f, cy - height * .03f)
                line(cx + width * .02f, cy - height * .03f, cx + width * .07f, cy - height * .03f)
            }
            "Church" -> {
                canvas.drawRect(cx - width * .15f, cy - height * .04f, cx + width * .15f, cy + height * .20f, accent)
                canvas.drawArc(cx - width * .15f, cy - height * .18f, cx + width * .15f, cy + height * .10f, 180f, 180f, false, accent)
                canvas.drawRect(cx - width * .035f, cy + height * .08f, cx + width * .035f, cy + height * .20f, white)
                line(cx, cy - height * .23f, cx, cy - height * .12f); line(cx - width * .035f, cy - height * .19f, cx + width * .035f, cy - height * .19f)
            }
            "Mosques" -> {
                canvas.drawArc(cx - width * .17f, cy - height * .09f, cx + width * .17f, cy + height * .20f, 180f, 180f, false, accent)
                canvas.drawRect(cx - width * .17f, cy + height * .04f, cx + width * .17f, cy + height * .20f, fill)
                canvas.drawRect(cx - width * .23f, cy - height * .13f, cx - width * .18f, cy + height * .20f, accent)
                canvas.drawRect(cx + width * .18f, cy - height * .13f, cx + width * .23f, cy + height * .20f, accent)
                canvas.drawArc(cx + width * .04f, cy - height * .21f, cx + width * .14f, cy - height * .10f, 70f, 220f, false, accent)
            }
            "Islamic Geometric Patterns", "Quran Calligraphy", "Arabic Artwork" -> {
                for (row in -2..2) for (col in -2..2) {
                    val x = cx + col * width * .075f
                    val y = cy + row * height * .055f
                    val diamond = Path().apply { moveTo(x, y - height * .035f); lineTo(x + width * .035f, y); lineTo(x, y + height * .035f); lineTo(x - width * .035f, y); close() }
                    canvas.drawPath(diamond, accent)
                    if (category != "Islamic Geometric Patterns") canvas.drawCircle(x, y, width * .009f, fill)
                }
            }
            "Buddha", "Dharma", "Meditation" -> {
                canvas.drawCircle(cx, cy - height * .09f, width * .055f, accent)
                val body = Path().apply { moveTo(cx, cy - height * .02f); quadTo(cx - width * .13f, cy + height * .08f, cx - width * .17f, cy + height * .17f); quadTo(cx, cy + height * .22f, cx + width * .17f, cy + height * .17f); quadTo(cx + width * .13f, cy + height * .08f, cx, cy - height * .02f); close() }
                canvas.drawPath(body, accent)
                line(cx - width * .20f, cy + height * .18f, cx + width * .20f, cy + height * .18f)
                if (category == "Dharma") {
                    canvas.drawCircle(cx, cy + height * .02f, width * .13f, accent)
                    for (i in 0..7) { val a = i * Math.PI / 4; line(cx, cy + height * .02f, cx + (width * .13f * kotlin.math.cos(a)).toFloat(), cy + height * .02f + (width * .13f * kotlin.math.sin(a)).toFloat()) }
                }
            }
            "Lotus", "Flowers" -> {
                for (i in 0..7) petal(cx, cy + height * .07f, width * .045f, height * .09f, i * 45f)
                canvas.drawCircle(cx, cy + height * .07f, width * .035f, fill)
                line(cx, cy + height * .16f, cx, cy + height * .24f)
            }
            "Nature", "Mountains", "Sunrise", "Ocean", "Universal Inspiration" -> {
                canvas.drawCircle(cx, cy - height * .05f, width * .09f, fill)
                if (category == "Ocean") {
                    for (i in 0..3) canvas.drawArc(cx - width * .22f, cy + height * (.04f + i * .045f), cx + width * .22f, cy + height * (.18f + i * .045f), 0f, 180f, false, accent)
                } else {
                    val hill = Path().apply { moveTo(cx - width * .28f, cy + height * .18f); lineTo(cx - width * .10f, cy + height * .02f); lineTo(cx + width * .02f, cy + height * .15f); lineTo(cx + width * .18f, cy - height * .01f); lineTo(cx + width * .29f, cy + height * .18f) }
                    canvas.drawPath(hill, accent)
                }
            }
            else -> {
                if (tradition == 5) {
                    canvas.drawCircle(cx, cy, width * .08f, fill)
                    for (i in 0..5) petal(cx, cy, width * .04f, height * .08f, i * 60f)
                } else canvas.drawCircle(cx, cy, width * .12f, accent)
            }
        }
    }

    /**
     * Samples a horizontal band of the bitmap in the text region and returns Color.WHITE or Color.BLACK
     * depending on which gives better contrast against the average luminance of that area.
     */
    private fun contrastTextColor(bitmap: Bitmap, width: Int, height: Int, position: String?): Int {
        val bandTop = when (position) {
            "Top" -> (height * 0.22f).toInt()
            "Middle" -> (height * 0.38f).toInt()
            else -> (height * 0.58f).toInt()   // Bottom (default)
        }
        val bandBottom = (bandTop + height * 0.32f).toInt().coerceAtMost(height - 1)
        val sampleLeft = (width * 0.08f).toInt()
        val sampleRight = (width * 0.92f).toInt()
        val stepX = maxOf(1, (sampleRight - sampleLeft) / 20)
        val stepY = maxOf(1, (bandBottom - bandTop) / 10)
        var totalLum = 0.0
        var count = 0
        var y = bandTop
        while (y < bandBottom) {
            var x = sampleLeft
            while (x < sampleRight) {
                val px = bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y.coerceIn(0, bitmap.height - 1))
                val r = Color.red(px) / 255.0
                val g = Color.green(px) / 255.0
                val b = Color.blue(px) / 255.0
                // Relative luminance (WCAG formula)
                totalLum += 0.2126 * r + 0.7152 * g + 0.0722 * b
                count++
                x += stepX
            }
            y += stepY
        }
        val avgLum = if (count > 0) totalLum / count else 0.5
        return if (avgLum > 0.45) Color.BLACK else Color.WHITE
    }

    private fun shuffleSeed(prefs: android.content.SharedPreferences): Long =
        prefs.getLong("wallpaper_shuffle_seed", 0L).takeIf { it != 0L }
            ?: (System.nanoTime() or 1L).also { prefs.edit().putLong("wallpaper_shuffle_seed", it).apply() }

    private fun layout(text: String, paint: TextPaint, textSizePx: Float, width: Int, maxLines: Int): StaticLayout {
        paint.textSize = textSizePx
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setLineSpacing(0f, 1.15f)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .build()
    }
}
