package com.scripturedaily.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.data.Verse
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The Home daily-verse card, styled by [theme] (one of ten, rotating by calendar day). */
@Composable
fun ThemedVerseCard(
    verse: Verse,
    theme: VerseTheme,
    fontSize: Int,
    modifier: Modifier = Modifier,
    showScripture: Boolean = true,
    showReference: Boolean = true,
    showTranslation: Boolean = true,
    showBranding: Boolean = true
) {
    val text = Color(theme.text); val ref = Color(theme.reference); val accent = Color(theme.accent)
    val light = VerseThemes.contrast(theme.top, 0xFF000000) > 8.0
    val family = if (theme.serif) FontFamily.Serif else FontFamily.Default
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .semantics { contentDescription = "Daily verse, ${theme.name} theme" }
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(listOf(Color(theme.top), Color(theme.bottom)), Offset.Zero, Offset(size.width * 0.6f, size.height)))
            drawArt(theme.art, accent, Color(theme.text))
        }
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (showScripture) Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(theme.chip).let { if (theme.chip == 0L) Color.Transparent else it })
                    .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(verse.scripture.uppercase(), color = if (light) Color(theme.reference) else accent,
                        fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("“${verse.text}”", color = text, fontSize = fontSize.sp, lineHeight = (fontSize + 12).sp, fontFamily = family,
                fontWeight = FontWeight.Normal, textAlign = if (theme.serif) TextAlign.Center else TextAlign.Start,
                style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content), modifier = Modifier.fillMaxWidth())
            if (showTranslation && !verse.translation.isNullOrBlank()) {
                Text(verse.translation, color = ref.copy(alpha = 0.85f), fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
            if (showReference) {
                Box(Modifier.fillMaxWidth(0.3f).height(1.dp).align(Alignment.End).background(accent.copy(alpha = 0.6f)))
                Text(verse.reference(), color = ref, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
            if (showBranding) Text("WisdomOne", color = ref.copy(alpha = 0.72f), fontSize = 9.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Low-contrast decorative art; it never carries text, and sits behind the verse at low opacity. */
private fun DrawScope.drawArt(art: String, accent: Color, ink: Color) {
    val w = size.width; val h = size.height
    fun ring(c: Offset, r: Float, a: Float, sw: Float = 1.5f) = drawCircle(accent.copy(alpha = a), r, c, style = Stroke(sw.dp.toPx()))
    when (art) {
        "sunrise" -> {
            drawCircle(Brush.radialGradient(listOf(Color(0x66FFFFFF), Color.Transparent), Offset(w * 0.85f, h * 0.1f), w * 0.55f), w * 0.55f, Offset(w * 0.85f, h * 0.1f))
            for (i in 0 until 12) { val a = PI * (0.55 + i * 0.07); drawLine(accent.copy(alpha = 0.18f), Offset(w * 0.85f, h * 0.1f), Offset(w * 0.85f + (w * cos(a)).toFloat(), h * 0.1f + (w * sin(a)).toFloat()), 2.dp.toPx()) }
            drawCircle(accent.copy(alpha = 0.25f), w * 0.1f, Offset(w * 0.85f, h * 0.1f))
        }
        "nature" -> {
            for ((i, a) in listOf(0.16f, 0.22f).withIndex()) {
                val p = Path().apply { moveTo(0f, h); lineTo(0f, h * (0.8f - i * 0.08f)); quadraticBezierTo(w * 0.3f, h * (0.66f - i * 0.1f), w * 0.6f, h * (0.82f - i * 0.06f)); quadraticBezierTo(w * 0.85f, h * (0.9f - i * 0.1f), w, h * (0.74f - i * 0.04f)); lineTo(w, h); close() }
                drawPath(p, accent.copy(alpha = a))
            }
            for (i in 0 until 7) { val x = w * (0.08f + i * 0.14f); drawOval(accent.copy(alpha = 0.14f), Offset(x, h * 0.06f + (i % 3) * h * 0.04f), Size(w * 0.07f, w * 0.035f)) }
        }
        "minimal" -> {
            drawLine(accent.copy(alpha = 0.5f), Offset(w * 0.08f, h * 0.17f), Offset(w * 0.26f, h * 0.17f), 2.dp.toPx())
            drawLine(accent.copy(alpha = 0.3f), Offset(w * 0.74f, h * 0.9f), Offset(w * 0.92f, h * 0.9f), 2.dp.toPx())
        }
        "pastel" -> {
            drawCircle(Color(0x55FFB7D5), w * 0.3f, Offset(w * 0.95f, h * 0.05f)); drawCircle(Color(0x55B7C9FF), w * 0.26f, Offset(w * 0.05f, h * 0.95f)); drawCircle(Color(0x44FFE7A8), w * 0.14f, Offset(w * 0.2f, h * 0.15f))
        }
        "mountains" -> {
            drawCircle(Color(0x33FFFFFF), w * 0.07f, Offset(w * 0.78f, h * 0.16f))
            for ((i, a) in listOf(0.12f, 0.2f, 0.3f).withIndex()) {
                val base = h * (0.74f + i * 0.08f); val p = Path().apply { moveTo(0f, h); lineTo(0f, base) }
                var x = 0f; var up = true; while (x < w) { x += w * 0.18f; p.lineTo(x, if (up) base - h * (0.16f - i * 0.03f) else base - h * 0.04f); up = !up }
                p.lineTo(w, h); p.close(); drawPath(p, Color.White.copy(alpha = a))
            }
        }
        "floral" -> for ((x, y, r) in listOf(Triple(0.92f, 0.1f, 0.1f), Triple(0.06f, 0.92f, 0.12f), Triple(0.84f, 0.9f, 0.07f))) {
            val c = Offset(w * x, h * y)
            for (k in 0 until 6) { val a = k * PI / 3; drawCircle(accent.copy(alpha = 0.22f), w * r * 0.55f, Offset(c.x + (w * r * 0.6f * cos(a)).toFloat(), c.y + (w * r * 0.6f * sin(a)).toFloat())) }
            drawCircle(Color(0x66FFD54F), w * r * 0.3f, c)
        }
        "typography" -> {
            drawLine(accent.copy(alpha = 0.5f), Offset(w * 0.1f, h * 0.06f), Offset(w * 0.9f, h * 0.06f), 1.dp.toPx()); drawLine(accent.copy(alpha = 0.5f), Offset(w * 0.1f, h * 0.94f), Offset(w * 0.9f, h * 0.94f), 1.dp.toPx())
            drawCircle(accent.copy(alpha = 0.5f), 3.dp.toPx(), Offset(w * 0.5f, h * 0.06f)); drawCircle(accent.copy(alpha = 0.5f), 3.dp.toPx(), Offset(w * 0.5f, h * 0.94f))
        }
        "ocean" -> for (n in 0 until 4) {
            val base = h * (0.72f + n * 0.07f); val p = Path().apply { moveTo(0f, h); lineTo(0f, base) }
            var x = 0f; while (x <= w) { p.lineTo(x, base + (sin(x / w * 2 * PI + n) * h * 0.02f).toFloat()); x += w / 40f }
            p.lineTo(w, h); p.close(); drawPath(p, Color.White.copy(alpha = 0.07f + n * 0.03f))
        }
        "abstract" -> {
            val c = Offset(w * 0.88f, h * 0.12f); for (i in 1..5) ring(c, w * 0.09f * i, 0.28f - i * 0.04f)
            val c2 = Offset(w * 0.08f, h * 0.92f); for (i in 1..4) ring(c2, w * 0.08f * i, 0.2f - i * 0.03f)
        }
        "sunset" -> {
            drawCircle(Brush.radialGradient(listOf(Color(0x88FFC857), Color.Transparent), Offset(w * 0.5f, h * 1.02f), w * 0.8f), w * 0.8f, Offset(w * 0.5f, h * 1.02f))
            drawCircle(accent.copy(alpha = 0.35f), w * 0.14f, Offset(w * 0.5f, h * 1.02f))
        }
    }
}
