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

/** Persists the user's last studio choices. */
object StudioPrefs {
    private const val FILE = "reader"

    fun load(context: Context): CardStyle {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        fun <T : Enum<T>> pick(key: String, values: Array<T>, default: T): T =
            values.firstOrNull { it.name == p.getString(key, null) } ?: default
        return CardStyle(
            template = pick("studio_template", CardTemplate.values(), CardTemplate.NIGHT),
            pattern = pick("studio_pattern", CardPattern.values(), CardPattern.MANDALA),
            format = pick("studio_format", CardFormat.values(), CardFormat.STORY),
            fontScale = p.getFloat("studio_font_scale", 1f).coerceIn(0.7f, 1.5f),
            font = pick("studio_font", CardFont.values(), CardFont.SERIF),
            showMark = p.getBoolean("studio_mark", true)
        )
    }

    fun save(context: Context, s: CardStyle) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("studio_template", s.template.name).putString("studio_pattern", s.pattern.name)
            .putString("studio_format", s.format.name).putFloat("studio_font_scale", s.fontScale)
            .putString("studio_font", s.font.name).putBoolean("studio_mark", s.showMark).apply()
    }
}

object ShareActions {
    private val WHATSAPP = listOf("com.whatsapp", "com.whatsapp.w4b")

    private fun cacheUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun writeCacheImage(context: Context, bmp: Bitmap, name: String = "wisdomone_card.png"): Uri {
        val file = File(context.cacheDir, name)
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        return cacheUri(context, file)
    }

    fun shareImage(context: Context, bmp: Bitmap, caption: String) {
        val uri = writeCacheImage(context, bmp)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share verse card").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun installedWhatsApp(context: Context): String? =
        WHATSAPP.firstOrNull { pkg -> runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess }

    /** Opens WhatsApp's status composer with the image/video. Falls back to the share sheet when WhatsApp is missing. */
    fun shareToWhatsApp(context: Context, uri: Uri, mime: String, caption: String): Boolean {
        val pkg = installedWhatsApp(context)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pkg != null) `package` = pkg
        }
        return if (pkg != null) { context.startActivity(send); true } else {
            context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Toast.makeText(context, "WhatsApp is not installed — choose another app", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun shareVideo(context: Context, uri: Uri, caption: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share video").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
