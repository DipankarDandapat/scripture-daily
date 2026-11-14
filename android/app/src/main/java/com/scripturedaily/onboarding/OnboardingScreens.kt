package com.scripturedaily.onboarding

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.outlined.Image as ImageOutlined
import androidx.compose.material.icons.outlined.Widgets as WidgetsOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.scripturedaily.R
import com.scripturedaily.widget.ScriptureWidgetReceiver
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

/** Colours taken from the WisdomOne splash / optional-setup design. */
private object OB {
    val Ink = Color(0xFF2A1156)          // headings
    val InkSoft = Color(0xFF54428A)      // body copy
    val Purple = Color(0xFF6A1FB0)       // accents / progress
    val ButtonStart = Color(0xFF4A1A82)
    val ButtonEnd = Color(0xFF5E2296)
    val Amber = Color(0xFFF59E1B)        // feature icons
    val CardBorder = Color(0xFFEBDCEE)
    val BadgeBg = Color(0xFFF6E7F1)
    val ProgressOff = Color(0xFFE8DCE6)
    val Success = Color(0xFF1F8F55)
    val BgTop = Color(0xFFF8EEF6)
    val BgMid = Color(0xFFFCF0F0)
    val BgBottom = Color(0xFFFBE6EA)
    val Serif = FontFamily.Serif
}

// ── Shared helpers ───────────────────────────────────────────────────────────

/** The designs use a light background, so keep system-bar icons dark regardless of the in-app theme. */
@Composable
private fun LightSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevStatus = controller?.isAppearanceLightStatusBars
        val prevNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            if (prevStatus != null) controller.isAppearanceLightStatusBars = prevStatus
            if (prevNav != null) controller.isAppearanceLightNavigationBars = prevNav
        }
    }
}

/** Soft-fades the chosen edges of a composable so artwork with a white / pale background blends into the screen. */
private fun Modifier.fadeEdges(
    top: Float = 0f,
    bottom: Float = 0f,
    left: Float = 0f,
    right: Float = 0f,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (top > 0f || bottom > 0f) {
            val stops = buildList {
                if (top > 0f) { add(0f to Color.Transparent); add(top to Color.Black) } else add(0f to Color.Black)
                if (bottom > 0f) { add((1f - bottom) to Color.Black); add(1f to Color.Transparent) } else add(1f to Color.Black)
            }
            drawRect(Brush.verticalGradient(colorStops = stops.toTypedArray()), blendMode = BlendMode.DstIn)
        }
        if (left > 0f || right > 0f) {
            val stops = buildList {
                if (left > 0f) { add(0f to Color.Transparent); add(left to Color.Black) } else add(0f to Color.Black)
                if (right > 0f) { add((1f - right) to Color.Black); add(1f to Color.Transparent) } else add(1f to Color.Black)
            }
            drawRect(Brush.horizontalGradient(colorStops = stops.toTypedArray()), blendMode = BlendMode.DstIn)
        }
    }

/** Text that shrinks until it fits its box (never clipped mid-line); falls back to an ellipsis at the minimum size. */
@Composable
private fun FitText(
    text: String,
    baseSize: Float,
    maxLines: Int,
    modifier: Modifier = Modifier,
    color: Color,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    textAlign: TextAlign = TextAlign.Start,
    lineHeightFactor: Float = 1.3f,
    shadow: Shadow? = null,
    minScale: Float = 0.55f,
) {
    var scale by remember(text, baseSize) { mutableFloatStateOf(1f) }
    val size = baseSize * scale
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textAlign = textAlign,
        fontSize = size.sp,
        lineHeight = (size * lineHeightFactor).sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(shadow = shadow),
        onTextLayout = { if (it.hasVisualOverflow && scale > minScale) scale *= 0.92f },
    )
}

private fun shortVerse(text: String?, limit: Int): String? =
    text?.trim()?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotEmpty() }
        ?.let {
            if (it.length <= limit) it
            else it.take(limit).let { cut -> cut.substringBeforeLast(' ', cut) }.trimEnd(',', ';', ':', '-', ' ') + "…"
        }

// ── Splash ───────────────────────────────────────────────────────────────────

/**
 * First-run splash. Everything (background, wave, logo, copy, button, footer) is drawn in the very first frame —
 * no staggered fades — on top of a window background of the same colour, so nothing flashes or shifts.
 */
@Composable
fun SplashScreen(onContinue: () -> Unit) {
    LightSystemBars()
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to OB.BgTop,
                    0.30f to OB.BgMid,
                    0.75f to OB.BgMid,
                    1f to OB.BgBottom,
                )
            )
    ) {
        val k = min(maxWidth.value / 411f, maxHeight.value / 914f).coerceIn(0.72f, 1.2f)
        val footerHeight = maxWidth / 2.9f

        // Decorative wave, top-left.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val back = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.78f, 0f)
                cubicTo(w * 0.62f, h * 0.035f, w * 0.38f, h * 0.045f, w * 0.20f, h * 0.085f)
                cubicTo(w * 0.09f, h * 0.11f, w * 0.03f, h * 0.14f, 0f, h * 0.165f)
                close()
            }
            drawPath(
                back,
                Brush.linearGradient(
                    0f to Color(0xFFD9BEEE).copy(alpha = 0.75f),
                    1f to Color(0xFFF1DCEF).copy(alpha = 0.25f),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.8f, h * 0.12f),
                )
            )
            val front = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.42f, 0f)
                cubicTo(w * 0.30f, h * 0.02f, w * 0.14f, h * 0.035f, 0f, h * 0.075f)
                close()
            }
            drawPath(front, Color(0xFFC9A5E6).copy(alpha = 0.45f))
        }

        // Footer artwork, faded at its top edge so it melts into the background.
        Image(
            painter = painterResource(R.drawable.wisdomone_bottom_sunrise_footer_clean),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .fadeEdges(top = 0.42f)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .height(maxHeight - footerHeight * 0.78f)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_wisdomone_ui_logo),
                contentDescription = "WisdomOne",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size((92 * k).dp).clip(CircleShape)
            )
            Spacer(Modifier.height((10 * k).dp))
            Text(
                "WisdomOne",
                fontSize = (31 * k).sp,
                fontWeight = FontWeight.Bold,
                color = OB.Ink,
            )
            Spacer(Modifier.height((20 * k).dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(0.62f)
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFDDB88C)))
                Image(
                    painter = painterResource(R.drawable.ic_lotus_gold),
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 10.dp).size((26 * k).dp)
                )
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFDDB88C)))
            }
            Spacer(Modifier.height((22 * k).dp))
            Text(
                "A quieter moment,",
                fontSize = (31 * k).sp, lineHeight = (40 * k).sp,
                fontFamily = OB.Serif, fontWeight = FontWeight.Bold,
                color = OB.Ink, textAlign = TextAlign.Center, maxLines = 1,
            )
            Text(
                "whenever you",
                fontSize = (31 * k).sp, lineHeight = (40 * k).sp,
                fontFamily = OB.Serif, fontWeight = FontWeight.Bold,
                color = OB.Purple, textAlign = TextAlign.Center, maxLines = 1,
            )
            Text(
                "need it.",
                fontSize = (31 * k).sp, lineHeight = (40 * k).sp,
                fontFamily = OB.Serif, fontWeight = FontWeight.Bold,
                color = OB.Ink, textAlign = TextAlign.Center, maxLines = 1,
            )
            Spacer(Modifier.height((18 * k).dp))
            Text(
                "Daily wisdom from the world’s\ngreatest scriptures.",
                fontSize = (17 * k).sp, lineHeight = (26 * k).sp,
                color = OB.Ink.copy(alpha = 0.9f), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height((34 * k).dp))
            PrimaryPill(
                label = "Begin your journey",
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                height = (56 * k).dp,
                fontSize = (18 * k),
                onClick = onContinue,
            )
        }
    }
}

// ── Reusable buttons ─────────────────────────────────────────────────────────

@Composable
private fun PrimaryPill(
    label: String,
    onClick: () -> Unit,
    height: Dp,
    fontSize: Float,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(OB.ButtonStart, OB.ButtonEnd)))
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size((fontSize * 1.35f).dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, color = Color.White, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (trailingIcon != null) {
            Spacer(Modifier.width(10.dp))
            Icon(trailingIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size((fontSize * 1.3f).dp))
        }
    }
}

@Composable
private fun DonePill(label: String, height: Dp, fontSize: Float) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(OB.Success.copy(alpha = 0.10f))
            .border(BorderStroke(1.5.dp, OB.Success.copy(alpha = 0.55f)), RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = OB.Success, modifier = Modifier.size((fontSize * 1.3f).dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = OB.Success, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun OutlinePill(label: String, height: Dp, fontSize: Float, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.55f))
            .border(BorderStroke(1.5.dp, OB.ButtonStart), RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = OB.ButtonStart, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Footer (shared by all setup screens) ─────────────────────────────────────

@Composable
private fun SetupFooter(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // Far hills — pale lavender
        drawPath(
            Path().apply {
                moveTo(0f, h * 0.62f)
                cubicTo(w * 0.10f, h * 0.22f, w * 0.24f, h * 0.02f, w * 0.40f, h * 0.36f)
                cubicTo(w * 0.50f, h * 0.58f, w * 0.60f, h * 0.30f, w * 0.74f, h * 0.10f)
                cubicTo(w * 0.86f, h * -0.04f, w * 0.95f, h * 0.20f, w, h * 0.28f)
                lineTo(w, h); lineTo(0f, h); close()
            },
            Brush.verticalGradient(0f to Color(0xFFE2CFF3), 1f to Color(0xFFD2B8EC))
        )
        // Mid hills
        drawPath(
            Path().apply {
                moveTo(0f, h * 0.80f)
                cubicTo(w * 0.16f, h * 0.42f, w * 0.30f, h * 0.46f, w * 0.50f, h * 0.70f)
                cubicTo(w * 0.66f, h * 0.90f, w * 0.80f, h * 0.36f, w, h * 0.52f)
                lineTo(w, h); lineTo(0f, h); close()
            },
            Brush.verticalGradient(0f to Color(0xFFB991E4), 1f to Color(0xFF9A6BD4))
        )
        // Front hills — deep purple
        drawPath(
            Path().apply {
                moveTo(0f, h * 0.90f)
                cubicTo(w * 0.24f, h * 0.64f, w * 0.46f, h * 1.02f, w * 0.72f, h * 0.80f)
                cubicTo(w * 0.86f, h * 0.68f, w * 0.94f, h * 0.76f, w, h * 0.72f)
                lineTo(w, h); lineTo(0f, h); close()
            },
            Brush.verticalGradient(0f to Color(0xFF7B3FC0), 1f to Color(0xFF4A1F8C))
        )
    }
}

// ── Setup flow ───────────────────────────────────────────────────────────────

private data class SetupStep(
    val title: String,
    val subtitle: String,
    val features: List<Pair<ImageVector, String>>,
    val primaryLabel: String,
    val doneLabel: String,
    val primaryIcon: ImageVector,
)

private val setupSteps = listOf(
    SetupStep(
        title = "A moment of wisdom,\nevery day.",
        subtitle = "Receive meaningful verse reminders and gentle notifications to stay inspired and mindful.",
        features = listOf(
            Icons.Rounded.CalendarMonth to "Daily verse reminders",
            Icons.Rounded.LocalFireDepartment to "Build a consistent habit",
            Icons.Rounded.Notifications to "Short, uplifting notifications",
        ),
        primaryLabel = "Enable Notifications",
        doneLabel = "Notifications enabled",
        primaryIcon = Icons.Rounded.NotificationsNone,
    ),
    SetupStep(
        title = "Let wisdom greet you.",
        subtitle = "Bring inspiration to your lock screen with a beautiful verse wallpaper from your selected tradition.",
        features = listOf(
            Icons.Rounded.Image to "Daily or scheduled verse wallpaper",
            Icons.Rounded.Settings to "Change anytime in Settings",
        ),
        primaryLabel = "Enable Verse Wallpaper",
        doneLabel = "Verse wallpaper enabled",
        primaryIcon = Icons.Outlined.ImageOutlined,
    ),
    SetupStep(
        title = "Keep a verse close.",
        subtitle = "Add the Scripture Daily home-screen widget to see an inspiring verse right on your home screen.",
        features = listOf(
            Icons.Rounded.Widgets to "Quick access to today’s verse",
            Icons.Rounded.Star to "Beautiful, lightweight widget",
            Icons.Rounded.PhoneAndroid to "Works on most Android devices",
        ),
        primaryLabel = "Add Home-Screen Widget",
        doneLabel = "Widget added",
        primaryIcon = Icons.Outlined.WidgetsOutlined,
    ),
)

/**
 * The three optional first-run setup screens. Fixed layout — never scrolls: the illustration takes whatever height is
 * left after the copy and buttons, and text sizes tighten on short screens.
 */
@Composable
fun FirstRunSetupScreen(
    initialStep: Int,
    remindersOn: Boolean,
    wallpaperOn: Boolean,
    verseText: String?,
    verseCitation: String?,
    onStepChange: (Int) -> Unit,
    onEnableNotifications: () -> Unit,
    onEnableWallpaper: () -> Unit,
    onAddWidget: () -> Unit,
    onFinish: () -> Unit,
) {
    LightSystemBars()
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(initialStep.coerceIn(0, 2)) }
    fun goTo(s: Int) { step = s; onStepChange(s) }
    BackHandler(enabled = step > 0) { goTo(step - 1) }

    // Re-evaluate permission / widget state whenever the user comes back from a system dialog or settings page.
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) resumeTick++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notifPermission = remember(resumeTick, remindersOn) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    var widgetPollTick by remember { mutableIntStateOf(0) }
    val widgetAdded = remember(resumeTick, widgetPollTick) {
        runCatching {
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, ScriptureWidgetReceiver::class.java)).isNotEmpty()
        }.getOrDefault(false)
    }
    // The launcher's "add widget" dialog may not pause the activity on every device — poll briefly after the request.
    var widgetRequested by remember { mutableIntStateOf(0) }
    LaunchedEffect(widgetRequested) {
        if (widgetRequested == 0) return@LaunchedEffect
        repeat(20) { kotlinx.coroutines.delay(1_000); widgetPollTick++ }
    }
    val done = when (step) {
        0 -> remindersOn && notifPermission
        1 -> wallpaperOn
        else -> widgetAdded
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to OB.BgTop, 0.25f to OB.BgMid, 0.8f to OB.BgMid, 1f to OB.BgBottom))
    ) {
        val navInset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
        val k = min(maxWidth.value / 411f, maxHeight.value / 914f).coerceIn(0.74f, 1.15f)
        val compact = maxHeight < 700.dp
        val tiny = maxHeight < 600.dp

        SetupFooter(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(58.dp + navInset)
        )

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = navInset + (if (compact) 26.dp else 30.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = if (compact) 3.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_wisdomone_ui_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size((38 * k).dp).clip(CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text("WisdomOne", fontSize = (21 * k).sp, fontWeight = FontWeight.Bold, color = OB.Ink)
                Spacer(Modifier.weight(1f))
                Text(
                    "Skip setup",
                    fontSize = (15 * k).sp,
                    fontWeight = FontWeight.Medium,
                    color = OB.Purple,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onFinish)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
            // Badge
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(OB.BadgeBg)
                    .padding(horizontal = 18.dp, vertical = if (compact) 4.dp else 7.dp)
            ) {
                Text("Optional Setup · ${step + 1} of 3", fontSize = (15 * k).sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF5B2A8C))
            }
            Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
            // Progress
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i <= step) OB.Purple else OB.ProgressOff)
                    )
                }
            }

            // Everything below the progress bar swaps per step.
            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "setup-step",
            ) { s ->
                val spec = setupSteps[s]
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Illustration: takes the remaining height.
                    Box(
                        Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp).clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (s) {
                            0 -> BellIllustration()
                            1 -> WallpaperIllustration(verseText, verseCitation)
                            else -> WidgetIllustration(verseText, verseCitation)
                        }
                    }

                    // Copy
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            spec.title,
                            fontFamily = OB.Serif, fontWeight = FontWeight.Bold,
                            fontSize = ((if (compact) 27 else 31) * k).sp,
                            lineHeight = ((if (compact) 33 else 36) * k).sp,
                            color = OB.Ink, textAlign = TextAlign.Center,
                        )
                        if (!tiny) {
                            Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
                            Text(
                                spec.subtitle,
                                fontSize = ((if (compact) 14 else 15) * k).sp,
                                lineHeight = ((if (compact) 19 else 21) * k).sp,
                                color = OB.InkSoft, textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                        // Feature card
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.55f))
                                .border(BorderStroke(1.dp, OB.CardBorder), RoundedCornerShape(18.dp))
                                .padding(horizontal = 18.dp, vertical = if (compact) 8.dp else 11.dp),
                            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp),
                        ) {
                            spec.features.forEach { (icon, label) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, tint = OB.Amber, modifier = Modifier.size((26 * k).dp))
                                    Spacer(Modifier.width(16.dp))
                                    Text(label, fontSize = ((if (compact) 14 else 16) * k).sp, fontWeight = FontWeight.SemiBold, color = OB.Ink, maxLines = 1)
                                }
                            }
                        }
                        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))

                        val btnH = (if (compact) 48 else 58).dp
                        val btnFont = (if (compact) 16 else 19) * k
                        if (done && s == step) {
                            DonePill(spec.doneLabel, btnH, btnFont)
                        } else {
                            PrimaryPill(
                                label = spec.primaryLabel,
                                leadingIcon = spec.primaryIcon,
                                height = btnH,
                                fontSize = btnFont,
                                onClick = {
                                    when (s) {
                                        0 -> onEnableNotifications()
                                        1 -> onEnableWallpaper()
                                        else -> { onAddWidget(); widgetRequested++ }
                                    }
                                },
                            )
                        }
                        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                        OutlinePill(
                            label = if (s < 2) "Next" else "Finish",
                            height = (if (compact) 44 else 54).dp,
                            fontSize = btnFont,
                            onClick = { if (s < 2) goTo(s + 1) else onFinish() },
                        )
                        Spacer(Modifier.height(if (compact) 2.dp else 6.dp))
                        Text(
                            "Skip setup",
                            fontSize = (16 * k).sp,
                            fontWeight = FontWeight.Medium,
                            color = OB.ButtonStart,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button, onClick = onFinish)
                                .padding(horizontal = 14.dp, vertical = if (compact) 4.dp else 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Illustrations ────────────────────────────────────────────────────────────

/** Screen 1 — the golden bell. The artwork is 3:2 with a pale background, faded on every edge. */
@Composable
private fun BellIllustration() {
    Image(
        painter = painterResource(R.drawable.screen1_notification_top_clean),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = 1.10f; scaleY = 1.10f }
            .fadeEdges(top = 0.22f, bottom = 0.20f, left = 0.20f, right = 0.20f)
    )
}

/**
 * Screen 2 — the phone artwork (1199×1312) cropped to its upper part, with a live lock-screen overlay
 * (clock, date and the current verse) positioned from the artwork's own screen rectangle so it scales on every device.
 */
@Composable
private fun WallpaperIllustration(verseText: String?, citation: String?) {
    val verse = shortVerse(verseText, 96) ?: "कर्मण्येवाधिकारस्ते मा फलेषु कदाचन ॥"
    val cite = citation ?: "Bhagavad Gita · 2.47"
    var now by remember { mutableStateOf(LocalTime.now()) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) { now = LocalTime.now(); today = LocalDate.now(); kotlinx.coroutines.delay(1_000) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val imgW = 1199f
        val imgH = 1312f
        val showFraction = 0.60f                                   // how much of the phone's height we show
        val s = min(maxHeight.value / (imgH * showFraction), maxWidth.value / imgW)   // dp per image px
        val drawW = imgW * s
        val visH = min(maxHeight.value, imgH * s)

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(drawW.dp, visH.dp)
                .fadeEdges(top = 0.06f, bottom = if (visH < imgH * s - 1f) 0.30f else 0.12f, left = 0.10f, right = 0.10f)
        ) {
            Image(
                painter = painterResource(R.drawable.screen2_verse_wallpaper_top),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
            // Phone screen rectangle in image pixels: x 285..910, y 92..(bottom).
            val sx = 285f * s
            val sw = 625f * s
            val sy = 92f * s
            val sh = visH - sy
            val textShadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), 6f)
            Column(
                Modifier
                    .offset(sx.dp, sy.dp)
                    .size(sw.dp, sh.dp)
                    .clip(RoundedCornerShape(topStart = (30 * s).dp, topEnd = (30 * s).dp))
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.10f),
                            0.85f to Color.Black.copy(alpha = 0.50f),
                            1f to Color.Black.copy(alpha = 0.50f),
                        )
                    )
                    .padding(horizontal = (sw * 0.07f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height((sw * 0.10f).dp))
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size((sw * 0.085f).dp))
                Text(
                    now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    color = Color.White, fontWeight = FontWeight.Light,
                    fontSize = (sw * 0.20f).sp, lineHeight = (sw * 0.23f).sp,
                    style = TextStyle(shadow = textShadow),
                )
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())),
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = (sw * 0.065f).sp,
                    style = TextStyle(shadow = textShadow),
                )
                Spacer(Modifier.weight(1f))
                FitText(
                    text = "“$verse”",
                    baseSize = sw * 0.082f,
                    maxLines = 3,
                    color = Color.White,
                    fontFamily = OB.Serif,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    shadow = textShadow,
                    modifier = Modifier.fillMaxWidth().heightIn(max = (sw * 0.34f).dp),
                )
                Spacer(Modifier.height((sw * 0.03f).dp))
                Text(
                    cite,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = (sw * 0.055f).sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(shadow = textShadow),
                )
                Spacer(Modifier.height((sh * 0.17f).dp))
            }
        }
    }
}

/** Screen 3 — phone home screen artwork (1536×1024) with the current verse written inside its widget card. */
@Composable
private fun WidgetIllustration(verseText: String?, citation: String?) {
    val verse = shortVerse(verseText, 76) ?: "Be the change you wish to see in the world."
    val cite = citation ?: "General Wisdom"
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val ratio = 1536f / 1024f
        val drawW = min(maxWidth.value, maxHeight.value * ratio)
        val drawH = drawW / ratio

        Box(
            Modifier
                .align(Alignment.Center)
                .size(drawW.dp, drawH.dp)
                .fadeEdges(top = 0.10f, bottom = 0.14f, left = 0.14f, right = 0.14f)
        ) {
            Image(
                painter = painterResource(R.drawable.screen3_home_widget_top),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            // Widget card in the artwork: x 397..1140, y 220..615 of 1536×1024.
            val cx = drawW * (397f / 1536f)
            val cy = drawH * (220f / 1024f)
            val cw = drawW * (743f / 1536f)
            val ch = drawH * (395f / 1024f)
            // Text sits in the light upper part of the card (the mountains darken the lower third).
            Column(
                Modifier
                    .offset(cx.dp, cy.dp)
                    .size(cw.dp, ch.dp)
                    .padding(start = (cw * 0.07f).dp, end = (cw * 0.16f).dp, top = (ch * 0.07f).dp),
            ) {
                FitText(
                    text = "“$verse”",
                    baseSize = cw * 0.092f,
                    maxLines = 4,
                    color = OB.Ink,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().heightIn(max = (ch * 0.42f).dp),
                    lineHeightFactor = 1.2f,
                )
                Spacer(Modifier.height((ch * 0.03f).dp))
                Text(
                    "— $cite",
                    color = OB.Ink.copy(alpha = 0.9f),
                    fontSize = (cw * 0.056f).sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
