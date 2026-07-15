package com.personal.mymusic.playback

import android.content.Intent
import android.app.PendingIntent
import com.personal.mymusic.ui.MainActivity
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.personal.mymusic.data.cache.CacheManager
import com.personal.mymusic.data.network.NewPipeDownloader

import android.media.audiofx.Equalizer
import android.os.Bundle
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var equalizer: Equalizer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Create an HTTP data source factory that supports cross-protocol redirects
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(NewPipeDownloader.USER_AGENT)

        // Warm up SimpleCache on a background thread to avoid blocking the main thread
        // (StandaloneDatabaseProvider opens SQLite which is slow on first launch).
        // Use plain HTTP source initially; the player will pick up the cached source
        // once the cache is ready because CacheDataSource falls back to the upstream source.
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(CacheManager.getCache(applicationContext))
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(applicationContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(applicationContext).setDataSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(audioAttributes, true) // Request audio focus automatically
            .setHandleAudioBecomingNoisy(true) // Pause on headphone unplug / Bluetooth disconnect
            .build()

        // Initialize Equalizer with the current audio session ID
        initEqualizer(player!!.audioSessionId)

        val sessionActivityIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("open_now_playing", true)
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(applicationContext, player!!)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == "SET_EQUALIZER") {
                        val preset = args.getString("preset", "NORMAL")
                        getSharedPreferences("music_prefs", MODE_PRIVATE).edit().putString("equalizer_preset", preset).apply()
                        applyEqualizerPreset(preset)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
                }
            })
            .build()
            
        player?.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    // Stop foreground service state when playback is paused, allowing the service to be stopped or reclaimed
                    stopForeground(false)
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                initEqualizer(audioSessionId)
            }
        })
    }

    private fun initEqualizer(sessionId: Int) {
        if (sessionId == 0) return
        try {
            equalizer?.release()
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
            }
            val preset = getSharedPreferences("music_prefs", MODE_PRIVATE)
                .getString("equalizer_preset", "NORMAL") ?: "NORMAL"
            applyEqualizerPreset(preset)
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "Error initializing Equalizer: $sessionId", e)
        }
    }

    private fun applyEqualizerPreset(preset: String) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            when (preset.uppercase()) {
                "BASS_BOOST" -> {
                    if (numBands > 0) eq.setBandLevel(0.toShort(), 800.toShort())
                    if (numBands > 1) eq.setBandLevel(1.toShort(), 600.toShort())
                    if (numBands > 2) eq.setBandLevel(2.toShort(), 0.toShort())
                    if (numBands > 3) eq.setBandLevel(3.toShort(), (-200).toShort())
                    if (numBands > 4) eq.setBandLevel(4.toShort(), (-200).toShort())
                }
                "TREBLE_BOOST" -> {
                    if (numBands > 0) eq.setBandLevel(0.toShort(), (-200).toShort())
                    if (numBands > 1) eq.setBandLevel(1.toShort(), (-100).toShort())
                    if (numBands > 2) eq.setBandLevel(2.toShort(), 0.toShort())
                    if (numBands > 3) eq.setBandLevel(3.toShort(), 600.toShort())
                    if (numBands > 4) eq.setBandLevel(4.toShort(), 800.toShort())
                }
                else -> {
                    for (i in 0 until numBands) {
                        eq.setBandLevel(i.toShort(), 0.toShort())
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaybackService", "Error applying equalizer preset: $preset", e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        player?.pause()
        stopSelf()
    }

    override fun onDestroy() {
        equalizer?.release()
        equalizer = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
