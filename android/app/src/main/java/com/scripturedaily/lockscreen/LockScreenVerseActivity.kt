package com.scripturedaily.lockscreen

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

private val CardBg = androidx.compose.ui.graphics.Color(0xFF2D1B4E)
private val CardGold = androidx.compose.ui.graphics.Color(0xFFD4A574)

/** Verse card drawn over the keyguard. Dismiss returns to the lock screen. */
class LockScreenVerseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val bgColor = CardBg.toArgb()
        window.setBackgroundDrawable(ColorDrawable(bgColor))

        // Modern API — no deprecation warnings
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val scripture = intent.getStringExtra(EXTRA_SCRIPTURE).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val reference = intent.getStringExtra(EXTRA_REFERENCE).orEmpty()
        if (text.isBlank()) { finish(); return }
        setContent {
            LockCard(scripture, text, reference, onDismiss = { finish() })
        }
    }

    companion object {
        const val EXTRA_SCRIPTURE = "scripture"
        const val EXTRA_TEXT = "text"
        const val EXTRA_REFERENCE = "reference"
        const val EXTRA_DEMO = "demo"
    }
}

@Composable
private fun LockCard(scripture: String, text: String, reference: String, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(CardBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "WISDOMONE",
            color = CardGold.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(24.dp))
        Text(
            scripture.uppercase(),
            color = CardGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = CardGold.copy(alpha = 0.2f))
        Spacer(Modifier.height(20.dp))
        Text(
            "\u201c$text\u201d",
            color = androidx.compose.ui.graphics.Color(0xFFF5F0E8),
            fontSize = 26.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(Modifier.height(20.dp))
        Text(
            reference,
            color = androidx.compose.ui.graphics.Color(0xFF8A7A9A),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CardGold,
                contentColor = CardBg
            )
        ) {
            Text("Close", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}
