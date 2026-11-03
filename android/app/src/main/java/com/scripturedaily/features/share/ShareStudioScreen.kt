package com.scripturedaily.features.share

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.R
import com.scripturedaily.data.Verse
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.Pill
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
fun FormatRow(selected: CardFormat, onSelect: (CardFormat) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CardFormat.values().toList()) { f ->
            Pill("${f.label} · ${f.hint}", selected == f, { onSelect(f) })
        }
    }
}

@Composable
fun TemplateRow(selected: CardTemplate, onSelect: (CardTemplate) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(CardTemplate.values().toList()) { t ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelect(t) }) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(t.top), Color(t.bottom))))
                        .border(BorderStroke(if (selected == t) 3.dp else 1.dp, if (selected == t) W.Gold else W.Gold.copy(alpha = 0.3f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("Aa", color = Color(t.text), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Spacer(Modifier.height(4.dp))
                Text(t.label, fontSize = 11.sp, color = if (selected == t) W.Ink else W.InkSoft, fontWeight = if (selected == t) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun PatternRow(selected: CardPattern, onSelect: (CardPattern) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CardPattern.values().toList()) { p -> Pill(p.label, selected == p, { onSelect(p) }) }
    }
}

@Composable
fun FontControls(style: CardStyle, onChange: (CardStyle) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CardFont.values().toList()) { f -> Pill(f.label, style.font == f, { onChange(style.copy(font = f)) }) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("A", fontSize = 12.sp, color = W.InkSoft)
        Slider(
            value = style.fontScale, onValueChange = { onChange(style.copy(fontScale = (Math.round(it * 20) / 20f))) },
            valueRange = 0.7f..1.5f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(thumbColor = W.Gold, activeTrackColor = W.Gold, inactiveTrackColor = W.Gold.copy(alpha = 0.25f))
        )
        Text("A", fontSize = 22.sp, color = W.Ink, fontWeight = FontWeight.Bold)
    }
    Text("Text size ${(style.fontScale * 100).toInt()}% — long verses shrink automatically so they always fit.", fontSize = 11.sp, color = W.InkSoft)
}

@Composable
fun MarkToggle(style: CardStyle, onChange: (CardStyle) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Show WisdomOne mark", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = W.Ink)
            Text("A small logo so friends can find the app. The verse reference and source always stay visible.", fontSize = 11.sp, color = W.InkSoft, lineHeight = 15.sp)
        }
        Switch(checked = style.showMark, onCheckedChange = { onChange(style.copy(showMark = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = W.Gold))
    }
}

/** Live preview of a card. Re-renders (off the main thread) whenever the style or verse changes. */
@Composable
fun CardPreview(verse: Verse, style: CardStyle, modifier: Modifier = Modifier, previewWidth: Int = 540) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(verse.id, verse.text, style) {
        bitmap = withContext(Dispatchers.Default) { CardRenderer.render(context, CardContent.from(verse), style, previewWidth) }
    }
    Box(
        modifier.aspectRatio(style.format.w.toFloat() / style.format.h).shadow(10.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        val b = bitmap
        if (b != null) Image(b.asImageBitmap(), contentDescription = "Verse card preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else CircularProgressIndicator(color = W.Gold, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
    }
}

@Composable
fun ShareStudioScreen(verse: Verse, onBack: () -> Unit, onShared: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var style by remember { mutableStateOf(StudioPrefs.load(context)) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(style) { StudioPrefs.save(context, style) }
    val caption = "“${verse.text}”\n\n— ${verse.citation()}\n\nvia WisdomOne"

    suspend fun renderFull(): Bitmap = withContext(Dispatchers.Default) { CardRenderer.render(context, CardContent.from(verse), style) }

    OverlayScreen("Share Studio", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CardPreview(verse, style, Modifier.height(430.dp))
            }
            SectionLabel("Format")
            FormatRow(style.format) { style = style.copy(format = it) }
            SectionLabel("Template")
            TemplateRow(style.template) { style = style.copy(template = it) }
            SectionLabel("Background")
            PatternRow(style.pattern) { style = style.copy(pattern = it) }
            SectionLabel("Text")
            FontControls(style) { style = it }
            SectionLabel("Branding")
            MarkToggle(style) { style = it }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { scope.launch { busy = true; ShareActions.shareImage(context, renderFull(), caption); busy = false; onShared() } },
                    enabled = !busy, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = W.Ink), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) { Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Share", fontWeight = FontWeight.SemiBold) }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            val uri = ShareActions.writeCacheImage(context, renderFull(), "wisdomone_status.png")
                            ShareActions.shareToWhatsApp(context, uri, "image/png", caption)
                            busy = false; onShared()
                        }
                    },
                    enabled = !busy, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF25D366).copy(alpha = 0.7f)), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1FA855))
                ) { Icon(painterResourceCompat(), null, Modifier.size(18.dp), tint = Color.Unspecified); Spacer(Modifier.width(6.dp)); Text("WhatsApp", fontWeight = FontWeight.SemiBold) }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            val uri = ShareActions.saveImageToGallery(context, renderFull(), "WisdomOne_${verse.id}_${System.currentTimeMillis()}")
                            Toast.makeText(context, if (uri != null) "Saved to Pictures/WisdomOne" else "Saving needs Android 10+. Use Share instead.", Toast.LENGTH_SHORT).show()
                            busy = false
                        }
                    },
                    enabled = !busy, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, W.Gold.copy(alpha = 0.6f)), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = W.Ink)
                ) { Icon(Icons.Outlined.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save", fontWeight = FontWeight.SemiBold) }
            }
            if (busy) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = W.Gold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp)); Text("Creating your card…", fontSize = 12.sp, color = W.InkSoft)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun painterResourceCompat() = androidx.compose.ui.res.painterResource(R.drawable.ic_whatsapp)
