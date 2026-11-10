package com.scripturedaily.features.explain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
<<<<<<< HEAD
import androidx.compose.foundation.shape.RoundedCornerShape
=======
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
>>>>>>> settings_redesign
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.Verse
import com.scripturedaily.ui.ErrorBox
import com.scripturedaily.ui.LoadingBox
import com.scripturedaily.ui.W
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val cache = HashMap<Int, String>()

/** "Explain this to me like I'm 5": a short, simple, modern explanation of the verse, in the verse's own language. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
<<<<<<< HEAD
fun ExplainSheet(verse: Verse, onDismiss: () -> Unit) {
=======
fun ExplainSheet(verse: Verse, onDismiss: () -> Unit, onReflect: (() -> Unit)? = null) {
>>>>>>> settings_redesign
    var text by remember(verse.id) { mutableStateOf(cache[verse.id]) }
    var error by remember(verse.id) { mutableStateOf<String?>(null) }
    var attempt by remember(verse.id) { mutableIntStateOf(0) }

    LaunchedEffect(verse.id, attempt) {
        if (text != null) return@LaunchedEffect
        error = null
        try {
            val r = withContext(Dispatchers.IO) { ApiClient.api.explain(verse.id) }
            if (r.isSuccessful) { text = r.body()!!.text; cache[verse.id] = text!! }
            else error = if (r.code() == 404) "We don't have a simple explanation for this verse yet." else "Couldn't load the explanation (error ${r.code()})."
        } catch (_: Exception) { error = "Couldn't load the explanation. Check your connection and try again." }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = W.Parchment) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("🧒", fontSize = 26.sp)
                Text("  Explained simply", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = W.Ink)
            }
            Text("“${verse.text}”", fontSize = 14.sp, lineHeight = 21.sp, color = W.InkSoft, maxLines = 5, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text("${verse.citation()}", fontSize = 12.sp, color = W.Gold, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            val t = text
            when {
                t != null -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(W.Gold.copy(alpha = 0.14f)).padding(18.dp)) {
                    Text(t, fontSize = 19.sp, lineHeight = 29.sp, color = W.Ink, fontWeight = FontWeight.Medium)
                }
                error != null -> ErrorBox(error!!, { attempt++ })
                else -> LoadingBox("Making it simple…")
            }
<<<<<<< HEAD
=======
            if (onReflect != null) {
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.Button(
                    onClick = onReflect, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = W.Purple, contentColor = W.OnPurple)
                ) {
                    Text("🌱  Today's Reflection", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                }
            }
>>>>>>> settings_redesign
        }
    }
}
