package com.personal.mymusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personal.mymusic.data.database.PlaylistDao
import com.personal.mymusic.data.database.PlaylistEntity
import com.personal.mymusic.data.database.SongEntity
import com.personal.mymusic.data.network.NewPipeService
import com.personal.mymusic.domain.model.SearchResult
import com.personal.mymusic.domain.model.Song
import com.personal.mymusic.domain.model.LyricLine
import com.personal.mymusic.domain.model.LyricsState
import com.personal.mymusic.MyApplication
import com.personal.mymusic.playback.PlaybackManager
import com.personal.mymusic.ui.screens.AppScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    application: Application,
    private val playlistDao: PlaylistDao,
    private val newPipeService: NewPipeService,
    val playbackManager: PlaybackManager
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("music_prefs", Application.MODE_PRIVATE)
    
    private val _cacheSizeLimitGb = MutableStateFlow(sharedPrefs.getFloat("cache_size_gb", 1.0f))
    val cacheSizeLimitGb: StateFlow<Float> = _cacheSizeLimitGb.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(sharedPrefs.getFloat("crossfade_seconds", 0.0f))
    val crossfadeSeconds: StateFlow<Float> = _crossfadeSeconds.asStateFlow()

    // Playlists without Liked Songs
    val playlists: StateFlow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .map { list -> list.filter { it.name != "Liked Songs" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Liked Songs Auto-Playlist State
    private val _likedPlaylistId = MutableStateFlow<Long?>(null)
    val likedPlaylistId: StateFlow<Long?> = _likedPlaylistId.asStateFlow()

    val likedSongs: StateFlow<List<Song>> = _likedPlaylistId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else playlistDao.getSongsForPlaylist(id).map { entities ->
                entities.map { it.toDomainSong() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val likedSongIds: StateFlow<Set<String>> = likedSongs
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    val selectedPlaylistSongs: StateFlow<List<Song>> = _selectedPlaylistId
        .flatMapLatest { playlistId ->
            if (playlistId == null) flowOf(emptyList())
            else playlistDao.getSongsForPlaylist(playlistId).map { entities ->
                entities.map { it.toDomainSong() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Navigation State for Configuration Changes Retention and Deeplinking
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Home)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Search History Suggestions
    val recentSearches: StateFlow<List<String>> = playlistDao.getRecentSearches()
        .map { list -> list.map { it.query } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Trending Feed Suggestions
    private val _trendingFeed = MutableStateFlow<Map<String, List<SearchResult>>>(emptyMap())
    val trendingFeed: StateFlow<Map<String, List<SearchResult>>> = _trendingFeed.asStateFlow()

    private val _isTrendingLoading = MutableStateFlow(false)
    val isTrendingLoading: StateFlow<Boolean> = _isTrendingLoading.asStateFlow()

    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()
    
    private var lyricsFetchJob: Job? = null
    
    // Update Checking State
    val updateManager = (application as MyApplication).container.updateManager
    
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun checkForUpdates(isAutoCheck: Boolean = false) {
        if (_updateState.value is UpdateState.Checking || _updateState.value is UpdateState.Downloading) return
        
        _updateState.value = UpdateState.Checking
        viewModelScope.launch {
            try {
                val updateInfo = updateManager.checkForUpdates()
                if (updateInfo.isUpdateAvailable) {
                    _updateState.value = UpdateState.UpdateAvailable(
                        latestVersion = updateInfo.latestVersionName,
                        downloadUrl = updateInfo.downloadUrl,
                        releaseNotes = updateInfo.releaseNotes
                    )
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (e: Exception) {
                if (!isAutoCheck) {
                    _updateState.value = UpdateState.Error(e.localizedMessage ?: "Failed to check for updates")
                } else {
                    _updateState.value = UpdateState.Idle
                }
            }
        }
    }

    fun startUpdateDownload(latestVersion: String, downloadUrl: String, onError: (String) -> Unit) {
        _updateState.value = UpdateState.Downloading
        updateManager.downloadAndInstallUpdate(
            latestVersion = latestVersion,
            downloadUrl = downloadUrl,
            onDownloadStarted = {
                // Keep it in downloading state
            },
            onError = { errMsg ->
                _updateState.value = UpdateState.Error(errMsg)
                onError(errMsg)
            }
        )
    }
    
    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    init {
        // Automatically check for updates on startup
        checkForUpdates(isAutoCheck = true)
        
        // Apply saved cache size to CacheManager on init
        val savedGb = sharedPrefs.getFloat("cache_size_gb", 1.0f)
        val bytes = (savedGb * 1024 * 1024 * 1024).toLong()
        com.personal.mymusic.data.cache.CacheManager.updateCacheSize(application, bytes)

        // Find or create Liked Songs auto-playlist
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collect { dbPlaylists ->
                val liked = dbPlaylists.find { it.name == "Liked Songs" }
                if (liked != null) {
                    _likedPlaylistId.value = liked.id
                } else {
                    val newId = playlistDao.insertPlaylist(PlaylistEntity(name = "Liked Songs"))
                    _likedPlaylistId.value = newId
                }
            }
        }

        // Load trending feed
        loadTrendingFeed()

        // Start lyrics observation
        viewModelScope.launch {
            playbackManager.currentSong.collectLatest { song ->
                if (song == null) {
                    _lyricsState.value = LyricsState.Idle
                } else {
                    fetchLyricsForSong(song)
                }
            }
        }
    }

    fun loadTrendingFeed(isBackground: Boolean = false) {
        viewModelScope.launch {
            if (!isBackground) {
                _isTrendingLoading.value = true
            }
            try {
                val feed = newPipeService.getTrendingFeed()
                _trendingFeed.value = feed
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to load trending feed", e)
            } finally {
                if (!isBackground) {
                    _isTrendingLoading.value = false
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return
        
        // Add to search history
        addSearchQuery(query)
        
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            try {
                val results = newPipeService.search(query)
                _searchResults.value = results
            } catch (e: Exception) {
                e.printStackTrace()
                _searchError.value = e.message ?: "Failed to perform search"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchAndPlaySong(query: String) {
        viewModelScope.launch {
            try {
                // Set loading indicator
                playbackManager.setCurrentlyLoadingSongId(query)
                val results = newPipeService.search(query)
                if (results.isNotEmpty()) {
                    val item = results.first()
                    val song = Song(
                        id = item.id,
                        title = item.title,
                        channel = item.channel,
                        durationSeconds = item.durationSeconds,
                        thumbnailUrl = item.thumbnailUrl
                    )
                    playbackManager.playSongImmediate(song)
                } else {
                    android.widget.Toast.makeText(getApplication(), "No results found for $query", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "searchAndPlaySong failed for $query", e)
                android.widget.Toast.makeText(getApplication(), "Search failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                playbackManager.setCurrentlyLoadingSongId(null)
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistDao.insertPlaylist(PlaylistEntity(name = name))
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            playlistDao.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlistId)
            if (_selectedPlaylistId.value == playlistId) {
                _selectedPlaylistId.value = null
            }
        }
    }

    fun selectPlaylist(playlistId: Long?) {
        _selectedPlaylistId.value = playlistId
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            playlistDao.addSongToPlaylist(playlistId, song.toEntity())
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            playlistDao.deletePlaylistSongCrossRef(playlistId, songId)
        }
    }

    // Search History DB Helpers
    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            playlistDao.insertSearchQuery(com.personal.mymusic.data.database.SearchHistoryEntity(query.trim()))
        }
    }

    fun deleteSearchQuery(query: String) {
        viewModelScope.launch {
            playlistDao.deleteSearchQuery(query)
        }
    }

    // Liked Songs Auto-Playlist Helpers
    fun toggleLikeSong(song: Song) {
        val likedId = _likedPlaylistId.value ?: return
        viewModelScope.launch {
            val songEntity = SongEntity(
                id = song.id,
                title = song.title,
                channel = song.channel,
                durationSeconds = song.durationSeconds,
                thumbnailUrl = song.thumbnailUrl
            )
            if (likedSongIds.value.contains(song.id)) {
                playlistDao.deletePlaylistSongCrossRef(likedId, song.id)
            } else {
                playlistDao.addSongToPlaylist(likedId, songEntity)
            }
        }
    }

    fun setCrossfadeSeconds(seconds: Float) {
        _crossfadeSeconds.value = seconds
        sharedPrefs.edit().putFloat("crossfade_seconds", seconds).apply()
    }

    fun moveSongInPlaylist(playlistId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val songs = selectedPlaylistSongs.value.toMutableList()
            if (fromIndex in songs.indices && toIndex in songs.indices) {
                val song = songs.removeAt(fromIndex)
                songs.add(toIndex, song)
                playlistDao.reorderSongs(playlistId, songs.map { it.id })
            }
        }
    }

    fun setCacheSizeLimit(gb: Float) {
        _cacheSizeLimitGb.value = gb
        sharedPrefs.edit().putFloat("cache_size_gb", gb).apply()
        val bytes = (gb * 1024 * 1024 * 1024).toLong()
        com.personal.mymusic.data.cache.CacheManager.updateCacheSize(getApplication(), bytes)
    }

    fun clearCache() {
        viewModelScope.launch {
            com.personal.mymusic.data.cache.CacheManager.clearCache()
        }
    }

    private fun SongEntity.toDomainSong() = Song(
        id = id,
        title = title,
        channel = channel,
        durationSeconds = durationSeconds,
        thumbnailUrl = thumbnailUrl
    )

    private fun Song.toEntity() = SongEntity(
        id = id,
        title = title,
        channel = channel,
        durationSeconds = durationSeconds,
        thumbnailUrl = thumbnailUrl
    )

    private fun fetchLyricsForSong(song: Song) {
        lyricsFetchJob?.cancel()
        lyricsFetchJob = viewModelScope.launch {
            _lyricsState.value = LyricsState.Loading
            try {
                val lyricsService = getApplication<MyApplication>().container.lyricsService
                val result = lyricsService.fetchLyrics(song.title, song.channel, song.durationSeconds)
                if (result != null) {
                    if (!result.syncedLyrics.isNullOrEmpty()) {
                        val parsed = parseLrc(result.syncedLyrics)
                        if (parsed.isNotEmpty()) {
                            _lyricsState.value = LyricsState.Success(parsed, isSynced = true)
                            return@launch
                        }
                    }
                    if (!result.plainLyrics.isNullOrEmpty()) {
                        val lines = result.plainLyrics.split("\n").map { LyricLine(-1L, it) }
                        _lyricsState.value = LyricsState.Success(lines, isSynced = false)
                        return@launch
                    }
                }
                _lyricsState.value = LyricsState.NotAvailable
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error fetching lyrics", e)
                _lyricsState.value = LyricsState.NotAvailable
            }
        }
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = lrcText.lineSequence()
        val result = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?\\]")
        
        for (line in lines) {
            val matches = regex.findAll(line).toList()
            if (matches.isEmpty()) continue
            
            val text = line.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty() && matches.size == 1 && line.contains(Regex("\\[(ti|ar|al|by|offset|length):"))) {
                continue
            }
            
            for (match in matches) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msStr = match.groupValues[3]
                var ms = 0L
                if (msStr.isNotEmpty()) {
                    ms = msStr.toLong()
                    if (msStr.length == 2) {
                        ms *= 10
                    }
                }
                val timestampMs = (min * 60 + sec) * 1000 + ms
                result.add(LyricLine(timestampMs, text))
            }
        }
        return result.sortedBy { it.timestampMs }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val playlistDao: PlaylistDao,
    private val newPipeService: NewPipeService,
    private val playbackManager: PlaybackManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, playlistDao, newPipeService, playbackManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    ) : UpdateState
    object UpToDate : UpdateState
    data class Error(val message: String) : UpdateState
    object Downloading : UpdateState
}
