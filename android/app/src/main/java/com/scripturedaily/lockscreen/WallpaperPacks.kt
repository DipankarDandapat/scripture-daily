package com.scripturedaily.lockscreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** The three mutually-exclusive lock-screen behaviours (Settings → Lock Screen & Wallpaper). Persisted as `wallpaper_mode`. */
object WallpaperMode {
    /** "Change Verse and Image Together" (default): every new verse also advances the artwork inside the chosen tradition's pack. */
    const val VERSE_AND_IMAGE = "verse_and_image"
    /** "Keep Same Image": the verse changes, the selected artwork stays. */
    const val KEEP_IMAGE = "keep_image"
    /** Both options off: the verse is drawn on the multi-theme colour backgrounds, no artwork. */
    const val COLORS = "colors"

    fun normalize(raw: String?): String = when (raw?.trim()?.lowercase()) {
        KEEP_IMAGE -> KEEP_IMAGE
        COLORS -> COLORS
        else -> VERSE_AND_IMAGE      // includes legacy change_verse / keep_same_verse and the default
    }
}

/** 20 bundled wallpapers per tradition, stored as assets/wallpapers/<slug>/NN.webp (NN = 01..20). */
object WallpaperPacks {
    const val COUNT = 20

    fun slug(religionId: Int): String = when (religionId) { 1 -> "hinduism"; 2 -> "christianity"; 3 -> "islam"; 4 -> "buddhism"; else -> "general" }

    fun traditionName(religionId: Int): String = when (religionId) { 1 -> "Hinduism"; 2 -> "Christianity"; 3 -> "Islam"; 4 -> "Buddhism"; else -> "General Wisdom" }

    fun clampIndex(i: Int) = i.coerceIn(1, COUNT)

    /** Artwork used for the n-th refresh in "change together" mode: starts at the selected image and walks through the pack. */
    fun rotatedIndex(selected: Int, rotation: Int): Int = Math.floorMod(clampIndex(selected) - 1 + rotation, COUNT) + 1

    /** Decodes an image; [maxWidthPx] > 0 down-samples it (thumbnails). Returns null if the asset is missing. */
    fun load(context: Context, religionId: Int, index: Int, maxWidthPx: Int = 0): Bitmap? = runCatching {
        val path = "wallpapers/${slug(religionId)}/%02d.webp".format(clampIndex(index))
        val opts = BitmapFactory.Options()
        if (maxWidthPx > 0) {
            context.assets.open(path).use { s ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(s, null, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= maxWidthPx) sample *= 2
                opts.inSampleSize = sample
            }
        }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()
}
