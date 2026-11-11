package com.scripturedaily.gate

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.BuildConfig
import com.scripturedaily.R
import com.scripturedaily.config.AppConfigManager
import com.scripturedaily.gate.AppGate.Decision
import com.scripturedaily.ui.W
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val PREFS = "app_gate"
private const val MAINTENANCE_AUTO_CHECK_MS = 120_000L

/**
 * Wraps the whole app. Nothing inside [content] is composed — no Home, verse, journal, settings, deep link or notification
 * target — until the configuration has been loaded and neither a mandatory update nor maintenance mode applies.
 *
 * [onBlocked] runs whenever the app is blocked, so pending deep links / notification targets are dropped instead of
 * opening later behind the user's back.
 */
@Composable
fun AppGateHost(onBlocked: () -> Unit, content: @Composable () -> Unit) {
    val startupDone by AppConfigManager.startupDone.collectAsState()
    LaunchedEffect(Unit) { if (!AppConfigManager.startupDone.value) AppConfigManager.awaitStartup() }
    if (!startupDone) { GateLoadingScreen(); return }

    val snapshot by AppConfigManager.state.collectAsState()
    val fetchOk by AppConfigManager.lastFetchOk.collectAsState()
    val decision = remember(snapshot, fetchOk) { AppGate.decide(snapshot, BuildConfig.VERSION_NAME, fetchOk) }
    var continueOffline by rememberSaveable { mutableStateOf(false) }

    val blocking = decision is Decision.MandatoryUpdate || decision is Decision.Maintenance
    GateState.blocking = blocking
    LaunchedEffect(blocking) { if (blocking) onBlocked() }

    when (decision) {
        is Decision.MandatoryUpdate -> MandatoryUpdateScreen(decision.update)
        is Decision.Maintenance -> MaintenanceScreen(decision.maintenance, decision.fromCache)
        Decision.Unverified -> if (continueOffline) content() else ConnectionErrorScreen(onContinue = { continueOffline = true })
        is Decision.OptionalUpdate -> { content(); OptionalUpdatePrompt(decision.update) }
        Decision.Open -> content()
    }
}

/** Read by components outside Compose (app-open ads, lock-screen activity) so they never appear over / around a blocked app. */
object GateState {
    @Volatile var blocking: Boolean = false

    /** Evaluates the current configuration without Compose (e.g. from another Activity). */
    fun isBlockingNow(): Boolean = with(AppGate) {
        decide(AppConfigManager.current, BuildConfig.VERSION_NAME, AppConfigManager.lastFetchOk.value).blocksApp
    }
}

// ── Shared pieces ────────────────────────────────────────────────────────────

@Composable
private fun GateScaffold(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(W.Parchment)) {
        // Soft brand halos (decorative)
        Box(Modifier.size(340.dp).offset(x = 140.dp, y = (-90).dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(W.Gold.copy(alpha = 0.18f), Color.Transparent))))
        Box(Modifier.size(280.dp).align(Alignment.BottomStart).offset(x = (-70).dp, y = 70.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(W.Purple.copy(alpha = 0.10f), Color.Transparent))))
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(Modifier.widthIn(max = 460.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
        }
    }
}

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_wisdomone_ui_logo), null, contentScale = ContentScale.Crop, modifier = Modifier.size(30.dp).clip(CircleShape))
        Spacer(Modifier.width(8.dp))
        Text("WisdomOne", color = W.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun GatePrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = W.InkButton, contentColor = W.OnPurple)
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
}

@Composable
private fun GateSecondaryButton(text: String, loading: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, W.Gold)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = W.Gold, strokeWidth = 2.dp)
        else Text(text, color = W.Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/** Closes the app the Android way: finish this task's activities (no force-stop, no crash). */
private fun closeApp(context: Context) { (context as? Activity)?.finishAffinity() }

@Composable
private fun GateLoadingScreen() {
    GateScaffold {
        Image(painterResource(R.drawable.ic_wisdomone_ui_logo), null, contentScale = ContentScale.Crop, modifier = Modifier.size(84.dp).clip(CircleShape))
        Spacer(Modifier.height(18.dp))
        Text("WisdomOne", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = W.Ink)
        Spacer(Modifier.height(18.dp))
        CircularProgressIndicator(color = W.Gold, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        Spacer(Modifier.height(14.dp))
        Text("Getting things ready…", fontSize = 13.sp, color = W.InkSoft)
    }
}

// ── Maintenance ──────────────────────────────────────────────────────────────

@Composable
fun MaintenanceScreen(config: AppGate.MaintenanceConfig, fromCache: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    BackHandler { closeApp(context) }   // never back into app screens

    // Gentle automatic re-check while the screen is visible, so users get in as soon as maintenance ends.
    LaunchedEffect(Unit) {
        while (true) { delay(MAINTENANCE_AUTO_CHECK_MS); AppConfigManager.refresh(force = true) }
    }

    GateScaffold {
        BrandMark()
        Spacer(Modifier.height(26.dp))
        MaintenanceIllustration(Modifier.size(200.dp))
        Spacer(Modifier.height(26.dp))
        Text(config.title, fontSize = 27.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, color = W.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(config.message, fontSize = 15.sp, lineHeight = 23.sp, color = W.InkMid, textAlign = TextAlign.Center)
        config.estimatedEnd?.let { end ->
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(W.Surface)
                    .border(1.dp, W.Gold.copy(alpha = .35f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(W.Gold))
                Spacer(Modifier.width(12.dp))
                Text(estimatedLine(end), color = W.Ink, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        if (fromCache) {
            Spacer(Modifier.height(12.dp))
            Text("We couldn't reach WisdomOne just now, so this is the last status we received.", color = W.InkSoft, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        note?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = W.InkMid, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.semantics { contentDescription = it })
        }
        Spacer(Modifier.height(30.dp))
        GatePrimaryButton("Close App") { closeApp(context) }
        if (config.allowRetry) {
            Spacer(Modifier.height(12.dp))
            GateSecondaryButton("Check Again", loading = checking) {
                checking = true; note = null
                scope.launch {
                    val ok = AppConfigManager.refresh(force = true)
                    checking = false
                    // If maintenance ended, AppGateHost recomposes into the normal app on its own.
                    note = if (ok) "We're still working on it. Thanks for your patience!" else "Couldn't connect. Check your internet connection and try again."
                }
            }
        }
    }
}

private fun estimatedLine(end: Instant, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
    if (!end.isAfter(now)) return "We expect to be back any moment now."
    val local = end.atZone(zone)
    val sameDay = local.toLocalDate() == now.atZone(zone).toLocalDate()
    val time = local.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return if (sameDay) "We'll be back around $time today."
    else "We'll be back around $time, ${local.format(DateTimeFormatter.ofPattern("EEE d MMM"))}."
}

/** Calm hourglass inside a slowly turning ring of light — drawn in code so it follows Light / Dark automatically. */
@Composable
private fun MaintenanceIllustration(modifier: Modifier = Modifier) {
    val gold = W.Gold; val purple = W.Purple; val ink = W.Ink; val surface = W.Surface
    val t = rememberInfiniteTransition(label = "maintenance")
    val turn by t.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "ring")
    val sand by t.animateFloat(0.15f, 0.85f, infiniteRepeatable(tween(6_000, easing = LinearEasing), RepeatMode.Restart), label = "sand")
    Canvas(modifier.semantics { contentDescription = "Maintenance in progress" }) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = .28f), Color.Transparent), c, r), r, c)
        drawCircle(surface, r * .62f, c)
        drawCircle(gold.copy(alpha = .45f), r * .62f, c, style = Stroke(r * .018f))
        rotate(turn, c) {
            for (i in 0 until 24) {
                val a = Math.toRadians(i * 15.0)
                val p = Offset(c.x + (r * .80f * Math.cos(a)).toFloat(), c.y + (r * .80f * Math.sin(a)).toFloat())
                drawCircle(gold.copy(alpha = if (i % 6 == 0) .9f else .35f), if (i % 6 == 0) r * .028f else r * .016f, p)
            }
        }
        // Hourglass
        val w = r * .44f; val h = r * .62f
        val top = c.y - h / 2; val bottom = c.y + h / 2
        val frame = Stroke(r * .035f, cap = StrokeCap.Round)
        drawLine(purple, Offset(c.x - w / 2 - r * .05f, top), Offset(c.x + w / 2 + r * .05f, top), r * .045f, StrokeCap.Round)
        drawLine(purple, Offset(c.x - w / 2 - r * .05f, bottom), Offset(c.x + w / 2 + r * .05f, bottom), r * .045f, StrokeCap.Round)
        val glass = Path().apply {
            moveTo(c.x - w / 2, top); lineTo(c.x + w / 2, top); lineTo(c.x + r * .04f, c.y); lineTo(c.x + w / 2, bottom)
            lineTo(c.x - w / 2, bottom); lineTo(c.x - r * .04f, c.y); close()
        }
        drawPath(glass, ink.copy(alpha = .75f), style = frame)
        // Sand: the top drains while the bottom fills.
        val topLevel = (1 - sand) * (h / 2) * .8f
        val upper = Path().apply {
            val y = c.y - topLevel
            val half = (w / 2) * (topLevel / (h / 2)) + r * .02f
            moveTo(c.x - half, y); lineTo(c.x + half, y); lineTo(c.x, c.y - r * .01f); close()
        }
        drawPath(upper, gold)
        val pile = sand * (h / 2) * .75f
        val lower = Path().apply {
            moveTo(c.x - w / 2 + r * .05f, bottom - r * .03f); lineTo(c.x + w / 2 - r * .05f, bottom - r * .03f)
            lineTo(c.x, bottom - r * .03f - pile); close()
        }
        drawPath(lower, gold)
        drawLine(gold, Offset(c.x, c.y), Offset(c.x, bottom - r * .03f - pile), r * .012f)
    }
}

// ── Updates ──────────────────────────────────────────────────────────────────

private fun openStore(context: Context, url: String) {
    val pkg = context.packageName
    val candidates = listOfNotNull(url.takeIf { it.isNotBlank() }, "market://details?id=$pkg", "https://play.google.com/store/apps/details?id=$pkg")
    for (u in candidates) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return }
        catch (_: ActivityNotFoundException) { }
    }
}

@Composable
fun MandatoryUpdateScreen(update: AppGate.UpdateConfig) {
    val context = LocalContext.current
    BackHandler { closeApp(context) }   // cannot be bypassed
    GateScaffold {
        BrandMark()
        Spacer(Modifier.height(30.dp))
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(W.Gold.copy(alpha = 0.14f)).border(BorderStroke(1.5.dp, W.Gold.copy(alpha = 0.35f)), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("↑", fontSize = 40.sp, color = W.Gold, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(26.dp))
        Text(update.mandatoryTitle, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = W.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(update.mandatoryMessage, fontSize = 15.sp, color = W.InkMid, textAlign = TextAlign.Center, lineHeight = 23.sp)
        if (update.latestVersion.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text("Installed ${BuildConfig.VERSION_NAME} · Latest ${update.latestVersion}", fontSize = 12.sp, color = W.InkSoft)
        }
        Spacer(Modifier.height(34.dp))
        GatePrimaryButton("Update Now") { openStore(context, update.playStoreUrl) }
        Spacer(Modifier.height(12.dp))
        GateSecondaryButton("Close App") { closeApp(context) }
    }
}

@Composable
private fun OptionalUpdatePrompt(update: AppGate.UpdateConfig) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var visible by remember(update.latestVersion) {
        mutableStateOf(AppGate.shouldPromptOptional(update, prefs.getString("later_version", null), prefs.getLong("later_at", 0L), System.currentTimeMillis()))
    }
    if (!visible) return
    fun later() {
        prefs.edit().putString("later_version", update.latestVersion).putLong("later_at", System.currentTimeMillis()).apply()
        visible = false
    }
    AlertDialog(
        onDismissRequest = ::later,
        containerColor = W.Surface,
        icon = { Text("✦", color = W.Gold, fontSize = 26.sp) },
        title = { Text(update.title, fontWeight = FontWeight.Bold, color = W.Ink, textAlign = TextAlign.Center) },
        text = { Text(update.message, color = W.InkMid, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center) },
        confirmButton = {
            Button(onClick = { openStore(context, update.playStoreUrl); later() }, colors = ButtonDefaults.buttonColors(containerColor = W.InkButton, contentColor = W.OnPurple)) {
                Text("Update Now", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = ::later) { Text("Later", color = W.InkMid) } }
    )
}

// ── First launch without a connection ────────────────────────────────────────

@Composable
private fun ConnectionErrorScreen(onContinue: () -> Unit) {
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    GateScaffold {
        BrandMark()
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(96.dp).clip(CircleShape).background(W.Gold.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Text("☁", fontSize = 40.sp, color = W.Gold)
        }
        Spacer(Modifier.height(24.dp))
        Text("You're offline", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = W.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "WisdomOne couldn't check its service status. Connect to the internet and try again, or continue — some features need a connection.",
            fontSize = 15.sp, color = W.InkMid, textAlign = TextAlign.Center, lineHeight = 23.sp
        )
        Spacer(Modifier.height(32.dp))
        GatePrimaryButton(if (checking) "Checking…" else "Try Again", enabled = !checking) {
            checking = true
            scope.launch { AppConfigManager.refresh(force = true); checking = false }
        }
        Spacer(Modifier.height(12.dp))
        GateSecondaryButton("Continue") { onContinue() }
    }
}
