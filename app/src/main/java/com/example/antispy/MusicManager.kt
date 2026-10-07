package com.example.antispy

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

object MusicManager {

    private var mediaPlayer: MediaPlayer? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun playCustomMusic(uri: Uri) {
        stopBackgroundMusic()
        try {
            appContext?.let { ctx ->
                mediaPlayer = MediaPlayer.create(ctx, uri).apply {
                    isLooping = true
                    start()
                }
            }
        } catch (_: Exception) {}
    }

    fun playBackgroundMusic() {
        if (mediaPlayer?.isPlaying == true) return
        mediaPlayer?.start()
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
