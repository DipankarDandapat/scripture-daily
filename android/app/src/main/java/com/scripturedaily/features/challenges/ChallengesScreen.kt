package com.scripturedaily.features.challenges

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonParser
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.Badge
import com.scripturedaily.data.Challenge
import com.scripturedaily.data.ChallengeDay
import com.scripturedaily.data.ChallengeDetail
import com.scripturedaily.features.explain.ExplainSheet
import com.scripturedaily.ui.EmptyBox
import com.scripturedaily.ui.ErrorBox
import com.scripturedaily.ui.LoadingBox
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.SurfaceCard
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.time.LocalDate

private fun parseColor(hex: String, fallback: Color = W.Gold): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

private fun <T> Response<T>.problem(default: String): String {
    val detail = runCatching { JsonParser.parseString(errorBody()?.string() ?: "").asJsonObject.get("detail").asString }.getOrNull()
    return when {
        code() == 401 || code() == 403 -> "Please connect to the internet and reopen the app to sign in."
        detail != null -> detail
        else -> default
    }
}

@Composable
fun ChallengesScreen(languageId: Int, religionId: Int, onBack: () -> Unit) {
    var openId by remember { mutableStateOf<Int?>(null) }
    var list by remember { mutableStateOf<List<Challenge>?>(null) }
    var badges by remember { mutableStateOf<List<Badge>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt, openId, languageId) {
        if (openId != null) return@LaunchedEffect
        error = null
        try {
            list = withContext(Dispatchers.IO) { ApiClient.api.challenges(LocalDate.now().toString(), languageId.takeIf { it > 0 }) }
            badges = runCatching { withContext(Dispatchers.IO) { ApiClient.api.badges() } }.getOrDefault(emptyList())
        } catch (_: Exception) { error = "Couldn't load challenges. Check your connection and try again." }
    }

    val id = openId
    if (id != null) { ChallengeDetailScreen(id, languageId, religionId) { openId = null }; return }

    OverlayScreen("Challenges", onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Build a habit one verse at a time. Finish a track to earn its badge.", fontSize = 13.sp, color = W.InkSoft, lineHeight = 19.sp)
            if (badges.isNotEmpty()) {
                Text("YOUR BADGES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = W.Gold, letterSpacing = 1.2.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(badges) { b ->
                        Column(
                            Modifier.clip(RoundedCornerShape(18.dp)).background(parseColor(b.color).copy(alpha = 0.15f)).border(1.dp, parseColor(b.color).copy(alpha = 0.5f), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) { Text(b.badgeIcon, fontSize = 30.sp); Text(b.badgeName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = W.Ink) }
                    }
                }
            }
            when {
                error != null -> ErrorBox(error!!, { attempt++ })
                list == null -> LoadingBox("Loading challenges…")
                list!!.isEmpty() -> EmptyBox("No challenges are available right now.")
                else -> list!!.forEach { c -> ChallengeCard(c) { openId = c.id } }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ChallengeCard(c: Challenge, onClick: () -> Unit) {
    val color = parseColor(c.color)
    val done = c.progress.completedDays.size
    SurfaceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Text(c.icon, fontSize = 28.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = W.Ink)
                Text(c.subtitle, fontSize = 12.sp, color = W.InkSoft, lineHeight = 17.sp)
            }
            if (c.progress.badgeEarned) Text(c.badgeIcon, fontSize = 26.sp)
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { done.toFloat() / c.totalDays }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = color, trackColor = color.copy(alpha = 0.2f))
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                when {
                    c.progress.completed -> "${c.totalDays} days · Completed 🎉"
                    done == 0 -> "${c.totalDays} days · Not started"
                    else -> "${c.totalDays} days · $done done · ${c.totalDays - done} to go"
                },
                fontSize = 12.sp, color = W.InkMid, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
            )
            Text(if (c.progress.completed) "Review" else if (done == 0) "Start" else "Continue", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChallengeDetailScreen(id: Int, languageId: Int, religionId: Int, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<ChallengeDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf<Int?>(null) }
    var working by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var badge by remember { mutableStateOf<Pair<String, String>?>(null) }
    var explainFor by remember { mutableStateOf<com.scripturedaily.data.Verse?>(null) }

    LaunchedEffect(attempt) {
        error = null
        try {
            val d = withContext(Dispatchers.IO) { ApiClient.api.challenge(id, languageId, religionId, LocalDate.now().toString()) }
            detail = d
            if (expanded == null) expanded = d.days.firstOrNull { it.status == "available" }?.dayNumber
        } catch (_: Exception) { error = "Couldn't load this challenge. Check your connection and try again." }
    }

    OverlayScreen(detail?.title ?: "Challenge", onBack) {
        val d = detail
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                error != null -> ErrorBox(error!!, { attempt++ })
                d == null -> LoadingBox()
                else -> {
                    val color = parseColor(d.color)
                    val done = d.progress.completedDays.size
                    SurfaceCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(d.icon, fontSize = 34.sp); Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.subtitle, fontSize = 13.sp, color = W.InkMid, lineHeight = 18.sp)
                                Text("Badge: ${d.badgeIcon} ${d.badgeName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(d.description, fontSize = 12.sp, color = W.InkSoft, lineHeight = 17.sp)
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(progress = { done.toFloat() / d.totalDays }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = color, trackColor = color.copy(alpha = 0.2f))
                        Text(if (d.progress.completed) "All ${d.totalDays} days complete!" else "${d.totalDays}-day challenge · $done completed · ${d.totalDays - done} remaining", fontSize = 12.sp, color = W.InkMid, modifier = Modifier.padding(top = 6.dp))
                    }
                    if (d.progress.missedDays > 0 && !d.progress.completed && done > 0) Text(
                        "Welcome back 🌿 Your progress is safe — pick up right where you left off at day ${d.progress.nextDay ?: done + 1}.",
                        fontSize = 13.sp, color = W.InkMid, lineHeight = 19.sp,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.12f)).padding(12.dp))
                    if (d.languageFallback) Text("This challenge isn't translated into your language yet, so it is shown in English. Verses are still in your language.", fontSize = 11.sp, color = W.InkSoft)
                    if (d.isUniversal) Text("🌍 Universal challenge: verses come from many traditions.", fontSize = 11.sp, color = W.InkSoft)
                    notice?.let { n -> Text(n, fontSize = 13.sp, color = W.InkMid, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.14f)).padding(12.dp)) }
                    d.days.forEach { day ->
                        DayRow(day, color, expanded == day.dayNumber, working, d.progress.availableTomorrow == true,
                            onToggle = { expanded = if (expanded == day.dayNumber) null else day.dayNumber },
                            onExplain = { explainFor = day.verse },
                            onComplete = {
                                scope.launch {
                                    working = true; notice = null
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            ApiClient.api.joinChallenge(d.id)
                                            ApiClient.api.completeDay(d.id, day.dayNumber, LocalDate.now().toString())
                                        }
                                        if (result.isSuccessful) {
                                            val body = result.body()!!
                                            if (body.badgeEarnedNow) badge = (body.badgeIcon ?: "🏅") to (body.badgeName ?: "Badge")
                                            notice = if (body.progress.completed) "Challenge complete! 🎉" else "Day ${day.dayNumber} done. Come back tomorrow for day ${day.dayNumber + 1}."
                                            expanded = null
                                            attempt++
                                        } else notice = result.problem("Couldn't save your progress. Please try again.")
                                    } catch (_: Exception) { notice = "Couldn't reach the server. Check your connection and try again." }
                                    working = false
                                }
                            })
                    }
                    if (d.progress.completed) OutlinedButton(
                        onClick = { scope.launch { withContext(Dispatchers.IO) { runCatching { ApiClient.api.restartChallenge(d.id) } }; expanded = null; notice = "Fresh start — your badge stays with you."; attempt++ } },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                    ) { Text("Start over") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    badge?.let { (icon, name) ->
        AlertDialog(
            onDismissRequest = { badge = null }, containerColor = W.Parchment,
            title = { Text("Badge unlocked!", fontWeight = FontWeight.Bold, color = W.Ink) },
            text = { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, fontSize = 64.sp); Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = W.Ink, textAlign = TextAlign.Center); Text("You completed the whole challenge. Well done!", fontSize = 13.sp, color = W.InkSoft, textAlign = TextAlign.Center) } },
            confirmButton = { TextButton(onClick = { badge = null }) { Text("Wonderful", color = W.Gold, fontWeight = FontWeight.Bold) } }
        )
    }
    explainFor?.let { ExplainSheet(it) { explainFor = null } }
}

@Composable
private fun DayRow(day: ChallengeDay, color: Color, expanded: Boolean, working: Boolean, waitUntilTomorrow: Boolean, onToggle: () -> Unit, onExplain: () -> Unit, onComplete: () -> Unit) {
    val locked = day.status == "locked"
    val done = day.status == "completed"
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(BorderStroke(if (day.status == "available") 1.5.dp else 1.dp, if (day.status == "available") color else W.Gold.copy(alpha = 0.18f)), RoundedCornerShape(18.dp))
    ) {
        Row(Modifier.fillMaxWidth().clickable(enabled = !locked, onClick = onToggle).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(if (done) color else color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                when {
                    done -> Icon(Icons.Outlined.Check, "Completed", tint = Color.White, modifier = Modifier.size(18.dp))
                    locked -> Icon(Icons.Outlined.Lock, "Locked", tint = W.InkSoft, modifier = Modifier.size(15.dp))
                    else -> Text("${day.dayNumber}", fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Day ${day.dayNumber} · ${day.title}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (locked) W.InkSoft else W.Ink)
                if (locked) Text("Unlocks after day ${day.dayNumber - 1}", fontSize = 11.sp, color = W.InkSoft)
            }
        }
        if (expanded && !locked) Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            day.verse?.let { v ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.10f)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("“${v.text}”", fontSize = 15.sp, lineHeight = 23.sp, color = W.Ink)
                    Text("${v.citation()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
                    TextButton(onClick = onExplain, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("🧒 Explain simply", fontSize = 12.sp, color = W.InkMid, fontWeight = FontWeight.SemiBold) }
                }
            }
            Text("REFLECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
            Text(day.reflection, fontSize = 14.sp, lineHeight = 21.sp, color = W.InkMid)
            Text("TODAY'S ACTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
            Text(day.action, fontSize = 14.sp, lineHeight = 21.sp, color = W.Ink, fontWeight = FontWeight.Medium)
            if (day.status == "available" && waitUntilTomorrow) Text("🌙 Today's day is done. Day ${day.dayNumber} unlocks tomorrow, so come back then.", fontSize = 13.sp, color = W.InkMid, lineHeight = 19.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f)).padding(12.dp))
            else if (day.status == "available") Button(
                onClick = onComplete, enabled = !working, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                if (working) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Mark day ${day.dayNumber} complete", fontWeight = FontWeight.Bold)
            }
        }
    }
}
