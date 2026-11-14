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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Each theme has its own draw function with a completely different visual structure:
 *  MORNING/EVENING/GOLDEN_HOUR  – horizon sun + rolling hills + dust motes
 *  AFTERNOON                    – bright sky + fluffy cloud shapes + green meadow
 *  NIGHT/STARFIELD              – deep space + dense star field + fireflies
 *  MONSOON                      – dark storm clouds + heavy rain streaks + lightning flash
 *  DESERT_DAWN                  – sand dunes + heat shimmer + god rays
 *  FOREST                       – tall tree silhouettes + dappled light shafts + floating leaves
 *  OCEAN                        – layered waves + foam lines + horizon glow
 *  SNOW                         – white ground + falling snowflakes + bare tree silhouette
 *  LOTUS                        – still pond reflection + floating lotus + drifting petals
 *  CANDLELIGHT                  – dark vignette + single flame + warm halo rings
 *  CHERRY_BLOSSOM               – pale sky + branch silhouette + falling petals
 */
class LiveWallpaperRenderer {

    // ── Shared particle pool (seeded, stable) ─────────────────────────────────
    private val rnd = Random(42)
    private val pts = Array(80) {
        floatArrayOf(
            rnd.nextFloat(),                          // x  0..1
            rnd.nextFloat(),                          // y  0..1
            0.003f + rnd.nextFloat() * 0.018f,        // size
            0.002f + rnd.nextFloat() * 0.014f,        // speed
            rnd.nextFloat() * 6.28f,                  // phase
            (20 + rnd.nextInt(180)).toFloat(),         // alpha
            (rnd.nextFloat() - 0.5f) * 0.05f          // drift
        )
    }
    private val stars = Array(120) {
        floatArrayOf(rnd.nextFloat(), rnd.nextFloat() * 0.7f,
            0.4f + rnd.nextFloat() * 2.0f, rnd.nextFloat() * 6.28f)
    }

    // ── Paints ────────────────────────────────────────────────────────────────
    private val fill  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val scrim = Paint(Paint.ANTI_ALIAS_FLAG)
    private val body  = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    }
    private val meta  = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0.08f
    }
    private val brand = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER; letterSpacing = 0.12f
    }

    // ── Text layout cache ─────────────────────────────────────────────────────
    private var cacheKey = ""
    private var bodyLayout: StaticLayout? = null
    private var refLayout:  StaticLayout? = null

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun wa(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
    private fun rgb(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
    private fun lerpC(a: Int, b: Int, t: Float) = Color.rgb(
        (Color.red(a)   + (Color.red(b)   - Color.red(a))   * t.coerceIn(0f,1f)).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t.coerceIn(0f,1f)).toInt(),
        (Color.blue(a)  + (Color.blue(b)  - Color.blue(a))  * t.coerceIn(0f,1f)).toInt())

    private fun skyRect(canvas: Canvas, w: Int, h: Int, top: Int, mid: Int, bot: Int) {
        fill.shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
            intArrayOf(top, mid, bot), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        fill.alpha = 255
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fill)
        fill.shader = null
    }

    private fun hill(w: Int, h: Int, base: Float, amp: Float, freq: Float, phase: Float): Path =
        Path().apply {
            val ex = w * 0.3f; moveTo(-ex, h.toFloat()); var x = -ex
            while (x <= w + ex) {
                lineTo(x, h * base - h * amp * (
                    0.6f * sin(x / w * freq * PI.toFloat() + phase) +
                    0.4f * sin(x / w * freq * 2.1f * PI.toFloat() + phase * 1.8f)))
                x += w / 50f
            }
            lineTo(w + ex, h.toFloat()); close()
        }

    private fun prepareText(w: Int, h: Int, textColor: Int, accentColor: Int,
                             text: String, reference: String) {
        val key = "$w|$h|${text.hashCode()}|${reference.hashCode()}|$textColor"
        if (key == cacheKey) return; cacheKey = key
        val cw = (min(w, (h * 0.62f).toInt()) * 0.80f).toInt().coerceAtLeast(1)
        body.color = textColor
        body.setShadowLayer(w * 0.022f, 0f, w * 0.007f, wa(Color.BLACK, 200))
        meta.color = accentColor
        meta.setShadowLayer(w * 0.016f, 0f, w * 0.005f, wa(Color.BLACK, 170))
        var sz = cw * 0.068f; var bl: StaticLayout
        do {
            body.textSize = sz
            bl = StaticLayout.Builder.obtain("\u201c$text\u201d", 0, text.length + 2, body, cw)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 1.20f)
                .setIncludePad(false).build()
            sz *= 0.94f
        } while (bl.height > h * 0.28f && sz > cw * 0.028f)
        bodyLayout = bl
        meta.textSize = cw * 0.036f
        refLayout = StaticLayout.Builder.obtain(reference.uppercase(), 0, reference.length, meta, cw)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(2).setIncludePad(false).build()
    }

    private fun drawText(canvas: Canvas, w: Int, h: Int, fadeIn: Float) {
        val bl = bodyLayout ?: return; val rl = refLayout
        val gap = h * 0.018f; val top = h * 0.30f
        val blockH = bl.height + gap + (rl?.height ?: 0f).toFloat()
        val pad = h * 0.055f
        val alpha = (fadeIn * 255).toInt().coerceIn(0, 255)
        canvas.save()
        canvas.translate((w - bl.width) / 2f, top)
        canvas.saveLayerAlpha(0f, 0f, bl.width.toFloat(), (blockH + gap).coerceAtLeast(1f), alpha)
        bl.draw(canvas)
        if (rl != null) { canvas.translate(0f, bl.height + gap); rl.draw(canvas) }
        canvas.restore(); canvas.restore()
        brand.color = wa(Color.WHITE, (fadeIn * 150).toInt())
        brand.textSize = max(w, 1) * 0.026f
        canvas.drawText("WISDOMONE", w / 2f, top + blockH + pad + h * 0.022f, brand)
    }


    // ═══════════════════════════════════════════════════════════════════════════
    // THEME DRAW FUNCTIONS — each has a completely different visual structure
    // ═══════════════════════════════════════════════════════════════════════════

    /** MORNING / EVENING / GOLDEN_HOUR — horizon sun rising/setting over rolling hills */
    private fun drawSunrise(canvas: Canvas, w: Int, h: Int, t: Float, xOff: Float,
                             skyTop: Int, skyMid: Int, skyBot: Int,
                             sunColor: Int, hillFar: Int, hillMid: Int, hillNear: Int,
                             dustColor: Int, text: String, ref: String, fade: Float) {
        skyRect(canvas, w, h, skyTop, skyMid, skyBot)
        // Horizon glow band
        fill.shader = LinearGradient(0f, h * 0.55f, 0f, h * 0.80f,
            intArrayOf(wa(sunColor, 0), wa(sunColor, 120), wa(sunColor, 0)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, h * 0.55f, w.toFloat(), h * 0.80f, fill); fill.shader = null
        // Sun orb on horizon
        val breathe = 1f + 0.04f * sin(t * 0.45f)
        val sx = w * (0.68f - (xOff - 0.5f) * 0.06f); val sy = h * 0.72f
        val sr = w * 0.13f * breathe
        fill.shader = RadialGradient(sx, sy, sr * 3f,
            intArrayOf(wa(sunColor, 160), wa(sunColor, 50), wa(sunColor, 0)),
            null, Shader.TileMode.CLAMP)
        canvas.drawCircle(sx, sy, sr * 3f, fill); fill.shader = null
        fill.color = sunColor; fill.alpha = 230
        canvas.drawCircle(sx, sy, sr, fill)
        // God rays from sun
        for (i in 0..5) {
            val angle = (-0.6f + i * 0.24f + sin(t * 0.06f + i) * 0.05f) * PI.toFloat()
            val ex = sx + cos(angle) * h * 1.3f; val ey = sy + sin(angle) * h * 1.3f
            val rw = w * 0.14f
            val rp = Path().apply {
                moveTo(sx - rw * 0.06f, sy); lineTo(sx + rw * 0.06f, sy)
                lineTo(ex + rw * 0.45f, ey); lineTo(ex - rw * 0.45f, ey); close()
            }
            fill.shader = LinearGradient(sx, sy, ex, ey,
                intArrayOf(wa(sunColor, 35), wa(sunColor, 0)), null, Shader.TileMode.CLAMP)
            canvas.drawPath(rp, fill); fill.shader = null
        }
        // Rolling hills (3 layers, parallax)
        val hc = intArrayOf(hillFar, hillMid, hillNear)
        val hb = floatArrayOf(0.78f, 0.84f, 0.91f)
        val ha = floatArrayOf(0.055f, 0.045f, 0.038f)
        val hf = floatArrayOf(2.1f, 3.2f, 4.3f)
        val hp = floatArrayOf(0.4f, 2.1f, 4.2f)
        for (i in 0..2) {
            val shift = sin(t * 0.04f + i) * w * 0.016f - (xOff - 0.5f) * w * 0.12f * ((i+1)/3f)
            canvas.save(); canvas.translate(shift, 0f)
            fill.color = hc[i]; fill.alpha = 255
            canvas.drawPath(hill(w, h, hb[i], ha[i], hf[i], hp[i]), fill)
            canvas.restore()
        }
        // Dust motes drifting upward
        for (p in pts.take(20)) {
            val py = ((p[1] - t * p[3] * 0.22f) % 1f + 1f) % 1f
            val px = p[0] + 0.02f * sin(t * 0.28f + p[4])
            fill.color = dustColor
            fill.alpha = (p[5] * (0.5f + 0.5f * sin(t * 0.45f + p[4]))).toInt().coerceIn(0, 255)
            canvas.drawCircle(px * w, py * h, p[2] * w, fill)
        }
        prepareText(w, h, Color.WHITE, rgb(255, 226, 160), text, ref)
        drawText(canvas, w, h, fade)
    }

    /** AFTERNOON — bright blue sky, puffy cloud shapes, green meadow */
    private fun drawAfternoon(canvas: Canvas, w: Int, h: Int, t: Float, xOff: Float,
                               text: String, ref: String, fade: Float) {
        skyRect(canvas, w, h, rgb(80,170,230), rgb(160,210,240), rgb(110,185,145))
        // Sun high up
        val sx = w * 0.75f; val sy = h * 0.10f; val sr = w * 0.09f
        fill.shader = RadialGradient(sx, sy, sr * 4f,
            intArrayOf(wa(rgb(255,255,200), 140), wa(rgb(255,255,200), 0)),
            null, Shader.TileMode.CLAMP)
        canvas.drawCircle(sx, sy, sr * 4f, fill); fill.shader = null
        fill.color = rgb(255, 255, 220); fill.alpha = 240
        canvas.drawCircle(sx, sy, sr, fill)
        // Puffy clouds — each cloud = cluster of overlapping circles
        val cloudData = arrayOf(
            floatArrayOf(0.15f, 0.18f, 0.22f),
            floatArrayOf(0.55f, 0.12f, 0.18f),
            floatArrayOf(0.80f, 0.22f, 0.16f))
        for (cd in cloudData) {
            val cx = (cd[0] + sin(t * 0.025f + cd[2]) * 0.04f) * w
            val cy = cd[1] * h; val cr = cd[2] * w
            fill.color = rgb(255, 255, 255); fill.alpha = 210
            canvas.drawCircle(cx, cy, cr, fill)
            canvas.drawCircle(cx + cr * 0.7f, cy + cr * 0.15f, cr * 0.75f, fill)
            canvas.drawCircle(cx - cr * 0.65f, cy + cr * 0.2f, cr * 0.65f, fill)
            canvas.drawCircle(cx + cr * 0.2f, cy - cr * 0.35f, cr * 0.6f, fill)
            // Cloud shadow base
            fill.color = rgb(200, 220, 240); fill.alpha = 80
            canvas.drawOval(RectF(cx - cr * 1.4f, cy + cr * 0.5f, cx + cr * 1.4f, cy + cr * 0.9f), fill)
        }
        // Meadow hills
        val hc = intArrayOf(rgb(80,150,100), rgb(55,120,80), rgb(30,90,55))
        for (i in 0..2) {
            val shift = -(xOff - 0.5f) * w * 0.10f * ((i+1)/3f)
            canvas.save(); canvas.translate(shift, 0f)
            fill.color = hc[i]; fill.alpha = 255
            canvas.drawPath(hill(w, h, 0.80f + i * 0.06f, 0.04f - i * 0.005f, 2.5f + i, 1.0f + i * 1.5f), fill)
            canvas.restore()
        }
        // Floating light dust
        for (p in pts.take(15)) {
            val py = ((p[1] - t * p[3] * 0.18f) % 1f + 1f) % 1f
            fill.color = rgb(255, 255, 200)
            fill.alpha = (p[5] * 0.35f).toInt().coerceIn(0, 80)
            canvas.drawCircle(p[0] * w, py * h, p[2] * w * 0.7f, fill)
        }
        prepareText(w, h, rgb(15, 30, 15), rgb(40, 90, 50), text, ref)
        drawText(canvas, w, h, fade)
    }

    /** NIGHT / STARFIELD — deep space, dense twinkling stars, firefly orbs */
    private fun drawNight(canvas: Canvas, w: Int, h: Int, t: Float, xOff: Float,
                           skyTop: Int, skyBot: Int, starColor: Int, fireflyColor: Int,
                           hillFar: Int, hillMid: Int, hillNear: Int,
                           text: String, ref: String, fade: Float) {
        skyRect(canvas, w, h, skyTop, lerpC(skyTop, skyBot, 0.5f), skyBot)
        // Moon
        val mx = w * (0.72f - (xOff - 0.5f) * 0.05f); val my = h * 0.12f; val mr = w * 0.09f
        val breathe = 1f + 0.03f * sin(t * 0.3f)
        fill.shader = RadialGradient(mx, my, mr * 4f,
            intArrayOf(wa(starColor, 100), wa(starColor, 0)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(mx, my, mr * 4f, fill); fill.shader = null
        fill.color = rgb(240, 245, 255); fill.alpha = 230
        canvas.drawCircle(mx, my, mr * breathe, fill)
        // Stars — dense twinkling field
        for (s in stars) {
            val tw = (90 + 130 * sin(t * 0.85f + s[3])).toInt().coerceIn(10, 240)
            fill.color = starColor; fill.alpha = tw
            canvas.drawCircle(s[0] * w, s[1] * h, s[2] * w / 520f, fill)
        }
        // Milky way band — faint diagonal smear
        fill.shader = LinearGradient(0f, h * 0.05f, w.toFloat(), h * 0.55f,
            intArrayOf(wa(rgb(180,190,255), 0), wa(rgb(180,190,255), 28), wa(rgb(180,190,255), 0)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.6f, fill); fill.shader = null
        // Dark hills silhouette
        val hc = intArrayOf(hillFar, hillMid, hillNear)
        for (i in 0..2) {
            val shift = -(xOff - 0.5f) * w * 0.10f * ((i+1)/3f)
            canvas.save(); canvas.translate(shift, 0f)
            fill.color = hc[i]; fill.alpha = 255
            canvas.drawPath(hill(w, h, 0.82f + i * 0.05f, 0.04f, 2.8f + i * 0.8f, i * 1.6f), fill)
            canvas.restore()
        }
        // Fireflies
        for (p in pts.take(28)) {
            val glow = (0.35f + 0.65f * sin(t * 1.1f + p[4])).coerceIn(0f, 1f)
            val fx = (p[0] + sin(t * 0.16f + p[4]) * 0.04f + 1f) % 1f
            val fy = (p[1] + cos(t * 0.12f + p[4] * 1.4f) * 0.03f + 1f) % 1f
            fill.color = wa(fireflyColor, (p[5] * glow).toInt().coerceIn(0, 255))
            canvas.drawCircle(fx * w, fy * h, p[2] * w * 0.65f, fill)
            fill.color = wa(fireflyColor, (p[5] * glow * 0.22f).toInt().coerceIn(0, 255))
            canvas.drawCircle(fx * w, fy * h, p[2] * w * 2.4f, fill)
        }
        prepareText(w, h, rgb(235, 240, 255), rgb(200, 190, 250), text, ref)
        drawText(canvas, w, h, fade)
    }


    /** MONSOON — dark storm clouds, heavy diagonal rain, lightning flash */
    private fun drawMonsoon(canvas: Canvas, w: Int, h: Int, t: Float, xOff: Float,
                             text: String, ref: String, fade: Float) {
        skyRect(canvas, w, h, rgb(30,40,58), rgb(50,65,85), rgb(65,85,100))
        // Storm cloud masses — large dark ovals layered
        val clouds = arrayOf(
            floatArrayOf(0.25f, 0.15f, 0.55f, 0.22f),
            floatArrayOf(0.70f, 0.10f, 0.50f, 0.20f),
            floatArrayOf(0.50f, 0.25f, 0.65f, 0.18f))
        for (cd in clouds) {
            val cx = (cd[0] + sin(t * 0.018f + cd[3]) * 0.02f) * w
            val cy = cd[1] * h; val rw = cd[2] * w; val rh = cd[3] * h
            fill.color = rgb(35, 45, 62); fill.alpha = 220
            canvas.drawOval(RectF(cx - rw, cy - rh, cx + rw, cy + rh), fill)
            fill.color = rgb(25, 33, 48); fill.alpha = 160
            canvas.drawOval(RectF(cx - rw * 0.7f, cy - rh * 1.3f, cx + rw * 0.7f, cy + rh * 0.5f), fill)
        }
        // Lightning flash (occasional)
        val flashPhase = (t * 0.3f) % 1f
        if (flashPhase < 0.04f) {
            fill.color = rgb(220, 230, 255); fill.alpha = (flashPhase / 0.04f * 180).toInt()
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fill)
            // Lightning bolt path
            val lx = w * 0.6f
            val lp = Path().apply {
                moveTo(lx, h * 0.25f); lineTo(lx - w * 0.04f, h * 0.45f)
                lineTo(lx + w * 0.02f, h * 0.45f); lineTo(lx - w * 0.05f, h * 0.70f)
            }
            stroke.color = rgb(255, 255, 200); stroke.alpha = 220; stroke.strokeWidth = w * 0.006f
            canvas.drawPath(lp, stroke)
        }
        // Heavy rain streaks
        stroke.strokeWidth = w * 0.0028f
        for (p in pts) {
            val py = ((p[1] + t * p[3] * 2.2f) % 1f)
            val px = p[0] - (xOff - 0.5f) * 0.03f
            val len = w * p[2] * 2.8f
            stroke.color = wa(rgb(160, 195, 225), (p[5] * 0.65f).toInt().coerceIn(0, 255))
            canvas.drawLine(px * w, py * h, px * w - len * 0.25f, py * h + len, stroke)
        }
        // Dark ground
        fill.color = rgb(15, 22, 32); fill.alpha = 255
        canvas.drawPath(hill(w, h, 0.88f, 0.03f, 3.5f, 1.2f), fill)
        fill.color = rgb(10, 16, 24)
        canvas.drawPath(hill(w, h, 0.93f, 0.025f, 4.8f, 3.1f), fill)
        prepareText(w, h, rgb(220, 235, 255), rgb(150, 185, 220), text, ref)
        drawText(canvas, w, h, fade)
    }

    /** DESERT_DAWN — sand dunes, heat shimmer lines, warm god rays */
    private fun drawDesert(canvas: Canvas, w: Int, h: Int, t: Float, xOff: Float,
                            text: String, ref: String, fade: Float) {
        skyRect(canvas, w, h, rgb(255,175,75), rgb(235,125,55), rgb(185,85,45))
        // Sun on horizon
        val sx = w * (0.65f - (xOff - 0.5f) * 0.05f); val sy = h * 0.68f
        val sr = w * 0.14f
        fill.shader = RadialGradient(sx, sy, sr * 3.5f,
            intArrayOf(wa(rgb(255,220,140), 180), wa(rgb(255,180,80), 60), wa(rgb(255,140,40), 0)),
            null, Shader.TileMode.CLAMP)
        canvas.drawCircle(sx, sy, sr * 3.5f, fill); fill.shader = null
        fill.color = rgb(255, 240, 180); fill.alpha = 240
        canvas.drawCircle(sx, sy, sr, fill)
        // God rays
        for (i in 0..6) {
            val angle = (-0.7f + i * 0.23f + sin(t * 0.05f + i) * 0.04f) * PI.toFloat()
            val ex = sx + cos(angle) * h * 1.5f; val ey = sy + sin(angle) * h * 1.5f
            val rw = w * 0.12f
            val rp = Path().apply {
                moveTo(sx - rw * 0.05f, sy); lineTo(sx + rw * 0.05f, sy)
                lineTo(ex + rw * 0.5f, ey); lineTo(ex - rw * 0.5f, ey); close()
            }
            fill.shader = LinearGradient(sx, sy, ex, ey,
                intArrayOf(wa(rgb(255,210,100), 42), wa(rgb(255,210,100), 0)),
                null, Shader.TileMode.CLAMP)
            canvas.drawPath(rp, fill); fill.shader = null
        }
        // Sand dunes — smooth, wide, low amplitude
        val duneColors = intArrayOf(rgb(200,140,70), rgb(175,110,50), rgb(145,85,35))
        val duneBase = floatArrayOf(0.72f, 0.80f, 0.88f)
        for (i in 0..2) {
            val shift = sin(t * 0.03f + i * 1.2f) * w * 0.01f - (xOff - 0.5f) * w * 0.08f * ((i+1)/3f)
            canvas.save(); canvas.translate(shift, 0f)
            fill.color = duneColors[i]; fill.alpha = 255
            canvas.drawPath(hill(w, h, duneBase[i], 0.06f - i * 0.01f, 1.5f + i * 0.5f, i * 2.1f), fill)
            canvas.restore()
        }
        // Heat shimmer — faint horizontal wavy lines near horizon
        stroke.strokeWidth = w * 0.002f
        for (i in 0..8) {
            val shimY = h * (0.65f + i * 0.018f)
            val path = Path(); path.moveTo(0f, shimY); var sx2 = 0f
            while (sx2 <= w) {
                path.lineTo(sx2, shimY + sin(sx2 / w * 8f * PI.toFloat() + t * 1.5f + i) * h * 0.004f)
                sx2 += w / 40f
            }
            stroke.color = wa(rgb(255, 200, 120), (18 - i * 1).coerceAtLeast(4))
            canvas.drawPath(path, stroke)
        }
        // Dust motes
        for (p in pts.take(18)) {
            val py = ((p[1] - t * p[3] * 0.15f) % 1f + 1f) % 1f
            fill.color = rgb(255, 210, 140)
            fill.alpha = (p[5] * 0.4f).toInt().coerceIn(0, 100)
            canvas.drawCircle(p[0] * w, py * h, p[2] * w * 0.8f, fill)
        }
        prepareText(w, h, Color.WHITE, rgb(255, 215, 130), text, ref)
        drawText(canvas, w, h, fade)
    }

    /** FOREST — tall tree silhouettes, dappled light shafts, floating leaves */
    private fun drawForest(canvas: Canvas, w: Int, h: Int, t: Float, xOff: Float,
                            text: String, ref: String, fade: Float) {
        skyRect(canvas, w, h, rgb(15,50,25), rgb(30,75,40), rgb(50,100,55))
        // Canopy light shafts from top
        for (i in 0..5) {
            val lx = w * (0.1f + i * 0.16f + sin(t * 0.04f + i) * 0.02f)
            val path = Path().apply {
                moveTo(lx - w * 0.04f, 0f); lineTo(lx + w * 0.04f, 0f)
                lineTo(lx + w * 0.12f, h.toFloat()); lineTo(lx - w * 0.12f, h.toFloat()); close()
            }
            fill.shader = LinearGradient(lx, 0f, lx, h.toFloat(),
                intArrayOf(wa(rgb(180,240,150), 55), wa(rgb(180,240,150), 0)),
                null, Shader.TileMode.CLAMP)
            canvas.drawPath(path, fill); fill.shader = null
        }
        // Ground layer
        fill.color = rgb(8, 35, 15); fill.alpha = 255
        canvas.drawRect(0f, h * 0.85f, w.toFloat(), h.toFloat(), fill)
        // Tree silhouettes — tall thin trunks + triangular canopy
        val treeX = floatArrayOf(0.05f, 0.18f, 0.32f, 0.48f, 0.62f, 0.75f, 0.88f, 0.96f)
        val treeH = floatArrayOf(0.70f, 0.55f, 0.65f, 0.72f, 0.58f, 0.68f, 0.60f, 0.50f)
        val treeW = floatArrayOf(0.08f, 0.06f, 0.07f, 0.09f, 0.06f, 0.08f, 0.07f, 0.05f)
        for (i in treeX.indices) {
            val tx = treeX[i] * w - (xOff - 0.5f) * w * 0.08f * (if (i % 2 == 0) 1f else -1f)
            val th = treeH[i]; val tw = treeW[i]
            fill.color = rgb(8, 30, 12); fill.alpha = 255
            // Trunk
            canvas.drawRect(tx - w * 0.008f, h * 0.85f, tx + w * 0.008f, h * (th + 0.05f), fill)
            // Canopy layers (3 triangles stacked)
            for (layer in 0..2) {
                val ly = h * (th - layer * 0.12f)
                val lw = tw * w * (1.2f - layer * 0.25f)
                val tp = Path().apply {
                    moveTo(tx, ly - h * 0.18f); lineTo(tx - lw, ly + h * 0.04f)
                    lineTo(tx + lw, ly + h * 0.04f); close()
                }
                fill.color = rgb(10 + layer * 5, 38 + layer * 8, 15 + layer * 5)
                canvas.drawPath(tp, fill)
            }
        }
        // Floating leaves
        for (p in pts.take(35)) {
            val py = ((p[1] + t * p[3] * 0.55f) % 1f)
            val px = (p[0] + sin(t * 0.3f + p[4]) * 0.025f + p[6] * t * 0.08f + 1f) % 1f
            val sz = p[2] * w * 0.85f
            fill.color = wa(rgb(80, 180, 60), (p[5] * 0.85f).toInt().coerceIn(0, 255))
            canvas.save(); canvas.translate(px * w, py * h)
            canvas.rotate(p[4] * 57f + t * p[6] * 40f)
            canvas.drawOval(RectF(-sz, -sz * 0.4f, sz, sz * 0.4f), fill)
            canvas.restore()
        }
        prepareText(w, h, rgb(220, 255, 210), rgb(130, 210, 110), text, ref)
        drawText(canvas, w, h, fade)
    }


    /** OCEAN — layered animated waves, foam crests, horizon glow, sea spray */
    private fun drawOcean(canvas: Canvas, w: Int, h: Int, t: Float, xOff: Float,
                           text: String, ref: String, fade: Float) {
        // Sky
        skyRect(canvas, w, h, rgb(8,55,115), rgb(15,95,155), rgb(20,130,175))
        // Horizon glow
        fill.shader = LinearGradient(0f, h * 0.42f, 0f, h * 0.58f,
            intArrayOf(wa(rgb(140,215,255), 0), wa(rgb(140,215,255), 90), wa(rgb(140,215,255), 0)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, h * 0.42f, w.toFloat(), h * 0.58f, fill); fill.shader = null
        // Moon/sun reflection streak on water
        val refX = w * 0.5f
        fill.shader = LinearGradient(refX - w * 0.06f, 0f, refX + w * 0.06f, 0f,
            intArrayOf(wa(rgb(180,235,255), 0), wa(rgb(180,235,255), 120), wa(rgb(180,235,255), 0)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(refX - w * 0.06f, h * 0.52f, refX + w * 0.06f, h.toFloat(), fill)
        fill.shader = null
        // Deep ocean base
        fill.color = rgb(5, 40, 90); fill.alpha = 255
        canvas.drawRect(0f, h * 0.52f, w.toFloat(), h.toFloat(), fill)
        // Wave layers — 6 layers from far (small) to near (large)
        val waveColors = intArrayOf(
            rgb(15,80,140), rgb(12,70,130), rgb(10,60,120),
            rgb(8,50,110), rgb(6,42,100), rgb(5,35,90))
        val waveBases = floatArrayOf(0.55f, 0.62f, 0.68f, 0.74f, 0.80f, 0.87f)
        val waveAmps  = floatArrayOf(0.012f, 0.016f, 0.020f, 0.025f, 0.030f, 0.038f)
        val waveFreqs = floatArrayOf(4.0f, 3.5f, 3.0f, 2.5f, 2.2f, 1.8f)
        val waveSpeeds = floatArrayOf(0.6f, 0.75f, 0.9f, 1.1f, 1.3f, 1.6f)
        for (i in 0..5) {
            val shift = -(xOff - 0.5f) * w * 0.06f * ((i+1)/6f)
            val path = Path()
            path.moveTo(-shift, h.toFloat())
            var wx = -shift - w * 0.1f
            while (wx <= w + w * 0.1f) {
                val wy = h * waveBases[i] + h * waveAmps[i] *
                    sin(wx / w * waveFreqs[i] * PI.toFloat() + t * waveSpeeds[i])
                path.lineTo(wx, wy); wx += w / 55f
            }
            path.lineTo(w + w * 0.1f - shift, h.toFloat()); path.close()
            fill.color = waveColors[i]; fill.alpha = 255
            canvas.drawPath(path, fill)
            // Foam crest on top of each wave
            if (i >= 3) {
                stroke.color = wa(Color.WHITE, 55 - (i - 3) * 10)
                stroke.strokeWidth = w * 0.003f
                val fp = Path(); fp.moveTo(-shift, h * waveBases[i])
                var fx = -shift - w * 0.1f
                while (fx <= w + w * 0.1f) {
                    fp.lineTo(fx, h * waveBases[i] + h * waveAmps[i] *
                        sin(fx / w * waveFreqs[i] * PI.toFloat() + t * waveSpeeds[i]))
                    fx += w / 55f
                }
                canvas.drawPath(fp, stroke)
            }
        }
        // Sea spray particles
        for (p in pts.take(22)) {
            val py = ((p[1] - t * p[3] * 0.35f + 1f) % 1f) * 0.5f + 0.5f
            val px = (p[0] + sin(t * 0.25f + p[4]) * 0.015f + 1f) % 1f
            fill.color = wa(rgb(180, 235, 255), (p[5] * 0.5f).toInt().coerceIn(0, 130))
            canvas.drawCircle(px * w, py * h, p[2] * w * 0.5f, fill)
        }
        prepareText(w, h, Color.WHITE, rgb(140, 220, 255), text, ref)
        drawText(canvas, w, h, fade)
    }

    /** SNOW — white ground, bare tree silhouette, falling snowflakes */
    private fun drawSnow(canvas: Canvas, w: Int, h: Int, t: Float, xOff: Float,
                          text: String, ref: String, fade: Float) {
        skyRect(canvas, w, h, rgb(165,190,225), rgb(200,218,242), rgb(225,238,255))
        // Distant snow hills
        val hc = intArrayOf(rgb(195,215,240), rgb(215,228,248), rgb(235,242,255))
        for (i in 0..2) {
            val shift = -(xOff - 0.5f) * w * 0.07f * ((i+1)/3f)
            canvas.save(); canvas.translate(shift, 0f)
            fill.color = hc[i]; fill.alpha = 255
            canvas.drawPath(hill(w, h, 0.72f + i * 0.08f, 0.06f - i * 0.01f, 2.0f + i, i * 1.8f), fill)
            canvas.restore()
        }
        // Snow ground
        fill.color = rgb(240, 248, 255); fill.alpha = 255
        canvas.drawRect(0f, h * 0.88f, w.toFloat(), h.toFloat(), fill)
        // Bare tree silhouette (centre-left)
        val tx = w * (0.28f - (xOff - 0.5f) * 0.05f)
        fill.color = rgb(60, 75, 100); fill.alpha = 200
        // Trunk
        canvas.drawRect(tx - w * 0.012f, h * 0.45f, tx + w * 0.012f, h * 0.88f, fill)
        // Branches — recursive-ish pairs
        val branches = arrayOf(
            floatArrayOf(tx, h * 0.55f, -0.18f, 0.14f),
            floatArrayOf(tx, h * 0.55f,  0.18f, 0.14f),
            floatArrayOf(tx, h * 0.65f, -0.14f, 0.11f),
            floatArrayOf(tx, h * 0.65f,  0.14f, 0.11f),
            floatArrayOf(tx, h * 0.73f, -0.10f, 0.09f),
            floatArrayOf(tx, h * 0.73f,  0.10f, 0.09f))
        for (b in branches) {
            stroke.color = rgb(60, 75, 100); stroke.alpha = 180
            stroke.strokeWidth = w * 0.006f
            canvas.drawLine(b[0], b[1], b[0] + b[2] * w, b[1] - b[3] * h, stroke)
        }
        // Snowflakes
        for (p in pts) {
            val py = ((p[1] + t * p[3] * 0.38f) % 1f)
            val px = (p[0] + sin(t * 0.38f + p[4]) * 0.022f + (xOff - 0.5f) * 0.02f + 1f) % 1f
            fill.color = wa(Color.WHITE, (p[5] * 0.88f).toInt().coerceIn(0, 255))
            canvas.drawCircle(px * w, py * h, p[2] * w * 0.55f, fill)
        }
        prepareText(w, h, rgb(20, 35, 65), rgb(90, 120, 175), text, ref)
        drawText(canvas, w, h, fade)
    }

    /** LOTUS POND — still water reflection, floating lotus flowers, drifting petals */
    private fun drawLotus(canvas: Canvas, w: Int, h: Int, t: Float, xOff: Float,
                           text: String, ref: String, fade: Float) {
        skyRect(canvas, w, h, rgb(175,95,155), rgb(205,135,175), rgb(225,175,205))
        // Pond (lower half) — dark reflective water
        fill.color = rgb(55, 85, 70); fill.alpha = 255
        canvas.drawRect(0f, h * 0.58f, w.toFloat(), h.toFloat(), fill)
        // Sky reflection in water (mirrored gradient)
        fill.shader = LinearGradient(0f, h * 0.58f, 0f, h.toFloat(),
            intArrayOf(wa(rgb(205,135,175), 80), wa(rgb(55,85,70), 0)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, h * 0.58f, w.toFloat(), h.toFloat(), fill); fill.shader = null
        // Water ripple lines
        stroke.strokeWidth = w * 0.0022f
        for (i in 0..7) {
            val wy = h * (0.62f + i * 0.048f)
            val path = Path(); path.moveTo(0f, wy); var wx = 0f
            while (wx <= w) {
                path.lineTo(wx, wy + sin(wx / w * 5f * PI.toFloat() + t * 0.7f + i * 0.5f) * h * 0.007f)
                wx += w / 55f
            }
            stroke.color = wa(rgb(120, 180, 145), (28 - i * 2).coerceAtLeast(6))
            canvas.drawPath(path, stroke)
        }
        // Lotus flowers — 3 flowers at water surface
        val lotusX = floatArrayOf(0.22f, 0.55f, 0.80f)
        val lotusY = floatArrayOf(0.72f, 0.68f, 0.75f)
        val lotusR = floatArrayOf(0.055f, 0.065f, 0.048f)
        for (i in lotusX.indices) {
            val lx = lotusX[i] * w; val ly = lotusY[i] * h; val lr = lotusR[i] * w
            val bob = sin(t * 0.4f + i * 1.8f) * h * 0.005f
            // Lily pad
            fill.color = rgb(45, 110, 65); fill.alpha = 200
            canvas.drawOval(RectF(lx - lr * 1.3f, ly + bob - lr * 0.4f, lx + lr * 1.3f, ly + bob + lr * 0.4f), fill)
            // Petals (8 petals around centre)
            for (p in 0..7) {
                val angle = p * PI.toFloat() / 4f + t * 0.08f
                val px = lx + cos(angle) * lr * 0.7f; val py = ly + bob + sin(angle) * lr * 0.5f
                fill.color = wa(rgb(240, 160, 200), 200)
                canvas.drawOval(RectF(px - lr * 0.35f, py - lr * 0.55f, px + lr * 0.35f, py + lr * 0.55f), fill)
            }
            // Centre
            fill.color = rgb(255, 220, 100); fill.alpha = 230
            canvas.drawCircle(lx, ly + bob, lr * 0.28f, fill)
        }
        // Drifting petals
        for (p in pts.take(30)) {
            val py = ((p[1] + t * p[3] * 0.45f) % 1f)
            val px = (p[0] + sin(t * 0.32f + p[4]) * 0.028f + p[6] * t * 0.06f + 1f) % 1f
            val sz = p[2] * w * 0.8f
            fill.color = wa(rgb(240, 160, 200), (p[5] * 0.85f).toInt().coerceIn(0, 255))
            canvas.save(); canvas.translate(px * w, py * h)
            canvas.rotate(p[4] * 57f + t * p[6] * 35f)
            canvas.drawOval(RectF(-sz, -sz * 0.42f, sz, sz * 0.42f), fill)
            canvas.restore()
        }
        prepareText(w, h, rgb(55, 15, 38), rgb(195, 90, 145), text, ref)
        drawText(canvas, w, h, fade)
    }


    /** CANDLELIGHT — near-black vignette, single animated flame, warm halo rings */
    private fun drawCandlelight(canvas: Canvas, w: Int, h: Int, t: Float,
                                 text: String, ref: String, fade: Float) {
        // Dark background
        fill.color = rgb(18, 8, 2); fill.alpha = 255
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fill)
        // Warm radial glow from candle position
        val cx = w * 0.5f; val cy = h * 0.78f
        val flicker = 1f + 0.06f * sin(t * 7.8f) + 0.03f * sin(t * 13.2f)
        fill.shader = RadialGradient(cx, cy, w * 0.7f * flicker,
            intArrayOf(wa(rgb(255,160,50), 180), wa(rgb(200,80,20), 80),
                       wa(rgb(100,30,5), 30), wa(rgb(18,8,2), 0)),
            floatArrayOf(0f, 0.25f, 0.55f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), fill); fill.shader = null
        // Halo rings (concentric, pulsing)
        for (i in 1..4) {
            val hr = w * 0.12f * i * flicker
            stroke.color = wa(rgb(255, 140, 40), (50 - i * 10).coerceAtLeast(5))
            stroke.strokeWidth = w * 0.004f
            canvas.drawCircle(cx, cy, hr, stroke)
        }
        // Candle body
        fill.color = rgb(240, 220, 180); fill.alpha = 220
        canvas.drawRoundRect(RectF(cx - w * 0.025f, cy, cx + w * 0.025f, cy + h * 0.08f),
            w * 0.01f, w * 0.01f, fill)
        // Flame — teardrop shape
        val fh = h * 0.06f * flicker; val fw = w * 0.022f
        val flamePath = Path().apply {
            moveTo(cx, cy - fh)
            cubicTo(cx + fw * 1.5f, cy - fh * 0.5f, cx + fw, cy, cx, cy)
            cubicTo(cx - fw, cy, cx - fw * 1.5f, cy - fh * 0.5f, cx, cy - fh)
        }
        fill.shader = LinearGradient(cx, cy - fh, cx, cy,
            intArrayOf(rgb(255,255,180), rgb(255,180,40), rgb(255,80,0)),
            null, Shader.TileMode.CLAMP)
        canvas.drawPath(flamePath, fill); fill.shader = null
        // Flame inner bright core
        fill.color = rgb(255, 255, 220); fill.alpha = 200
        canvas.drawOval(RectF(cx - fw * 0.4f, cy - fh * 0.7f, cx + fw * 0.4f, cy - fh * 0.15f), fill)
        // Floating ember particles
        for (p in pts.take(20)) {
            val py = ((p[1] - t * p[3] * 0.5f + 1f) % 1f)
            val px = (cx / w + sin(t * 0.4f + p[4]) * 0.04f + p[6] * t * 0.05f + 1f) % 1f
            val emberAlpha = (p[5] * (1f - py) * 0.7f).toInt().coerceIn(0, 200)
            fill.color = wa(rgb(255, 140, 30), emberAlpha)
            canvas.drawCircle(px * w, py * h, p[2] * w * 0.4f, fill)
        }
        prepareText(w, h, rgb(255, 230, 175), rgb(255, 185, 95), text, ref)
        drawText(canvas, w, h, fade)
    }

    /** CHERRY BLOSSOM — pale pink sky, dark branch silhouette, falling petals */
    private fun drawCherryBlossom(canvas: Canvas, w: Int, h: Int, t: Float, xOff: Float,
                                   text: String, ref: String, fade: Float) {
        skyRect(canvas, w, h, rgb(255,218,232), rgb(255,200,220), rgb(242,178,202))
        // Soft sun glow top-right
        val sx = w * 0.78f; val sy = h * 0.08f
        fill.shader = RadialGradient(sx, sy, w * 0.45f,
            intArrayOf(wa(rgb(255,240,248), 160), wa(rgb(255,220,235), 0)),
            null, Shader.TileMode.CLAMP)
        canvas.drawCircle(sx, sy, w * 0.45f, fill); fill.shader = null
        // Branch silhouette — main trunk + spreading branches
        val bx = w * (0.15f - (xOff - 0.5f) * 0.04f)
        stroke.color = rgb(80, 30, 50); stroke.alpha = 200
        // Main trunk
        stroke.strokeWidth = w * 0.022f
        canvas.drawLine(bx, h.toFloat(), bx + w * 0.08f, h * 0.45f, stroke)
        // Primary branches
        val branches = arrayOf(
            floatArrayOf(bx + w*0.08f, h*0.45f,  0.28f, -0.22f, 0.016f),
            floatArrayOf(bx + w*0.08f, h*0.45f,  0.12f, -0.28f, 0.014f),
            floatArrayOf(bx + w*0.08f, h*0.45f, -0.05f, -0.25f, 0.012f),
            floatArrayOf(bx + w*0.06f, h*0.55f,  0.22f, -0.18f, 0.010f),
            floatArrayOf(bx + w*0.04f, h*0.62f, -0.08f, -0.20f, 0.009f))
        for (b in branches) {
            stroke.strokeWidth = w * b[4]
            canvas.drawLine(b[0], b[1], b[0] + b[2] * w, b[1] + b[3] * h, stroke)
            // Sub-branches
            stroke.strokeWidth = w * b[4] * 0.55f
            canvas.drawLine(b[0] + b[2]*w*0.6f, b[1]+b[3]*h*0.6f,
                b[0]+b[2]*w*0.6f + b[3]*w*0.12f, b[1]+b[3]*h*0.6f + b[2]*h*0.08f, stroke)
            canvas.drawLine(b[0]+b[2]*w*0.6f, b[1]+b[3]*h*0.6f,
                b[0]+b[2]*w*0.6f - b[3]*w*0.10f, b[1]+b[3]*h*0.6f + b[2]*h*0.06f, stroke)
        }
        // Blossom clusters on branch tips
        fill.color = wa(rgb(255, 200, 220), 180)
        val blossomPts = arrayOf(
            floatArrayOf(bx + w*0.36f, h*0.23f), floatArrayOf(bx + w*0.20f, h*0.17f),
            floatArrayOf(bx + w*0.03f, h*0.20f), floatArrayOf(bx + w*0.28f, h*0.37f),
            floatArrayOf(bx - w*0.04f, h*0.42f))
        for (bp in blossomPts) {
            for (k in 0..4) {
                val ba = k * PI.toFloat() * 0.4f + t * 0.1f
                canvas.drawCircle(bp[0] + cos(ba) * w * 0.025f,
                    bp[1] + sin(ba) * w * 0.025f, w * 0.018f, fill)
            }
            fill.color = rgb(255, 220, 100); fill.alpha = 200
            canvas.drawCircle(bp[0], bp[1], w * 0.008f, fill)
            fill.color = wa(rgb(255, 200, 220), 180)
        }
        // Soft ground
        fill.color = rgb(220, 160, 185); fill.alpha = 255
        canvas.drawPath(hill(w, h, 0.90f, 0.03f, 3.0f, 1.5f), fill)
        fill.color = rgb(200, 140, 165)
        canvas.drawPath(hill(w, h, 0.95f, 0.02f, 4.5f, 3.2f), fill)
        // Falling petals
        for (p in pts) {
            val py = ((p[1] + t * p[3] * 0.48f) % 1f)
            val px = (p[0] + sin(t * 0.33f + p[4]) * 0.030f + p[6] * t * 0.07f + 1f) % 1f
            val sz = p[2] * w * 0.85f
            fill.color = wa(rgb(255, 200, 220), (p[5] * 0.88f).toInt().coerceIn(0, 255))
            canvas.save(); canvas.translate(px * w, py * h)
            canvas.rotate(p[4] * 57f + t * p[6] * 38f)
            canvas.drawOval(RectF(-sz, -sz * 0.42f, sz, sz * 0.42f), fill)
            canvas.restore()
        }
        prepareText(w, h, rgb(75, 18, 38), rgb(215, 95, 135), text, ref)
        drawText(canvas, w, h, fade)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════════

    fun draw(canvas: Canvas, w: Int, h: Int, timeMs: Long, theme: AmbientWisdom.LiveTheme,
             text: String, reference: String, xOffset: Float = 0.5f) {
        if (w <= 0 || h <= 0) return
        val t = timeMs / 1000f
        val fade = (timeMs / 2000f).coerceIn(0f, 1f)
        when (AmbientWisdom.resolveTheme(theme, java.time.LocalTime.now().hour)) {
            AmbientWisdom.LiveTheme.MORNING ->
                drawSunrise(canvas, w, h, t, xOffset,
                    rgb(255,214,153), rgb(246,163,120), rgb(132,92,140),
                    rgb(255,228,160), rgb(176,110,128), rgb(120,72,112), rgb(72,42,82),
                    rgb(255,240,200), text, reference, fade)
            AmbientWisdom.LiveTheme.AFTERNOON ->
                drawAfternoon(canvas, w, h, t, xOffset, text, reference, fade)
            AmbientWisdom.LiveTheme.EVENING ->
                drawSunrise(canvas, w, h, t, xOffset,
                    rgb(58,40,98), rgb(214,108,96), rgb(250,178,104),
                    rgb(255,196,120), rgb(140,70,92), rgb(92,44,78), rgb(46,22,52),
                    rgb(255,214,170), text, reference, fade)
            AmbientWisdom.LiveTheme.NIGHT ->
                drawNight(canvas, w, h, t, xOffset,
                    rgb(8,14,40), rgb(40,58,108), rgb(214,224,255), rgb(203,190,247),
                    rgb(34,50,92), rgb(22,34,70), rgb(12,20,46), text, reference, fade)
            AmbientWisdom.LiveTheme.GOLDEN_HOUR ->
                drawSunrise(canvas, w, h, t, xOffset,
                    rgb(255,140,40), rgb(255,100,60), rgb(180,60,80),
                    rgb(255,220,100), rgb(160,60,70), rgb(110,40,60), rgb(60,20,40),
                    rgb(255,240,180), text, reference, fade)
            AmbientWisdom.LiveTheme.MONSOON ->
                drawMonsoon(canvas, w, h, t, xOffset, text, reference, fade)
            AmbientWisdom.LiveTheme.DESERT_DAWN ->
                drawDesert(canvas, w, h, t, xOffset, text, reference, fade)
            AmbientWisdom.LiveTheme.FOREST ->
                drawForest(canvas, w, h, t, xOffset, text, reference, fade)
            AmbientWisdom.LiveTheme.OCEAN ->
                drawOcean(canvas, w, h, t, xOffset, text, reference, fade)
            AmbientWisdom.LiveTheme.SNOW ->
                drawSnow(canvas, w, h, t, xOffset, text, reference, fade)
            AmbientWisdom.LiveTheme.LOTUS ->
                drawLotus(canvas, w, h, t, xOffset, text, reference, fade)
            AmbientWisdom.LiveTheme.STARFIELD ->
                drawNight(canvas, w, h, t, xOffset,
                    rgb(2,4,20), rgb(10,18,55), rgb(200,210,255), rgb(180,190,255),
                    rgb(15,25,60), rgb(10,18,45), rgb(5,10,30), text, reference, fade)
            AmbientWisdom.LiveTheme.CANDLELIGHT ->
                drawCandlelight(canvas, w, h, t, text, reference, fade)
            AmbientWisdom.LiveTheme.CHERRY_BLOSSOM ->
                drawCherryBlossom(canvas, w, h, t, xOffset, text, reference, fade)
            else ->
                drawNight(canvas, w, h, t, xOffset,
                    rgb(8,14,40), rgb(40,58,108), rgb(214,224,255), rgb(203,190,247),
                    rgb(34,50,92), rgb(22,34,70), rgb(12,20,46), text, reference, fade)
        }
    }
}
