package com.example.antispy

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.sin

object MusicManager {

    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false

    fun playBackgroundMusic() {
        if (isPlaying) return
        isPlaying = true
        Thread { generateAndPlayTone() }.start()
    }

    fun stopBackgroundMusic() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    private fun generateAndPlayTone() {
        val sampleRate = 44100
        val duration = 8
        val bufferSize = sampleRate * duration

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        audioTrack?.play()

        val frequencies = floatArrayOf(262f, 294f, 330f, 349f, 392f, 349f, 330f, 294f)
        val noteDuration = sampleRate / 4

        val audioBuffer = ShortArray(noteDuration)

        for (freq in frequencies) {
            if (!isPlaying) break

            for (i in 0 until noteDuration) {
                val angle = 2.0 * Math.PI * freq * i / sampleRate
                val sample = (Short.MAX_VALUE * 0.3 * sin(angle)).toShort()
                audioBuffer[i] = sample
            }
            audioTrack?.write(audioBuffer, 0, noteDuration)
        }

        val silence = ShortArray(sampleRate / 2)
        audioTrack?.write(silence, 0, silence.size)

        if (isPlaying) {
            generateAndPlayTone()
        }
    }
}
