package com.example.antispy

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.sin

object MusicManager {

    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false
    private var musicThread: Thread? = null
    var currentTrackIndex = 0

    fun playBackgroundMusic() {
        if (isPlaying) return
        isPlaying = true
        musicThread = Thread { generateAndPlayTone() }.apply { start() }
    }

    fun stopBackgroundMusic() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        musicThread?.interrupt()
        musicThread = null
    }

    private fun generateAndPlayTone() {
        val sampleRate = 44100
        val duration = 12
        val bufferSize = sampleRate * duration

        try {
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
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack?.play()

            val frequencies = if (currentTrackIndex == 0) {
                floatArrayOf(330f, 392f, 440f, 523f, 440f, 392f, 330f, 294f, 262f, 294f, 330f, 392f)
            } else {
                floatArrayOf(220f, 262f, 330f, 392f, 494f, 392f, 330f, 262f, 440f, 349f, 294f, 262f)
            }

            val noteDuration = sampleRate / 6
            val audioBuffer = ShortArray(noteDuration)

            for (freq in frequencies) {
                if (!isPlaying) break

                for (i in 0 until noteDuration) {
                    val angle = 2.0 * Math.PI * freq * i / sampleRate
                    val envelope = 1.0 - (i.toDouble() / noteDuration)
                    val sample = (Short.MAX_VALUE * 0.25 * sin(angle) * envelope).toInt().toShort()
                    audioBuffer[i] = sample
                }
                audioTrack?.write(audioBuffer, 0, noteDuration)
            }

            val silence = ShortArray(sampleRate / 3)
            audioTrack?.write(silence, 0, silence.size)

            if (isPlaying) {
                generateAndPlayTone()
            }
        } catch (e: Exception) {
            isPlaying = false
        }
    }
}
