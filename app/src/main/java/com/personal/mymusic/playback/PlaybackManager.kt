package com.personal.mymusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import android.os.Handler
import android.os.Looper
import com.personal.mymusic.data.network.NewPipeService
import com.personal.mymusic.domain.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.os.Bundle
import androidx.media3.session.SessionCommand

class PlaybackManager(
    private val context: Context,
    private val newPipeService: NewPipeService
) {
    private val controllerDeferred = CompletableDeferred<MediaController>()

    private val _currentlyLoadingSongId = MutableStateFlow<String?>(null)
    val currentlyLoadingSongId: StateFlow<String?> = _currentlyLoadingSongId.asStateFlow()

    fun setCurrentlyLoadingSongId(id: String?) {
        _currentlyLoadingSongId.value = id
    }

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()
    
    private val _equalizerPreset = MutableStateFlow(context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE).getString("equalizer_preset", "NORMAL") ?: "NORMAL")
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var prefetchJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _autoplayEnabled = MutableStateFlow(context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE).getBoolean("autoplay_enabled", true))
    val autoplayEnabled: StateFlow<Boolean> = _autoplayEnabled.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val mediaController = controllerFuture?.get()
                controller = mediaController
                android.util.Log.d("PlaybackManager", "Controller connected successfully: $mediaController")
                if (mediaController != null) {
                    controllerDeferred.complete(mediaController)
                    initController(mediaController)
                } else {
                    controllerDeferred.completeExceptionally(IllegalStateException("MediaController is null"))
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackManager", "Controller connection failed", e)
                controllerDeferred.completeExceptionally(e)
                e.printStackTrace()
            }
        }, Handler(Looper.getMainLooper())::post)
    }

    private fun initController(controller: MediaController) {
        _isPlaying.value = controller.isPlaying
        _duration.value = controller.duration.coerceAtLeast(0)
        _currentPosition.value = controller.currentPosition.coerceAtLeast(0)
        _shuffleEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode
        updateQueueFromController(controller)
        updateCurrentSongFromMediaItem(controller.currentMediaItem)
        checkAndTriggerAutoplay()

        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentSongFromMediaItem(mediaItem)
                _duration.value = controller.duration.coerceAtLeast(0)
                triggerPrefetchNextSong()
                checkAndTriggerAutoplay()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _duration.value = controller.duration.coerceAtLeast(0)
                if (playbackState == Player.STATE_READY) {
                    triggerPrefetchNextSong()
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                updateQueueFromController(controller)
                checkAndTriggerAutoplay()
            }
        })

        if (controller.isPlaying) {
            startProgressTracker()
        }
    }

    private fun updateCurrentSongFromMediaItem(mediaItem: MediaItem?) {
        if (mediaItem == null) {
            _currentSong.value = null
            return
        }
        val metadata = mediaItem.mediaMetadata
        val isAutoplay = metadata.extras?.getBoolean("isAutoplay", false) ?: false
        _currentSong.value = Song(
            id = mediaItem.mediaId,
            title = metadata.title?.toString() ?: "Unknown Title",
            channel = metadata.artist?.toString() ?: "Unknown Channel",
            durationSeconds = if (controller != null) controller!!.duration / 1000 else 0,
            thumbnailUrl = metadata.artworkUri?.toString() ?: "",
            streamUrl = mediaItem.requestMetadata.mediaUri?.toString(),
            isAutoplay = isAutoplay
        )
    }

    private fun updateQueueFromController(controller: MediaController) {
        val songs = mutableListOf<Song>()
        for (i in 0 until controller.mediaItemCount) {
            val item = controller.getMediaItemAt(i)
            val metadata = item.mediaMetadata
            val isAutoplay = metadata.extras?.getBoolean("isAutoplay", false) ?: false
            songs.add(
                Song(
                    id = item.mediaId,
                    title = metadata.title?.toString() ?: "Unknown Title",
                    channel = metadata.artist?.toString() ?: "Unknown Channel",
                    durationSeconds = 0,
                    thumbnailUrl = metadata.artworkUri?.toString() ?: "",
                    streamUrl = item.requestMetadata.mediaUri?.toString(),
                    isAutoplay = isAutoplay
                )
            )
        }
        _queue.value = songs
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            val sharedPrefs = context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
            while (isActive) {
                controller?.let { player ->
                    _currentPosition.value = player.currentPosition.coerceAtLeast(0)
                    _duration.value = player.duration.coerceAtLeast(0)
                    
                    // 1. Sleep Timer Countdown using absolute timestamp
                    val endTimestamp = sleepTimerEndTimestamp
                    if (endTimestamp != null) {
                        val remaining = (endTimestamp - System.currentTimeMillis()) / 1000
                        if (remaining <= 0) {
                            player.pause()
                            cancelSleepTimer()
                        } else {
                            _sleepTimerRemainingSeconds.value = remaining
                        }
                    } else {
                        _sleepTimerRemainingSeconds.value = null
                    }

                    // 2. Crossfade volume adjustment
                    val crossfadeSeconds = sharedPrefs.getFloat("crossfade_seconds", 0.0f)
                    if (crossfadeSeconds > 0) {
                        val duration = player.duration
                        val currentPosition = player.currentPosition
                        val crossfadeMs = (crossfadeSeconds * 1000).toLong()
                        
                        if (duration > 2 * crossfadeMs) {
                            if (currentPosition < crossfadeMs) {
                                val volume = currentPosition.toFloat() / crossfadeMs
                                player.volume = volume.coerceIn(0f, 1f)
                            } else if (duration - currentPosition < crossfadeMs) {
                                val volume = (duration - currentPosition).toFloat() / crossfadeMs
                                player.volume = volume.coerceIn(0f, 1f)
                            } else {
                                player.volume = 1f
                            }
                        } else {
                            player.volume = 1f
                        }
                    } else {
                        player.volume = 1f
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun triggerPrefetchNextSong() {
        val player = controller ?: return
        val currentIndex = player.currentMediaItemIndex
        val nextIndex = currentIndex + 1
        
        if (nextIndex < player.mediaItemCount) {
            val nextItem = player.getMediaItemAt(nextIndex)
            val nextSongId = nextItem.mediaId
            val nextUri = nextItem.requestMetadata.mediaUri
            
            if (nextUri == null || nextUri.toString().isEmpty()) {
                // Next song's URL is not resolved yet. Fetch it in the background!
                prefetchJob?.cancel()
                prefetchJob = scope.launch {
                    try {
                        val streamUrl = newPipeService.getAudioStreamUrl(nextSongId)
                        withContext(Dispatchers.Main) {
                            if (player.currentMediaItemIndex == currentIndex) {
                                val updatedMetadata = nextItem.mediaMetadata
                                val updatedItem = MediaItem.Builder()
                                    .setMediaId(nextSongId)
                                    .setMediaMetadata(updatedMetadata)
                                    .setUri(android.net.Uri.parse(streamUrl))
                                    .setRequestMetadata(
                                        MediaItem.RequestMetadata.Builder()
                                            .setMediaUri(android.net.Uri.parse(streamUrl))
                                            .build()
                                    )
                                    .build()
                                player.replaceMediaItem(nextIndex, updatedItem)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun playSongImmediate(song: Song) {
        android.util.Log.d("PlaybackManager", "PLAYBACK_STEP: tap received for ${song.title}")
        _currentlyLoadingSongId.value = song.id
        scope.launch {
            try {
                val player = controller ?: controllerDeferred.await()
                _playbackError.value = null
                
                android.util.Log.d("PlaybackManager", "PLAYBACK_STEP: stream URL resolution started for ${song.id}")
                // Fetch URL if not present
                val streamUrl = song.streamUrl ?: newPipeService.getAudioStreamUrl(song.id)
                android.util.Log.d("PlaybackManager", "PLAYBACK_STEP: stream URL resolved for ${song.id}")
                
                val updatedSong = song.copy(streamUrl = streamUrl)
                
                withContext(Dispatchers.Main) {
                    player.clearMediaItems()
                    val mediaItem = createMediaItem(updatedSong)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    android.util.Log.d("PlaybackManager", "PLAYBACK_STEP: ExoPlayer prepared")
                    player.play()
                    android.util.Log.d("PlaybackManager", "PLAYBACK_STEP: playback started")
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackManager", "PLAYBACK_STEP: playback failed", e)
                withContext(Dispatchers.Main) {
                    val errMsg = "Playback failed: ${e.message ?: "Unknown error"}"
                    _playbackError.value = errMsg
                    android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                if (_currentlyLoadingSongId.value == song.id) {
                    _currentlyLoadingSongId.value = null
                }
            }
        }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val startSong = songs[startIndex]
        android.util.Log.d("PlaybackManager", "PLAYBACK_STEP: tap received for playlist, starting at ${startSong.title}")
        _currentlyLoadingSongId.value = startSong.id
        scope.launch {
            try {
                val player = controller ?: controllerDeferred.await()
                _playbackError.value = null
                
                android.util.Log.d("PlaybackManager", "PLAYBACK_STEP: stream URL resolution started for starting song ${startSong.id}")
                // Resolve stream URL for the starting song immediately
                val startStreamUrl = startSong.streamUrl ?: newPipeService.getAudioStreamUrl(startSong.id)
                android.util.Log.d("PlaybackManager", "PLAYBACK_STEP: stream URL resolved for starting song ${startSong.id}")
                
                withContext(Dispatchers.Main) {
                    player.clearMediaItems()
                    
                    val mediaItems = songs.mapIndexed { index, song ->
                        val url = if (index == startIndex) startStreamUrl else song.streamUrl
                        createMediaItem(song.copy(streamUrl = url))
                    }
                    
                    player.setMediaItems(mediaItems, startIndex, 0)
                    player.prepare()
                    android.util.Log.d("PlaybackManager", "PLAYBACK_STEP: ExoPlayer prepared")
                    player.play()
                    android.util.Log.d("PlaybackManager", "PLAYBACK_STEP: playback started")
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackManager", "PLAYBACK_STEP: playback failed for playlist", e)
                withContext(Dispatchers.Main) {
                    val errMsg = "Playback failed: ${e.message ?: "Unknown error"}"
                    _playbackError.value = errMsg
                    android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                if (_currentlyLoadingSongId.value == startSong.id) {
                    _currentlyLoadingSongId.value = null
                }
            }
        }
    }

    private fun createMediaItem(song: Song): MediaItem {
        val extras = Bundle().apply {
            putBoolean("isAutoplay", song.isAutoplay)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.channel)
            .setArtworkUri(android.net.Uri.parse(song.thumbnailUrl))
            .setExtras(extras)
            .build()

        val uriString = song.streamUrl ?: ""
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setMediaMetadata(metadata)
            .setUri(android.net.Uri.parse(uriString))
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(android.net.Uri.parse(uriString))
                    .build()
            )
            .build()
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun skipToNext() {
        val player = controller ?: return
        if (player.hasNextMediaItem()) {
            val nextIndex = player.currentMediaItemIndex + 1
            val nextItem = player.getMediaItemAt(nextIndex)
            val nextUri = nextItem.requestMetadata.mediaUri
            
            if (nextUri == null || nextUri.toString().isEmpty()) {
                // If not pre-fetched yet, fetch it immediately before skipping
                scope.launch {
                    try {
                        val streamUrl = newPipeService.getAudioStreamUrl(nextItem.mediaId)
                        withContext(Dispatchers.Main) {
                            val updatedItem = MediaItem.Builder()
                                .setMediaId(nextItem.mediaId)
                                .setMediaMetadata(nextItem.mediaMetadata)
                                .setUri(android.net.Uri.parse(streamUrl))
                                .setRequestMetadata(
                                    MediaItem.RequestMetadata.Builder()
                                        .setMediaUri(android.net.Uri.parse(streamUrl))
                                        .build()
                                )
                                .build()
                            player.replaceMediaItem(nextIndex, updatedItem)
                            player.seekToNext()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        player.seekToNext()
                    }
                }
            } else {
                player.seekToNext()
            }
        }
    }

    fun skipToPrevious() {
        controller?.seekToPrevious()
    }

    fun toggleShuffle() {
        controller?.let {
            it.shuffleModeEnabled = !it.shuffleModeEnabled
        }
    }

    fun setRepeatMode(mode: Int) {
        controller?.let {
            it.repeatMode = mode
        }
    }

    // Sleep Timer Controls
    private var sleepTimerEndTimestamp: Long? = null

    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        sleepTimerEndTimestamp = System.currentTimeMillis() + (minutes * 60 * 1000L)
        _sleepTimerRemainingSeconds.value = minutes * 60L
    }

    fun cancelSleepTimer() {
        sleepTimerEndTimestamp = null
        _sleepTimerRemainingSeconds.value = null
    }

    // Equalizer Controls
    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
        controller?.let {
            val args = Bundle().apply {
                putString("preset", preset)
            }
            it.sendCustomCommand(SessionCommand("SET_EQUALIZER", Bundle.EMPTY), args)
        }
    }

    fun setShuffleEnabled(enabled: Boolean) {
        controller?.let {
            it.shuffleModeEnabled = enabled
        }
    }

    fun release() {
        stopProgressTracker()
        prefetchJob?.cancel()
        scope.cancel()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

    // Autoplay implementation
    private var lastAutoplayTriggerSongId: String? = null
    private var autoplayJob: Job? = null

    fun setAutoplayEnabled(enabled: Boolean) {
        _autoplayEnabled.value = enabled
        context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE).edit().putBoolean("autoplay_enabled", enabled).apply()
        if (!enabled) {
            removeAutoplaySongs()
        } else {
            checkAndTriggerAutoplay()
        }
    }

    fun removeAutoplaySongs() {
        val player = controller ?: return
        scope.launch(Dispatchers.Main) {
            val toRemove = mutableListOf<Int>()
            for (i in 0 until player.mediaItemCount) {
                val item = player.getMediaItemAt(i)
                val isAutoplay = item.mediaMetadata.extras?.getBoolean("isAutoplay", false) ?: false
                if (isAutoplay) {
                    toRemove.add(i)
                }
            }
            // Remove from end to start to avoid index shifting issues
            for (index in toRemove.reversed()) {
                if (index < player.mediaItemCount) {
                    player.removeMediaItem(index)
                }
            }
        }
    }

    private fun checkAndTriggerAutoplay() {
        val player = controller ?: return
        val currentSong = _currentSong.value ?: return
        
        if (_autoplayEnabled.value && player.currentMediaItemIndex == player.mediaItemCount - 1) {
            if (lastAutoplayTriggerSongId != currentSong.id) {
                lastAutoplayTriggerSongId = currentSong.id
                fetchAndAppendRelatedSongs(currentSong)
            }
        }
    }

    private fun fetchAndAppendRelatedSongs(currentSong: Song) {
        autoplayJob?.cancel()
        autoplayJob = scope.launch {
            try {
                android.util.Log.d("PlaybackManager", "Autoplay: fetching related songs for: ${currentSong.title}")
                val query = deriveRelatedQuery(currentSong)
                android.util.Log.d("PlaybackManager", "Autoplay: search query: $query")
                val results = newPipeService.search(query)
                
                val filteredResults = results
                    .filter { it.id != currentSong.id } // Filter out the current song
                    
                val topResults = filteredResults.take(5) // Take top 5 similar songs
                if (topResults.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val player = controller ?: return@withContext
                        val currentIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }.toSet()
                        
                        val newSongs = topResults
                            .filter { it.id !in currentIds }
                            .map { res ->
                                Song(
                                    id = res.id,
                                    title = res.title,
                                    channel = res.channel,
                                    durationSeconds = res.durationSeconds,
                                    thumbnailUrl = res.thumbnailUrl,
                                    isAutoplay = true
                                )
                            }
                            
                        if (newSongs.isNotEmpty()) {
                            val mediaItems = newSongs.map { createMediaItem(it) }
                            player.addMediaItems(mediaItems)
                            android.util.Log.d("PlaybackManager", "Autoplay: successfully added ${newSongs.size} songs to queue")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackManager", "Autoplay failed to fetch related songs", e)
            }
        }
    }

    private fun deriveRelatedQuery(song: Song): String {
        val title = song.title.lowercase()
        val channel = song.channel.lowercase()
        
        val regionalKeywords = listOf(
            "rajasthani", "marwari", "bhojpuri", "haryanvi", "punjabi", "garhwali",
            "maithili", "sufi", "ghazal", "bhajan", "devotional", "gujarati", 
            "bengali", "odia", "assamese", "kannada", "telugu", "tamil", "malayalam",
            "marathi", "konkani", "pahadi", "folk", "himachali", "dogri", "santhali",
            "qawwali", "classical", "carnatic", "hindustani", "sawariya", "dairy",
            "rajasthan", "marwar", "chetak"
        )
        
        val matched = regionalKeywords.firstOrNull { title.contains(it) || channel.contains(it) }
        
        val cleanArtist = song.channel
            .replace(" - Topic", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .replace("official", "", ignoreCase = true)
            .trim()
            
        val cleanTitle = song.title
            .replace(Regex("\\((.*?)\\)"), "")
            .replace(Regex("\\[(.*?)\\]"), "")
            .trim()
            
        return if (matched != null) {
            "$cleanArtist $matched song"
        } else {
            val words = cleanTitle.split(" ").filter { it.length > 3 }.take(3).joinToString(" ")
            if (words.isNotEmpty()) {
                "$cleanArtist $words"
            } else {
                "$cleanArtist song"
            }
        }
    }
}
