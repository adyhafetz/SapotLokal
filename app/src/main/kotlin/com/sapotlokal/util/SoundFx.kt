package com.sapotlokal.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * SoundFx — small game sounds made on the fly (soft sine notes), so the app needs no extra
 * audio files. They follow the phone's media volume and silent mode like any other game sound.
 */
object SoundFx {

    private const val RATE = 22050
    private val pool = Executors.newCachedThreadPool()

    /** A frequency in Hz held for [ms] milliseconds. 0 Hz = a rest. */
    private data class Note(val hz: Double, val ms: Int)

    fun tick() = play(listOf(Note(1500.0, 18)), 0.25f)
    fun flip() = play(listOf(Note(740.0, 40)), 0.3f)
    fun match() = play(listOf(Note(660.0, 90), Note(880.0, 130)), 0.4f)
    fun miss() = play(listOf(Note(330.0, 110)), 0.25f)

    /** Short rising fanfare: C5 E5 G5 C6. */
    fun win() = play(
        listOf(Note(523.25, 120), Note(659.25, 120), Note(783.99, 120), Note(1046.5, 320)), 0.45f
    )

    private fun play(notes: List<Note>, volume: Float) {
        pool.execute {
            try {
                val pcm = synthesize(notes, volume)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(notes.sumOf { it.ms }.toLong() + 80)
                track.release()
            } catch (e: Exception) {
                // Sound is a nicety; never let it break a game.
            }
        }
    }

    private fun synthesize(notes: List<Note>, volume: Float): ShortArray {
        val out = ArrayList<Short>()
        for (n in notes) {
            val count = RATE * n.ms / 1000
            for (i in 0 until count) {
                // Quick fade in/out so notes do not click.
                val env = minOf(1.0, i / (RATE * 0.005), (count - i) / (RATE * 0.02))
                val s = if (n.hz <= 0) 0.0 else sin(2 * PI * n.hz * i / RATE) * env * volume
                out.add((s * Short.MAX_VALUE).toInt().toShort())
            }
        }
        return out.toShortArray()
    }
}
