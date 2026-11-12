package com.scripturedaily.features.share

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Vector illustrations drawn behind the verse. They are symbolic (lotus, cross, geometry, mountains …), drawn
 * with the template's accent colour at low opacity so the verse text always stays readable. No licensed images.
 */
object Motifs {
    private fun stroke(accent: Int, w: Int, alpha: Int, width: Float = 0.003f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = accent; this.alpha = alpha; strokeWidth = w * width; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    private fun fill(accent: Int, alpha: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = accent; this.alpha = alpha }

    /** Draws [motif] for a card of w×h. Unknown motifs draw nothing (a plain card). */
    fun draw(canvas: Canvas, motif: String, accent: Int, w: Int, h: Int) {
        when (motif) {
            "om"             -> om(canvas, accent, w, h)
            "lotus"          -> lotus(canvas, accent, w, h)
            "diya"           -> diya(canvas, accent, w, h)
            "peacock"        -> peacock(canvas, accent, w, h)
            "trident"        -> trident(canvas, accent, w, h)
            "rangoli"        -> rangoli(canvas, accent, w, h)
            "chakra"         -> chakra(canvas, accent, w, h)
            "kalash"         -> kalash(canvas, accent, w, h)
            "yantra"         -> yantra(canvas, accent, w, h)
            "templeArch"     -> templeArch(canvas, accent, w, h)
            "cross"          -> cross(canvas, accent, w, h)
            "dove"           -> dove(canvas, accent, w, h)
            "olive"          -> olive(canvas, accent, w, h)
            "stained"        -> stained(canvas, accent, w, h)
            "ichthys"        -> ichthys(canvas, accent, w, h)
            "candle"         -> candle(canvas, accent, w, h)
            "wheat"          -> wheat(canvas, accent, w, h)
            "anchor"         -> anchor(canvas, accent, w, h)
            "vine"           -> vine(canvas, accent, w, h)
            "geometry"       -> geometry(canvas, accent, w, h)
            "crescent"       -> crescent(canvas, accent, w, h)
            "mosque"         -> mosque(canvas, accent, w, h)
            "arabesque"      -> arabesque(canvas, accent, w, h)
            "lantern"        -> lantern(canvas, accent, w, h)
            "muqarnas"       -> muqarnas(canvas, accent, w, h)
            "starPolygon"    -> starPolygon(canvas, accent, w, h)
            "prayerBeads"    -> prayerBeads(canvas, accent, w, h)
            "calligraphyArch"-> calligraphyArch(canvas, accent, w, h)
            "dharma"         -> dharma(canvas, accent, w, h)
            "bodhi"          -> bodhi(canvas, accent, w, h)
            "halo"           -> halo(canvas, accent, w, h)
            "endlessKnot"    -> endlessKnot(canvas, accent, w, h)
            "zenCircle"      -> zenCircle(canvas, accent, w, h)
            "prayerFlags"    -> prayerFlags(canvas, accent, w, h)
            "stupa"          -> stupa(canvas, accent, w, h)
            "koi"            -> koi(canvas, accent, w, h)
            "mountains"      -> mountains(canvas, accent, w, h)
            "sunrise"        -> sunrise(canvas, accent, w, h)
            "leaves"         -> leaves(canvas, accent, w, h)
            "treeOfLife"     -> treeOfLife(canvas, accent, w, h)
            "galaxy"         -> galaxy(canvas, accent, w, h)
            "feather"        -> feather(canvas, accent, w, h)
            "spiral"         -> spiral(canvas, accent, w, h)
            "aurora"         -> aurora(canvas, accent, w, h)
        }
    }

    // ── Hindu ───────────────────────────────────────────────────────────────
    private fun om(c: Canvas, accent: Int, w: Int, h: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; alpha = 34; typeface = Typeface.DEFAULT_BOLD; textSize = w * 0.95f; textAlign = Paint.Align.CENTER }
        c.drawText("ॐ", w / 2f, h * 0.5f + w * 0.33f, p)
        val ring = stroke(accent, w, 40)
        for (i in 1..3) c.drawCircle(w / 2f, h * 0.5f, w * (0.34f + 0.09f * i), ring)
    }

    private fun petal(r: Float) = Path().apply {
        moveTo(0f, 0f); quadTo(r * 0.55f, -r * 0.45f, 0f, -r); quadTo(-r * 0.55f, -r * 0.45f, 0f, 0f); close()
    }

    private fun lotus(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f + w * 0.12f
        val line = stroke(accent, w, 70); val body = fill(accent, 22)
        for ((r, count, alpha) in listOf(Triple(w * 0.46f, 9, 18), Triple(w * 0.36f, 7, 24), Triple(w * 0.25f, 5, 32))) {
            val path = petal(r)
            for (i in 0 until count) {
                val a = -90f + (i - (count - 1) / 2f) * (150f / max(1, count - 1)) * (if (count > 5) 0.9f else 1f)
                c.save(); c.translate(cx, cy); c.rotate(a + 90f); c.rotate(-90f + 90f)
                body.alpha = alpha; c.drawPath(path, body); c.drawPath(path, line); c.restore()
            }
        }
        c.drawCircle(cx, cy - w * 0.02f, w * 0.03f, fill(accent, 60))
    }

    private fun diya(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val by = h * 0.58f; val bw = w * 0.5f
        val bowl = Path().apply {
            moveTo(cx - bw / 2, by); quadTo(cx - bw * 0.42f, by + bw * 0.34f, cx, by + bw * 0.34f)
            quadTo(cx + bw * 0.42f, by + bw * 0.34f, cx + bw / 2, by); quadTo(cx, by + bw * 0.1f, cx - bw / 2, by); close()
        }
        c.drawPath(bowl, fill(accent, 40)); c.drawPath(bowl, stroke(accent, w, 80))
        val flame = Path().apply {
            moveTo(cx, by - bw * 0.62f); quadTo(cx + bw * 0.2f, by - bw * 0.3f, cx, by - bw * 0.06f); quadTo(cx - bw * 0.2f, by - bw * 0.3f, cx, by - bw * 0.62f); close()
        }
        c.drawPath(flame, fill(accent, 70)); c.drawPath(flame, stroke(accent, w, 110))
        for (i in 1..4) c.drawCircle(cx, by - bw * 0.3f, bw * 0.3f * i, stroke(accent, w, 36 - i * 6))
    }

    private fun peacock(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.46f
        c.save(); c.rotate(-14f, cx, cy)
        val stem = Path().apply { moveTo(cx, cy + w * 0.62f); quadTo(cx - w * 0.06f, cy + w * 0.2f, cx, cy - w * 0.2f) }
        c.drawPath(stem, stroke(accent, w, 80, 0.004f))
        val barb = stroke(accent, w, 38, 0.0022f)
        for (i in 0..38) {
            val t = i / 38f; val y = cy + w * 0.55f - t * w * 0.85f
            val spread = w * (0.06f + 0.2f * sin(PI * min(1f, t * 1.1f)).toFloat())
            c.drawLine(cx, y, cx + spread, y - w * 0.07f, barb); c.drawLine(cx, y, cx - spread, y - w * 0.07f, barb)
        }
        for ((rx, a) in listOf(w * 0.17f to 30, w * 0.12f to 55, w * 0.075f to 85, w * 0.035f to 120)) {
            c.save(); c.scale(1f, 1.35f, cx, cy - w * 0.12f)
            c.drawCircle(cx, cy - w * 0.12f, rx, if (a > 100) fill(accent, 90) else stroke(accent, w, a, 0.004f)); c.restore()
        }
        c.restore()
    }

    private fun trident(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val top = h * 0.3f; val bottom = h * 0.74f; val line = stroke(accent, w, 80, 0.012f)
        c.drawLine(cx, top + w * 0.06f, cx, bottom, line)
        val mid = Path().apply { moveTo(cx - w * 0.03f, top + w * 0.08f); lineTo(cx, top - w * 0.04f); lineTo(cx + w * 0.03f, top + w * 0.08f) }
        c.drawPath(mid, line)
        val arc = Path().apply {
            moveTo(cx - w * 0.2f, top - w * 0.03f); quadTo(cx - w * 0.2f, top + w * 0.2f, cx, top + w * 0.2f)
            quadTo(cx + w * 0.2f, top + w * 0.2f, cx + w * 0.2f, top - w * 0.03f)
        }
        c.drawPath(arc, line)
        for (s in listOf(-1, 1)) {
            val tip = Path().apply { moveTo(cx + s * w * 0.23f, top + w * 0.02f); lineTo(cx + s * w * 0.2f, top - w * 0.06f); lineTo(cx + s * w * 0.17f, top + w * 0.02f) }
            c.drawPath(tip, line)
        }
        c.drawCircle(cx, bottom + w * 0.01f, w * 0.015f, fill(accent, 80))
        for (i in 1..3) c.drawCircle(cx, h * 0.5f, w * 0.2f * i, stroke(accent, w, 24))
    }

    private fun rangoli(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val line = stroke(accent, w, 50, 0.003f)
        for (i in 0 until 8) {
            val a = i * PI / 4
            for (r in listOf(0.15f, 0.28f, 0.40f)) {
                val x = cx + (w * r * cos(a)).toFloat(); val y = cy + (w * r * sin(a)).toFloat()
                c.drawCircle(x, y, w * 0.025f, line)
            }
        }
        for (r in listOf(0.15f, 0.28f, 0.40f)) c.drawCircle(cx, cy, w * r, stroke(accent, w, 30, 0.002f))
        c.drawCircle(cx, cy, w * 0.05f, fill(accent, 60))
    }

    private fun chakra(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val r = w * 0.38f; val line = stroke(accent, w, 70, 0.005f)
        c.drawCircle(cx, cy, r, line); c.drawCircle(cx, cy, r * 0.18f, fill(accent, 80))
        for (i in 0 until 24) {
            val a = i * 2 * PI / 24
            val x1 = cx + (r * 0.18f * cos(a)).toFloat(); val y1 = cy + (r * 0.18f * sin(a)).toFloat()
            val x2 = cx + (r * cos(a)).toFloat(); val y2 = cy + (r * sin(a)).toFloat()
            c.drawLine(x1, y1, x2, y2, stroke(accent, w, 40, 0.002f))
        }
    }

    private fun kalash(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val by = h * 0.65f; val line = stroke(accent, w, 80, 0.005f)
        val body = Path().apply {
            moveTo(cx - w * 0.12f, by - w * 0.35f)
            quadTo(cx - w * 0.22f, by - w * 0.15f, cx - w * 0.18f, by)
            lineTo(cx + w * 0.18f, by)
            quadTo(cx + w * 0.22f, by - w * 0.15f, cx + w * 0.12f, by - w * 0.35f)
            close()
        }
        c.drawPath(body, fill(accent, 30)); c.drawPath(body, line)
        c.drawLine(cx - w * 0.12f, by - w * 0.35f, cx + w * 0.12f, by - w * 0.35f, line)
        c.drawLine(cx - w * 0.07f, by - w * 0.38f, cx + w * 0.07f, by - w * 0.38f, line)
        c.drawLine(cx, by - w * 0.38f, cx, by - w * 0.52f, line)
        c.drawCircle(cx, by - w * 0.55f, w * 0.04f, fill(accent, 70))
        for (i in 1..3) c.drawLine(cx - w * 0.06f * i, by - w * 0.2f, cx + w * 0.06f * i, by - w * 0.2f, stroke(accent, w, 40, 0.002f))
    }

    private fun yantra(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val r = w * 0.38f; val line = stroke(accent, w, 60, 0.003f)
        c.drawCircle(cx, cy, r, line)
        for (rot in listOf(0f, 60f, 120f)) {
            val tri = Path()
            for (i in 0 until 3) {
                val a = Math.toRadians((rot + i * 120.0))
                val x = cx + (r * 0.85f * cos(a)).toFloat(); val y = cy + (r * 0.85f * sin(a)).toFloat()
                if (i == 0) tri.moveTo(x, y) else tri.lineTo(x, y)
            }
            tri.close(); c.drawPath(tri, stroke(accent, w, 70, 0.004f))
        }
        c.drawCircle(cx, cy, r * 0.15f, fill(accent, 80))
    }

    private fun templeArch(c: Canvas, accent: Int, w: Int, h: Int) {
        val l = w * 0.18f; val r = w * 0.82f; val bot = h * 0.85f; val mid = (l + r) / 2f
        val arch = Path().apply {
            moveTo(l, bot); lineTo(l, h * 0.45f)
            arcTo(l, h * 0.2f, r, h * 0.45f + (r - l) / 2, 180f, 180f, false)
            lineTo(r, bot); close()
        }
        c.drawPath(arch, fill(accent, 20)); c.drawPath(arch, stroke(accent, w, 80, 0.005f))
        c.drawLine(mid, h * 0.2f, mid, bot, stroke(accent, w, 40, 0.003f))
        for (i in 1..3) c.drawLine(l, h * (0.5f + i * 0.08f), r, h * (0.5f + i * 0.08f), stroke(accent, w, 25, 0.002f))
    }

    // ── Christian ───────────────────────────────────────────────────────────
    private fun cross(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.48f
        val ray = stroke(accent, w, 26, 0.002f)
        for (i in 0 until 24) { val a = i * 2 * PI / 24; c.drawLine(cx, cy, cx + (w * cos(a)).toFloat(), cy + (w * sin(a)).toFloat(), ray) }
        val bar = fill(accent, 38); val thick = w * 0.075f; val arm = w * 0.28f
        c.drawRoundRect(cx - thick / 2, cy - w * 0.46f, cx + thick / 2, cy + w * 0.5f, thick / 3, thick / 3, bar)
        c.drawRoundRect(cx - arm, cy - w * 0.22f, cx + arm, cy - w * 0.22f + thick, thick / 3, thick / 3, bar)
        c.drawRoundRect(cx - thick / 2, cy - w * 0.46f, cx + thick / 2, cy + w * 0.5f, thick / 3, thick / 3, stroke(accent, w, 70))
        c.drawRoundRect(cx - arm, cy - w * 0.22f, cx + arm, cy - w * 0.22f + thick, thick / 3, thick / 3, stroke(accent, w, 70))
        for (i in 1..3) c.drawCircle(cx, cy, w * 0.16f * i, stroke(accent, w, 24))
    }

    private fun dove(c: Canvas, accent: Int, w: Int, h: Int) {
        val u = w / 100f; val ox = w * 0.12f; val oy = h * 0.4f
        fun p(x: Float, y: Float) = ox + x * u * 0.76f to oy + y * u * 0.76f
        val body = Path().apply {
            moveTo(p(86f, 30f).first, p(86f, 30f).second)
            cubicTo(p(80f, 24f).first, p(80f, 24f).second, p(70f, 26f).first, p(70f, 26f).second, p(66f, 34f).first, p(66f, 34f).second)
            cubicTo(p(52f, 38f).first, p(52f, 38f).second, p(30f, 44f).first, p(30f, 44f).second, p(6f, 66f).first, p(6f, 66f).second)
            cubicTo(p(30f, 62f).first, p(30f, 62f).second, p(44f, 62f).first, p(44f, 62f).second, p(58f, 54f).first, p(58f, 54f).second)
            cubicTo(p(72f, 52f).first, p(72f, 52f).second, p(82f, 44f).first, p(82f, 44f).second, p(86f, 30f).first, p(86f, 30f).second); close()
        }
        val wing = Path().apply {
            moveTo(p(62f, 38f).first, p(62f, 38f).second)
            cubicTo(p(54f, 12f).first, p(54f, 12f).second, p(34f, -6f).first, p(34f, -6f).second, p(14f, -4f).first, p(14f, -4f).second)
            cubicTo(p(30f, 8f).first, p(30f, 8f).second, p(40f, 24f).first, p(40f, 24f).second, p(44f, 42f).first, p(44f, 42f).second); close()
        }
        c.drawPath(body, fill(accent, 40)); c.drawPath(body, stroke(accent, w, 90, 0.004f))
        c.drawPath(wing, fill(accent, 55)); c.drawPath(wing, stroke(accent, w, 100, 0.004f))
        c.drawCircle(p(76f, 30f).first, p(76f, 30f).second, w * 0.006f, fill(accent, 140))
        for (i in 1..3) c.drawCircle(w / 2f, h * 0.5f, w * 0.22f * i, stroke(accent, w, 20))
    }

    private fun olive(c: Canvas, accent: Int, w: Int, h: Int) {
        val line = stroke(accent, w, 80, 0.005f); val leaf = fill(accent, 40)
        val stem = Path().apply { moveTo(w * 0.1f, h * 0.7f); quadTo(w * 0.5f, h * 0.62f, w * 0.92f, h * 0.34f) }
        c.drawPath(stem, line)
        val pm = android.graphics.PathMeasure(stem, false); val pos = FloatArray(2); val tan = FloatArray(2)
        for (i in 1..9) {
            pm.getPosTan(pm.length * i / 10f, pos, tan)
            val ang = Math.toDegrees(Math.atan2(tan[1].toDouble(), tan[0].toDouble())).toFloat()
            for (side in listOf(-1, 1)) {
                c.save(); c.translate(pos[0], pos[1]); c.rotate(ang + side * 52f)
                val lf = Path().apply { moveTo(0f, 0f); quadTo(w * 0.05f, -w * 0.04f * side, w * 0.13f, 0f); quadTo(w * 0.05f, w * 0.04f * side, 0f, 0f); close() }
                c.drawPath(lf, leaf); c.drawPath(lf, stroke(accent, w, 80, 0.002f)); c.restore()
            }
        }
    }

    private fun stained(c: Canvas, accent: Int, w: Int, h: Int) {
        val l = w * 0.2f; val r = w * 0.8f; val top = h * 0.2f; val bot = h * 0.78f; val mid = (l + r) / 2f
        val arch = Path().apply { moveTo(l, bot); lineTo(l, top + (r - l) / 2); arcTo(l, top, r, top + (r - l), 180f, 180f, false); lineTo(r, bot); close() }
        c.drawPath(arch, fill(accent, 16)); c.drawPath(arch, stroke(accent, w, 90, 0.005f))
        val line = stroke(accent, w, 60, 0.003f)
        c.drawLine(mid, top, mid, bot, line); c.drawLine(l + (r - l) / 3, top + w * 0.1f, l + (r - l) / 3, bot, line); c.drawLine(r - (r - l) / 3, top + w * 0.1f, r - (r - l) / 3, bot, line)
        c.drawCircle(mid, top + (r - l) * 0.38f, (r - l) * 0.16f, line)
        for (i in 0 until 8) { val a = i * PI / 4; c.drawLine(mid, top + (r - l) * 0.38f, mid + (cos(a) * (r - l) * 0.16f).toFloat(), top + (r - l) * 0.38f + (sin(a) * (r - l) * 0.16f).toFloat(), line) }
        val pane = fill(accent, 30)
        c.drawRect(l + (r - l) / 3, h * 0.5f, mid, h * 0.62f, pane); c.drawRect(mid, h * 0.4f, r - (r - l) / 3, h * 0.5f, pane)
    }

    private fun ichthys(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val line = stroke(accent, w, 80, 0.006f)
        val fish = Path().apply {
            moveTo(cx - w * 0.28f, cy)
            cubicTo(cx - w * 0.28f, cy - w * 0.14f, cx + w * 0.18f, cy - w * 0.14f, cx + w * 0.28f, cy)
            cubicTo(cx + w * 0.18f, cy + w * 0.14f, cx - w * 0.28f, cy + w * 0.14f, cx - w * 0.28f, cy)
        }
        c.drawPath(fish, stroke(accent, w, 80, 0.006f))
        val tail = Path().apply {
            moveTo(cx + w * 0.28f, cy); lineTo(cx + w * 0.44f, cy - w * 0.12f)
            lineTo(cx + w * 0.44f, cy + w * 0.12f); close()
        }
        c.drawPath(tail, stroke(accent, w, 80, 0.006f))
        c.drawCircle(cx - w * 0.1f, cy, w * 0.012f, fill(accent, 120))
    }

    private fun candle(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val by = h * 0.72f
        c.drawRoundRect(cx - w * 0.06f, by - w * 0.38f, cx + w * 0.06f, by, w * 0.02f, w * 0.02f, fill(accent, 40))
        c.drawRoundRect(cx - w * 0.06f, by - w * 0.38f, cx + w * 0.06f, by, w * 0.02f, w * 0.02f, stroke(accent, w, 80, 0.004f))
        val flame = Path().apply {
            moveTo(cx, by - w * 0.52f); quadTo(cx + w * 0.06f, by - w * 0.42f, cx, by - w * 0.38f); quadTo(cx - w * 0.06f, by - w * 0.42f, cx, by - w * 0.52f); close()
        }
        c.drawPath(flame, fill(accent, 90))
        for (i in 1..4) c.drawCircle(cx, by - w * 0.45f, w * 0.08f * i, stroke(accent, w, 30 - i * 5, 0.002f))
    }

    private fun wheat(c: Canvas, accent: Int, w: Int, h: Int) {
        val line = stroke(accent, w, 70, 0.004f)
        for (s in listOf(-1f, 0f, 1f)) {
            val cx = w * (0.5f + s * 0.18f); val top = h * 0.2f; val bot = h * 0.78f
            c.drawLine(cx, top, cx, bot, line)
            for (i in 0..5) {
                val y = top + (bot - top) * i / 5f
                for (side in listOf(-1f, 1f)) {
                    val gx = cx + side * w * 0.07f; val gy = y - w * 0.04f
                    c.drawLine(cx, y, gx, gy, line)
                    c.drawOval(gx - w * 0.025f, gy - w * 0.05f, gx + w * 0.025f, gy, fill(accent, 35))
                }
            }
        }
    }

    private fun anchor(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val line = stroke(accent, w, 80, 0.007f)
        c.drawCircle(cx, cy - w * 0.28f, w * 0.06f, line)
        c.drawLine(cx, cy - w * 0.22f, cx, cy + w * 0.3f, line)
        val cross = Path().apply { moveTo(cx - w * 0.14f, cy - w * 0.14f); lineTo(cx + w * 0.14f, cy - w * 0.14f) }
        c.drawPath(cross, line)
        val arc = Path().apply { addArc(cx - w * 0.2f, cy + w * 0.1f, cx + w * 0.2f, cy + w * 0.5f, 0f, 180f) }
        c.drawPath(arc, line)
        c.drawLine(cx - w * 0.2f, cy + w * 0.3f, cx - w * 0.08f, cy + w * 0.3f, line)
        c.drawLine(cx + w * 0.08f, cy + w * 0.3f, cx + w * 0.2f, cy + w * 0.3f, line)
    }

    private fun vine(c: Canvas, accent: Int, w: Int, h: Int) {
        val line = stroke(accent, w, 70, 0.004f)
        val stem = Path().apply { moveTo(w * 0.08f, h * 0.9f); cubicTo(w * 0.3f, h * 0.6f, w * 0.7f, h * 0.4f, w * 0.92f, h * 0.1f) }
        c.drawPath(stem, line)
        val pm = android.graphics.PathMeasure(stem, false); val pos = FloatArray(2); val tan = FloatArray(2)
        for (i in 1..7) {
            pm.getPosTan(pm.length * i / 8f, pos, tan)
            val ang = Math.toDegrees(Math.atan2(tan[1].toDouble(), tan[0].toDouble())).toFloat()
            for (side in listOf(-1f, 1f)) {
                c.save(); c.translate(pos[0], pos[1]); c.rotate(ang + side * 60f)
                val lf = Path().apply { moveTo(0f, 0f); quadTo(w * 0.06f, -w * 0.04f, w * 0.12f, 0f); quadTo(w * 0.06f, w * 0.04f, 0f, 0f); close() }
                c.drawPath(lf, fill(accent, 30)); c.drawPath(lf, stroke(accent, w, 70, 0.002f)); c.restore()
            }
        }
    }

    // ── Islamic ──────────────────────────────────────────────────────────────
    private fun star8(c: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        for (rot in listOf(0f, 45f)) {
            val path = Path()
            for (i in 0 until 4) {
                val a = Math.toRadians((rot + 45 + i * 90).toDouble())
                val x = cx + (r * cos(a)).toFloat(); val y = cy + (r * sin(a)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close(); c.drawPath(path, paint)
        }
    }

    private fun geometry(c: Canvas, accent: Int, w: Int, h: Int) {
        val line = stroke(accent, w, 46, 0.0028f); val step = w * 0.25f
        var y = -step / 2; var row = 0
        while (y < h + step) {
            var x = -step / 2 + (if (row % 2 == 0) 0f else step / 2)
            while (x < w + step) { star8(c, x, y, step * 0.5f, line); c.drawCircle(x, y, step * 0.24f, line); x += step }
            y += step / 2; row++
        }
    }

    private fun crescent(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.46f; val r = w * 0.3f
        val outer = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        val inner = Path().apply { addCircle(cx + r * 0.42f, cy - r * 0.08f, r * 0.84f, Path.Direction.CW) }
        outer.op(inner, Path.Op.DIFFERENCE)
        c.drawPath(outer, fill(accent, 60)); c.drawPath(outer, stroke(accent, w, 110, 0.004f))
        val star = Path()
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) w * 0.05f else w * 0.021f; val a = -PI / 2 + i * PI / 5
            val x = cx + r * 0.62f + (rr * cos(a)).toFloat(); val y = cy - r * 0.2f + (rr * sin(a)).toFloat()
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close(); c.drawPath(star, fill(accent, 110))
        val rnd = java.util.Random(11); val dot = fill(accent, 90)
        repeat(26) { c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.7f, w * (0.002f + rnd.nextFloat() * 0.003f), dot) }
    }

    private fun mosque(c: Canvas, accent: Int, w: Int, h: Int) {
        val base = h * 0.99f; val sil = fill(accent, 48)
        val g = h * 0.8f
        // central dome
        val dome = Path().apply { moveTo(w * 0.3f, base); lineTo(w * 0.3f, g + w * 0.1f); arcTo(w * 0.3f, g - w * 0.08f, w * 0.7f, g + w * 0.28f, 180f, 180f, false); lineTo(w * 0.7f, base); close() }
        c.drawPath(dome, sil)
        c.drawRect(w * 0.495f, g - w * 0.16f, w * 0.505f, g - w * 0.06f, sil); c.drawCircle(w * 0.5f, g - w * 0.17f, w * 0.012f, sil)
        for (mx in listOf(0.14f, 0.86f)) {
            c.drawRect(w * (mx - 0.025f), g - w * 0.1f, w * (mx + 0.025f), base, sil)
            val tip = Path().apply { moveTo(w * (mx - 0.032f), g - w * 0.1f); lineTo(w * mx, g - w * 0.19f); lineTo(w * (mx + 0.032f), g - w * 0.1f); close() }
            c.drawPath(tip, sil)
        }
        for (mx in listOf(0.24f, 0.76f)) {
            val small = Path().apply { moveTo(w * (mx - 0.06f), base); lineTo(w * (mx - 0.06f), g + w * 0.2f); arcTo(w * (mx - 0.06f), g + w * 0.1f, w * (mx + 0.06f), g + w * 0.3f, 180f, 180f, false); lineTo(w * (mx + 0.06f), base); close() }
            c.drawPath(small, sil)
        }
        c.drawCircle(w * 0.82f, h * 0.18f, w * 0.05f, fill(accent, 40))
    }

    private fun arabesque(c: Canvas, accent: Int, w: Int, h: Int) {
        val line = stroke(accent, w, 42, 0.0028f); val r = w * 0.12f
        var y = 0f; var row = 0
        while (y < h + r) {
            var x = if (row % 2 == 0) 0f else r * 0.87f * 0f + r
            while (x < w + r * 2) { c.drawCircle(x, y, r, line); x += r * 2f }
            y += r * 1.0f; row++
        }
        val cx = w / 2f; val cy = h * 0.5f
        for (i in 1..3) c.drawCircle(cx, cy, w * 0.14f * i, stroke(accent, w, 30, 0.004f))
    }

    private fun lantern(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val line = stroke(accent, w, 80, 0.005f)
        val body = Path().apply {
            moveTo(cx - w * 0.1f, cy - w * 0.28f); lineTo(cx - w * 0.16f, cy + w * 0.18f)
            lineTo(cx + w * 0.16f, cy + w * 0.18f); lineTo(cx + w * 0.1f, cy - w * 0.28f); close()
        }
        c.drawPath(body, fill(accent, 30)); c.drawPath(body, line)
        c.drawLine(cx - w * 0.1f, cy - w * 0.28f, cx + w * 0.1f, cy - w * 0.28f, line)
        c.drawLine(cx - w * 0.16f, cy + w * 0.18f, cx + w * 0.16f, cy + w * 0.18f, line)
        c.drawLine(cx, cy - w * 0.28f, cx, cy - w * 0.36f, line)
        for (i in 1..3) c.drawLine(cx - w * 0.16f, cy - w * 0.1f + i * w * 0.1f, cx + w * 0.16f, cy - w * 0.1f + i * w * 0.1f, stroke(accent, w, 40, 0.002f))
        for (i in 1..4) c.drawCircle(cx, cy, w * 0.12f * i, stroke(accent, w, 20, 0.002f))
    }

    private fun muqarnas(c: Canvas, accent: Int, w: Int, h: Int) {
        val line = stroke(accent, w, 60, 0.003f); val fill2 = fill(accent, 22)
        val levels = listOf(0.18f to 0.08f, 0.28f to 0.12f, 0.40f to 0.16f)
        for ((yFrac, size) in levels) {
            val y = h * yFrac; val count = (w / (size * w * 2.2f)).toInt().coerceAtLeast(3)
            val step = w / count.toFloat()
            for (i in 0 until count) {
                val cx = step * (i + 0.5f)
                val cell = Path().apply {
                    moveTo(cx, y - size * w); lineTo(cx + size * w, y); lineTo(cx, y + size * w * 0.5f); lineTo(cx - size * w, y); close()
                }
                c.drawPath(cell, fill2); c.drawPath(cell, line)
            }
        }
    }

    private fun starPolygon(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f
        for (points in listOf(8, 6)) {
            val r1 = w * (if (points == 8) 0.38f else 0.24f); val r2 = r1 * 0.45f
            val star = Path()
            for (i in 0 until points * 2) {
                val r = if (i % 2 == 0) r1 else r2
                val a = -PI / 2 + i * PI / points
                val x = cx + (r * cos(a)).toFloat(); val y = cy + (r * sin(a)).toFloat()
                if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
            }
            star.close(); c.drawPath(star, stroke(accent, w, 70, 0.004f))
        }
        c.drawCircle(cx, cy, w * 0.06f, fill(accent, 80))
    }

    private fun prayerBeads(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val r = w * 0.36f; val bead = fill(accent, 70)
        c.drawCircle(cx, cy, r, stroke(accent, w, 30, 0.002f))
        for (i in 0 until 33) {
            val a = i * 2 * PI / 33
            c.drawCircle(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat(), w * 0.018f, bead)
        }
        c.drawLine(cx, cy - r, cx, cy - r - w * 0.1f, stroke(accent, w, 60, 0.004f))
        c.drawCircle(cx, cy - r - w * 0.13f, w * 0.025f, fill(accent, 90))
    }

    private fun calligraphyArch(c: Canvas, accent: Int, w: Int, h: Int) {
        val l = w * 0.12f; val r = w * 0.88f; val bot = h * 0.88f; val mid = (l + r) / 2f
        val arch = Path().apply {
            moveTo(l, bot); lineTo(l, h * 0.42f)
            arcTo(l, h * 0.18f, r, h * 0.42f + (r - l) / 2, 180f, 180f, false)
            lineTo(r, bot)
        }
        c.drawPath(arch, stroke(accent, w, 80, 0.005f))
        for (i in 1..2) c.drawPath(arch.also {}, stroke(accent, w, 30, 0.002f))
        val line = stroke(accent, w, 40, 0.003f)
        for (i in 0 until 6) c.drawLine(l + (r - l) * i / 5f, h * 0.55f, l + (r - l) * i / 5f, bot, line)
        c.drawCircle(mid, h * 0.3f, w * 0.04f, fill(accent, 70))
    }

    // ── Buddhist ─────────────────────────────────────────────────────────────
    private fun dharma(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val r = w * 0.4f; val line = stroke(accent, w, 80, 0.006f)
        c.drawCircle(cx, cy, r, line); c.drawCircle(cx, cy, r * 0.82f, stroke(accent, w, 50, 0.003f)); c.drawCircle(cx, cy, r * 0.2f, line); c.drawCircle(cx, cy, r * 0.08f, fill(accent, 90))
        for (i in 0 until 8) {
            val a = i * PI / 4; val x1 = cx + (r * 0.2f * cos(a)).toFloat(); val y1 = cy + (r * 0.2f * sin(a)).toFloat()
            val x2 = cx + (r * 0.82f * cos(a)).toFloat(); val y2 = cy + (r * 0.82f * sin(a)).toFloat()
            c.drawLine(x1, y1, x2, y2, line)
            c.drawCircle(cx + (r * 0.92f * cos(a)).toFloat(), cy + (r * 0.92f * sin(a)).toFloat(), r * 0.06f, fill(accent, 80))
            val a2 = a + PI / 8
            c.drawLine(cx + (r * 0.82f * cos(a)).toFloat(), cy + (r * 0.82f * sin(a)).toFloat(), cx + (r * 0.6f * cos(a2)).toFloat(), cy + (r * 0.6f * sin(a2)).toFloat(), stroke(accent, w, 36, 0.003f))
        }
    }

    private fun bodhi(c: Canvas, accent: Int, w: Int, h: Int) {
        fun leafPath(s: Float) = Path().apply {
            moveTo(0f, s * 0.5f); cubicTo(-s * 0.75f, s * 0.35f, -s * 0.55f, -s * 0.35f, 0f, -s * 0.5f)
            cubicTo(s * 0.55f, -s * 0.35f, s * 0.75f, s * 0.35f, 0f, s * 0.5f); close()
        }
        fun drawLeaf(cx: Float, cy: Float, s: Float, rot: Float, a: Int) {
            c.save(); c.translate(cx, cy); c.rotate(rot)
            val leaf = Path().apply {
                moveTo(0f, -s * 0.55f); cubicTo(-s * 0.55f, -s * 0.3f, -s * 0.5f, s * 0.2f, 0f, s * 0.3f)
                cubicTo(s * 0.05f, s * 0.5f, 0f, s * 0.8f, 0f, s * 0.95f); cubicTo(0f, s * 0.8f, -s * 0.05f, s * 0.5f, 0f, s * 0.3f)
                cubicTo(s * 0.5f, s * 0.2f, s * 0.55f, -s * 0.3f, 0f, -s * 0.55f); close()
            }
            c.drawPath(leaf, fill(accent, a / 2)); c.drawPath(leaf, stroke(accent, w, a, 0.004f))
            c.drawLine(0f, -s * 0.55f, 0f, s * 0.95f, stroke(accent, w, a / 2, 0.002f))
            for (i in 1..4) { val y = -s * 0.4f + i * s * 0.22f; c.drawLine(0f, y, -s * 0.28f, y - s * 0.12f, stroke(accent, w, a / 3, 0.002f)); c.drawLine(0f, y, s * 0.28f, y - s * 0.12f, stroke(accent, w, a / 3, 0.002f)) }
            c.restore()
        }
        drawLeaf(w * 0.5f, h * 0.44f, w * 0.7f, 8f, 80)
        drawLeaf(w * 0.14f, h * 0.2f, w * 0.22f, -30f, 45); drawLeaf(w * 0.88f, h * 0.7f, w * 0.26f, 35f, 45)
    }

    private fun halo(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.46f
        for (i in 1..6) c.drawCircle(cx, cy, w * 0.09f * i, stroke(accent, w, 62 - i * 7, 0.004f))
        c.drawCircle(cx, cy, w * 0.07f, fill(accent, 70))
        for (i in 0 until 16) { val a = i * PI / 8; c.drawLine(cx + (w * 0.12f * cos(a)).toFloat(), cy + (w * 0.12f * sin(a)).toFloat(), cx + (w * 0.2f * cos(a)).toFloat(), cy + (w * 0.2f * sin(a)).toFloat(), stroke(accent, w, 60)) }
    }

    // ── General / nature ──────────────────────────────────────────────────────
    private fun mountains(c: Canvas, accent: Int, w: Int, h: Int) {
        c.drawCircle(w * 0.72f, h * 0.22f, w * 0.07f, fill(accent, 60))
        for ((layer, alpha) in listOf(0 to 30, 1 to 40, 2 to 55)) {
            val base = h * (0.72f + layer * 0.07f); val path = Path()
            path.moveTo(0f, h.toFloat()); path.lineTo(0f, base)
            var x = 0f; var up = true
            while (x < w) { val peak = w * (0.1f + 0.06f * ((layer * 3 + (x / w * 7).toInt()) % 3)); x += w * 0.16f; path.lineTo(x, if (up) base - peak else base - peak * 0.35f); up = !up }
            path.lineTo(w.toFloat(), h.toFloat()); path.close(); c.drawPath(path, fill(accent, alpha))
        }
    }

    private fun sunrise(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.74f
        for (i in 0 until 17) { val a = PI + i * PI / 16; c.drawLine(cx + (w * 0.2f * cos(a)).toFloat(), cy + (w * 0.2f * sin(a)).toFloat(), cx + (w * 0.6f * cos(a)).toFloat(), cy + (w * 0.6f * sin(a)).toFloat(), stroke(accent, w, 40, 0.004f)) }
        val sun = Path().apply { addArc(cx - w * 0.17f, cy - w * 0.17f, cx + w * 0.17f, cy + w * 0.17f, 180f, 180f); close() }
        c.drawPath(sun, fill(accent, 70)); c.drawLine(0f, cy, w.toFloat(), cy, stroke(accent, w, 70, 0.004f))
    }

    private fun leaves(c: Canvas, accent: Int, w: Int, h: Int) {
        val rnd = java.util.Random(23); val leaf = Path().apply { moveTo(0f, 0f); quadTo(w * 0.05f, -w * 0.05f, w * 0.14f, 0f); quadTo(w * 0.05f, w * 0.05f, 0f, 0f); close() }
        repeat(22) {
            c.save(); c.translate(rnd.nextFloat() * w, rnd.nextFloat() * h); c.rotate(rnd.nextFloat() * 360f); val s = 0.5f + rnd.nextFloat() * 1.2f; c.scale(s, s)
            c.drawPath(leaf, fill(accent, 16 + rnd.nextInt(26))); c.drawPath(leaf, stroke(accent, w, 36, 0.002f)); c.restore()
        }
    }

    private fun endlessKnot(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val r = w * 0.3f; val line = stroke(accent, w, 70, 0.005f)
        val path = Path().apply {
            moveTo(cx - r, cy)
            cubicTo(cx - r, cy - r, cx, cy - r, cx, cy)
            cubicTo(cx, cy + r, cx + r, cy + r, cx + r, cy)
            cubicTo(cx + r, cy - r, cx, cy - r, cx, cy)
            cubicTo(cx, cy + r, cx - r, cy + r, cx - r, cy)
        }
        c.drawPath(path, line)
        for (i in 1..2) c.drawCircle(cx, cy, r * 0.3f * i, stroke(accent, w, 30, 0.002f))
    }

    private fun zenCircle(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f
        val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = accent; alpha = 90
            strokeWidth = w * 0.022f; strokeCap = Paint.Cap.ROUND
        }
        c.drawPath(Path().apply { addArc(cx - w * 0.34f, cy - w * 0.34f, cx + w * 0.34f, cy + w * 0.34f, 120f, 300f) }, brush)
        c.drawCircle(cx, cy, w * 0.04f, fill(accent, 50))
    }

    private fun prayerFlags(c: Canvas, accent: Int, w: Int, h: Int) {
        val line = stroke(accent, w, 60, 0.003f)
        c.drawLine(0f, h * 0.22f, w.toFloat(), h * 0.28f, line)
        val alphas = listOf(50, 35, 45, 30, 55)
        for (i in 0..4) {
            val x = w * (0.08f + i * 0.2f); val y = h * (0.22f + i * 0.012f)
            val flag = Path().apply { moveTo(x, y); lineTo(x + w * 0.14f, y); lineTo(x + w * 0.14f, y + h * 0.1f); lineTo(x, y + h * 0.1f); close() }
            c.drawPath(flag, fill(accent, alphas[i])); c.drawPath(flag, line)
        }
    }

    private fun stupa(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val sil = fill(accent, 45); val line = stroke(accent, w, 70, 0.004f)
        val base = Path().apply { moveTo(w * 0.15f, h * 0.88f); lineTo(w * 0.85f, h * 0.88f); lineTo(w * 0.78f, h * 0.72f); lineTo(w * 0.22f, h * 0.72f); close() }
        c.drawPath(base, sil); c.drawPath(base, line)
        val dome = Path().apply { addArc(cx - w * 0.22f, h * 0.44f, cx + w * 0.22f, h * 0.72f, 180f, 180f) }
        c.drawPath(dome, sil); c.drawPath(dome, line)
        c.drawLine(cx, h * 0.44f, cx, h * 0.18f, line)
        for (i in 1..4) c.drawLine(cx - w * 0.04f, h * (0.44f - i * 0.05f), cx + w * 0.04f, h * (0.44f - i * 0.05f), line)
        c.drawCircle(cx, h * 0.16f, w * 0.025f, fill(accent, 80))
    }

    private fun koi(c: Canvas, accent: Int, w: Int, h: Int) {
        val line = stroke(accent, w, 70, 0.005f)
        for ((ox, oy, flip) in listOf(Triple(w * 0.3f, h * 0.42f, 1f), Triple(w * 0.7f, h * 0.58f, -1f))) {
            val body = Path().apply {
                moveTo(ox, oy - w * 0.22f * flip)
                cubicTo(ox + w * 0.14f, oy - w * 0.1f * flip, ox + w * 0.14f, oy + w * 0.1f * flip, ox, oy + w * 0.22f * flip)
                cubicTo(ox - w * 0.14f, oy + w * 0.1f * flip, ox - w * 0.14f, oy - w * 0.1f * flip, ox, oy - w * 0.22f * flip)
            }
            c.drawPath(body, fill(accent, 35)); c.drawPath(body, line)
            c.drawCircle(ox + w * 0.04f, oy - w * 0.14f * flip, w * 0.012f, fill(accent, 100))
        }
        c.drawCircle(w / 2f, h * 0.5f, w * 0.28f, stroke(accent, w, 30, 0.002f))
    }

    private fun treeOfLife(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val line = stroke(accent, w, 60, 0.004f)
        c.drawLine(cx, h * 0.88f, cx, h * 0.5f, stroke(accent, w, 70, 0.006f))
        fun branch(x: Float, y: Float, angle: Double, len: Float, depth: Int) {
            if (depth == 0) return
            val ex = x + (len * cos(angle)).toFloat(); val ey = y + (len * sin(angle)).toFloat()
            c.drawLine(x, y, ex, ey, line)
            branch(ex, ey, angle - 0.45, len * 0.68f, depth - 1)
            branch(ex, ey, angle + 0.45, len * 0.68f, depth - 1)
        }
        branch(cx, h * 0.5f, -PI / 2, w * 0.22f, 5)
        c.drawCircle(cx, h * 0.88f, w * 0.04f, fill(accent, 60))
    }

    private fun galaxy(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val rnd = java.util.Random(7)
        repeat(80) {
            val r = rnd.nextFloat() * w * 0.44f; val a = rnd.nextFloat() * 2 * PI.toFloat() + r * 0.012f
            val x = cx + (r * cos(a)).toFloat(); val y = cy + (r * sin(a)).toFloat()
            c.drawCircle(x, y, w * (0.002f + rnd.nextFloat() * 0.005f), fill(accent, 40 + rnd.nextInt(60)))
        }
        for (i in 1..3) c.drawCircle(cx, cy, w * 0.1f * i, stroke(accent, w, 20, 0.002f))
    }

    private fun feather(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val line = stroke(accent, w, 70, 0.004f)
        val spine = Path().apply { moveTo(cx, h * 0.18f); quadTo(cx + w * 0.1f, h * 0.5f, cx - w * 0.04f, h * 0.82f) }
        c.drawPath(spine, stroke(accent, w, 90, 0.005f))
        val pm = android.graphics.PathMeasure(spine, false); val pos = FloatArray(2); val tan = FloatArray(2)
        for (i in 1..18) {
            pm.getPosTan(pm.length * i / 19f, pos, tan)
            val ang = Math.toDegrees(Math.atan2(tan[1].toDouble(), tan[0].toDouble())).toFloat()
            val len = w * (0.06f + 0.1f * sin(PI * i / 19.0).toFloat())
            for (side in listOf(-1f, 1f)) {
                c.save(); c.translate(pos[0], pos[1]); c.rotate(ang + side * 70f)
                c.drawLine(0f, 0f, len, 0f, line); c.restore()
            }
        }
    }

    private fun spiral(c: Canvas, accent: Int, w: Int, h: Int) {
        val cx = w / 2f; val cy = h * 0.5f; val path = Path()
        var first = true
        for (i in 0..360) {
            val a = i * 6 * PI / 180.0; val r = w * 0.008f * i / 20f
            val x = cx + (r * cos(a)).toFloat(); val y = cy + (r * sin(a)).toFloat()
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
        }
        c.drawPath(path, stroke(accent, w, 60, 0.004f))
        c.drawCircle(cx, cy, w * 0.03f, fill(accent, 70))
    }

    private fun aurora(c: Canvas, accent: Int, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = accent; strokeCap = Paint.Cap.ROUND }
        for (i in 0..5) {
            val y = h * (0.2f + i * 0.08f); val amp = w * (0.06f + i * 0.02f)
            paint.alpha = 20 + i * 8; paint.strokeWidth = w * (0.006f + i * 0.002f)
            val wave = Path().apply {
                moveTo(0f, y)
                for (x in 0..w step (w / 6)) {
                    val cy = y + amp * sin(PI * x / (w / 3.0) + i).toFloat()
                    quadTo(x - w / 12f, cy, x.toFloat(), y + amp * sin(PI * x / (w / 3.0) + i + PI / 2).toFloat())
                }
            }
            c.drawPath(wave, paint)
        }
    }
}
