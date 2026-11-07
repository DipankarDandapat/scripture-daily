package com.scripturedaily.lockscreentime

import android.graphics.Color
import java.time.LocalDateTime

/** Stable four-period palette derived from the device's local clock. */
data class TimePalette(
    val period: String,
    val background: Int,
    val accent: Int,
    val primaryText: Int,
    val secondaryText: Int
) {
    companion object {
        fun now(intervalHours: Int = 6, time: LocalDateTime = LocalDateTime.now()): TimePalette {
            val interval = intervalHours.takeIf { it in setOf(1, 6, 12, 24) } ?: 6
            val palettes = listOf(
                TimePalette("Night", Color.rgb(24, 31, 54), Color.rgb(174, 164, 225), Color.rgb(247, 245, 255), Color.rgb(190, 195, 218)),
                TimePalette("Morning", Color.rgb(35, 76, 64), Color.rgb(238, 198, 124), Color.rgb(250, 248, 239), Color.rgb(205, 222, 209)),
                TimePalette("Afternoon", Color.rgb(45, 75, 92), Color.rgb(246, 205, 132), Color.rgb(255, 251, 242), Color.rgb(213, 226, 231)),
                TimePalette("Evening", Color.rgb(68, 43, 65), Color.rgb(238, 174, 133), Color.rgb(255, 246, 239), Color.rgb(224, 198, 198))
            )
            val localHour = time.toLocalDate().toEpochDay() * 24L + time.hour
            val paletteIndex = Math.floorMod(localHour / interval, palettes.size.toLong()).toInt()
            return palettes[paletteIndex]
        }
    }
}
