package com.scripturedaily.ambient

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws one frame of the WisdomOne live wallpaper: a generative sky for the time of day, a breathing sun / moon, drifting
 * motes of light, three slow parallax hills and the current verse on a readable glass panel.
 *
 * Used by [VerseLiveWallpaperService] and by the in-app preview, so what users preview is exactly what they get.
 * All shaders, paths and text layouts are cached per size / theme / verse; a frame only moves a few shapes.
 */
class LiveWallpaperRenderer {

    data class Palette(
        val skyTop: Int, val skyMid: Int, val skyBottom: Int, val glow: Int, val orb: Int,
        val hillFar: Int, val hillMid: Int, val hillNear: Int, val text: Int, val accent: Int, val stars: Boolean,
    )

    companion object {
        fun palette(theme: AmbientWisdom.LiveTheme): Palette = when (theme) {
            AmbientWisdom.LiveTheme.MORNING -> Palette(
                Color.rgb(255, 214, 153), Color.rgb(246, 163, 120), Color.rgb(132, 92, 140), Color.rgb(255, 228, 160), Color.rgb(255, 240, 200),
                Color.rgb(176, 110, 128), Color.rgb(120, 72, 112), Color.rgb(72, 42, 82), Color.WHITE, Color.rgb(255, 226, 160), false)
            AmbientWisdom.LiveTheme.AFTERNOON -> Palette(
                Color.rgb(132, 196, 236), Color.rgb(196, 230, 236), Color.rgb(150, 200, 160), Color.rgb(255, 250, 220), Color.rgb(255, 255, 240),
                Color.rgb(112, 168, 140), Color.rgb(70, 134, 104), Color.rgb(36, 92, 72), Color.WHITE, Color.rgb(255, 236, 170), false)
            AmbientWisdom.LiveTheme.EVENING -> Palette(
                Color.rgb(58, 40, 98), Color.rgb(214, 108, 96), Color.rgb(250, 178, 104), Color.rgb(255, 196, 120), Color.rgb(255, 214, 170),
                Color.rgb(140, 70, 92), Color.rgb(92, 44, 78), Color.rgb(46, 22, 52), Color.WHITE, Color.rgb(255, 214, 150), false)
            else -> Palette(
                Color.rgb(8, 14, 40), Color.rgb(20, 36, 78), Color.rgb(40, 58, 108), Color.rgb(214, 224, 255), Color.rgb(170, 190, 255),
                Color.rgb(34, 50, 92), Color.rgb(22, 34, 70), Color.rgb(12, 20, 46), Color.rgb(240, 244, 255), Color.rgb(203, 190, 247), true)
        }
    }

    private class Orb(val x: Float, val y: Float, val r: Float, val speed: Float, val phase: Float, val alpha: Int)

    private val orbs = Random(7).let { rnd -> List(16) { Orb(rnd.nextFloat(), rnd.nextFloat(), 0.006f + rnd.nextFloat() * 0.022f, 0.004f + rnd.nextFloat() * 0.010f, rnd.nextFloat() * 6.28f, 30 + rnd.nextInt(60)) } }
    private val starField = Random(11).let { rnd -> List(70) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat() * 0.6f, 0.6f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 6.28f) } }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL) }
    private val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0.08f }
    private val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; letterSpacing = 0.12f }

    private var cacheKey = ""
    private var sky: Shader? = null
    private var glow: Shader? = null
    private var bodyLayout: StaticLayout? = null
    private var refLayout: StaticLayout? = null
    private var hillPaths = emptyList<Path>()

    private fun hill(w: Int, h: Int, base: Float, amp: Float, waves: Float, phase: Float): Path = Path().apply {
        val extra = w * 0.25f   // drawn wider than the screen so the parallax shift never shows an edge
        moveTo(-extra, h.toFloat())
        var x = -extra
        while (x <= w + extra) {
            val y = h * base - h * amp * (0.6f * sin((x / w) * waves * PI.toFloat() + phase) + 0.4f * sin((x / w) * waves * 2.3f * PI.toFloat() + phase * 1.7f))
            lineTo(x, y); x += w / 48f
        }
        lineTo(w + extra, h.toFloat()); close()
    }

    private fun prepare(w: Int, h: Int, p: Palette, theme: AmbientWisdom.LiveTheme, text: String, reference: String) {
        val key = "$w x $h|$theme|${text.hashCode()}|${reference.hashCode()}"
        if (key == cacheKey) return
        cacheKey = key
        sky = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(p.skyTop, p.skyMid, p.skyBottom), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        glow = RadialGradient(0f, 0f, w * 0.16f * 3.2f, intArrayOf(withAlpha(p.glow, 150), withAlpha(p.glow, 40), withAlpha(p.glow, 0)), floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
        hillPaths = listOf(hill(w, h, 0.80f, 0.045f, 2.2f, 0.4f), hill(w, h, 0.86f, 0.040f, 3.1f, 2.0f), hill(w, h, 0.93f, 0.035f, 4.0f, 4.1f))
        val contentWidth = (min(w, (h * 0.62f).toInt()) * 0.78f).toInt().coerceAtLeast(1)   // narrow column on wide / unfolded screens
        bodyPaint.color = p.text
        metaPaint.color = p.accent
        var size = contentWidth * 0.066f
        var layout: StaticLayout
        do {
            bodyPaint.textSize = size
            layout = StaticLayout.Builder.obtain("“$text”", 0, text.length + 2, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 1.18f).setIncludePad(false).build()
            size *= 0.94f
        } while (layout.height > h * 0.26f && size > contentWidth * 0.03f)   // shrink to fit, never ellipsize the verse
        bodyLayout = layout
        metaPaint.textSize = contentWidth * 0.036f
        refLayout = StaticLayout.Builder.obtain(reference.uppercase(), 0, reference.length, metaPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(2).setIncludePad(false).build()
    }

    /**
     * @param timeMs animation clock (ms). Pass a constant for a still frame.
     * @param xOffset launcher page offset 0..1 (parallax); 0.5 when unknown.
     */
    fun draw(canvas: Canvas, w: Int, h: Int, timeMs: Long, theme: AmbientWisdom.LiveTheme, text: String, reference: String, xOffset: Float = 0.5f) {
        if (w <= 0 || h <= 0) return
        val p = palette(theme)
        prepare(w, h, p, theme, text, reference)
        val t = timeMs / 1000f

        fill.shader = sky; fill.alpha = 255
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fill)
        fill.shader = null

        if (p.stars) for (s in starField) {
            fill.color = Color.WHITE
            fill.alpha = (110 + 110 * sin(t * 0.8f + s[3])).toInt().coerceIn(20, 230)
            canvas.drawCircle(s[0] * w, s[1] * h, s[2] * w / 600f, fill)
        }

        // Sun / moon: breathes very slowly (±4 % over ~14 s)
        val breathe = 1f + 0.04f * sin(t * 2f * PI.toFloat() / 14f)
        val cx = w * (0.70f - (xOffset - 0.5f) * 0.06f)
        // Kept clear of the verse panel (30–58 % of the height): sunrise / sunset sit on the horizon, noon sun / moon up high.
        val cy = h * when (theme) { AmbientWisdom.LiveTheme.MORNING -> 0.70f; AmbientWisdom.LiveTheme.AFTERNOON -> 0.13f; AmbientWisdom.LiveTheme.EVENING -> 0.73f; else -> 0.13f }
        val r = w * if (theme == AmbientWisdom.LiveTheme.AFTERNOON || theme == AmbientWisdom.LiveTheme.NIGHT) 0.11f else 0.16f
        canvas.save(); canvas.translate(cx, cy); canvas.scale(breathe, breathe)
        fill.shader = glow
        canvas.drawCircle(0f, 0f, r * 3.2f, fill)
        fill.shader = null
        fill.color = p.glow; fill.alpha = 235
        canvas.drawCircle(0f, 0f, r * 0.55f, fill)
        canvas.restore()

        // Motes of light drifting upward
        for (o in orbs) {
            val y = ((o.y - t * o.speed * 0.25f) % 1f + 1f) % 1f
            val x = o.x + 0.02f * sin(t * 0.3f + o.phase)
            fill.color = p.orb; fill.alpha = (o.alpha * (0.6f + 0.4f * sin(t * 0.5f + o.phase))).toInt().coerceIn(0, 255)
            canvas.drawCircle(x * w, y * h, o.r * w, fill)
        }

        // Hills with slow drift + launcher parallax
        val colors = intArrayOf(p.hillFar, p.hillMid, p.hillNear)
        hillPaths.forEachIndexed { i, path ->
            val depth = (i + 1) / 3f
            val shift = (sin(t * 0.05f + i) * w * 0.02f) - (xOffset - 0.5f) * w * 0.12f * depth
            canvas.save(); canvas.translate(shift, 0f)
            fill.color = colors[i]; fill.alpha = 255
            canvas.drawPath(path, fill)
            canvas.restore()
        }

        // Verse panel (upper-middle third: clear of the status bar, the clock and most launcher dock rows)
        val body = bodyLayout ?: return
        val ref = refLayout
        val pad = w * 0.06f
        val gap = h * 0.018f
        val blockH = body.height + gap + (ref?.height ?: 0)
        val top = h * 0.30f
        val panel = RectF((w - body.width) / 2f - pad, top - pad, (w + body.width) / 2f + pad, top + blockH + pad)
        panelPaint.color = if (p.stars) Color.argb(96, 6, 10, 30) else Color.argb(92, 20, 10, 30)
        canvas.drawRoundRect(panel, w * 0.05f, w * 0.05f, panelPaint)
        canvas.save(); canvas.translate((w - body.width) / 2f, top)
        body.draw(canvas)
        if (ref != null) { canvas.translate(0f, body.height + gap); ref.draw(canvas) }
        canvas.restore()
        brandPaint.color = withAlpha(p.text, 170); brandPaint.textSize = max(w, 1) * 0.026f
        canvas.drawText("WISDOMONE", w / 2f, panel.bottom + h * 0.035f, brandPaint)
    }

    private fun withAlpha(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
}
