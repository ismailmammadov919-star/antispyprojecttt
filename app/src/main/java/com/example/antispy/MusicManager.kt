package com.example.antispy

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.sin

object MusicManager {

    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false
    @Volatile private var isPaused = false
    private var musicThread: Thread? = null
    var currentTrackIndex = 0

    fun init(context: android.content.Context) {}

    fun playBackgroundMusic() {
        if (isPlaying && !isPaused) return
        if (isPaused) {
            isPaused = false
            return
        }
        stopBackgroundMusic()
        isPlaying = true
        isPaused = false
        musicThread = Thread { generateAndPlayTone() }.apply { start() }
    }

    fun pauseBackgroundMusic() {
        if (isPlaying) {
            isPaused = true
            try {
                audioTrack?.pause()
            } catch (_: Exception) {}
        }
    }

    fun stopBackgroundMusic() {
        isPlaying = false
        isPaused = false
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

            while (isPlaying) {
                for (freq in frequencies) {
                    if (!isPlaying) break

                    while (isPaused) {
                        Thread.sleep(100)
                        if (!isPlaying) break
                    }

                    for (i in 0 until noteDuration) {
                        val angle = 2.0 * Math.PI * freq * i / sampleRate
                        val envelope = 1.0 - (i.toDouble() / noteDuration)
                        val sample = (Short.MAX_VALUE * 0.25 * sin(angle) * envelope).toInt().toShort()
                        audioBuffer[i] = sample
                    }
                    audioTrack?.write(audioBuffer, 0, noteDuration)
                }
            }
        } catch (e: Exception) {
            isPlaying = false
        }
    }
}
