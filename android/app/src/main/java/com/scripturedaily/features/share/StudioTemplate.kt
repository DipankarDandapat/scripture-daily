package com.scripturedaily.features.share

import android.content.Context
import android.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.scripturedaily.data.ApiClient
import com.scripturedaily.data.StudioTemplateDto

/**
 * A Share / Video Studio template. [tradition] == null marks a *common* template (shown to everyone);
 * otherwise it is the religion code the template belongs to and it is shown only to that tradition.
 * Templates are data (the `studio_templates` table); the lists below are the offline fallback.
 */
data class StudioTemplate(
    val code: String, val label: String, val tradition: String?, val motif: String,
    val top: Int, val bottom: Int, val text: Int, val accent: Int, val muted: Int, val light: Boolean, val emoji: String = ""
) {
    val isCommon get() = tradition == null
}

fun StudioTemplateDto.toTemplate() = StudioTemplate(
    code, name, tradition, motif, hex(colorTop), hex(colorBottom), hex(colorText), hex(colorAccent), hex(colorMuted), isLight, emoji
)

private fun hex(s: String): Int = runCatching { Color.parseColor(s) }.getOrDefault(Color.BLACK)
private fun t(code: String, label: String, tradition: String?, motif: String, top: String, bottom: String, text: String, accent: String, muted: String, light: Boolean, emoji: String) =
    StudioTemplate(code, label, tradition, motif, hex(top), hex(bottom), hex(text), hex(accent), hex(muted), light, emoji)

/** A background colour the user can apply on top of any template; text colours adapt for contrast. */
data class BgSwatch(val label: String, val top: Int, val bottom: Int, val light: Boolean)

object StudioTemplates {
    const val GENERAL = "general wisdom"
    private val gson = Gson()

    val DEFAULT = t("midnight", "Midnight", null, "mandala", "#0F0A1A", "#2D1B4E", "#F5F0E8", "#D4A574", "#B5A8C4", false, "🌙")

    val BUILT_IN: List<StudioTemplate> = listOf(
        DEFAULT,
        t("sunrise", "Sunrise", null, "rays", "#F6A55F", "#FCE3B4", "#3B2314", "#8A4B14", "#6B4A33", true, "🌅"),
        t("forest", "Forest", null, "leaves", "#0E2A22", "#1F5A44", "#EEF5EE", "#BFE3A0", "#A9C9B5", false, "🌲"),
        t("ocean", "Ocean", null, "waves", "#0A2540", "#1E6F9F", "#FFFFFF", "#9FE1FF", "#B8D8EA", false, "🌊"),
        t("rose", "Rose", null, "bokeh", "#3A1426", "#A24A6A", "#FFF1F4", "#FFC4D2", "#E6B9C5", false, "🌸"),
        t("ivory", "Ivory", null, "none", "#FBF6EC", "#EADFC8", "#2A1F14", "#9A6B2F", "#6F5B45", true, "📜"),
        t("slate", "Slate", null, "none", "#1C2430", "#3A4A5E", "#F1F4F8", "#9CC3E8", "#B3BFCC", false, "🪨"),
        t("lavender", "Lavender mist", null, "bokeh", "#EDE7F6", "#C9B8E8", "#2B1F45", "#5E3FA3", "#5B4D7A", true, "💜"),
        t("om-saffron", "Om", "hinduism", "om", "#2A0E05", "#8A3A0B", "#FFF3E0", "#FFC857", "#E3B98A", false, "🕉️"),
        t("krishna-peacock", "Krishna · Peacock feather", "hinduism", "peacock", "#0A2F3A", "#1B6B6B", "#F2FBF8", "#7FE0C8", "#A8D5D0", false, "🪈"),
        t("radha-lotus", "Radha · Lotus", "hinduism", "lotus", "#4A1530", "#C0557F", "#FFF1F5", "#FFD1DC", "#F0BCCB", false, "🪷"),
        t("shiva-moon", "Shiva · Trident & moon", "hinduism", "trident", "#0B1021", "#2B3A67", "#EEF1FA", "#B8C4FF", "#AAB4D6", false, "🔱"),
        t("ganesha-diya", "Ganesha · Diya light", "hinduism", "diya", "#3A1A00", "#B5651D", "#FFF6E5", "#FFD27A", "#F0CFA0", false, "🪔"),
        t("cross-light", "Cross of light", "christianity", "cross", "#14213D", "#3E5C9A", "#F4F7FF", "#F2D388", "#B8C6E6", false, "✝️"),
        t("dove-peace", "Dove of peace", "christianity", "dove", "#E8F1FB", "#BFD6EE", "#17304F", "#3A6EA5", "#456B94", true, "🕊️"),
        t("olive-branch", "Olive branch", "christianity", "olive", "#1D3A2A", "#587A4B", "#F6F8EE", "#E6D88F", "#C2CFAE", false, "🌿"),
        t("stained-glass", "Church window", "christianity", "stained", "#2A1240", "#7A3E8E", "#FFF7FB", "#FFD479", "#E0BEE6", false, "⛪"),
        t("geometry-emerald", "Geometric emerald", "islam", "geometry", "#06302A", "#0F6B57", "#F0FBF6", "#E3C468", "#A9D0C4", false, "🕌"),
        t("crescent-night", "Crescent night", "islam", "crescent", "#0B1B3A", "#1D3C6E", "#F2F5FC", "#F5D77A", "#AEBBD8", false, "🌙"),
        t("mosque-dusk", "Mosque at dusk", "islam", "mosque", "#3A2A52", "#A9553A", "#FFF8EF", "#FFE0A3", "#FBE3D2", false, "🕌"),
        t("arabesque-gold", "Arabesque gold", "islam", "arabesque", "#1F1608", "#5C4318", "#FFF6DF", "#F0C75E", "#D8C192", false, "✨"),
        t("lotus-calm", "Lotus calm", "buddhism", "lotus", "#2C1B3F", "#8E4A7E", "#FFF2FA", "#FFD1EA", "#E3BCD6", false, "🪷"),
        t("dharma-wheel", "Dharma wheel", "buddhism", "dharma", "#3B1305", "#A4601A", "#FFF4E2", "#FFD27A", "#EBCB9E", false, "☸️"),
        t("bodhi-leaf", "Bodhi leaf", "buddhism", "bodhi", "#0F2E1C", "#3C7A4B", "#F0F9F1", "#C8EAA0", "#A9CDB0", false, "🍃"),
        t("meditation-dawn", "Meditation dawn", "buddhism", "halo", "#F9D9A8", "#F6A85B", "#3A2210", "#8A4A10", "#6B4A2A", true, "🧘"),
        t("nature-mountains", "Mountains", GENERAL, "mountains", "#1B3A4B", "#4A6F90", "#F6FAFD", "#FFE2A8", "#DCE7F1", false, "⛰️"),
        t("gratitude-sunrise", "Gratitude sunrise", GENERAL, "sunrise", "#F7B267", "#FFE3B8", "#3A2112", "#9A4A12", "#6B4A33", true, "🌄"),
        t("peace-leaves", "Quiet leaves", GENERAL, "leaves", "#193D2E", "#3B7A53", "#F3FAF4", "#D6EFA4", "#D3E8D9", false, "🍃"),
        t("calm-waves", "Calm waves", GENERAL, "waves", "#0D3050", "#3A8CBB", "#FFFFFF", "#BFEBFF", "#C5DDEC", false, "🌊"),
        t("universal-stars", "Universal stars", GENERAL, "stars", "#0B0D24", "#2B2A66", "#F4F2FF", "#FFD98A", "#B9B6DC", false, "✨"),
    )

    val SWATCHES = listOf(
        BgSwatch("Deep purple", hex("#1A1025").toInt(), hex("#3D2060").toInt(), false),
        BgSwatch("Charcoal", hex("#16181D").toInt(), hex("#363B46").toInt(), false),
        BgSwatch("Forest", hex("#0F2A20").toInt(), hex("#2E6B4E").toInt(), false),
        BgSwatch("Sky", hex("#1B4F7A").toInt(), hex("#6FB1E0").toInt(), false),
        BgSwatch("Gold", hex("#E7B25A").toInt(), hex("#FBE7B8").toInt(), true),
        BgSwatch("Cream", hex("#FBF6EC").toInt(), hex("#EFE4CC").toInt(), true),
        BgSwatch("Blush", hex("#F7D9E1").toInt(), hex("#FBEFF2").toInt(), true),
        BgSwatch("Mint", hex("#D3EEDF").toInt(), hex("#EEF9F2").toInt(), true),
    )

    /** The visibility rule: common templates for everyone, tradition templates only for the matching tradition. */
    fun visible(all: List<StudioTemplate>, religionCode: String?): List<StudioTemplate> =
        all.filter { it.tradition == null || (religionCode != null && it.tradition.equals(religionCode, ignoreCase = true)) }
            .sortedBy { it.isCommon }  // the user's own tradition first, then the common templates

    private fun cacheKey(religionCode: String?) = "studio_templates_${religionCode ?: "none"}"

    /** Cached server catalogue for this tradition if we have one, else the built-in fallback — always filtered. */
    fun load(context: Context, religionCode: String?): List<StudioTemplate> {
        val prefs = context.getSharedPreferences("reader", Context.MODE_PRIVATE)
        val cached = prefs.getString(cacheKey(religionCode), null)?.let { json ->
            runCatching { gson.fromJson<List<StudioTemplateDto>>(json, object : TypeToken<List<StudioTemplateDto>>() {}.type).map { it.toTemplate() } }.getOrNull()
        }?.takeIf { it.isNotEmpty() }
        return visible(cached ?: BUILT_IN, religionCode).ifEmpty { listOf(DEFAULT) }
    }

    /** Fetches the catalogue (the server applies the tradition filter) and caches it for offline use. */
    suspend fun refresh(context: Context, religionId: Int?, religionCode: String?): List<StudioTemplate>? = try {
        val dtos = ApiClient.api.studioTemplates("all", religionId)
        context.getSharedPreferences("reader", Context.MODE_PRIVATE).edit().putString(cacheKey(religionCode), gson.toJson(dtos)).apply()
        visible(dtos.map { it.toTemplate() }, religionCode).ifEmpty { null }
    } catch (_: Exception) { null }

    fun find(list: List<StudioTemplate>, code: String?): StudioTemplate = list.firstOrNull { it.code == code } ?: list.firstOrNull() ?: DEFAULT
}
