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

data class VideoOptions(
    val seconds: Int = 12,
    val music: MusicStyle = MusicStyle.CALM_PAD,
    val style: CardStyle,
    val width: Int = 720,
    val height: Int = 1280,
    val fps: Int = 24
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
        val background = CardRenderer.renderBackground(options.style, w, h)
        val foreground = CardRenderer.renderForeground(context, content, options.style, w, h)
        val pcm: ShortArray? = if (options.music == MusicStyle.NONE) null else MusicSynth.generate(options.music, options.seconds)

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
        val dots = Particles(w, h)

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
                            composeFrame(canvas, background, foreground, dots, frameIndex.toFloat() / fps, options.seconds.toFloat())
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
            background.recycle(); foreground.recycle(); frame.recycle()
        }
        output
    }

    private class Particles(val w: Int, val h: Int) {
        private val rnd = java.util.Random(5)
        val x = FloatArray(28) { rnd.nextFloat() * w }
        val y = FloatArray(28) { rnd.nextFloat() * h }
        val r = FloatArray(28) { w * (0.004f + rnd.nextFloat() * 0.008f) }
        val speed = FloatArray(28) { 12f + rnd.nextFloat() * 28f }
        val phase = FloatArray(28) { rnd.nextFloat() * 6.28f }
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()

    private fun composeFrame(c: Canvas, bg: Bitmap, fg: Bitmap, p: Particles, t: Float, total: Float) {
        val w = bg.width.toFloat(); val h = bg.height.toFloat()
        // 1) slow zoom on the background
        val zoom = 1f + 0.08f * (t / total)
        matrix.reset(); matrix.postScale(zoom, zoom, w / 2f, h / 2f)
        c.drawColor(Color.BLACK)
        c.drawBitmap(bg, matrix, bitmapPaint)
        // 2) drifting light particles
        for (i in p.x.indices) {
            val yy = ((p.y[i] - p.speed[i] * t) % h + h) % h
            val xx = p.x[i] + (sin(t * 0.6 + p.phase[i]) * w * 0.02).toFloat()
            dotPaint.alpha = (60 + 50 * sin(t * 1.3 + p.phase[i])).toInt().coerceIn(10, 140)
            c.drawCircle(xx, yy, p.r[i], dotPaint)
        }
        // 3) text fades and floats in
        val fadeIn = ((t - 0.6f) / 1.8f).coerceIn(0f, 1f)
        val ease = fadeIn * fadeIn * (3 - 2 * fadeIn)
        bitmapPaint.alpha = (ease * 255).toInt()
        c.drawBitmap(fg, 0f, (1 - ease) * h * 0.02f, bitmapPaint)
        bitmapPaint.alpha = 255
        // 4) soft fade to black at the very end
        val tail = ((t - (total - 0.8f)) / 0.8f).coerceIn(0f, 1f)
        if (tail > 0f) c.drawColor(Color.argb((tail * 255).toInt(), 0, 0, 0))
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
