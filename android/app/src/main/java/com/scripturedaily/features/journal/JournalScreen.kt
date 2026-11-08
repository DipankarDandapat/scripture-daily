package com.scripturedaily.features.journal

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.ads.AdSlot
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.ReflectionDto
import com.scripturedaily.features.mood.MoodJournalContent
import com.scripturedaily.ui.EmptyBox
import com.scripturedaily.ui.ErrorBox
import com.scripturedaily.ui.LoadingBox
import com.scripturedaily.ui.OverlayScreen
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Discover → Journal. Two tabs, as in the Journal design: **Mood** (check-in statistics) and **Reflections** (everything the user
 * saved from "Today's Reflection": browse by month/week, open, edit, share, delete).
 */
@Composable
fun JournalScreen(onBack: () -> Unit, onWriteFirstReflection: () -> Unit, initialTab: Int = 0) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    OverlayScreen("Journal", onBack) {
        Text("Track your feelings and reflect on your journey.", fontSize = 13.sp, color = W.InkMid, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))
        JournalTabs(tab) { tab = it }
        Spacer(Modifier.height(4.dp))
        if (tab == 0) MoodJournalContent(Modifier.weight(1f), onOpenReflections = { tab = 1 })
        else ReflectionsTab(Modifier.weight(1f), onWriteFirstReflection)
    }
}

@Composable
private fun JournalTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(W.SurfaceAlt).padding(4.dp)) {
        listOf("🙂" to "Mood", "🌱" to "Reflections").forEachIndexed { i, (emoji, label) ->
            val on = i == selected
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (on) W.Purple else Color.Transparent).clickable { onSelect(i) }.padding(vertical = 12.dp)
                    .semantics { contentDescription = "$label tab${if (on) ", selected" else ""}" },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 18.sp); Spacer(Modifier.width(8.dp))
                Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (on) W.OnPurple else W.Ink)
            }
        }
    }
}

@Composable
private fun ReflectionsTab(modifier: Modifier, onWriteFirstReflection: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var items by remember { mutableStateOf<List<ReflectionDto>?>(null) }
    var totalEver by remember { mutableIntStateOf(-1) }
    var markedDates by remember { mutableStateOf(emptySet<LocalDate>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<ReflectionDto?>(null) }
    var deleting by remember { mutableStateOf<ReflectionDto?>(null) }

    LaunchedEffect(month, reload) {
        error = null
        try {
            val page = withContext(Dispatchers.IO) { ApiClient.api.reflections(limit = 100, month = month.toString()) }
            items = page.items
            val dates = withContext(Dispatchers.IO) { ApiClient.api.reflectionDates(120) }
            markedDates = dates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
            if (totalEver < 0 || page.total > 0) totalEver = if (page.total > 0) page.total else totalEver.coerceAtLeast(0)
            if (page.total == 0 && totalEver <= 0) totalEver = withContext(Dispatchers.IO) { ApiClient.api.reflections(limit = 1).total }
        } catch (_: Exception) { error = "Couldn't load your reflections. Check your connection and try again." }
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MonthBar(month, onPrev = { month = month.minusMonths(1); selectedDay = null }, onNext = { month = month.plusMonths(1); selectedDay = null })
        WeekStrip(selectedDay ?: if (YearMonth.from(LocalDate.now()) == month) LocalDate.now() else month.atDay(1), markedDates, selectedDay) { d -> selectedDay = if (selectedDay == d) null else d }
        val all = items
        val shown = all?.let { list -> selectedDay?.let { d -> list.filter { it.reflectionDate == d.toString() } } ?: list }
        when {
            error != null -> ErrorBox(error!!, { reload++ })
            shown == null -> LoadingBox("Loading your reflections…")
            shown.isEmpty() && totalEver <= 0 -> EmptyReflections(onWriteFirstReflection)
            shown.isEmpty() -> EmptyBox(if (selectedDay != null) "No reflection on this day." else "No reflections in ${month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))}.")
            else -> shown.forEachIndexed { index, r ->
                ReflectionCard(r, onEdit = { editing = r }, onDelete = { deleting = r }, onShare = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${r.verseReference}\n\n${r.question}\n\n“${r.answer}”\n\n— my reflection on WisdomOne")
                    context.startActivity(Intent.createChooser(send, "Share reflection"))
                })
                if (index == 1) AdSlot("journal_list_inline")
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    editing?.let { r ->
        EditReflectionSheet(r, onDismiss = { editing = null }, onSaved = { updated -> items = items?.map { if (it.id == updated.id) updated else it }; editing = null })
    }
    deleting?.let { r ->
        AlertDialog(
            onDismissRequest = { deleting = null }, containerColor = W.Surface,
            title = { Text("Delete this reflection?", fontWeight = FontWeight.Bold, color = W.Ink) },
            text = { Text("“${r.answer.take(120)}${if (r.answer.length > 120) "…" else ""}”\n\nThis will permanently delete your reflection. This can't be undone.", color = W.InkMid, fontSize = 14.sp, lineHeight = 20.sp) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch {
                        val ok = runCatching { withContext(Dispatchers.IO) { ApiClient.api.deleteReflection(r.id) }.isSuccessful }.getOrDefault(false)
                        if (ok) { items = items?.filter { it.id != r.id }; reload++; Toast.makeText(context, "Reflection deleted", Toast.LENGTH_SHORT).show() }
                        else Toast.makeText(context, "Couldn't delete. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Delete", color = W.Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel", color = W.InkMid) } }
        )
    }
}

@Composable
private fun MonthBar(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous month", onPrev)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = W.PurpleMid, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = W.Ink)
        }
        RoundIcon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next month", onNext)
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(W.SurfaceAlt).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, description, tint = W.Ink) }
}

/** Mon–Sun strip for the week containing [anchor]; a check marks days that have a saved reflection. */
@Composable
private fun WeekStrip(anchor: LocalDate, marked: Set<LocalDate>, selected: LocalDate?, onPick: (LocalDate) -> Unit) {
    val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until 7) {
            val d = start.plusDays(i.toLong())
            val has = d in marked
            val isSel = d == selected
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (has) W.Success.copy(alpha = 0.14f) else W.Surface)
                    .border(BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) W.Purple else W.Gold.copy(alpha = 0.3f)), RoundedCornerShape(14.dp))
                    .clickable(enabled = !d.isAfter(today)) { onPick(d) }.padding(vertical = 8.dp)
                    .semantics { contentDescription = "${d.format(DateTimeFormatter.ofPattern("EEEE MMMM d", Locale.getDefault()))}, ${if (has) "reflection saved" else "no reflection"}" },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${d.dayOfMonth}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (d.isAfter(today)) W.InkSoft else W.Ink)
                Text(d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()), fontSize = 11.sp, color = W.InkMid)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.size(18.dp).clip(CircleShape).background(if (has) W.Success else W.InkSoft.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    if (has) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ReflectionCard(r: ReflectionDto, onEdit: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    val date = remember(r.reflectionDate) { runCatching { LocalDate.parse(r.reflectionDate).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())) }.getOrDefault(r.reflectionDate) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(W.Surface).border(BorderStroke(1.dp, W.Gold.copy(alpha = 0.25f)), RoundedCornerShape(22.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(date, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = W.Ink, modifier = Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(50)).background(W.Success.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).clip(CircleShape).background(W.Success), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(11.dp)) }
                Spacer(Modifier.width(6.dp))
                Text("Reflection Saved", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = W.Success)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🪷", fontSize = 18.sp); Spacer(Modifier.width(8.dp))
            Text(r.verseReference.ifBlank { r.scripture }, fontSize = 13.sp, color = W.InkMid, fontWeight = FontWeight.Medium)
        }
        if (r.verseText.isNotBlank()) Text("“${r.verseText}”", fontSize = 15.sp, fontStyle = FontStyle.Italic, color = W.Ink, lineHeight = 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (W.IsDark) Color(0xFF3A2E24) else Color(0xFFFCE9D6)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.TrackChanges, null, tint = Color(0xFFD9534F), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text("Reflection Question", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (W.IsDark) W.Gold else Color(0xFF9A5B1E))
            }
            Text(r.question, fontSize = 15.sp, lineHeight = 21.sp, color = W.Ink, modifier = Modifier.padding(top = 4.dp))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(W.SurfaceAlt).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Edit, null, tint = W.PurpleMid, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text("My Reflection", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = W.PurpleMid)
            }
            Text(r.answer, fontSize = 15.sp, lineHeight = 21.sp, color = W.Ink, modifier = Modifier.padding(top = 4.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip(Icons.Outlined.Edit, "Edit", W.Ink, Modifier.weight(1f), onEdit)
            ActionChip(Icons.Outlined.Share, "Share", W.Ink, Modifier.weight(1f), onShare)
            ActionChip(Icons.Outlined.Delete, "Delete", W.Error, Modifier.weight(1f), onDelete)
        }
    }
}

@Composable
private fun ActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(46.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, W.Gold.copy(alpha = 0.4f)), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
        Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun EmptyReflections(onWrite: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(W.Surface).border(BorderStroke(1.dp, W.Gold.copy(alpha = 0.25f)), RoundedCornerShape(22.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🌱", fontSize = 44.sp)
        Text("Your journal is ready", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = W.Ink)
        Text("Reflect on today's verse in a few words. Your thoughts are saved here, privately, so you can see how you grow.", fontSize = 14.sp, color = W.InkMid, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Button(onClick = onWrite, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Purple, contentColor = W.OnPurple)) {
            Text("Write your first reflection", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
