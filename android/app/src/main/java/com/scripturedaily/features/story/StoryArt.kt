package com.scripturedaily.features.story

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.sin

/**
 * Procedural storybook illustrations. The database stores each scene as comma-separated tokens
 * (e.g. "day,trees,archer,bird"); this file turns those tokens into vector art.
 */
@Composable
fun StoryIllustration(art: String, palette: String, modifier: Modifier = Modifier) {
    val tokens = art.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) { drawScene(tokens, palette) }
    }
}

private class Sky(val top: Color, val bottom: Color, val ground: Color, val hill1: Color, val hill2: Color, val water: Color, val night: Boolean)

private fun skyFor(tokens: Set<String>, palette: String): Sky {
    val tint = when (palette) {
        "forest" -> Color(0xFF5E9E6E); "ocean" -> Color(0xFF4F8DB5); "rose" -> Color(0xFFB06A7E); "sand" -> Color(0xFFC9A05F); else -> Color(0xFF7A62A8)
    }
    return when {
        "night" in tokens -> Sky(Color(0xFF0B1030), Color(0xFF2A2F6A), Color(0xFF1E3A34), Color(0xFF1A2550), Color(0xFF14204A), Color(0xFF1B3F7A), true)
        "dusk" in tokens -> Sky(Color(0xFF5B3B7A), Color(0xFFF2A974), Color(0xFF4E7A4C), Color(0xFF6C4F86), tint.copy(alpha = 0.85f), Color(0xFF3E6FA0), false)
        "dawn" in tokens -> Sky(Color(0xFF8D6FB0), Color(0xFFFFCFA0), Color(0xFF6BA368), Color(0xFFB58BB5), tint.copy(alpha = 0.8f), Color(0xFF68A8D0), false)
        else -> Sky(Color(0xFF7EC8F0), Color(0xFFE9F7FF), Color(0xFF6BB56C), Color(0xFF8FC0A2), tint.copy(alpha = 0.7f), Color(0xFF4FA4D8), false)
    }
}

private fun DrawScope.drawScene(t: Set<String>, palette: String) {
    val w = size.width; val h = size.height
    val sky = skyFor(t, palette)
    val horizon = h * 0.64f
    drawRect(Brush.verticalGradient(listOf(sky.top, sky.bottom)), size = size)

    // sun / moon / stars
    if (sky.night) {
        for (i in 0 until 28) {
            val x = ((i * 53) % 100) / 100f * w; val y = ((i * 37) % 55) / 100f * h
            drawCircle(Color.White.copy(alpha = 0.55f + (i % 4) * 0.1f), radius = w * (0.0035f + (i % 3) * 0.002f), center = Offset(x, y))
        }
        drawCircle(Color(0xFFFFF3C4), w * 0.075f, Offset(w * 0.8f, h * 0.17f))
        drawCircle(sky.top, w * 0.06f, Offset(w * 0.83f, h * 0.15f))
    } else if ("sun" in t || "dawn" in t || "dusk" in t) {
        val low = "dawn" in t || "dusk" in t
        val c = if (low) Offset(w * 0.5f, horizon - h * 0.04f) else Offset(w * 0.8f, h * 0.18f)
        drawCircle(Brush.radialGradient(listOf(Color(0x66FFE9A8), Color.Transparent), c, w * 0.32f), w * 0.32f, c)
        drawCircle(Color(0xFFFFE08A), w * 0.075f, c)
    }
    if ("stars" in t && !sky.night) for (i in 0 until 10) drawCircle(Color.White.copy(alpha = 0.7f), w * 0.004f, Offset(((i * 61) % 100) / 100f * w, ((i * 29) % 40) / 100f * h))

    // distant mountains and hills
    if ("mountains" in t) {
        val p = Path().apply {
            moveTo(0f, horizon); lineTo(w * 0.18f, h * 0.36f); lineTo(w * 0.34f, horizon * 0.88f); lineTo(w * 0.52f, h * 0.30f)
            lineTo(w * 0.72f, horizon * 0.86f); lineTo(w * 0.88f, h * 0.38f); lineTo(w, horizon); close()
        }
        drawPath(p, sky.hill1.copy(alpha = 0.9f))
    }
    hillBand(sky.hill1, horizon - h * 0.07f, h * 0.07f, 0.8f, 0.0)
    hillBand(sky.hill2, horizon - h * 0.03f, h * 0.05f, 1.3f, 1.4)

    // ground
    drawRect(sky.ground, Offset(0f, horizon), Size(w, h - horizon))
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x22000000)), startY = horizon, endY = h), Offset(0f, horizon), Size(w, h - horizon))

    // water
    val hasWater = "river" in t || "sea" in t || "lake" in t
    if (hasWater) {
        val top = if ("sea" in t) horizon - h * 0.02f else horizon + h * 0.05f
        val bottom = if ("sea" in t) h * 0.78f else horizon + h * 0.2f
        drawRect(sky.water, Offset(0f, top), Size(w, bottom - top))
        for (i in 0 until 5) {
            val y = top + (bottom - top) * (0.15f + i * 0.17f)
            val p = Path().apply { moveTo(w * (0.05f + i * 0.17f), y); quadraticTo(w * (0.12f + i * 0.17f), y - h * 0.012f, w * (0.2f + i * 0.17f), y) }
            drawPath(p, Color.White.copy(alpha = 0.35f), style = Stroke(width = w * 0.004f))
        }
    }

    // road
    if ("road" in t) {
        val p = Path().apply { moveTo(w * 0.42f, horizon); lineTo(w * 0.58f, horizon); lineTo(w * 0.95f, h); lineTo(w * 0.05f, h); close() }
        drawPath(p, Color(0xFFC9A877))
        for (i in 0 until 4) drawRect(Color.White.copy(alpha = 0.5f), Offset(w * 0.49f, horizon + (h - horizon) * (0.1f + i * 0.24f)), Size(w * 0.02f, h * 0.03f))
    }

    // buildings and landmarks
    if ("hut" in t) hut(w * 0.8f, horizon + h * 0.12f, w * 0.17f)
    if ("palace" in t) palace(w * 0.5f, horizon + h * 0.08f, w * 0.5f)
    if ("well" in t) well(w * 0.2f, horizon + h * 0.14f, w * 0.18f)
    if ("trees" in t) { tree(w * 0.1f, horizon + h * 0.12f, w * 0.2f, sky); tree(w * 0.9f, horizon + h * 0.1f, w * 0.17f, sky); tree(w * 0.27f, horizon + h * 0.06f, w * 0.11f, sky) }
    if ("bridge" in t) bridge(w, horizon + h * 0.1f)

    // characters
    val ground = horizon + h * 0.25f
    if ("elephant" in t) elephant(w * 0.62f, ground, w * 0.3f)
    if ("giant" in t) person(w * 0.74f, ground, w * 0.2f, Color(0xFF6B4A3A), Color(0xFFE0B48A), armor = true)
    if ("yaksha" in t) person(w * 0.74f, ground - h * 0.02f, w * 0.15f, Color(0xFF2E2145), Color(0xFF5B4B7A), eyes = true)
    if ("archer" in t) { person(w * 0.28f, ground, w * 0.1f, Color(0xFF3F6BB0), Color(0xFFE8B98E)); bow(w * 0.36f, ground - w * 0.13f, w * 0.1f) }
    if ("traveller" in t) { person(w * 0.38f, ground, w * 0.1f, Color(0xFFB5543C), Color(0xFFE8B98E)); staff(w * 0.45f, ground, w * 0.15f) }
    if ("helper" in t) person(w * 0.62f, ground + h * 0.01f, w * 0.1f, Color(0xFF3F8E6E), Color(0xFFE0B48A))
    if ("boy" in t) person(w * 0.3f, ground, w * 0.07f, Color(0xFFD08A3E), Color(0xFFE8B98E))
    if ("kids" in t) { person(w * 0.22f, ground, w * 0.07f, Color(0xFFD16B8F), Color(0xFFE8B98E)); person(w * 0.34f, ground + h * 0.01f, w * 0.07f, Color(0xFF4F8DB5), Color(0xFFD9A57C)); person(w * 0.46f, ground, w * 0.07f, Color(0xFFE0B040), Color(0xFFE8B98E)) }
    if ("monkeys" in t) for (i in 0 until 5) monkey(w * (0.2f + i * 0.14f), if ("bridge" in t) horizon + h * 0.075f else ground + (i % 2) * h * 0.03f, w * 0.045f)
    if ("bird" in t) bird(w * 0.72f, h * 0.3f, w * 0.05f)

    // v2 scenery and animals
    if ("rain" in t) for (i in 0 until 40) { val x = ((i * 47) % 100) / 100f * w; val y = ((i * 31) % 70) / 100f * h; drawLine(Color(0xAA9FC5E8), Offset(x, y), Offset(x - w * 0.012f, y + h * 0.04f), w * 0.004f) }
    if ("cave" in t) { val c = Path().apply { moveTo(w * 0.05f, horizon + h * 0.2f); quadraticTo(w * 0.05f, horizon - h * 0.2f, w * 0.3f, horizon - h * 0.2f); quadraticTo(w * 0.55f, horizon - h * 0.2f, w * 0.55f, horizon + h * 0.2f); close() }; drawPath(c, Color(0xFF3C3550)); drawOval(Color(0xFF14101F), Offset(w * 0.14f, horizon - h * 0.04f), Size(w * 0.32f, h * 0.24f)) }
    if ("dome" in t) dome(w * 0.5f, horizon + h * 0.12f, w * 0.55f)
    if ("boat" in t) boat(w * 0.5f, horizon + h * 0.1f, w * 0.4f)
    if ("rainbow" in t) for ((i, c) in listOf(Color(0xFFE5534B), Color(0xFFF2A33A), Color(0xFFF7DE5B), Color(0xFF5BB974), Color(0xFF4F9FE0), Color(0xFF8A62C9)).withIndex()) {
        drawArc(c.copy(alpha = 0.8f), 180f, 180f, false, Offset(w * (0.12f + i * 0.02f), h * (0.12f + i * 0.02f)), Size(w * (0.76f - i * 0.04f), w * (0.76f - i * 0.04f)), style = Stroke(w * 0.022f)) }
    if ("fire" in t) flame(w * 0.7f, ground0(horizon, h), w * 0.16f, true)
    if ("lamp" in t) flame(w * 0.2f, horizon + h * 0.14f, w * 0.07f, false)
    if ("fish" in t) fish(w * 0.5f, horizon + h * 0.14f, w * 0.34f)
    if ("lion" in t) animal(w * 0.7f, horizon + h * 0.25f, w * 0.26f, Color(0xFFD9A441), mane = Color(0xFF8B5A22))
    if ("deer" in t) animal(w * 0.7f, horizon + h * 0.25f, w * 0.24f, Color(0xFFB07A45), antlers = true)
    if ("hare" in t) animal(w * 0.74f, horizon + h * 0.25f, w * 0.2f, Color(0xFFEFE8DC), ears = true)
    if ("tortoise" in t) animal(w * 0.34f, horizon + h * 0.26f, w * 0.2f, Color(0xFF5E9E5A), shell = true)
    if ("ant" in t) for (i in 0 until 5) animal(w * (0.15f + i * 0.17f), horizon + h * (0.27f + (i % 2) * 0.03f), w * 0.06f, Color(0xFF3A2A22))

    // flourishes
    if ("hearts" in t) for (i in 0 until 5) heart(w * (0.2f + i * 0.15f), h * (0.28f - (i % 3) * 0.05f), w * (0.028f + (i % 2) * 0.01f), Color(0xFFFF8FAB).copy(alpha = 0.85f))
    if ("flowers" in t) for (i in 0 until 8) { val x = w * (0.06f + i * 0.12f); val y = h * (0.88f + (i % 3) * 0.03f); drawCircle(Color(0xFFFFE066), w * 0.012f, Offset(x, y)); drawCircle(Color(0xFFFF8FAB), w * 0.007f, Offset(x, y)) }
}

private fun DrawScope.hillBand(color: Color, top: Float, amp: Float, freq: Float, phase: Double) {
    val p = Path().apply {
        moveTo(0f, size.height); lineTo(0f, top)
        var x = 0f
        while (x <= size.width) { lineTo(x, top + (sin(x / size.width * 2 * PI * freq + phase) * amp).toFloat()); x += size.width / 40f }
        lineTo(size.width, top); lineTo(size.width, size.height); close()
    }
    drawPath(p, color)
}

private fun DrawScope.tree(x: Float, baseY: Float, s: Float, sky: Sky) {
    drawRect(Color(0xFF6B4A32), Offset(x - s * 0.05f, baseY - s * 0.55f), Size(s * 0.1f, s * 0.55f))
    val leaf = if (sky.night) Color(0xFF1F5A44) else Color(0xFF3F8F55)
    drawCircle(leaf, s * 0.32f, Offset(x, baseY - s * 0.72f))
    drawCircle(leaf.copy(alpha = 0.9f), s * 0.24f, Offset(x - s * 0.22f, baseY - s * 0.58f))
    drawCircle(leaf.copy(alpha = 0.9f), s * 0.24f, Offset(x + s * 0.22f, baseY - s * 0.58f))
    drawCircle(Color(0x22FFFFFF), s * 0.14f, Offset(x - s * 0.08f, baseY - s * 0.8f))
}

private fun DrawScope.hut(x: Float, baseY: Float, s: Float) {
    drawRect(Color(0xFFD9B98C), Offset(x - s * 0.5f, baseY - s * 0.6f), Size(s, s * 0.6f))
    val roof = Path().apply { moveTo(x - s * 0.62f, baseY - s * 0.6f); lineTo(x, baseY - s * 1.1f); lineTo(x + s * 0.62f, baseY - s * 0.6f); close() }
    drawPath(roof, Color(0xFF9C463D))
    drawRect(Color(0xFF6B4A32), Offset(x - s * 0.1f, baseY - s * 0.34f), Size(s * 0.2f, s * 0.34f))
    drawRect(Color(0xFFFFE9A0), Offset(x + s * 0.2f, baseY - s * 0.46f), Size(s * 0.16f, s * 0.14f))
}

private fun DrawScope.palace(x: Float, baseY: Float, s: Float) {
    drawRect(Color(0xFFEBD9B4), Offset(x - s * 0.5f, baseY - s * 0.32f), Size(s, s * 0.32f))
    drawRect(Color(0xFFF4E8C9), Offset(x - s * 0.15f, baseY - s * 0.5f), Size(s * 0.3f, s * 0.5f))
    drawCircle(Color(0xFFD4A574), s * 0.15f, Offset(x, baseY - s * 0.5f))
    drawRect(Color(0xFFD4A574), Offset(x - s * 0.01f, baseY - s * 0.72f), Size(s * 0.02f, s * 0.08f))
    for (i in -2..2) if (i != 0) drawRect(Color(0xFF8A6A48), Offset(x + i * s * 0.18f - s * 0.025f, baseY - s * 0.22f), Size(s * 0.05f, s * 0.12f))
    drawRect(Color(0xFF6B4A32), Offset(x - s * 0.05f, baseY - s * 0.2f), Size(s * 0.1f, s * 0.2f))
}

private fun DrawScope.well(x: Float, baseY: Float, s: Float) {
    drawRect(Color(0xFF8C8C96), Offset(x - s * 0.5f, baseY - s * 0.45f), Size(s, s * 0.45f))
    drawRect(Color(0xFF3A3A48), Offset(x - s * 0.38f, baseY - s * 0.5f), Size(s * 0.76f, s * 0.12f))
    drawRect(Color(0xFF6B4A32), Offset(x - s * 0.5f, baseY - s * 0.95f), Size(s * 0.06f, s * 0.5f))
    drawRect(Color(0xFF6B4A32), Offset(x + s * 0.44f, baseY - s * 0.95f), Size(s * 0.06f, s * 0.5f))
    drawRect(Color(0xFF6B4A32), Offset(x - s * 0.55f, baseY - s * 1.0f), Size(s * 1.1f, s * 0.07f))
}

private fun DrawScope.bridge(w: Float, y: Float) {
    val p = Path().apply { moveTo(w * 0.03f, y + w * 0.02f); quadraticTo(w * 0.5f, y - w * 0.12f, w * 0.97f, y + w * 0.02f) }
    drawPath(p, Color(0xFF8B5A2B), style = Stroke(width = w * 0.03f))
}

private fun DrawScope.person(x: Float, baseY: Float, s: Float, robe: Color, skin: Color, armor: Boolean = false, eyes: Boolean = false) {
    val body = Path().apply {
        moveTo(x - s * 0.34f, baseY); lineTo(x - s * 0.2f, baseY - s * 0.95f); lineTo(x + s * 0.2f, baseY - s * 0.95f); lineTo(x + s * 0.34f, baseY); close()
    }
    drawPath(body, robe)
    if (armor) drawRect(Color(0x55FFFFFF), Offset(x - s * 0.2f, baseY - s * 0.75f), Size(s * 0.4f, s * 0.12f))
    drawCircle(skin, s * 0.2f, Offset(x, baseY - s * 1.12f))
    drawCircle(Color(0xFF2A1B14), s * 0.2f, Offset(x, baseY - s * 1.2f), style = androidx.compose.ui.graphics.drawscope.Fill)
    drawCircle(skin, s * 0.18f, Offset(x, baseY - s * 1.1f))
    drawCircle(Color(0xFF2A1B14), s * 0.022f, Offset(x - s * 0.07f, baseY - s * 1.12f))
    drawCircle(Color(0xFF2A1B14), s * 0.022f, Offset(x + s * 0.07f, baseY - s * 1.12f))
    if (eyes) { drawCircle(Color(0xFFFFE066), s * 0.03f, Offset(x - s * 0.07f, baseY - s * 1.12f)); drawCircle(Color(0xFFFFE066), s * 0.03f, Offset(x + s * 0.07f, baseY - s * 1.12f)) }
}

private fun DrawScope.bow(x: Float, y: Float, s: Float) {
    val arc = Path().apply { moveTo(x, y - s); quadraticTo(x + s * 0.8f, y, x, y + s) }
    drawPath(arc, Color(0xFF6B4A32), style = Stroke(width = s * 0.1f))
    drawLine(Color.White.copy(alpha = 0.8f), Offset(x, y - s), Offset(x, y + s), strokeWidth = s * 0.03f)
    drawLine(Color(0xFFDDDDDD), Offset(x - s * 0.5f, y), Offset(x + s * 1.1f, y), strokeWidth = s * 0.05f)
}

private fun DrawScope.staff(x: Float, baseY: Float, s: Float) {
    drawLine(Color(0xFF6B4A32), Offset(x, baseY), Offset(x - s * 0.1f, baseY - s * 1.2f), strokeWidth = s * 0.07f)
}

private fun DrawScope.monkey(x: Float, y: Float, s: Float) {
    val fur = Color(0xFF9A6B3F)
    drawCircle(fur, s * 0.9f, Offset(x, y))
    drawCircle(fur, s * 0.62f, Offset(x, y - s * 1.05f))
    drawCircle(fur, s * 0.28f, Offset(x - s * 0.62f, y - s * 1.15f)); drawCircle(fur, s * 0.28f, Offset(x + s * 0.62f, y - s * 1.15f))
    drawCircle(Color(0xFFE8C9A0), s * 0.36f, Offset(x, y - s * 0.95f))
    drawCircle(Color(0xFF2A1B14), s * 0.07f, Offset(x - s * 0.17f, y - s * 1.08f)); drawCircle(Color(0xFF2A1B14), s * 0.07f, Offset(x + s * 0.17f, y - s * 1.08f))
    val tail = Path().apply { moveTo(x + s * 0.8f, y); quadraticTo(x + s * 1.7f, y - s * 0.4f, x + s * 1.3f, y - s * 1.2f) }
    drawPath(tail, fur, style = Stroke(width = s * 0.25f))
}

private fun DrawScope.elephant(x: Float, baseY: Float, s: Float) {
    val gray = Color(0xFF8F94A8)
    drawOval(gray, Offset(x - s * 0.5f, baseY - s * 0.75f), Size(s * 0.9f, s * 0.6f))
    for (dx in listOf(-0.4f, -0.15f, 0.15f, 0.35f)) drawRect(Color(0xFF7A7F94), Offset(x + dx * s - s * 0.06f, baseY - s * 0.3f), Size(s * 0.12f, s * 0.3f))
    drawCircle(gray, s * 0.24f, Offset(x + s * 0.5f, baseY - s * 0.7f))
    val trunk = Path().apply { moveTo(x + s * 0.64f, baseY - s * 0.66f); quadraticTo(x + s * 0.9f, baseY - s * 0.3f, x + s * 0.72f, baseY - s * 0.12f) }
    drawPath(trunk, gray, style = Stroke(width = s * 0.12f))
    drawOval(Color(0xFFA6ABBE), Offset(x + s * 0.28f, baseY - s * 0.85f), Size(s * 0.28f, s * 0.36f))
    drawCircle(Color(0xFF2A1B14), s * 0.025f, Offset(x + s * 0.57f, baseY - s * 0.74f))
}

private fun DrawScope.bird(x: Float, y: Float, s: Float) {
    drawCircle(Color(0xFFC9803E), s * 0.45f, Offset(x, y))
    drawCircle(Color(0xFFC9803E), s * 0.3f, Offset(x + s * 0.5f, y - s * 0.25f))
    drawCircle(Color.White, s * 0.1f, Offset(x + s * 0.58f, y - s * 0.3f)); drawCircle(Color(0xFF2A1B14), s * 0.05f, Offset(x + s * 0.6f, y - s * 0.3f))
    val beak = Path().apply { moveTo(x + s * 0.76f, y - s * 0.25f); lineTo(x + s * 1.0f, y - s * 0.18f); lineTo(x + s * 0.76f, y - s * 0.12f); close() }
    drawPath(beak, Color(0xFFFFC857))
    val wing = Path().apply { moveTo(x - s * 0.1f, y); quadraticTo(x - s * 0.5f, y - s * 0.6f, x - s * 0.9f, y - s * 0.2f); quadraticTo(x - s * 0.5f, y + s * 0.1f, x - s * 0.1f, y) }
    drawPath(wing, Color(0xFFA9652A))
}

private fun ground0(horizon: Float, h: Float) = horizon + h * 0.25f

private fun DrawScope.dome(x: Float, baseY: Float, s: Float) {
    val body = Color(0xFFE9DFC7)
    drawRect(body, Offset(x - s * 0.3f, baseY - s * 0.25f), Size(s * 0.6f, s * 0.25f))
    drawArc(Color(0xFF4FA3A5), 180f, 180f, true, Offset(x - s * 0.3f, baseY - s * 0.55f), Size(s * 0.6f, s * 0.6f))
    drawLine(Color(0xFFE5B84B), Offset(x, baseY - s * 0.55f), Offset(x, baseY - s * 0.7f), s * 0.015f); drawCircle(Color(0xFFE5B84B), s * 0.025f, Offset(x, baseY - s * 0.72f))
    for (dx in listOf(-0.42f, 0.42f)) { drawRect(body, Offset(x + dx * s - s * 0.04f, baseY - s * 0.62f), Size(s * 0.08f, s * 0.62f)); drawCircle(Color(0xFF4FA3A5), s * 0.05f, Offset(x + dx * s, baseY - s * 0.64f)) }
    drawRoundRect(Color(0xFF8A6A3C), Offset(x - s * 0.06f, baseY - s * 0.14f), Size(s * 0.12f, s * 0.14f), androidx.compose.ui.geometry.CornerRadius(s * 0.06f))
}

private fun DrawScope.boat(x: Float, baseY: Float, s: Float) {
    val hull = Path().apply { moveTo(x - s * 0.5f, baseY - s * 0.12f); lineTo(x + s * 0.5f, baseY - s * 0.12f); quadraticTo(x + s * 0.4f, baseY + s * 0.1f, x + s * 0.2f, baseY + s * 0.1f); lineTo(x - s * 0.2f, baseY + s * 0.1f); quadraticTo(x - s * 0.4f, baseY + s * 0.1f, x - s * 0.5f, baseY - s * 0.12f); close() }
    drawPath(hull, Color(0xFF8A5A33))
    drawRect(Color(0xFFB98652), Offset(x - s * 0.3f, baseY - s * 0.32f), Size(s * 0.6f, s * 0.2f))
    val roof = Path().apply { moveTo(x - s * 0.36f, baseY - s * 0.32f); lineTo(x, baseY - s * 0.5f); lineTo(x + s * 0.36f, baseY - s * 0.32f); close() }
    drawPath(roof, Color(0xFF6B4528))
    for (i in 0 until 3) drawCircle(Color(0xFFFFF3C4), s * 0.03f, Offset(x - s * 0.15f + i * s * 0.15f, baseY - s * 0.22f))
}

private fun DrawScope.flame(x: Float, baseY: Float, s: Float, large: Boolean) {
    if (large) for (i in -1..1) { val p = Path().apply { moveTo(x + i * s * 0.5f, baseY); quadraticTo(x + i * s * 0.5f - s * 0.4f, baseY - s * 0.7f, x + i * s * 0.5f, baseY - s * 1.4f + abs(i) * s * 0.4f); quadraticTo(x + i * s * 0.5f + s * 0.4f, baseY - s * 0.7f, x + i * s * 0.5f, baseY) }; drawPath(p, Color(0xFFF2A33A)); drawPath(p, Color(0xFFFFE08A).copy(alpha = 0.6f), style = Stroke(s * 0.1f)) }
    else { drawOval(Color(0xFFB98652), Offset(x - s, baseY), Size(s * 2f, s * 0.8f)); val p = Path().apply { moveTo(x, baseY - s * 1.2f); quadraticTo(x + s * 0.5f, baseY - s * 0.5f, x, baseY); quadraticTo(x - s * 0.5f, baseY - s * 0.5f, x, baseY - s * 1.2f) }; drawPath(p, Color(0xFFFFC857)); drawCircle(Color(0x44FFE08A), s * 1.6f, Offset(x, baseY - s * 0.5f)) }
}

private fun DrawScope.fish(x: Float, baseY: Float, s: Float) {
    val c = Color(0xFF4F6F95)
    drawOval(c, Offset(x - s * 0.5f, baseY - s * 0.28f), Size(s * 0.8f, s * 0.5f))
    val tail = Path().apply { moveTo(x + s * 0.25f, baseY - s * 0.03f); lineTo(x + s * 0.55f, baseY - s * 0.3f); lineTo(x + s * 0.55f, baseY + s * 0.24f); close() }
    drawPath(tail, c); drawCircle(Color.White, s * 0.04f, Offset(x - s * 0.33f, baseY - s * 0.1f)); drawCircle(Color(0xFF2A1B14), s * 0.02f, Offset(x - s * 0.33f, baseY - s * 0.1f))
}

/** One friendly four-legged animal shape, dressed up by flags (mane, antlers, ears, shell). */
private fun DrawScope.animal(x: Float, baseY: Float, s: Float, color: Color, mane: Color? = null, antlers: Boolean = false, ears: Boolean = false, shell: Boolean = false) {
    for (dx in listOf(-0.3f, -0.1f, 0.15f, 0.3f)) drawRect(color.copy(alpha = 0.85f), Offset(x + dx * s - s * 0.04f, baseY - s * 0.25f), Size(s * 0.08f, s * 0.25f))
    drawOval(color, Offset(x - s * 0.4f, baseY - s * 0.55f), Size(s * 0.8f, s * 0.38f))
    if (shell) drawArc(Color(0xFF3F7A45), 180f, 180f, true, Offset(x - s * 0.36f, baseY - s * 0.62f), Size(s * 0.72f, s * 0.5f))
    val head = Offset(x + s * 0.45f, baseY - s * 0.5f)
    if (mane != null) drawCircle(mane, s * 0.3f, head)
    drawCircle(color, s * 0.2f, head)
    if (ears) for (dx in listOf(-0.05f, 0.08f)) drawOval(color, Offset(head.x + dx * s - s * 0.04f, head.y - s * 0.55f), Size(s * 0.08f, s * 0.38f))
    if (antlers) for (dx in listOf(-0.06f, 0.08f)) { drawLine(Color(0xFF6B4528), Offset(head.x + dx * s, head.y - s * 0.15f), Offset(head.x + dx * s * 2f, head.y - s * 0.5f), s * 0.03f); drawLine(Color(0xFF6B4528), Offset(head.x + dx * s * 1.5f, head.y - s * 0.33f), Offset(head.x + dx * s * 3f, head.y - s * 0.4f), s * 0.025f) }
    drawCircle(Color(0xFF2A1B14), s * 0.025f, Offset(head.x + s * 0.07f, head.y - s * 0.03f))
}

private fun DrawScope.heart(x: Float, y: Float, s: Float, color: Color) {
    val p = Path().apply {
        moveTo(x, y + s * 0.9f)
        cubicTo(x - s * 2.2f, y - s * 0.4f, x - s * 0.6f, y - s * 1.6f, x, y - s * 0.5f)
        cubicTo(x + s * 0.6f, y - s * 1.6f, x + s * 2.2f, y - s * 0.4f, x, y + s * 0.9f)
        close()
    }
    drawPath(p, color)
}

@Suppress("unused") private fun unitCircle(i: Int, n: Int): Offset = Offset(cos(2 * PI * i / n).toFloat(), sin(2 * PI * i / n).toFloat())
