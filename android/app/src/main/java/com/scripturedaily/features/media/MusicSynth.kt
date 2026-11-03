package com.scripturedaily.features.media

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

enum class MusicStyle(val label: String, val description: String) {
    CALM_PAD("Calm pad", "Warm, slow chords"),
    SOFT_BELLS("Soft bells", "Gentle chimes over a pad"),
    DEEP_DRONE("Deep drone", "Low meditative hum"),
    NONE("No music", "Silent video")
}

/**
 * Generates calm background music procedurally (sine pads, soft bells, drones), so the app ships
 * no licensed audio and the result is identical on every device. Mono, 16-bit PCM.
 */
object MusicSynth {
    const val SAMPLE_RATE = 44_100

    private val PROGRESSION = listOf(
        doubleArrayOf(110.00, 164.81, 220.00, 261.63),   // Am
        doubleArrayOf(87.31, 130.81, 174.61, 220.00),    // F
        doubleArrayOf(130.81, 196.00, 261.63, 329.63),   // C
        doubleArrayOf(98.00, 146.83, 196.00, 246.94)     // G
    )
    private val PENTATONIC = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50)

    fun generate(style: MusicStyle, seconds: Int): ShortArray {
        val n = SAMPLE_RATE * seconds
        val buf = FloatArray(n)
        when (style) {
            MusicStyle.NONE -> Unit
            MusicStyle.CALM_PAD -> pad(buf, 0.55f)
            MusicStyle.SOFT_BELLS -> { pad(buf, 0.38f); bells(buf, seconds) }
            MusicStyle.DEEP_DRONE -> drone(buf)
        }
        // master fades + gentle normalisation
        var peak = 0f
        for (v in buf) peak = max(peak, abs(v))
        val gain = if (peak > 0f) 0.62f / peak else 0f
        val fadeIn = SAMPLE_RATE * 1.2
        val fadeOut = SAMPLE_RATE * 2.0
        return ShortArray(n) { i ->
            val f = minOf(1.0, i / fadeIn, (n - i) / fadeOut).toFloat()
            (buf[i] * gain * f * 32767f).toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    private fun pad(buf: FloatArray, level: Float) {
        val seg = 4.0
        val segments = (buf.size / SAMPLE_RATE / seg).toInt() + 2
        for (k in 0 until segments) {
            val chord = PROGRESSION[k % PROGRESSION.size]
            val start = k * seg - 1.0
            val length = seg + 2.0
            val s0 = max(0, (start * SAMPLE_RATE).toInt())
            val s1 = minOf(buf.size, ((start + length) * SAMPLE_RATE).toInt())
            for (i in s0 until s1) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = sin(PI * (t - start) / length).pow(2)
                val trem = 0.88 + 0.12 * sin(2 * PI * 0.23 * t)
                var sample = 0.0
                for ((idx, f) in chord.withIndex()) {
                    val amp = 1.0 / (1 + idx * 0.35)
                    sample += amp * (sin(2 * PI * f * t) + 0.6 * sin(2 * PI * f * 1.004 * t + idx) + 0.18 * sin(2 * PI * f * 2 * t))
                }
                buf[i] += (sample * env * trem * level / 6.0).toFloat()
            }
        }
    }

    private fun bells(buf: FloatArray, seconds: Int) {
        val rnd = java.util.Random(11)
        var t = 1.5
        while (t < seconds - 1.0) {
            val f = PENTATONIC[rnd.nextInt(PENTATONIC.size)]
            val amp = 0.16 + rnd.nextDouble() * 0.12
            val s0 = (t * SAMPLE_RATE).toInt()
            val len = (3.2 * SAMPLE_RATE).toInt()
            for (j in 0 until len) {
                val i = s0 + j
                if (i >= buf.size) break
                val lt = j.toDouble() / SAMPLE_RATE
                val env = exp(-1.9 * lt) * minOf(1.0, lt / 0.01)
                val v = sin(2 * PI * f * lt) + 0.32 * sin(2 * PI * f * 2.0 * lt) + 0.12 * sin(2 * PI * f * 2.76 * lt)
                buf[i] += (v * env * amp).toFloat()
            }
            t += 1.9 + rnd.nextDouble() * 1.8
        }
    }

    private fun drone(buf: FloatArray) {
        val notes = doubleArrayOf(55.0, 82.41, 110.0, 164.81)
        for (i in buf.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            var s = 0.0
            for ((idx, f) in notes.withIndex()) {
                val wobble = 1.0 + 0.0025 * sin(2 * PI * (0.07 + idx * 0.03) * t)
                s += (1.0 / (1 + idx)) * (sin(2 * PI * f * wobble * t) + 0.35 * sin(2 * PI * f * 2 * t))
            }
            val breathe = 0.75 + 0.25 * sin(2 * PI * 0.11 * t)
            buf[i] = (s * breathe * 0.22).toFloat()
        }
    }
}
