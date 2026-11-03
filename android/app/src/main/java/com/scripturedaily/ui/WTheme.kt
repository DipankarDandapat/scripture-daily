package com.scripturedaily.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared WisdomOne palette used by the feature screens (mirrors the colours in MainActivity). */
object W {
    val Purple = Color(0xFF3D2060)
    val PurpleDark = Color(0xFF2D1B4E)
    val PurpleMid = Color(0xFF5C3D8F)
    val Gold = Color(0xFFD4A574)
    val GoldLight = Color(0xFFE8C49A)
    val Parchment = Color(0xFFF7F2EE)
    val Ink = Color(0xFF1A1025)
    val InkMid = Color(0xFF4A3D5C)
    val InkSoft = Color(0xFF8A7A9A)
    val Error = Color(0xFF9C463D)
    val Success = Color(0xFF3F7D5A)
}

/** Full-screen overlay with a back arrow, title and system-bar insets. Handles the system back gesture. */
@Composable
fun OverlayScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = W.Parchment,
    contentColor: Color = W.Ink,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onBack)
    Box(modifier.fillMaxSize().background(background).clickable(enabled = false) {}, contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = 720.dp).windowInsetsPadding(WindowInsets.statusBars)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = contentColor) }
                Spacer(Modifier.width(4.dp))
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = contentColor, modifier = Modifier.weight(1f))
                actions()
            }
            Column(Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars), content = content)
        }
    }
}

@Composable
fun LoadingBox(message: String = "Loading…", modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = W.Gold, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            Text(message, color = W.InkSoft, fontSize = 13.sp)
        }
    }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(20.dp)).background(W.Error.copy(alpha = 0.08f)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(message, color = W.Error, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (onRetry != null) TextButton(onClick = onRetry) { Text("Try again", color = W.Error, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun EmptyBox(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(message, color = W.InkSoft, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** Soft tinted pill used for tags and filters. */
@Composable
fun Pill(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier, accent: Color = W.Gold) {
    val bg = if (selected) accent else accent.copy(alpha = 0.10f)
    val fg = if (selected) Color.White else W.InkMid
    Box(
        modifier.clip(RoundedCornerShape(50)).background(bg)
            .border(BorderStroke(1.dp, accent.copy(alpha = if (selected) 1f else 0.3f)), RoundedCornerShape(50))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = fg, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium) }
}

val CardShape = RoundedCornerShape(20.dp)
val ScreenPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(CardShape).background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, W.Gold.copy(alpha = 0.18f)), CardShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        content = content
    )
}
