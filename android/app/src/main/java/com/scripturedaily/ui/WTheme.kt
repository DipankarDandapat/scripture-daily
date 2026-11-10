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
<<<<<<< HEAD
=======
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
>>>>>>> settings_redesign
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

<<<<<<< HEAD
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
=======
/** Colour roles for one theme. Light values are the original WisdomOne palette; dark values keep body text >= 4.5:1 contrast. */
@Immutable
class WPalette(
    val purple: Color, val purpleDark: Color, val purpleMid: Color, val gold: Color, val goldLight: Color,
    val parchment: Color, val ink: Color, val inkMid: Color, val inkSoft: Color,
    val error: Color, val success: Color,
    val surface: Color,      // cards / sheets
    val surfaceAlt: Color,   // tinted containers (chips, inputs)
    val onPurple: Color,     // text/icons on a purple fill
    val isDark: Boolean
)

val LightWPalette = WPalette(
    purple = Color(0xFF3D2060), purpleDark = Color(0xFF2D1B4E), purpleMid = Color(0xFF5C3D8F),
    gold = Color(0xFFD4A574), goldLight = Color(0xFFE8C49A), parchment = Color(0xFFF7F2EE),
    ink = Color(0xFF1A1025), inkMid = Color(0xFF4A3D5C), inkSoft = Color(0xFF6F5F80),
    error = Color(0xFF9C463D), success = Color(0xFF3F7D5A),
    surface = Color.White, surfaceAlt = Color(0xFFF1EAF8), onPurple = Color.White, isDark = false
)

val DarkWPalette = WPalette(
    purple = Color(0xFF8A63D2), purpleDark = Color(0xFFD7B4F0), purpleMid = Color(0xFFB594E0),
    gold = Color(0xFFE2B88A), goldLight = Color(0xFFEFCFA8), parchment = Color(0xFF15111B),
    ink = Color(0xFFF5EFF8), inkMid = Color(0xFFD8CCE3), inkSoft = Color(0xFFB3A5C4),
    error = Color(0xFFFF9A8F), success = Color(0xFF7FD1A0),
    surface = Color(0xFF241C30), surfaceAlt = Color(0xFF2E2540), onPurple = Color.White, isDark = true
)

val LocalWPalette = staticCompositionLocalOf { LightWPalette }

/** Shared WisdomOne palette. Every property follows the active (light/dark) theme, so screens never hard-code a colour. */
object W {
    val Purple: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.purple
    val PurpleDark: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.purpleDark
    val PurpleMid: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.purpleMid
    val Gold: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.gold
    val GoldLight: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.goldLight
    val Parchment: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.parchment
    val Ink: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.ink
    val InkMid: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.inkMid
    val InkSoft: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.inkSoft
    val Error: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.error
    val Success: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.success
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.surface
    val SurfaceAlt: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.surfaceAlt
    val OnPurple: Color @Composable @ReadOnlyComposable get() = LocalWPalette.current.onPurple
    /** Fill for the strong dark button used across the app: near-black in Light, brand purple in Dark (white text on both). */
    val InkButton: Color @Composable @ReadOnlyComposable get() = if (LocalWPalette.current.isDark) LocalWPalette.current.purple else LocalWPalette.current.ink
    val IsDark: Boolean @Composable @ReadOnlyComposable get() = LocalWPalette.current.isDark
}

/** Applies the WisdomOne palette and the matching Material colour scheme (used by dialogs, sheets, switches, text fields). */
@Composable
fun WisdomOneTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkWPalette else LightWPalette
    val scheme = if (dark) darkColorScheme(
        primary = p.purple, onPrimary = Color.White, secondary = p.gold, background = p.parchment, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink, surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.inkMid,
        surfaceContainer = p.surface, surfaceContainerHigh = p.surfaceAlt, surfaceContainerHighest = p.surfaceAlt,
        outline = p.inkSoft, outlineVariant = Color(0xFF4A3D5C), error = p.error, primaryContainer = Color(0xFF3D2A63), onPrimaryContainer = p.ink
    ) else lightColorScheme(
        primary = p.purple, onPrimary = Color.White, secondary = p.gold, background = p.parchment, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink, surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.inkMid,
        outline = p.inkSoft, outlineVariant = Color(0xFFD9CDE6), error = p.error
    )
    CompositionLocalProvider(LocalWPalette provides p) { MaterialTheme(colorScheme = scheme, content = content) }
>>>>>>> settings_redesign
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
