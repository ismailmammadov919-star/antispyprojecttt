package com.example.antispy

import android.content.Context
import android.media.MediaPlayer

object MusicManager {

    private var mediaPlayer: MediaPlayer? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun playBackgroundMusic() {
        if (mediaPlayer?.isPlaying == true) return
        try {
            if (mediaPlayer == null && appContext != null) {
                mediaPlayer = MediaPlayer.create(appContext, R.raw.track).apply {
                    isLooping = true
                }
            }
            mediaPlayer?.start()
        } catch (_: Exception) {}
    }

    fun pauseBackgroundMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun stopBackgroundMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
    }
}
