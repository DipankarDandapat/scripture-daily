package com.scripturedaily.features.journal

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.ReflectionDto
import com.scripturedaily.data.ReflectionSaveBody
import com.scripturedaily.data.ReflectionUpdateBody
import com.scripturedaily.data.TodayReflectionDto
import com.scripturedaily.data.Verse
import com.scripturedaily.ui.LoadingBox
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

const val REFLECTION_MAX = 500

/** Tappable starting thoughts, chosen by the verse's topic (the design shows three per question). */
private fun suggestionsFor(topics: List<String>?): List<Pair<ImageVector, String>> {
    val key = topics.orEmpty().firstOrNull { it in THOUGHTS } ?: "default"
    val texts = THOUGHTS[key] ?: THOUGHTS.getValue("default")
    return listOf(Icons.Outlined.Favorite, Icons.Outlined.Groups, Icons.Outlined.TrackChanges).zip(texts)
}

private val THOUGHTS: Map<String, List<String>> = mapOf(
    "default" to listOf("Be kind to someone", "Help without expectation", "Do my work sincerely"),
    "love" to listOf("Say something kind", "Call someone I care about", "Forgive someone"),
    "patience" to listOf("Pause before reacting", "Listen fully", "Take a deep breath"),
    "forgiveness" to listOf("Let go of a grudge", "Reach out to someone", "Forgive myself"),
    "gratitude" to listOf("Thank someone today", "Notice small blessings", "Count three gifts"),
    "compassion" to listOf("Help someone in need", "Listen without fixing", "Be gentle with myself"),
    "peace" to listOf("Breathe slowly", "Release a worry", "Create a quiet moment"),
    "truth" to listOf("Speak honestly", "Admit a mistake", "Face what I avoid"),
    "humility" to listOf("Ask for help", "Learn from someone", "Celebrate another's win"),
    "hope" to listOf("Take one small step", "Encourage someone", "Look for a bright side"),
    "courage" to listOf("Face one fear", "Stand up for someone", "Do what is right"),
    "charity" to listOf("Give my time", "Share what I have", "Give quietly"),
    "wisdom" to listOf("Pause and think", "Seek advice", "Apply what I learned"),
    "faith" to listOf("Trust the process", "Pray or meditate", "Take a step in faith"),
    "self-control" to listOf("Pause before reacting", "Choose my words", "Say no to a habit"),
)

private fun friendlyDate(iso: String?): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
}.getOrDefault("Today")

/**
 * "Today's Reflection" bottom sheet (opened from the Explain sheet).
 * Loads the verse-specific question, lets the user write up to [REFLECTION_MAX] characters and saves it to the journal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayReflectionSheet(verse: Verse, onDismiss: () -> Unit, onSaved: (ReflectionDto) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var today by remember(verse.id) { mutableStateOf<TodayReflectionDto?>(null) }
    var loadError by remember(verse.id) { mutableStateOf<String?>(null) }
    var attempt by remember(verse.id) { mutableStateOf(0) }
    var answer by remember(verse.id) { mutableStateOf("") }
    var saving by remember(verse.id) { mutableStateOf(false) }
    var saveError by remember(verse.id) { mutableStateOf<String?>(null) }
    var saved by remember(verse.id) { mutableStateOf<ReflectionDto?>(null) }

    LaunchedEffect(verse.id, attempt) {
        loadError = null
        try {
            val t = withContext(Dispatchers.IO) { ApiClient.api.todaysReflection(verse.id, LocalDate.now().toString()) }
            today = t
            if (answer.isEmpty()) answer = t.existing?.answer.orEmpty()
        } catch (_: Exception) { loadError = "Couldn't load today's question. Check your connection and try again." }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = W.Parchment) {
        val done = saved
        if (done != null) SavedConfirmation(done, onDismiss)
        else {
            val t = today
            when {
                loadError != null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loadError!!, color = W.Error, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { attempt++ }) { Text("Try again", color = W.Ink) }
                }
                t == null -> LoadingBox("Finding today's question…")
                else -> ReflectionForm(
                    title = "Today's Reflection", subtitle = "A moment to connect this wisdom with your life.", dateLabel = friendlyDate(t.reflectionDate),
                    reference = t.verseReference, question = t.question, answer = answer, onAnswer = { answer = it.take(t.maxLength); saveError = null },
                    maxLength = t.maxLength, topics = verse.topics.orEmpty(), saving = saving, error = saveError,
                    primaryLabel = if (t.existing != null) "Update Reflection" else "Save Reflection",
                    onCancel = onDismiss,
                    onSave = {
                        if (answer.isBlank()) { saveError = "Please write a few words before saving."; return@ReflectionForm }
                        scope.launch {
                            saving = true; saveError = null
                            try {
                                val r = withContext(Dispatchers.IO) { ApiClient.api.saveReflection(ReflectionSaveBody(verse.id, answer.trim(), LocalDate.now().toString())) }
                                if (r.isSuccessful) { saved = r.body(); onSaved(r.body()!!) }
                                else saveError = if (r.code() == 422) "Please check your reflection and try again." else "Couldn't save (error ${r.code()}). Please try again."
                            } catch (_: Exception) { saveError = "Couldn't save. Check your connection and try again — your words are still here." }
                            saving = false
                        }
                    }
                )
            }
        }
    }
}

/** Edit an existing journal entry (opened from Journal → Reflections → Edit). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReflectionSheet(reflection: ReflectionDto, onDismiss: () -> Unit, onSaved: (ReflectionDto) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var answer by remember(reflection.id) { mutableStateOf(reflection.answer) }
    var saving by remember(reflection.id) { mutableStateOf(false) }
    var error by remember(reflection.id) { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = W.Parchment) {
        ReflectionForm(
            title = "Edit Reflection", subtitle = "Your original question stays with this entry.", dateLabel = friendlyDate(reflection.reflectionDate),
            reference = reflection.verseReference, question = reflection.question, answer = answer, onAnswer = { answer = it.take(REFLECTION_MAX); error = null },
            maxLength = REFLECTION_MAX, topics = null, saving = saving, error = error, primaryLabel = "Save Changes", onCancel = onDismiss,
            onSave = {
                if (answer.isBlank()) { error = "Your reflection can't be empty. Use Delete if you want to remove it."; return@ReflectionForm }
                scope.launch {
                    saving = true; error = null
                    try {
                        val r = withContext(Dispatchers.IO) { ApiClient.api.updateReflection(reflection.id, ReflectionUpdateBody(answer.trim())) }
                        if (r.isSuccessful) { Toast.makeText(context, "Reflection updated", Toast.LENGTH_SHORT).show(); onSaved(r.body()!!) }
                        else error = "Couldn't update (error ${r.code()}). Please try again."
                    } catch (_: Exception) { error = "Couldn't update. Check your connection and try again." }
                    saving = false
                }
            }
        )
    }
}

@Composable
private fun ReflectionForm(
    title: String, subtitle: String, dateLabel: String, reference: String, question: String, answer: String, onAnswer: (String) -> Unit,
    maxLength: Int, topics: List<String>?, saving: Boolean, error: String?, primaryLabel: String, onCancel: () -> Unit, onSave: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌱", fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = W.Ink, modifier = Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(50)).background(W.SurfaceAlt).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = W.InkMid, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(dateLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = W.InkMid)
            }
        }
        Text(subtitle, fontSize = 14.sp, color = W.InkMid)
        if (reference.isNotBlank()) Text(reference, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = W.Gold)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (W.IsDark) Color(0xFF3A2E24) else Color(0xFFFCE9D6)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.TrackChanges, null, tint = Color(0xFFD9534F), modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Text(question, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = W.Ink, modifier = Modifier.semantics { contentDescription = "Reflection question: $question" })
        }
        OutlinedTextField(
            value = answer, onValueChange = onAnswer, modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
            placeholder = { Text("Write your reflection here...", color = W.InkSoft) },
            shape = RoundedCornerShape(16.dp), isError = error != null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = W.Ink, unfocusedTextColor = W.Ink, cursorColor = W.Purple,
                focusedBorderColor = W.Purple, unfocusedBorderColor = W.InkSoft.copy(alpha = 0.6f), errorBorderColor = W.Error,
                focusedContainerColor = W.Surface, unfocusedContainerColor = W.Surface
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(error.orEmpty(), color = W.Error, fontSize = 12.sp, modifier = Modifier.weight(1f).semantics { contentDescription = error.orEmpty() })
            Text("${answer.length} / $maxLength", fontSize = 13.sp, color = if (answer.length >= maxLength) W.Error else W.InkMid)
        }
        if (topics != null) {
            Text("Need a little help? Tap a thought to use it:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = W.InkMid)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestionsFor(topics).forEach { (icon, text) ->
                    Row(
                        Modifier.weight(1f).heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).background(W.Surface).border(BorderStroke(1.dp, W.Gold.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                            .clickable { onAnswer(if (answer.isBlank()) text else "${answer.trimEnd()} $text") }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = if (icon == Icons.Outlined.Favorite) Color(0xFFE5547A) else if (icon == Icons.Outlined.Groups) W.PurpleMid else Color(0xFFD9534F), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, color = W.Ink)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, enabled = !saving, modifier = Modifier.weight(0.8f).height(54.dp), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, W.Gold.copy(alpha = 0.5f))) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = W.Ink)
            }
            Button(onClick = onSave, enabled = !saving, modifier = Modifier.weight(1.5f).height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Purple, contentColor = W.OnPurple)) {
                if (saving) CircularProgressIndicator(color = W.OnPurple, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else { Icon(Icons.Outlined.Check, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(primaryLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun SavedConfirmation(r: ReflectionDto, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(W.Success), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
        Text("Reflection saved", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = W.Ink)
        Text("It's in your Journal under Discover → Journal → Reflections. Come back any time to read or edit it.", fontSize = 14.sp, color = W.InkMid, lineHeight = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("“${r.answer}”", fontSize = 14.sp, color = W.InkMid, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = W.Purple, contentColor = W.OnPurple)) {
            Text("Done", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
