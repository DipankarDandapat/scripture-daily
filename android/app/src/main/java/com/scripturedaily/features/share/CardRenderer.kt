package com.scripturedaily.features.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.scripturedaily.R
import com.scripturedaily.data.Verse
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class CardFormat(val label: String, val hint: String, val w: Int, val h: Int) {
    WHATSAPP("WhatsApp Status", "9:16", 1080, 1920),
    STORY("Instagram Story", "9:16", 1080, 1920),
    SQUARE("Square post", "1:1", 1080, 1080),
    PORTRAIT("Portrait post", "4:5", 1080, 1350)
}

/** Background art drawn behind the verse. [TEMPLATE] uses the selected template's own motif (om, cross, geometry …). */
enum class CardPattern(val label: String) { TEMPLATE("Template art"), NONE("Plain"), MANDALA("Mandala"), STARS("Stars"), WAVES("Waves"), RAYS("Rays"), BOKEH("Bokeh") }

enum class CardFont(val label: String) { SERIF("Serif"), SANS("Sans"), ITALIC("Italic"), MONO("Typewriter") }

enum class TextAlignment(val label: String) { START("Left"), CENTER("Centre"), END("Right") }

enum class TextPlacement(val label: String) { TOP("Top"), MIDDLE("Middle"), BOTTOM("Bottom") }

/** Text colours the user can pick instead of the template's own (null = template default). */
val TEXT_COLORS: List<Pair<String, Int>> = listOf(
    "Ivory" to 0xFFF5F0E8.toInt(), "White" to 0xFFFFFFFF.toInt(), "Gold" to 0xFFFFD98A.toInt(),
    "Ink" to 0xFF1A1025.toInt(), "Brown" to 0xFF3B2314.toInt(), "Forest" to 0xFF123524.toInt()
)

data class CardStyle(
    val template: StudioTemplate = StudioTemplates.DEFAULT,
    val pattern: CardPattern = CardPattern.TEMPLATE,
    val format: CardFormat = CardFormat.STORY,
    val fontScale: Float = 1f,
    val font: CardFont = CardFont.SERIF,
    val showMark: Boolean = true,
    val align: TextAlignment = TextAlignment.CENTER,
    val spacing: Float = 1f,                 // line spacing multiplier 0.8 .. 1.8
    val placement: TextPlacement = TextPlacement.MIDDLE,
    val textColor: Int? = null,              // overrides the template's text colour
    val swatch: BgSwatch? = null             // overrides the template's background gradient
) {
    /** The template with the user's colour overrides applied (text/accent follow the background for contrast). */
    fun resolved(): StudioTemplate {
        var t = template
        swatch?.let { sw ->
            t = if (sw.light) t.copy(top = sw.top, bottom = sw.bottom, text = 0xFF2A1F14.toInt(), accent = 0xFF8A4B14.toInt(), muted = 0xFF6B5A48.toInt(), light = true)
            else t.copy(top = sw.top, bottom = sw.bottom, text = 0xFFF5F0E8.toInt(), accent = 0xFFE3C58B.toInt(), muted = 0xFFB9B0C4.toInt(), light = false)
        }
        textColor?.let { t = t.copy(text = it) }
        return t
    }
}

/** What a card says. The reference and source are always drawn. */
data class CardContent(val text: String, val scripture: String, val reference: String, val source: String) {
    companion object {
        fun from(v: Verse) = CardContent(
            text = v.text, scripture = v.scripture, reference = v.reference(),
            source = v.translation?.takeIf { it.isNotBlank() }?.let { "Source: ${v.scripture} · $it" } ?: "Source: ${v.scripture}"
        )
    }
}

/** Draws verse cards with Android's Canvas. One renderer feeds the studio, wallpapers and the video generator. */
object CardRenderer {
    private class Zones(val label: Float, val textTop: Float, val textBottom: Float, val line: Float, val ref: Float, val src: Float, val mark: Float, val base: Float)

    private fun zones(w: Int, h: Int): Zones {
        val ratio = h.toFloat() / w
        return when {
            ratio >= 1.7f -> Zones(.13f, .24f, .71f, .765f, .795f, .83f, .925f, .064f)
            ratio >= 1.2f -> Zones(.115f, .21f, .69f, .745f, .78f, .82f, .925f, .060f)
            else -> Zones(.12f, .21f, .67f, .735f, .78f, .83f, .925f, .056f)
        }
    }

    fun render(context: Context, content: CardContent, style: CardStyle, width: Int = style.format.w): Bitmap {
        val height = (width.toLong() * style.format.h / style.format.w).toInt()
        return renderSized(context, content, style, width, height)
    }

    fun renderSized(context: Context, content: CardContent, style: CardStyle, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawBackground(canvas, style, w, h)
        drawForeground(context, canvas, content, style, w, h, 1f)
        return bmp
    }

    fun renderBackground(style: CardStyle, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawBackground(Canvas(bmp), style, w, h)
        return bmp
    }

    /** Transparent layer with just the text, reference, source and mark. */
    fun renderForeground(context: Context, content: CardContent, style: CardStyle, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawForeground(context, Canvas(bmp), content, style, w, h, 1f)
        return bmp
    }

    // ── background ──────────────────────────────────────────────────────────
    fun drawBackground(canvas: Canvas, style: CardStyle, w: Int, h: Int) {
        val t = style.resolved()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, w * 0.35f, h.toFloat(), t.top, t.bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        // soft glow behind the text
        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        val glowColor = if (t.light) Color.argb(110, 255, 255, 255) else Color.argb(70, Color.red(t.accent), Color.green(t.accent), Color.blue(t.accent))
        glow.shader = RadialGradient(w / 2f, h * 0.42f, max(w, h) * 0.55f, glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glow)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE; color = t.accent; alpha = 40; strokeWidth = w * 0.0025f
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.FILL; color = t.accent; alpha = 36 }
        when (style.pattern) {
            CardPattern.NONE -> Unit
            CardPattern.TEMPLATE -> Motifs.draw(canvas, style.template.motif, t.accent, w, h)
            CardPattern.MANDALA -> {
                val cx = w / 2f; val cy = h * 0.5f
                for (ring in 1..6) canvas.drawCircle(cx, cy, w * 0.14f * ring, stroke)
                val petal = Path()
                for (i in 0 until 16) {
                    val a = i * 2 * PI / 16
                    val rx = cx + (w * 0.42f * cos(a)).toFloat(); val ry = cy + (w * 0.42f * sin(a)).toFloat()
                    petal.reset(); petal.addCircle(rx, ry, w * 0.05f, Path.Direction.CW)
                    canvas.drawPath(petal, stroke)
                }
            }
            CardPattern.STARS -> {
                val rnd = java.util.Random(42)
                val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.accent }
                repeat(90) {
                    star.alpha = 40 + rnd.nextInt(130)
                    canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, w * (0.002f + rnd.nextFloat() * 0.0045f), star)
                }
            }
            CardPattern.WAVES -> {
                for (n in 0 until 5) {
                    val path = Path()
                    val baseY = h * (0.80f + n * 0.04f)
                    path.moveTo(0f, h.toFloat()); path.lineTo(0f, baseY)
                    var x = 0f
                    while (x <= w) { path.lineTo(x, baseY + (sin(x / w * 2 * PI + n) * h * 0.012f).toFloat()); x += w / 60f }
                    path.lineTo(w.toFloat(), h.toFloat()); path.close()
                    fill.alpha = 18 + n * 6
                    canvas.drawPath(path, fill)
                }
            }
            CardPattern.RAYS -> {
                val cx = w / 2f; val cy = -h * 0.05f
                for (i in 0 until 14) {
                    val a = PI * (0.12 + 0.76 * i / 13.0)
                    val path = Path()
                    path.moveTo(cx, cy)
                    path.lineTo(cx + (h * 1.3f * cos(a - 0.02)).toFloat(), cy + (h * 1.3f * sin(a - 0.02)).toFloat())
                    path.lineTo(cx + (h * 1.3f * cos(a + 0.02)).toFloat(), cy + (h * 1.3f * sin(a + 0.02)).toFloat())
                    path.close(); fill.alpha = 22; canvas.drawPath(path, fill)
                }
            }
            CardPattern.BOKEH -> {
                val rnd = java.util.Random(7)
                repeat(14) {
                    fill.alpha = 14 + rnd.nextInt(24)
                    canvas.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, w * (0.03f + rnd.nextFloat() * 0.09f), fill)
                }
            }
        }
    }

    // ── text, reference, source, mark ───────────────────────────────────────
    /** [reveal] < 1 fades the verse words in one by one (used by the video's typewriter animation). */
    fun drawForeground(context: Context, canvas: Canvas, c: CardContent, style: CardStyle, w: Int, h: Int, alpha: Float, reveal: Float = 1f) {
        val z = zones(w, h)
        val t = style.resolved()
        val a255 = (alpha * 255).toInt().coerceIn(0, 255)
        val marginX = w * 0.11f
        val textWidth = (w - 2 * marginX).toInt()

        fun colored(color: Int, extra: Float = 1f): Int =
            Color.argb((Color.alpha(color) * alpha * extra).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

        // scripture label
        val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colored(t.accent); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = w * 0.032f; letterSpacing = 0.2f; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(c.scripture.uppercase(), w / 2f, h * z.label, label)
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colored(t.accent, 0.7f) }
        canvas.drawRect(w * 0.42f, h * z.label + w * 0.02f, w * 0.58f, h * z.label + w * 0.02f + w * 0.003f, rule)

        // opening quote mark
        val quote = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colored(t.accent, 0.35f); typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); textSize = w * 0.19f
        }
        canvas.drawText("“", marginX * 0.7f, h * z.textTop + w * 0.05f, quote)

        // verse text, auto-fitted to the available band
        val fontStyle = if (style.font == CardFont.ITALIC) Typeface.ITALIC else Typeface.NORMAL
        val family = when (style.font) { CardFont.SANS -> Typeface.SANS_SERIF; CardFont.MONO -> Typeface.MONOSPACE; else -> Typeface.SERIF }
        val layoutAlign = when (style.align) { TextAlignment.START -> Layout.Alignment.ALIGN_NORMAL; TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER; TextAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE }
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colored(t.text); typeface = Typeface.create(family, fontStyle)
        }
        val bandTop = h * z.textTop + w * 0.04f
        val bandHeight = h * (z.textBottom - z.textTop) - w * 0.04f
        var size = w * z.base * style.fontScale
        val minSize = w * 0.026f
        var layout: StaticLayout
        while (true) {
            body.textSize = size
            layout = StaticLayout.Builder.obtain(c.text, 0, c.text.length, body, textWidth)
                .setAlignment(layoutAlign).setLineSpacing(size * 0.22f * style.spacing, 1f)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).build()
            if (layout.height <= bandHeight || size <= minSize) break
            size *= 0.94f
        }
        if (reveal < 1f) {
            val spanned = android.text.SpannableStringBuilder(c.text)
            val words = Regex("\\S+").findAll(c.text).toList()
            val base = colored(t.text)
            words.forEachIndexed { i, m ->
                val a = ((reveal * (words.size + 1) - i).coerceIn(0f, 1f) * Color.alpha(base)).toInt()
                spanned.setSpan(android.text.style.ForegroundColorSpan(Color.argb(a, Color.red(base), Color.green(base), Color.blue(base))), m.range.first, m.range.last + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            layout = StaticLayout.Builder.obtain(spanned, 0, spanned.length, body, textWidth)
                .setAlignment(layoutAlign).setLineSpacing(size * 0.22f * style.spacing, 1f)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).build()
        }
        val slack = (bandHeight - layout.height).coerceAtLeast(0f)
        val top = bandTop + when (style.placement) { TextPlacement.TOP -> 0f; TextPlacement.MIDDLE -> slack / 2f; TextPlacement.BOTTOM -> slack }
        canvas.save(); canvas.translate(marginX, top); layout.draw(canvas); canvas.restore()

        // divider + reference + source (always shown)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colored(t.accent, 0.8f) }
        canvas.drawRect(marginX, h * z.line, w - marginX, h * z.line + w * 0.003f, line)
        val ref = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colored(t.accent); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = w * 0.034f
            textAlign = Paint.Align.CENTER
        }
        drawFitted(canvas, c.reference, ref, w / 2f, h * z.ref, w - 2 * marginX)
        val src = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colored(t.muted); typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textSize = w * 0.027f
            textAlign = Paint.Align.CENTER
        }
        drawFitted(canvas, c.source, src, w / 2f, h * z.src, w - 2 * marginX)

        // optional WisdomOne mark: round app logo + wordmark
        if (style.showMark) {
            val markPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colored(t.accent); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = w * 0.03f
                letterSpacing = 0.12f; textAlign = Paint.Align.LEFT
            }
            val text = "WisdomOne"
            val logoSize = w * 0.055f
            val gap = w * 0.015f
            val total = logoSize + gap + markPaint.measureText(text)
            val startX = (w - total) / 2f
            val cy = h * z.mark
            val logo = logoBitmap(context)
            if (logo != null) {
                val clip = Path().apply { addCircle(startX + logoSize / 2, cy - logoSize * 0.35f, logoSize / 2, Path.Direction.CW) }
                canvas.save(); canvas.clipPath(clip)
                val lp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.alpha = a255 }
                canvas.drawBitmap(logo, null, android.graphics.RectF(startX, cy - logoSize * 0.85f, startX + logoSize, cy + logoSize * 0.15f), lp)
                canvas.restore()
            }
            canvas.drawText(text, startX + logoSize + gap, cy, markPaint)
        }
    }

    @Volatile private var logoCache: Bitmap? = null
    private fun logoBitmap(context: Context): Bitmap? = logoCache ?: BitmapFactory.decodeResource(context.resources, R.drawable.ic_wisdomone_ui_logo)?.also { logoCache = it }

    private fun drawFitted(canvas: Canvas, text: String, paint: TextPaint, cx: Float, y: Float, maxWidth: Float) {
        var size = paint.textSize
        while (paint.measureText(text) > maxWidth && size > 8f) { size *= 0.95f; paint.textSize = size }
        canvas.drawText(text, cx, y, paint)
    }
}
