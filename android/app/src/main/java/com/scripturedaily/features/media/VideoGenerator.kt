package com.scripturedaily.features.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.scripturedaily.features.share.CardContent
import com.scripturedaily.features.share.CardRenderer
import com.scripturedaily.features.share.CardStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.sin

enum class TextAnimation(val label: String) { FADE("Fade in"), RISE("Rise"), WORDS("Word by word"), ZOOM("Zoom in") }
enum class BgMotion(val label: String) { ZOOM_IN("Slow zoom"), ZOOM_OUT("Zoom out"), PAN("Gentle pan"), STILL("Still") }
enum class VideoTransition(val label: String) { FADE_BLACK("Fade to black"), FADE_WHITE("Fade to white"), NONE("None") }

data class VideoOptions(
    val seconds: Int = 12,
    val music: MusicStyle = MusicStyle.CALM_PAD,
    val style: CardStyle,
    val width: Int = 720,
    val height: Int = 1280,
    val fps: Int = 24,
    val animation: TextAnimation = TextAnimation.FADE,
    val motion: BgMotion = BgMotion.ZOOM_IN,
    val transition: VideoTransition = VideoTransition.FADE_BLACK,
    val particles: Boolean = true,
    val volume: Float = 0.8f
)

/**
 * Renders a vertical verse video (slow zoom, drifting light, text fade-in) and muxes calm music into an MP4
 * using the platform's H.264 + AAC encoders. Output is ready for WhatsApp Status, Reels and Shorts.
 */
object VideoGenerator {
    private const val TIMEOUT_US = 10_000L

    suspend fun generate(
        context: Context, content: CardContent, options: VideoOptions, output: File, onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.Default) {
        val w = options.width; val h = options.height; val fps = options.fps
        val totalFrames = options.seconds * fps
        val composer = FrameComposer(context, content, options, w, h)
        val pcm: ShortArray? = if (options.music == MusicStyle.NONE) null else MusicSynth.generate(options.music, options.seconds, options.volume)

        if (output.exists()) output.delete()
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 3_500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoCodec.start()

        var audioCodec: MediaCodec? = null
        if (pcm != null) {
            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, MusicSynth.SAMPLE_RATE, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 96_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
            }
            audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).also {
                it.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); it.start()
            }
        }

        var videoTrack = -1; var audioTrack = -1; var muxerStarted = false
        var videoDone = false; var audioDone = audioCodec == null
        var videoInputDone = false; var audioInputDone = audioCodec == null
        var frameIndex = 0; var pcmOffset = 0
        val frame = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(frame)
        val pixels = IntArray(w * h)
        val info = MediaCodec.BufferInfo()

        fun maybeStart() {
            if (!muxerStarted && videoTrack >= 0 && (audioCodec == null || audioTrack >= 0)) { muxer.start(); muxerStarted = true }
        }

        // Pulls finished samples out of a codec; returns true when the codec reached end of stream.
        fun drain(codec: MediaCodec, isVideo: Boolean): Boolean {
            while (true) {
                val formatKnown = if (isVideo) videoTrack >= 0 else audioTrack >= 0
                if (formatKnown && !muxerStarted) return false  // wait for the other track's format
                val index = codec.dequeueOutputBuffer(info, 0)
                when {
                    index == MediaCodec.INFO_TRY_AGAIN_LATER -> return false
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (isVideo) videoTrack = muxer.addTrack(codec.outputFormat) else audioTrack = muxer.addTrack(codec.outputFormat)
                        maybeStart()
                    }
                    index >= 0 -> {
                        val data = codec.getOutputBuffer(index)
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (info.size > 0 && data != null && muxerStarted) {
                            data.position(info.offset); data.limit(info.offset + info.size)
                            muxer.writeSampleData(if (isVideo) videoTrack else audioTrack, data, info)
                        }
                        val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(index, false)
                        if (eos) return true
                    }
                }
            }
        }

        try {
            while (!(videoDone && audioDone)) {
                coroutineContext.ensureActive()
                var progressed = false

                if (!videoInputDone) {
                    val inIndex = videoCodec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val ptsUs = frameIndex * 1_000_000L / fps
                        if (frameIndex >= totalFrames) {
                            videoCodec.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM); videoInputDone = true
                        } else {
                            composer.draw(canvas, frameIndex.toFloat() / fps)
                            frame.getPixels(pixels, 0, w, 0, 0, w, h)
                            val image = videoCodec.getInputImage(inIndex)
                            if (image != null) fillYuv(image, pixels, w, h)
                            videoCodec.queueInputBuffer(inIndex, 0, w * h * 3 / 2, ptsUs, 0)
                            frameIndex++
                            onProgress(frameIndex.toFloat() / totalFrames * 0.95f)
                        }
                        progressed = true
                    }
                }
                if (!videoDone) videoDone = drain(videoCodec, true)

                if (audioCodec != null && pcm != null && !audioInputDone) {
                    val inIndex = audioCodec.dequeueInputBuffer(0)
                    if (inIndex >= 0) {
                        val buffer = audioCodec.getInputBuffer(inIndex)!!
                        buffer.clear()
                        val ptsUs = pcmOffset * 1_000_000L / MusicSynth.SAMPLE_RATE
                        val count = minOf(pcm.size - pcmOffset, buffer.capacity() / 2, 4096)
                        if (count <= 0) {
                            audioCodec.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM); audioInputDone = true
                        } else {
                            val bb = ByteBuffer.allocate(count * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                            for (i in 0 until count) bb.putShort(pcm[pcmOffset + i])
                            bb.flip(); buffer.put(bb)
                            audioCodec.queueInputBuffer(inIndex, 0, count * 2, ptsUs, 0)
                            pcmOffset += count
                        }
                        progressed = true
                    }
                }
                if (audioCodec != null && !audioDone) audioDone = drain(audioCodec, false)
                if (!progressed) Thread.sleep(2)
            }
            onProgress(1f)
        } finally {
            runCatching { videoCodec.stop() }; runCatching { videoCodec.release() }
            audioCodec?.let { runCatching { it.stop() }; runCatching { it.release() } }
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
            composer.release(); frame.recycle()
        }
        output
    }

    /** ARGB pixels -> the codec's flexible YUV420 input image (handles any stride / semi-planar layout). */
    private fun fillYuv(image: Image, argb: IntArray, w: Int, h: Int) {
        val yPlane = image.planes[0]; val uPlane = image.planes[1]; val vPlane = image.planes[2]
        val yBuf = yPlane.buffer; val uBuf = uPlane.buffer; val vBuf = vPlane.buffer
        val yRow = yPlane.rowStride; val yPix = yPlane.pixelStride
        val uRow = uPlane.rowStride; val uPix = uPlane.pixelStride
        val vRow = vPlane.rowStride; val vPix = vPlane.pixelStride
        val yLine = ByteArray(w)
        for (row in 0 until h) {
            val base = row * w
            for (col in 0 until w) {
                val p = argb[base + col]
                val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
                yLine[col] = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(16, 235).toByte()
            }
            if (yPix == 1) { yBuf.position(row * yRow); yBuf.put(yLine, 0, minOf(w, yBuf.remaining())) }
            else for (col in 0 until w) yBuf.put(row * yRow + col * yPix, yLine[col])
            if (row % 2 == 0) {
                val cr = row / 2
                for (col in 0 until w / 2) {
                    val p = argb[base + col * 2]
                    val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
                    val u = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(16, 240).toByte()
                    val v = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(16, 240).toByte()
                    uBuf.put(cr * uRow + col * uPix, u)
                    vBuf.put(cr * vRow + col * vPix, v)
                }
            }
        }
    }
}


/**
 * Draws one frame of the verse video at time t. Shared by the encoder and the live preview so what you
 * preview is what you get: background motion, floating light, animated verse text and the end transition.
 */
class FrameComposer(private val context: Context, private val content: CardContent, private val o: VideoOptions, val w: Int, val h: Int) {
    private val background: Bitmap = CardRenderer.renderBackground(o.style, w, h)
    private val foreground: Bitmap? = if (o.animation == TextAnimation.WORDS) null else CardRenderer.renderForeground(context, content, o.style, w, h)
    private val rnd = java.util.Random(5)
    private val px = FloatArray(28) { rnd.nextFloat() * w }
    private val py = FloatArray(28) { rnd.nextFloat() * h }
    private val pr = FloatArray(28) { w * (0.004f + rnd.nextFloat() * 0.008f) }
    private val ps = FloatArray(28) { 12f + rnd.nextFloat() * 28f }
    private val pp = FloatArray(28) { rnd.nextFloat() * 6.28f }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (o.style.resolved().light) Color.rgb(255, 214, 140) else Color.WHITE }
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()
    private val total = o.seconds.toFloat()

    fun draw(c: Canvas, t: Float) {
        val wf = w.toFloat(); val hf = h.toFloat(); val progress = (t / total).coerceIn(0f, 1f)
        // 1) background motion
        matrix.reset()
        when (o.motion) {
            BgMotion.ZOOM_IN -> matrix.postScale(1f + 0.08f * progress, 1f + 0.08f * progress, wf / 2, hf / 2)
            BgMotion.ZOOM_OUT -> matrix.postScale(1.08f - 0.08f * progress, 1.08f - 0.08f * progress, wf / 2, hf / 2)
            BgMotion.PAN -> { matrix.postScale(1.06f, 1.06f, wf / 2, hf / 2); matrix.postTranslate((progress - 0.5f) * wf * 0.04f, 0f) }
            BgMotion.STILL -> Unit
        }
        c.drawColor(Color.BLACK)
        c.drawBitmap(background, matrix, bmpPaint)
        // 2) drifting light
        if (o.particles) for (i in px.indices) {
            val yy = ((py[i] - ps[i] * t) % hf + hf) % hf
            val xx = px[i] + (sin(t * 0.6 + pp[i]) * wf * 0.02).toFloat()
            dot.alpha = (60 + 50 * sin(t * 1.3 + pp[i])).toInt().coerceIn(10, 140)
            c.drawCircle(xx, yy, pr[i], dot)
        }
        // 3) verse text animation
        val fadeIn = ((t - 0.6f) / 1.8f).coerceIn(0f, 1f)
        val ease = fadeIn * fadeIn * (3 - 2 * fadeIn)
        when (o.animation) {
            TextAnimation.WORDS -> {
                val reveal = ((t - 0.6f) / (total * 0.55f).coerceIn(2.5f, 9f)).coerceIn(0f, 1f)
                CardRenderer.drawForeground(context, c, content, o.style, w, h, 1f, if (reveal >= 1f) 1f else reveal)
            }
            else -> {
                val fg = foreground ?: return
                c.save()
                when (o.animation) {
                    TextAnimation.RISE -> c.translate(0f, (1 - ease) * hf * 0.05f)
                    TextAnimation.ZOOM -> { val s = 0.9f + 0.1f * ease; c.scale(s, s, wf / 2, hf / 2) }
                    else -> c.translate(0f, (1 - ease) * hf * 0.02f)
                }
                bmpPaint.alpha = (ease * 255).toInt(); c.drawBitmap(fg, 0f, 0f, bmpPaint); bmpPaint.alpha = 255
                c.restore()
            }
        }
        // 4) transitions: a short fade-in from the transition colour, and the end fade
        val base = if (o.transition == VideoTransition.FADE_WHITE) 255 else 0
        if (o.transition != VideoTransition.NONE) {
            val intro = (1f - t / 0.6f).coerceIn(0f, 1f)
            if (intro > 0f) c.drawColor(Color.argb((intro * 255).toInt(), base, base, base))
            val tail = ((t - (total - 0.8f)) / 0.8f).coerceIn(0f, 1f)
            if (tail > 0f) c.drawColor(Color.argb((tail * 255).toInt(), base, base, base))
        }
    }

    fun release() { background.recycle(); foreground?.recycle() }
}
