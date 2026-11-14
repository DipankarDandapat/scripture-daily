package com.scripturedaily.features.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/** Persists the user's last studio choices. The template is stored by code and resolved against the tradition's visible list. */
object StudioPrefs {
    private const val FILE = "reader"

    fun load(context: Context, templates: List<StudioTemplate> = StudioTemplates.BUILT_IN): CardStyle {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        fun <T : Enum<T>> pick(key: String, values: Array<T>, default: T): T =
            values.firstOrNull { it.name == p.getString(key, null) } ?: default
        return CardStyle(
            template = StudioTemplates.find(templates, p.getString("studio_template_code", null)),
            pattern = pick("studio_pattern_v2", CardPattern.values(), CardPattern.TEMPLATE),
            format = pick("studio_format", CardFormat.values(), CardFormat.STORY),
            fontScale = p.getFloat("studio_font_scale", 1f).coerceIn(0.7f, 1.5f),
            font = pick("studio_font", CardFont.values(), CardFont.SERIF),
            showMark = p.getBoolean("studio_mark", true),
            align = pick("studio_align", TextAlignment.values(), TextAlignment.CENTER),
            spacing = p.getFloat("studio_spacing", 1f).coerceIn(0.8f, 1.8f),
            placement = pick("studio_placement", TextPlacement.values(), TextPlacement.MIDDLE),
            textColor = if (p.contains("studio_text_color")) p.getInt("studio_text_color", 0) else null,
            swatch = StudioTemplates.SWATCHES.firstOrNull { it.label == p.getString("studio_swatch", null) }
        )
    }

    fun save(context: Context, s: CardStyle) {
        val e = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("studio_template_code", s.template.code).putString("studio_pattern_v2", s.pattern.name)
            .putString("studio_format", s.format.name).putFloat("studio_font_scale", s.fontScale)
            .putString("studio_font", s.font.name).putBoolean("studio_mark", s.showMark)
            .putString("studio_align", s.align.name).putFloat("studio_spacing", s.spacing).putString("studio_placement", s.placement.name)
            .putString("studio_swatch", s.swatch?.label)
        if (s.textColor != null) e.putInt("studio_text_color", s.textColor) else e.remove("studio_text_color")
        e.apply()
    }
}

object ShareActions {
    private val WHATSAPP = listOf("com.whatsapp", "com.whatsapp.w4b")
    private const val SHARE_DIR = "shared"

    private fun cacheUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Writes the card to a uniquely named cache file (so a receiving app never sees a stale file with the same name). */
    fun writeCacheImage(context: Context, bmp: Bitmap, name: String = "wisdomone_card.png"): Uri {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        pruneOld(dir)
        val unique = name.substringBeforeLast('.') + "_" + System.currentTimeMillis() + "." + name.substringAfterLast('.', "png")
        val file = File(dir, unique)
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return cacheUri(context, file)
    }

    /** Old shared files are removed after a day; recent ones stay so the receiving app can still read them. */
    private fun pruneOld(dir: File) {
        val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) runCatching { it.delete() } }
    }

    /**
     * Builds an ACTION_SEND for a content Uri. The Uri is also attached as ClipData and read permission is granted
     * explicitly to the target, so the receiving app can open it whatever its Android version.
     */
    private fun sendIntent(context: Context, uri: Uri, mime: String, caption: String?, pkg: String?): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            if (!caption.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, caption)
            clipData = android.content.ClipData.newRawUri("WisdomOne", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (pkg != null) {
                `package` = pkg
                runCatching { context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
        }

    private fun chooser(context: Context, send: Intent, title: String) {
        val chooser = Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareImage(context: Context, bmp: Bitmap, caption: String) {
        val uri = writeCacheImage(context, bmp)
        chooser(context, sendIntent(context, uri, "image/png", caption, null), "Share verse card")
    }

    private fun installedWhatsApp(context: Context): String? =
        WHATSAPP.firstOrNull { pkg -> runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess }

    /**
     * Opens WhatsApp's share screen directly with the image/video attached. The caption is deliberately NOT sent as
     * EXTRA_TEXT here: some WhatsApp builds then treat the share as text-only and drop the media. Falls back to the
     * system share sheet when WhatsApp is missing or cannot handle the file. Returns true when WhatsApp opened.
     */
    fun shareToWhatsApp(context: Context, uri: Uri, mime: String, caption: String): Boolean {
        val pkg = installedWhatsApp(context)
        if (pkg != null) {
            try {
                context.startActivity(sendIntent(context, uri, mime, null, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: android.content.ActivityNotFoundException) { /* fall through to the share sheet */ }
            catch (_: SecurityException) { /* fall through to the share sheet */ }
        }
        try {
            chooser(context, sendIntent(context, uri, mime, caption, null), "Share")
            Toast.makeText(context, "WhatsApp isn't available — choose another app", Toast.LENGTH_SHORT).show()
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(context, "No app found to share with", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    fun shareVideo(context: Context, uri: Uri, caption: String) {
        chooser(context, sendIntent(context, uri, "video/mp4", caption, null), "Share video")
    }

    /** Saves a PNG into Pictures/WisdomOne. Returns the content Uri, or null when unsupported. */
    fun saveImageToGallery(context: Context, bmp: Bitmap, baseName: String): Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$baseName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WisdomOne")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0); resolver.update(uri, values, null, null)
            uri
        }.getOrElse { resolver.delete(uri, null, null); null }
    }

    /** Copies a generated video from the app cache into Movies/WisdomOne. */
    fun saveVideoToGallery(context: Context, source: File, baseName: String): Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$baseName.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/WisdomOne")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
            values.clear(); values.put(MediaStore.Video.Media.IS_PENDING, 0); resolver.update(uri, values, null, null)
            uri
        }.getOrElse { resolver.delete(uri, null, null); null }
    }

    fun videoUri(context: Context, file: File): Uri = cacheUri(context, file)
}
