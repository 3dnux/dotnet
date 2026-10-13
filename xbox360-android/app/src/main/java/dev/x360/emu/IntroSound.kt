package dev.x360.emu

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Acorde de arranque sintetizado (original): notas que entran escalonadas y se desvanecen. */
object IntroSound {
    private const val RATE = 44100
    private var track: AudioTrack? = null

    fun play() {
        try {
            val seconds = 3.2
            val n = (RATE * seconds).toInt()
            val pcm = ShortArray(n)
            // (frecuencia Hz, inicio s): La3 - Mi4 - La4 - Do#5 - Mi5
            val notes = listOf(220.0 to 0.0, 329.6 to 0.25, 440.0 to 0.5, 554.4 to 0.8, 659.3 to 1.1)
            for (i in 0 until n) {
                val ts = i.toDouble() / RATE
                var s = 0.0
                for ((f, start) in notes) {
                    val dt = ts - start
                    if (dt < 0) continue
                    val env = (1 - exp(-dt * 40)) * exp(-dt * 1.1)
                    s += env * (sin(2 * PI * f * dt) + 0.3 * sin(4 * PI * f * dt)) * 0.14
                }
                pcm[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
            val t = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(n * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            t.write(pcm, 0, n)
            t.play()
            track = t
        } catch (e: Exception) { track = null }  // sin audio no pasa nada
    }

    fun stop() {
        try { track?.stop(); track?.release() } catch (_: Exception) {}
        track = null
    }
}
