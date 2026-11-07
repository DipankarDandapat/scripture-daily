package com.scripturedaily.features.home

import java.time.LocalDate

/** Plain-data description of one daily-verse card theme (no Android dependencies so it can be unit-tested). */
data class VerseTheme(
    val index: Int, val name: String,
    val top: Long, val bottom: Long,          // ARGB gradient stops
    val text: Long, val reference: Long, val accent: Long, val chip: Long,
    val serif: Boolean = false, val art: String
)

/**
 * Ten visually distinct themes for the Home daily-verse card. One theme per calendar day, advancing 1 → 10 and
 * looping, chosen purely from the local date, so it never changes during the day or after a restart and needs no
 * network. Every text colour is checked against both gradient stops for WCAG AA contrast (see VerseThemesTest).
 */
object VerseThemes {
    val ALL: List<VerseTheme> = listOf(
        VerseTheme(1, "Sunrise gold", 0xFFFFE3A3, 0xFFF4B66A, 0xFF33200F, 0xFF5E3A12, 0xFF8A4B14, 0x33FFFFFF, art = "sunrise"),
        VerseTheme(2, "Peaceful nature", 0xFF144434, 0xFF2B7254, 0xFFFFFFFF, 0xFFEDF7F0, 0xFFBFE3A0, 0x26FFFFFF, art = "nature"),
        VerseTheme(3, "Minimalist dark", 0xFF0E0E10, 0xFF202226, 0xFFF4F4F5, 0xFFC9CACE, 0xFFD4A574, 0x1AFFFFFF, art = "minimal"),
        VerseTheme(4, "Soft pastel", 0xFFFDE7EF, 0xFFE3E8FB, 0xFF33294A, 0xFF554A70, 0xFF7B5EA7, 0x40FFFFFF, art = "pastel"),
        VerseTheme(5, "Mountain sky", 0xFF1F3F63, 0xFF446C99, 0xFFFFFFFF, 0xFFF4F8FC, 0xFFFFE2A8, 0x26FFFFFF, art = "mountains"),
        VerseTheme(6, "Floral", 0xFFFCE4EC, 0xFFF6B8CB, 0xFF451029, 0xFF6B2A47, 0xFFB03A6B, 0x40FFFFFF, art = "floral"),
        VerseTheme(7, "Elegant typography", 0xFFF8F2E6, 0xFFE9DDC3, 0xFF2A1F14, 0xFF5B4630, 0xFF9A6B2F, 0x00000000, serif = true, art = "typography"),
        VerseTheme(8, "Ocean", 0xFF063B5C, 0xFF176C93, 0xFFFFFFFF, 0xFFDDF0FA, 0xFF9FE1FF, 0x26FFFFFF, art = "ocean"),
        VerseTheme(9, "Spiritual abstract", 0xFF1A1030, 0xFF4B2A7A, 0xFFF5F0E8, 0xFFD9CFE8, 0xFFD4A574, 0x26FFFFFF, art = "abstract"),
        VerseTheme(10, "Warm sunset", 0xFF4A1A3B, 0xFFA8432A, 0xFFFFF8EE, 0xFFFFEAD6, 0xFFFFC857, 0x26FFFFFF, art = "sunset"),
    )

    /** 0-based theme slot for a date: days since the epoch modulo 10. Deterministic and device-independent. */
    fun indexFor(date: LocalDate): Int = Math.floorMod(date.toEpochDay(), ALL.size.toLong()).toInt()

    fun forDate(date: LocalDate): VerseTheme = ALL[indexFor(date)]

    /** WCAG relative luminance contrast ratio between two opaque ARGB colours. */
    fun contrast(a: Long, b: Long): Double {
        fun lum(c: Long): Double {
            fun ch(v: Long): Double { val s = v / 255.0; return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4) }
            return 0.2126 * ch((c shr 16) and 0xFF) + 0.7152 * ch((c shr 8) and 0xFF) + 0.0722 * ch(c and 0xFF)
        }
        val la = lum(a); val lb = lum(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
