package com.scripturedaily.features.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.data.TreeActivitiesDto
import com.scripturedaily.data.TreeActivityDto
import com.scripturedaily.data.TreeDto
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.W
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Trunk = Color(0xFF7A4A2A)
private val TrunkDark = Color(0xFF5E3820)
private val LeafA = Color(0xFF4FA24A)
private val LeafB = Color(0xFF6CBF5B)
private val LeafC = Color(0xFF3A8A3E)
private val Petal = Color(0xFFFFF7E6)
private val Fruit = Color(0xFFD9452B)

/** Draws the tree for a growth stage (1 = sprout … 5 = fruits) inside the current DrawScope. */
fun DrawScope.drawWisdomTree(stage: TreeStage, locked: Boolean = false, sway: Float = 0f) {
    val w = size.width; val h = size.height
    fun tint(c: Color) = if (locked) Color(0xFF9A939F) else c
    // mound
    drawOval(Brush.verticalGradient(listOf(tint(Color(0xFF7BB661)), tint(Color(0xFF4F8A45)))), Offset(w * 0.12f, h * 0.84f), Size(w * 0.76f, h * 0.16f))
    val cx = w * 0.5f + sway * w * 0.01f
    when (stage) {
        TreeStage.SPROUT -> {
            drawLine(tint(Color(0xFF5E9E4A)), Offset(w * 0.5f, h * 0.88f), Offset(cx, h * 0.42f), strokeWidth = w * 0.05f)
            leaf(cx, h * 0.46f, w * 0.3f, -30f, tint(LeafB)); leaf(cx, h * 0.52f, w * 0.26f, 205f, tint(LeafA)); leaf(cx, h * 0.62f, w * 0.18f, -15f, tint(LeafC))
        }
        TreeStage.LEAVES -> {
            drawLine(tint(Color(0xFF5E9E4A)), Offset(w * 0.5f, h * 0.88f), Offset(cx, h * 0.38f), strokeWidth = w * 0.045f)
            for ((i, y) in listOf(0.72f, 0.6f, 0.48f).withIndex()) {
                leaf(cx, h * y, w * (0.22f - i * 0.02f), -25f - i * 5f, tint(if (i % 2 == 0) LeafB else LeafA)); leaf(cx, h * (y + 0.03f), w * (0.2f - i * 0.02f), 205f + i * 5f, tint(LeafC))
            }
            leaf(cx, h * 0.4f, w * 0.17f, -80f, tint(LeafB)); leaf(cx, h * 0.4f, w * 0.17f, 260f, tint(LeafA))
        }
        else -> {
            trunk(cx, w, h, locked, stage != TreeStage.BRANCHES)
            val canopy = listOf(Offset(0.5f, 0.3f) to 0.2f, Offset(0.3f, 0.4f) to 0.15f, Offset(0.7f, 0.4f) to 0.15f, Offset(0.4f, 0.22f) to 0.14f, Offset(0.6f, 0.22f) to 0.14f, Offset(0.5f, 0.45f) to 0.14f)
            val shades = listOf(tint(LeafC), tint(LeafA), tint(LeafB))
            canopy.forEachIndexed { i, (o, r) -> drawCircle(shades[i % 3], w * r, Offset(w * o.x + sway * w * 0.01f, h * o.y)) }
            canopy.forEachIndexed { i, (o, r) -> drawCircle(shades[(i + 1) % 3].copy(alpha = 0.7f), w * r * 0.55f, Offset(w * (o.x + 0.03f) + sway * w * 0.01f, h * (o.y - 0.03f))) }
            if (stage == TreeStage.FLOWERS || stage == TreeStage.FRUITS) {
                val spots = listOf(0.34f to 0.34f, 0.5f to 0.22f, 0.66f to 0.36f, 0.42f to 0.46f, 0.6f to 0.5f, 0.27f to 0.46f, 0.74f to 0.46f, 0.52f to 0.35f)
                spots.forEach { (x, y) ->
                    val p = Offset(w * x + sway * w * 0.01f, h * y)
                    if (stage == TreeStage.FLOWERS) {
                        for (k in 0 until 5) { val a = Math.toRadians(k * 72.0); drawCircle(tint(Petal), w * 0.026f, Offset(p.x + (w * 0.03f * Math.cos(a)).toFloat(), p.y + (w * 0.03f * Math.sin(a)).toFloat())) }
                        drawCircle(tint(Color(0xFFFFC83D)), w * 0.02f, p)
                    } else {
                        drawCircle(tint(Fruit), w * 0.036f, p); drawCircle(Color.White.copy(alpha = 0.4f), w * 0.01f, Offset(p.x - w * 0.01f, p.y - w * 0.012f))
                        drawLine(tint(TrunkDark), Offset(p.x, p.y - w * 0.034f), Offset(p.x, p.y - w * 0.05f), strokeWidth = w * 0.006f)
                    }
                }
            }
        }
    }
    if (locked) drawRect(Color.White.copy(alpha = 0.0f))
}

private fun DrawScope.leaf(x: Float, y: Float, len: Float, angleDeg: Float, color: Color) {
    val a = Math.toRadians(angleDeg.toDouble())
    val ex = x + (len * Math.cos(a)).toFloat(); val ey = y + (len * Math.sin(a)).toFloat()
    val mx = (x + ex) / 2; val my = (y + ey) / 2
    val nx = -(ey - y) * 0.35f; val ny = (ex - x) * 0.35f
    val path = Path().apply { moveTo(x, y); quadraticTo(mx + nx, my + ny, ex, ey); quadraticTo(mx - nx, my - ny, x, y); close() }
    drawPath(path, color)
}

private fun DrawScope.trunk(cx: Float, w: Float, h: Float, locked: Boolean, branches: Boolean) {
    val c1 = if (locked) Color(0xFF8A8490) else Trunk; val c2 = if (locked) Color(0xFF6F6A75) else TrunkDark
    val t = Path().apply {
        moveTo(w * 0.44f, h * 0.9f); quadraticTo(w * 0.47f, h * 0.7f, w * 0.46f, h * 0.5f); lineTo(w * 0.54f, h * 0.5f)
        quadraticTo(w * 0.53f, h * 0.7f, w * 0.56f, h * 0.9f); close()
    }
    drawPath(t, Brush.horizontalGradient(listOf(c2, c1, c2)))
    val b = Stroke(width = w * 0.028f)
    for ((x1, y1, x2, y2) in listOf(listOf(0.5f, 0.6f, 0.3f, 0.42f), listOf(0.5f, 0.56f, 0.7f, 0.4f), listOf(0.5f, 0.5f, 0.42f, 0.28f), listOf(0.5f, 0.5f, 0.6f, 0.26f))) {
        val p = Path().apply { moveTo(w * x1, h * y1); quadraticTo(w * (x1 + x2) / 2, h * (y1 - 0.02f), w * x2, h * y2) }
        drawPath(p, c1, style = b)
    }
}

/** Soft sunrise-style card background used by the Home tree card and the journey screen. */
private val CardBgLight = Brush.linearGradient(listOf(Color(0xFFFFF4DC), Color(0xFFFBE9EF), Color(0xFFEDE6F8)))
private val CardBgDark = Brush.linearGradient(listOf(Color(0xFF3A2F26), Color(0xFF33243A), Color(0xFF2A2342)))
private val CardBg: Brush @Composable get() = if (W.IsDark) CardBgDark else CardBgLight

@Composable
fun TreeIllustration(stage: TreeStage, modifier: Modifier = Modifier, locked: Boolean = false, animated: Boolean = false, description: String = "") {
    val sway by if (animated) rememberInfiniteTransition(label = "sway").animateFloat(-1f, 1f, infiniteRepeatable(tween(3200), RepeatMode.Reverse), label = "s") else remember { mutableFloatStateOf(0f) }
    Canvas(modifier.semantics { if (description.isNotEmpty()) contentDescription = description }) { drawWisdomTree(stage, locked, sway) }
}

/** The single progress card on Home: tree, level + status, encouragement, "View journey" and the five milestones. */
@Composable
fun WisdomTreeCard(state: TreeState, onJourney: () -> Unit, modifier: Modifier = Modifier, activities: TreeActivitiesDto? = null, showActivities: Boolean = false) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(CardBg).border(BorderStroke(1.dp, W.Gold.copy(alpha = 0.25f)), RoundedCornerShape(26.dp))
            .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 2.dp)) {
                Text("🌱  Wisdom Tree", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = W.Ink)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFE8A6)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text("${state.stage.emoji}  Level ${state.level} · ${state.status}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4A3410))
                }
                Spacer(Modifier.height(10.dp))
                Text(TreeLogic.message(state), fontSize = 13.sp, color = W.InkMid, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                Text("View journey  →", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = W.PurpleDark,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onJourney).padding(vertical = 6.dp).semantics { contentDescription = "View your Wisdom Tree journey" })
            }
            TreeIllustration(state.stage, Modifier.width(150.dp).height(150.dp), animated = true, description = "Your Wisdom Tree at the ${state.stage.label} stage")
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(W.Gold.copy(alpha = 0.25f)))
        Spacer(Modifier.height(8.dp))
        MilestoneRow(state)
        // This level's own activities. When every one is done (nothing pending) the box is not composed at all.
        if (showActivities && activities != null && !activities.allCompleted) {
            Spacer(Modifier.height(10.dp))
            LevelActivityBox(activities, Modifier.padding(end = 6.dp))
        }
    }
}

/** The activity box for the current level: unique tasks with progress. Hidden by the caller once the level has no pending activity. */
@Composable
fun LevelActivityBox(a: TreeActivitiesDto, modifier: Modifier = Modifier, compact: Boolean = true) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(W.Surface.copy(alpha = if (W.IsDark) 0.6f else 0.75f))
        .border(BorderStroke(1.dp, W.Gold.copy(alpha = 0.3f)), RoundedCornerShape(18.dp)).padding(12.dp).semantics { contentDescription = "Level ${a.level} activities, ${a.completedCount} of ${a.totalCount} done" }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Level ${a.level} · ${a.title}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = W.Ink)
                if (!compact) Text(a.subtitle, fontSize = 12.sp, color = W.InkMid)
            }
            Text("${a.completedCount}/${a.totalCount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = W.PurpleDark)
        }
        Spacer(Modifier.height(6.dp))
        a.items.forEach { item -> ActivityRow(item) }
    }
}

@Composable
private fun ActivityRow(item: TreeActivityDto) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).semantics { contentDescription = "${item.title}: ${if (item.completed) "done" else "${item.progress} of ${item.target}"}" }, verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 2.dp).size(20.dp).clip(CircleShape).background(if (item.completed) W.Success else Color.Transparent)
            .border(BorderStroke(1.5.dp, if (item.completed) W.Success else W.InkSoft), CircleShape), contentAlignment = Alignment.Center) {
            if (item.completed) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (item.completed) W.InkSoft else W.Ink,
                textDecoration = if (item.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
            Text(item.description, fontSize = 11.sp, color = W.InkMid, lineHeight = 15.sp)
            if (!item.completed && item.target > 1) {
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(W.Gold.copy(alpha = 0.25f))) {
                    Box(Modifier.fillMaxWidth((item.progress.toFloat() / item.target).coerceIn(0f, 1f)).height(5.dp).background(W.Purple))
                }
            }
        }
        if (!item.completed) Text("${item.progress}/${item.target}", fontSize = 11.sp, color = W.InkSoft, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
    }
}

@Composable
fun MilestoneRow(state: TreeState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TreeStage.values().forEach { s ->
            val unlocked = state.unlocked(s); val current = s == state.stage && state.totalDays > 0
            Column(
                Modifier.weight(1f).padding(horizontal = 2.dp).clip(RoundedCornerShape(16.dp))
                    .then(if (current) Modifier.background(W.Surface.copy(alpha = 0.55f)).border(BorderStroke(1.5.dp, Color(0xFFE9B949)), RoundedCornerShape(16.dp)) else Modifier)
                    .padding(vertical = 6.dp).semantics { contentDescription = "${s.label}, ${s.range}, ${if (unlocked) "unlocked" else "locked"}" },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    TreeIllustration(s, Modifier.fillMaxSize(), locked = !unlocked)
                    if (!unlocked) Box(Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape).background(Color(0xFF5B5566)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Lock, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    } else if (!current) Box(Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape).background(W.Success), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
                Text(s.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (unlocked) W.Ink else W.InkSoft, maxLines = 1)
                Text(s.range.replace("Day ", "Day "), fontSize = 9.sp, color = W.InkSoft, maxLines = 1)
            }
        }
    }
}

/** Celebration shown when the tree reaches a new stage. Calm, positive, easy to dismiss. */
@Composable
fun MilestoneDialog(stage: TreeStage, onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, tween(600)) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = W.Parchment,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Continue", color = W.PurpleDark, fontWeight = FontWeight.Bold) } },
        title = { Text("Your tree has grown ${stage.emoji}", fontWeight = FontWeight.Bold, color = W.Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TreeIllustration(stage, Modifier.size(150.dp).scale(scale.value), animated = true)
                Spacer(Modifier.height(8.dp))
                Text(milestoneMessage(stage), color = W.InkMid, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }
    )
}

private fun milestoneMessage(s: TreeStage) = when (s) {
    TreeStage.SPROUT -> "A seed becomes a sprout. Every great tree begins with one quiet day."
    TreeStage.LEAVES -> "Fresh leaves are opening. Your daily reading is taking root."
    TreeStage.BRANCHES -> "Strong branches are reaching out. Your habit is growing steady."
    TreeStage.FLOWERS -> "Your tree is in bloom. What a beautiful thing you are nurturing."
    TreeStage.FRUITS -> "Your tree bears fruit. Thank you for making time for wisdom."
}

/** "View journey": the tree, unlocked milestones, reading history, totals and the next milestone. */
@Composable
fun TreeJourneyScreen(days: Set<LocalDate>, remote: TreeDto?, onBack: () -> Unit) {
    val today = LocalDate.now()
    val state = remember(days, today) { TreeLogic.state(days, today) }
    OverlayScreen("Your Wisdom Journey", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(CardBg).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TreeIllustration(state.stage, Modifier.size(210.dp), animated = true, description = "Your Wisdom Tree, ${state.stage.label} stage")
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFE8A6)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text("Level ${state.level} of ${TreeLogic.MAX_LEVELS} · Day ${state.journeyDay}/${TreeLogic.DAYS_PER_LEVEL}", fontWeight = FontWeight.Bold, color = Color(0xFF4A3410), fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(TreeLogic.message(state), fontSize = 14.sp, color = W.InkMid, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Reading days", "${state.totalDays}", Modifier.weight(1f)); Stat("Current streak", "${state.currentStreak}", Modifier.weight(1f)); Stat("Longest streak", "${state.longestStreak}", Modifier.weight(1f))
            }
            state.next?.let {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(W.Gold.copy(alpha = 0.12f)).padding(14.dp)) {
                    Text("Next: ${it.emoji} ${it.label} — ${state.daysToNext} more reading ${if (state.daysToNext == 1) "day" else "days"} (growth never resets; it simply waits for you).", fontSize = 13.sp, color = W.InkMid, lineHeight = 18.sp)
                }
            } ?: Text(
                if (state.journeyComplete) "You completed all five levels. Your achievement is permanently preserved in your reading history."
                else "Level ${state.level} is complete. Your next level begins with your next completed daily reading.",
                fontSize = 13.sp, color = W.InkMid, textAlign = TextAlign.Center
            )
            remote?.levels?.takeIf { it.isNotEmpty() }?.let { levels ->
                Text("YOUR 150-DAY JOURNEY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
                levels.forEach { lv ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(W.Surface.copy(alpha = if (lv.status == "locked") 0.5f else 0.95f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(when (lv.status) { "done" -> W.Success; "active" -> W.Purple; else -> W.InkSoft.copy(alpha = 0.35f) }), contentAlignment = Alignment.Center) {
                            if (lv.status == "done") Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            else if (lv.status == "locked") Icon(Icons.Outlined.Lock, null, tint = W.InkSoft, modifier = Modifier.size(16.dp))
                            else Text("${lv.level}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Level ${lv.level} · ${lv.title}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (lv.status == "locked") W.InkSoft else W.Ink)
                            Text("${lv.days} · ${lv.completed}/${lv.total} activities", fontSize = 12.sp, color = W.InkMid)
                        }
                    }
                }
                remote.activities?.let { a ->
                    if (a.allCompleted && !state.journeyComplete) Text("All Level ${a.level} activities are complete 🎉 — the next level's activities unlock when it begins.", fontSize = 13.sp, color = W.Success, lineHeight = 18.sp)
                    else if (!state.journeyComplete) LevelActivityBox(a, compact = false)
                }
            }
            Text("LEVEL ${state.level} GROWTH MILESTONES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
            TreeStage.values().forEach { s ->
                val unlocked = state.unlocked(s)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(W.Surface.copy(alpha = if (unlocked) 0.9f else 0.5f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TreeIllustration(s, Modifier.size(54.dp), locked = !unlocked)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${s.label} · ${s.range}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (unlocked) W.Ink else W.InkSoft)
                        Text(if (unlocked) "Unlocked" else "Unlocks at ${s.first} reading days", fontSize = 12.sp, color = if (unlocked) W.Success else W.InkSoft)
                    }
                    if (unlocked) Icon(Icons.Outlined.Check, null, tint = W.Success) else Icon(Icons.Outlined.Lock, null, tint = W.InkSoft)
                }
            }
            Text("READING HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
            HistoryGrid(days, today)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(W.Surface.copy(alpha = 0.85f)).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = W.PurpleDark); Text(label, fontSize = 11.sp, color = W.InkSoft)
    }
}

/** The last ~5 weeks as a calendar grid (history only — the Home tree card has no weekly tracker). */
@Composable
private fun HistoryGrid(days: Set<LocalDate>, today: LocalDate) {
    val start = today.minusDays(((today.dayOfWeek.value % 7) + 28).toLong())  // Sunday, 4 weeks ago
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(W.Surface.copy(alpha = 0.85f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, fontSize = 10.sp, color = W.InkSoft, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center) } }
        for (week in 0 until 5) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (d in 0 until 7) {
                val date = start.plusDays((week * 7 + d).toLong())
                val read = date in days; val future = date.isAfter(today)
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(when { read -> Color(0xFF6CBF5B); future -> Color.Transparent; else -> W.Gold.copy(alpha = 0.10f) })
                        .then(if (date == today) Modifier.border(BorderStroke(1.5.dp, W.Gold), CircleShape) else Modifier)
                        .semantics { contentDescription = "${date.format(DateTimeFormatter.ofPattern("MMM d"))}: ${if (read) "read" else if (future) "upcoming" else "not read"}" },
                    contentAlignment = Alignment.Center
                ) { Text("${date.dayOfMonth}", fontSize = 11.sp, color = if (read) Color.White else if (future) W.InkSoft.copy(alpha = 0.4f) else W.InkMid) }
            }
        }
    }
}
