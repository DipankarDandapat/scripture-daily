package com.scripturedaily.features.truths

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.TruthCard
import com.scripturedaily.data.TruthTopic
import com.scripturedaily.data.UniversalTruth
import com.scripturedaily.features.share.ShareActions
import com.scripturedaily.ui.EmptyBox
import com.scripturedaily.ui.ErrorBox
import com.scripturedaily.ui.LoadingBox
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.Pill
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun traditionColor(code: String): Color = when (code) {
    "hinduism" -> Color(0xFFE8833A); "christianity" -> Color(0xFF4A7BC8); "islam" -> Color(0xFF2E8B57)
    "buddhism" -> Color(0xFFC9A227); else -> Color(0xFF8A5CC8)
}

@Composable
fun UniversalTruthsScreen(languageId: Int, preferredReligionCode: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var topics by remember { mutableStateOf<List<TruthTopic>>(emptyList()) }
    var topicError by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    var seed by remember { mutableIntStateOf(0) }
    var truth by remember { mutableStateOf<UniversalTruth?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var left by remember { mutableStateOf<String?>(null) }
    var right by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        topicError = null
        try {
            topics = withContext(Dispatchers.IO) { ApiClient.api.truthTopics() }
            if (selected == null) selected = topics.firstOrNull { it.code == "love" }?.code ?: topics.firstOrNull()?.code
        } catch (_: Exception) { topicError = "Couldn't load topics. Check your connection." }
    }
    LaunchedEffect(selected, seed, languageId, reload) {
        val code = selected ?: return@LaunchedEffect
        loading = true; error = null
        try {
            val t = withContext(Dispatchers.IO) { ApiClient.api.universalTruth(code, languageId, seed) }
            truth = t
            val codes = t.cards.map { it.religionCode }
            if (left == null || left !in codes) left = preferredReligionCode?.takeIf { it in codes } ?: codes.first()
            if (right == null || right !in codes || right == left) right = codes.firstOrNull { it != left }
        } catch (_: Exception) { error = "Couldn't load this comparison. Try again." }
        loading = false
    }

    OverlayScreen("Universal Truths", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Many paths, shared wisdom. See how different traditions speak about the same virtue.", fontSize = 13.sp, color = W.InkSoft, lineHeight = 19.sp, modifier = Modifier.padding(horizontal = 20.dp))
            topicError?.let { ErrorBox(it, { reload++ }) }
            if (topics.isEmpty() && topicError == null) LoadingBox("Loading topics…")
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(topics) { t -> Pill("${t.icon} ${t.name}", selected == t.code, { selected = t.code; seed = 0 }) }
            }
            val t = truth
            when {
                loading && t == null -> LoadingBox("Finding verses…")
                error != null -> ErrorBox(error!!, { reload++ })
                t == null -> EmptyBox("Choose a topic to compare traditions.")
                else -> {
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${t.topic.icon} ${t.topic.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = W.Ink)
                        Text(t.topic.description, fontSize = 13.sp, color = W.InkSoft)
                        val a = t.cards.firstOrNull { it.religionCode == left }
                        val b = t.cards.firstOrNull { it.religionCode == right }
                        Text("Compare", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
                        Text("First tradition", fontSize = 11.sp, color = W.InkSoft)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(t.cards) { c -> Pill(c.religion, c.religionCode == left, { if (c.religionCode == right) right = left; left = c.religionCode }, accent = traditionColor(c.religionCode)) }
                        }
                        Text("Second tradition", fontSize = 11.sp, color = W.InkSoft)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(t.cards) { c -> Pill(c.religion, c.religionCode == right, { if (c.religionCode == left) left = right; right = c.religionCode }, accent = traditionColor(c.religionCode)) }
                        }
                        if (a != null && b != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                TruthColumn(a, Modifier.weight(1f)); TruthColumn(b, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { seed++ }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.Outlined.Shuffle, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Shuffle", fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(onClick = {
                                    val text = "${t.topic.icon} ${t.topic.name} across traditions\n\n“${a.verse.text}”\n— ${a.religion}, ${a.verse.scripture} · ${a.verse.reference()}\n\n“${b.verse.text}”\n— ${b.religion}, ${b.verse.scripture} · ${b.verse.reference()}\n\nvia WisdomOne"
                                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share comparison"))
                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.AutoMirrored.Outlined.TextSnippet, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Text", fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        val bmp = withContext(Dispatchers.Default) { ComparisonCard.render(context, t.topic.name, a, b) }
                                        ShareActions.shareImage(context, bmp, "${t.topic.name} across traditions — via WisdomOne")
                                    }
                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.Outlined.Image, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Image", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TruthColumn(card: TruthCard, modifier: Modifier) {
    val accent = traditionColor(card.religionCode)
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(W.Surface).border(1.5.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.clip(RoundedCornerShape(50)).background(accent.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(card.religion.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent, letterSpacing = 1.sp)
        }
        Text("“${card.verse.text}”", fontSize = 14.sp, lineHeight = 21.sp, color = W.Ink)
        Text(card.verse.scripture, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent)
        Text(card.verse.reference(), fontSize = 11.sp, color = W.InkSoft)
        card.verse.translation?.let { Text("Source: $it", fontSize = 10.sp, color = W.InkSoft, lineHeight = 14.sp) }
        if (card.languageFallback) Text("Shown in English — not yet available in your language.", fontSize = 10.sp, color = W.InkSoft)
    }
}


/** A shareable 4:5 image with two verses side by side. */
private object ComparisonCard {
    fun render(context: Context, topic: String, a: TruthCard, b: TruthCard): Bitmap {
        val w = 1080; val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFF0F0A1A.toInt(), 0xFF2D1B4E.toInt(), Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)
        val gold = 0xFFD4A574.toInt()
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = gold; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 42f; letterSpacing = 0.18f; textAlign = Paint.Align.CENTER }
        c.drawText("UNIVERSAL TRUTHS", w / 2f, 110f, title)
        val sub = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF5F0E8.toInt(); typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); textSize = 70f; textAlign = Paint.Align.CENTER }
        c.drawText(topic, w / 2f, 200f, sub)
        fun panel(card: TruthCard, left: Float, accent: Int) {
            val pw = (w - 3 * 40f) / 2f
            val box = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF }
            c.drawRoundRect(left, 260f, left + pw, h - 190f, 36f, 36f, box)
            val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 30f; letterSpacing = 0.12f }
            c.drawText(card.religion.uppercase(), left + 30f, 320f, label)
            val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF5F0E8.toInt(); typeface = Typeface.SERIF }
            var size = 40f
            var layout: StaticLayout
            val text = "“${card.verse.text}”"
            while (true) {
                body.textSize = size
                layout = StaticLayout.Builder.obtain(text, 0, text.length, body, (pw - 60f).toInt()).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(size * 0.2f, 1f).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).build()
                if (layout.height <= h - 190f - 350f - 140f || size <= 20f) break
                size -= 2f
            }
            c.save(); c.translate(left + 30f, 350f); layout.draw(c); c.restore()
            val ref = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 26f }
            val refLayout = StaticLayout.Builder.obtain("${card.verse.scripture}\n${card.verse.reference()}", 0, "${card.verse.scripture}\n${card.verse.reference()}".length, ref, (pw - 60f).toInt()).build()
            c.save(); c.translate(left + 30f, h - 190f - 40f - refLayout.height); refLayout.draw(c); c.restore()
        }
        panel(a, 40f, traditionColor(a.religionCode).toArgb())
        panel(b, 40f + (w - 120f) / 2f + 40f, traditionColor(b.religionCode).toArgb())
        val mark = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = gold; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 46f; letterSpacing = 0.1f; textAlign = Paint.Align.CENTER }
        c.drawText("WisdomOne", w / 2f, h - 80f, mark)
        return bmp
    }

    private fun Color.toArgb(): Int = android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
}
