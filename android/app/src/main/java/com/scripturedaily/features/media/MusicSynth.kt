package com.scripturedaily.features.media

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Calm background tracks. [tradition] is the religion id the track is inspired by (1 Hinduism, 2 Christianity, 3 Islam,
 * 4 Buddhism) or null for universal tracks. It only affects ordering — every track stays available to every user.
 */
enum class MusicStyle(val label: String, val description: String, val tradition: Int? = null) {
    CALM_PAD("Calm pad", "Warm, slow chords"),
    SOFT_BELLS("Soft bells", "Gentle chimes over a pad"),
    DEEP_DRONE("Deep drone", "Low meditative hum"),
    TANPURA("Tanpura drone", "Resonant Indian drone, for bhajan and meditation", 1),
    TEMPLE_OM("Temple bells & Om", "Deep Om hum with slow temple bells", 1),
    BANSURI("Bansuri raga", "Soft bamboo flute over a tanpura (Raga Yaman)", 1),
    SACRED_HUSH("Sacred hush", "Warm, reverent choir-like pad", 2),
    QUIET_PIANO("Quiet piano", "Slow, tender piano notes for prayer", 2),
    NEY_NIGHT("Ney at night", "Breathy reed flute over a still drone", 3),
    SINGING_BOWLS("Singing bowls", "Resonant bowls for mindfulness", 4),
    NONE("No music", "Silent video");

    companion object {
        /** Every track, the user's tradition first, then universal ones, then the other traditions'. "No music" is always last. */
        fun ordered(religionId: Int): List<MusicStyle> {
            val real = values().filter { it != NONE }
            val mine = real.filter { it.tradition != null && it.tradition == religionId }
            val universal = real.filter { it.tradition == null }
            val others = real.filter { it.tradition != null && it.tradition != religionId }
            return mine + universal + others + NONE
        }
    }
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
    private val SACRED = listOf(
        doubleArrayOf(146.83, 220.00, 293.66, 349.23),   // Dm
        doubleArrayOf(116.54, 174.61, 233.08, 293.66),   // B♭
        doubleArrayOf(174.61, 261.63, 349.23, 440.00),   // F
        doubleArrayOf(130.81, 196.00, 261.63, 329.63)    // C
    )
    /** Raga Yaman (C): C D E F♯ G A B, as ratios to the tonic. */
    private val YAMAN = doubleArrayOf(1.0, 9.0 / 8, 5.0 / 4, 45.0 / 32, 3.0 / 2, 27.0 / 16, 15.0 / 8)
    /** Hijaz (D): D E♭ F♯ G A B♭ C. */
    private val HIJAZ = doubleArrayOf(1.0, 16.0 / 15, 5.0 / 4, 4.0 / 3, 3.0 / 2, 8.0 / 5, 9.0 / 5)
    private val PENTATONIC = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50)

    fun generate(style: MusicStyle, seconds: Int, volume: Float = 1f): ShortArray {
        val n = SAMPLE_RATE * seconds
        val buf = FloatArray(n)
        when (style) {
            MusicStyle.NONE -> Unit
            MusicStyle.CALM_PAD -> pad(buf, 0.55f)
            MusicStyle.SOFT_BELLS -> { pad(buf, 0.38f); bells(buf, seconds) }
            MusicStyle.DEEP_DRONE -> drone(buf)
            MusicStyle.TANPURA -> tanpura(buf, 1.0f)
            MusicStyle.TEMPLE_OM -> templeOm(buf, seconds)
            MusicStyle.BANSURI -> { tanpura(buf, 0.55f); flute(buf, seconds, 261.63 * 2, YAMAN, 21, 1.0) }
            MusicStyle.SACRED_HUSH -> pad(buf, 0.6f, SACRED)
            MusicStyle.QUIET_PIANO -> { pad(buf, 0.18f, SACRED); piano(buf, seconds) }
            MusicStyle.NEY_NIGHT -> { drone(buf, 0.12f); flute(buf, seconds, 293.66, HIJAZ, 33, 1.15) }
            MusicStyle.SINGING_BOWLS -> singingBowls(buf, seconds)
        }
        // Loudness: normalise by RMS (not peak — sparse bells/plucks have tall peaks but little energy, which is why tracks
        // used to sound far too quiet), then a soft tanh limiter keeps the peaks smooth instead of clipping.
        var sum = 0.0
        for (v in buf) sum += v.toDouble() * v
        val rms = Math.sqrt(sum / n.coerceAtLeast(1)).toFloat()
        val vol = volume.coerceIn(0f, 1f)
        val gain = if (rms > 1e-6f) TARGET_RMS * vol / rms else 0f
        val fadeIn = SAMPLE_RATE * 1.2
        val fadeOut = SAMPLE_RATE * 2.0
        return ShortArray(n) { i ->
            val f = minOf(1.0, i / fadeIn, (n - i) / fadeOut).toFloat()
            val x = buf[i] * gain * f
            (Math.tanh(x.toDouble()).toFloat() * 0.97f * 32767f).toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    /** About -12 dBFS RMS at full volume (slider 100%); the default 80% lands near -14 dBFS, clearly audible under a voice-free video. */
    private const val TARGET_RMS = 0.25f

    private fun pad(buf: FloatArray, level: Float, progression: List<DoubleArray> = PROGRESSION) {
        val seg = 4.0
        val segments = (buf.size / SAMPLE_RATE / seg).toInt() + 2
        for (k in 0 until segments) {
            val chord = progression[k % progression.size]
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

    private fun drone(buf: FloatArray, level: Float = 0.22f) {
        val notes = doubleArrayOf(55.0, 82.41, 110.0, 164.81)
        for (i in buf.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            var s = 0.0
            for ((idx, f) in notes.withIndex()) {
                val wobble = 1.0 + 0.0025 * sin(2 * PI * (0.07 + idx * 0.03) * t)
                s += (1.0 / (1 + idx)) * (sin(2 * PI * f * wobble * t) + 0.35 * sin(2 * PI * f * 2 * t))
            }
            val breathe = 0.75 + 0.25 * sin(2 * PI * 0.11 * t)
            buf[i] += (s * breathe * level).toFloat()
        }
    }

    /** Plucked-string drone cycle Pa – Sa' – Sa' – Sa with a bright, buzzing (jivari) harmonic spectrum. */
    private fun tanpura(buf: FloatArray, level: Float) {
        val sa = 130.81
        val cycle = doubleArrayOf(sa * 1.5, sa * 2, sa * 2, sa)
        val period = 1.45
        var k = 0
        var t = 0.0
        while (t < buf.size.toDouble() / SAMPLE_RATE) {
            val f = cycle[k % 4]
            val s0 = (t * SAMPLE_RATE).toInt()
            val len = minOf((5.0 * SAMPLE_RATE).toInt(), buf.size - s0)
            for (j in 0 until len) {
                val lt = j.toDouble() / SAMPLE_RATE
                val env = exp(-0.75 * lt) * minOf(1.0, lt / 0.004)
                var v = 0.0
                for (h in 1..10) v += sin(2 * PI * f * h * lt + h * 0.3) / (h.toDouble().pow(0.85)) * (1.0 + 0.5 * sin(2 * PI * 0.4 * lt * h * 0.1))
                buf[s0 + j] += (v * env * 0.11 * level).toFloat()
            }
            t += period; k++
        }
    }

    /** A low "Om" hum (~136 Hz) with slow temple-bell strikes. */
    private fun templeOm(buf: FloatArray, seconds: Int) {
        val f0 = 136.1
        for (i in buf.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val swell = 0.8 + 0.2 * sin(2 * PI * 0.09 * t)
            val v = sin(2 * PI * f0 * t) + 0.55 * sin(2 * PI * f0 * 2 * t + 0.5) + 0.3 * sin(2 * PI * f0 * 3 * t) + 0.12 * sin(2 * PI * f0 * 4.01 * t)
            buf[i] += (v * swell * 0.12).toFloat()
        }
        val partials = doubleArrayOf(1.0, 2.0, 2.76, 5.4, 8.93)
        val amps = doubleArrayOf(1.0, 0.6, 0.45, 0.2, 0.1)
        var t = 1.2
        var idx = 0
        while (t < seconds - 0.5) {
            val f = if (idx % 2 == 0) 392.0 else 523.25
            val s0 = (t * SAMPLE_RATE).toInt()
            val len = minOf((5.5 * SAMPLE_RATE).toInt(), buf.size - s0)
            for (j in 0 until len) {
                val lt = j.toDouble() / SAMPLE_RATE
                var v = 0.0
                for (p in partials.indices) v += amps[p] * exp(-(0.9 + p * 0.7) * lt) * sin(2 * PI * f * partials[p] * lt)
                buf[s0 + j] += (v * minOf(1.0, lt / 0.005) * 0.2).toFloat()
            }
            t += 5.2; idx++
        }
    }

    /** Breathy flute melody on [scale] (ratios to [tonic]) with vibrato — a gentle stepwise phrase, no rhythm pressure. */
    private fun flute(buf: FloatArray, seconds: Int, tonic: Double, scale: DoubleArray, seed: Long, speed: Double) {
        val rnd = java.util.Random(seed)
        var degree = 2
        var t = 0.8
        while (t < seconds - 1.0) {
            val dur = (1.6 + rnd.nextDouble() * 1.6) / speed
            val f = tonic * scale[degree]
            val s0 = (t * SAMPLE_RATE).toInt()
            val len = minOf(((dur + 0.7) * SAMPLE_RATE).toInt(), buf.size - s0)
            val noise = java.util.Random(rnd.nextLong())
            for (j in 0 until len) {
                val lt = j.toDouble() / SAMPLE_RATE
                val env = minOf(1.0, lt / 0.28) * (if (lt < dur) 1.0 else exp(-6.0 * (lt - dur)))
                val vib = 1.0 + 0.004 * sin(2 * PI * 5.1 * lt) * minOf(1.0, lt / 0.6)
                val ph = 2 * PI * f * vib * lt
                val tone = sin(ph) + 0.22 * sin(2 * ph) + 0.06 * sin(3 * ph)
                val breath = (noise.nextDouble() - 0.5) * 0.06
                buf[s0 + j] += ((tone + breath) * env * 0.2).toFloat()
            }
            // mostly stepwise motion, occasional leap, tendency to return towards the tonic
            val step = when (rnd.nextInt(6)) { 0, 1 -> -1; 2, 3 -> 1; 4 -> -2; else -> 2 }
            degree = (degree + step).coerceIn(0, scale.size - 1)
            t += dur + 0.35 + rnd.nextDouble() * 0.7
        }
    }

    /** Slow, soft piano-like notes (decaying harmonics) on a pentatonic scale. */
    private fun piano(buf: FloatArray, seconds: Int) {
        val rnd = java.util.Random(5)
        val notes = doubleArrayOf(261.63, 293.66, 329.63, 392.0, 440.0, 523.25, 392.0, 329.63)
        var t = 0.9
        var i = 0
        while (t < seconds - 0.8) {
            val f = notes[(i + rnd.nextInt(3)) % notes.size]
            val s0 = (t * SAMPLE_RATE).toInt()
            val len = minOf((3.5 * SAMPLE_RATE).toInt(), buf.size - s0)
            for (j in 0 until len) {
                val lt = j.toDouble() / SAMPLE_RATE
                val v = sin(2 * PI * f * lt) * exp(-1.1 * lt) + 0.35 * sin(2 * PI * f * 2 * lt) * exp(-2.0 * lt) + 0.12 * sin(2 * PI * f * 3 * lt) * exp(-3.2 * lt)
                buf[s0 + j] += (v * minOf(1.0, lt / 0.006) * 0.3).toFloat()
            }
            t += 1.5 + rnd.nextDouble() * 1.1; i++
        }
    }

    /** Struck singing bowls: slightly detuned partial pairs that beat slowly, over a very soft drone. */
    private fun singingBowls(buf: FloatArray, seconds: Int) {
        drone(buf, 0.06f)
        val rnd = java.util.Random(8)
        val roots = doubleArrayOf(220.0, 293.66, 196.0, 261.63)
        var t = 0.8
        var i = 0
        while (t < seconds - 1.0) {
            val f = roots[i % roots.size]
            val s0 = (t * SAMPLE_RATE).toInt()
            val len = minOf((7.5 * SAMPLE_RATE).toInt(), buf.size - s0)
            for (j in 0 until len) {
                val lt = j.toDouble() / SAMPLE_RATE
                val env = minOf(1.0, lt / 0.03) * exp(-0.55 * lt)
                val v = sin(2 * PI * f * lt) + sin(2 * PI * (f + 0.45) * lt) +
                    0.5 * (sin(2 * PI * f * 2.71 * lt) + sin(2 * PI * (f * 2.71 + 0.7) * lt)) * exp(-0.5 * lt) +
                    0.2 * sin(2 * PI * f * 5.15 * lt) * exp(-1.2 * lt)
                buf[s0 + j] += (v * env * 0.16).toFloat()
            }
            t += 5.0 + rnd.nextDouble() * 1.5; i++
        }
    }
}

/** Plays a short sample of a music style so the user can choose before generating the video. */
object MusicPreview {
    private var track: android.media.AudioTrack? = null
    /** Incremented by every play()/stop(); a play() whose token is stale (the user switched track or left) never starts audio. */
    @Volatile private var token = 0

    suspend fun play(style: MusicStyle, volume: Float, onDone: () -> Unit) {
        stop()
        val my = ++token
        val pcm = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { MusicSynth.generate(style, 8, volume) }
        if (my != token) return
        val at = android.media.AudioTrack.Builder()
            .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(android.media.AudioFormat.Builder().setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(MusicSynth.SAMPLE_RATE).setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(pcm.size * 2).setTransferMode(android.media.AudioTrack.MODE_STATIC).build()
        at.write(pcm, 0, pcm.size)
        track = at
        at.play()
        kotlinx.coroutines.delay(8_200)
        if (track === at && my == token) { stop(); onDone() }
    }

    fun stop() {
        token++
        track?.let { runCatching { it.stop() }; runCatching { it.release() } }
        track = null
    }
}
