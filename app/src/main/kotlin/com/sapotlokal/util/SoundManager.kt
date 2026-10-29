package com.sapotlokal.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.sapotlokal.R

/**
 * SoundManager — Audio FX engine for SapotLokal gamification rewards.
 *
 * Implements a SoundPool singleton supporting Android SDK 26+ for zero-latency
 * playback of reward cues (e.g. "Ka-ching!" cash register sound on mission completion
 * or badge unlock).
 */
class SoundManager(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val kachingSoundId: Int = soundPool.load(context.applicationContext, R.raw.sound_kaching, 1)

    fun playKaching() {
        try {
            soundPool.play(kachingSoundId, 1f, 1f, 1, 0, 1f)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing kaching sound", e)
        }
    }

    fun release() {
        try {
            soundPool.release()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing SoundPool", e)
        }
    }

    companion object {
        @Volatile
        private var instance: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
        }

        fun playKaching(context: Context? = null) {
            instance?.playKaching() ?: run {
                context?.let { getInstance(it).playKaching() }
            }
        }
    }
}

