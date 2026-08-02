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
import kotlinx.coroutines.isActive

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class MainViewModel(
    application: Application,
    private val playlistDao: PlaylistDao,
    private val newPipeService: NewPipeService,
    val playbackManager: PlaybackManager
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("music_prefs", Application.MODE_PRIVATE)
    
    private val _userName = MutableStateFlow(sharedPrefs.getString("user_custom_name", "Vibe") ?: "Vibe")
    val userName: StateFlow<String> = _userName.asStateFlow()

    data class GreetingState(
        val title: String = "Good Evening 🌆",
        val subtitle: String = "Wind down with great music"
    )

    private val _greetingState = MutableStateFlow(calculateGreetingState())
    val greetingState: StateFlow<GreetingState> = _greetingState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _greetingState.value = calculateGreetingState()
                kotlinx.coroutines.delay(30_000L)
            }
        }
    }

    fun updateUserName(newName: String) {
        val trimmed = newName.trim()
        val nameToSave = if (trimmed.isBlank()) "Vibe" else trimmed
        sharedPrefs.edit().putString("user_custom_name", nameToSave).apply()
        _userName.value = nameToSave
        _greetingState.value = calculateGreetingState()
    }

    fun refreshGreeting() {
        _greetingState.value = calculateGreetingState()
    }

    private fun calculateGreetingState(): GreetingState {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentName = _userName.value.ifBlank { "Vibe" }
        val isDefaultName = currentName.equals("Vibe", ignoreCase = true)

        val greetingText = when (hour) {
            in 5..11 -> if (isDefaultName) "Good Morning ☀️" else "Good Morning, $currentName ☀️"
            in 12..16 -> if (isDefaultName) "Good Afternoon 🌤️" else "Good Afternoon, $currentName 🌤️"
            in 17..20 -> if (isDefaultName) "Good Evening 🌆" else "Good Evening, $currentName 🌆"
            else -> if (isDefaultName) "Good Night 🌙" else "Good Night, $currentName 🌙"
        }

        val subtitleText = when (hour) {
            in 5..11 -> "Start your day with music"
            in 12..16 -> "Keep the energy going"
            in 17..20 -> "Wind down with great music"
            else -> "Your late-night soundtrack"
        }

        return GreetingState(title = greetingText, subtitle = subtitleText)
    }

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

    val recentlyPlayed: StateFlow<List<Song>> = playlistDao.getRecentlyPlayed(20)
        .map { list -> list.map { it.toDomainSong() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedSongs: StateFlow<List<Song>> = playlistDao.getMostPlayedSongs(20)
        .map { list -> list.map { it.toDomainSong() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedArtists: StateFlow<List<String>> = playlistDao.getMostPlayedArtists(10)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun removeSongFromHistory(songId: String) {
        viewModelScope.launch {
            playlistDao.deletePlayHistorySong(songId)
        }
    }

    fun clearPlayHistory() {
        viewModelScope.launch {
            playlistDao.clearPlayHistory()
        }
    }

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

    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        setDarkTheme(!_isDarkTheme.value)
    }

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        sharedPrefs.edit().putBoolean("is_dark_theme", enabled).apply()
    }

    // Back Stack — tracks navigation history for proper back behavior
    private val _backStack = MutableStateFlow<List<AppScreen>>(emptyList())
    val backStack: StateFlow<List<AppScreen>> = _backStack.asStateFlow()

    /**
     * Navigate to a screen, pushing the current screen onto the back stack.
     * Navigating to Home always resets the stack.
     */
    fun navigateTo(screen: AppScreen) {
        val current = _currentScreen.value
        if (screen is AppScreen.Home) {
            // Going to Home clears the back stack
            _backStack.value = emptyList()
        } else {
            // Push current screen onto back stack (avoid duplicates at top)
            if (current != screen) {
                _backStack.value = _backStack.value + current
            }
        }
        _currentScreen.value = screen
    }

    /**
     * Pop the back stack. Returns true if the back press was consumed.
     * Returns false when on Home with empty stack (should exit app).
     */
    fun popBackStack(): Boolean {
        if (_isPlayerExpanded.value) {
            _isPlayerExpanded.value = false
            return true
        }
        val stack = _backStack.value
        return if (stack.isNotEmpty()) {
            val prev = stack.last()
            _backStack.value = stack.dropLast(1)
            _currentScreen.value = prev
            true
        } else {
            // On Home tab with no history — let app exit
            if (_currentScreen.value is AppScreen.Home) {
                false
            } else {
                // Not on Home — go to Home first
                _backStack.value = emptyList()
                _currentScreen.value = AppScreen.Home
                true
            }
        }
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    // ---- Playlist Recommendations ----
    private val _playlistRecommendations = MutableStateFlow<List<SearchResult>>(emptyList())
    val playlistRecommendations: StateFlow<List<SearchResult>> = _playlistRecommendations.asStateFlow()

    private val _isLoadingRecommendations = MutableStateFlow(false)
    val isLoadingRecommendations: StateFlow<Boolean> = _isLoadingRecommendations.asStateFlow()

    private var recommendationsJob: Job? = null

    /**
     * Derives artist/keyword-based search queries from the songs in a playlist
     * and fetches recommendations via YouTube search, filtering out already-added songs.
     */
    fun fetchPlaylistRecommendations(songs: List<Song>) {
        recommendationsJob?.cancel()
        if (songs.isEmpty()) {
            _playlistRecommendations.value = emptyList()
            return
        }
        recommendationsJob = viewModelScope.launch {
            _isLoadingRecommendations.value = true
            try {
                // Extract top artists (by frequency in playlist)
                val artistCounts = songs.groupingBy { it.channel.trim() }.eachCount()
                val topArtists = artistCounts.entries.sortedByDescending { it.value }.take(3).map { it.key }

                // Keyword heuristics: detect language/genre from artist names
                val isBollywood = topArtists.any { artist ->
                    listOf("arijit", "pritam", "atif", "shreya", "sonu", "kumar sanu",
                           "neha", "armaan", "jubin", "mohit").any { k -> artist.lowercase().contains(k) }
                }
                val isPunjabi = topArtists.any { artist ->
                    listOf("diljit", "sidhu", "ap dhillon", "karan", "shubh", "g khan").any { k -> artist.lowercase().contains(k) }
                }
                val genreTag = when {
                    isBollywood -> "hindi songs"
                    isPunjabi -> "punjabi songs"
                    else -> "songs"
                }

                val existingIds = songs.map { it.id }.toSet()
                val results = mutableListOf<SearchResult>()

                for (artist in topArtists) {
                    if (!isActive) break
                    val query = "$artist $genreTag"
                    try {
                        val found = newPipeService.search(query)
                        results.addAll(found.filter { it.id !in existingIds })
                    } catch (e: Exception) {
                        android.util.Log.e("Recommendations", "Failed query: $query", e)
                    }
                }

                // Deduplicate and take top 8
                _playlistRecommendations.value = results.distinctBy { it.id }.take(8)
            } catch (e: Exception) {
                android.util.Log.e("Recommendations", "fetchPlaylistRecommendations failed", e)
                _playlistRecommendations.value = emptyList()
            } finally {
                _isLoadingRecommendations.value = false
            }
        }
    }

    fun dismissRecommendation(songId: String) {
        _playlistRecommendations.value = _playlistRecommendations.value.filter { it.id != songId }
    }

    fun clearRecommendations() {
        recommendationsJob?.cancel()
        _playlistRecommendations.value = emptyList()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

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

    fun clearRecentSearches() {
        viewModelScope.launch {
            playlistDao.clearSearchHistory()
        }
    }

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

        // Collect search query changes with debounce for live search suggestions
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.trim().length >= 2) {
                        val suggestions = newPipeService.getSearchSuggestions(query)
                        _searchSuggestions.value = suggestions
                    } else {
                        _searchSuggestions.value = emptyList()
                    }
                }
        }
        
        // Apply saved cache size to CacheManager on init
        val savedGb = sharedPrefs.getFloat("cache_size_gb", 1.0f)
        val bytes = (savedGb * 1024 * 1024 * 1024).toLong()
        com.personal.mymusic.data.cache.CacheManager.updateCacheSize(application, bytes)

        // Find or create Liked Songs auto-playlist
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collect { dbPlaylists ->
                val liked = dbPlaylists.find { it.name == "Liked Songs" }
                val likedId = if (liked != null) {
                    liked.id
                } else {
                    playlistDao.insertPlaylist(PlaylistEntity(name = "Liked Songs"))
                }
                _likedPlaylistId.value = likedId
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
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            _searchError.value = null
        }
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

    private fun com.personal.mymusic.data.database.PlayHistoryEntity.toDomainSong() = Song(
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
