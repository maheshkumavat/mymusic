package com.personal.mymusic.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.mymusic.data.database.PlaylistEntity
import com.personal.mymusic.domain.model.SearchResult
import com.personal.mymusic.domain.model.Song
import com.personal.mymusic.ui.MainViewModel
import com.personal.mymusic.ui.UpdateState
import com.personal.mymusic.ui.components.*
import androidx.media3.common.Player
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import com.personal.mymusic.domain.model.LyricsState
import com.personal.mymusic.domain.model.LyricLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

sealed class AppScreen {
    object Search : AppScreen()
    object Library : AppScreen()
    data class PlaylistDetails(val playlistId: Long, val playlistName: String) : AppScreen()
    object Settings : AppScreen()
}

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (Long, String) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val error by viewModel.searchError.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    
    val focusManager = LocalFocusManager.current
    var isSearchFieldFocused by remember { mutableStateOf(false) }
    
    val recentSearches by viewModel.recentSearches.collectAsState()
    val trendingFeed by viewModel.trendingFeed.collectAsState()
    val isTrendingLoading by viewModel.isTrendingLoading.collectAsState()

    var showAddToPlaylistDialog by remember { mutableStateOf<Song?>(null) }

    // Observe lifecycle state to pause/resume auto-refresh
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppForegrounded by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAppForegrounded = true
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                isAppForegrounded = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-refresh the trending feed every 3 minutes (180 seconds) when search is empty and app is in foreground
    LaunchedEffect(isAppForegrounded, query) {
        if (isAppForegrounded && query.isEmpty()) {
            while (isActive) {
                delay(180000)
                android.util.Log.d("SearchScreen", "Periodic auto-refresh triggered for trending feed")
                viewModel.loadTrendingFeed(isBackground = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Explore Music",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search YouTube...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceGlass,
                    unfocusedContainerColor = SurfaceGlass,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                    .onFocusChanged { isSearchFieldFocused = it.isFocused },
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    viewModel.performSearch()
                    focusManager.clearFocus()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Search failed: $error", color = Color.Red, textAlign = TextAlign.Center)
            }
        } else if (query.isNotEmpty() && results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No results found for \"$query\"", color = Color.LightGray, textAlign = TextAlign.Center)
            }
        } else if (query.isNotEmpty() || results.isNotEmpty()) {
            val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(results) { _, item ->
                    val song = Song(
                        id = item.id,
                        title = item.title,
                        channel = item.channel,
                        durationSeconds = item.durationSeconds,
                        thumbnailUrl = item.thumbnailUrl
                    )
                    
                    SongRow(
                        song = song,
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.playbackManager.playSongImmediate(song)
                        },
                        isLoading = currentlyLoadingSongId == song.id,
                        trailingContent = {
                            IconButton(onClick = { showAddToPlaylistDialog = song }) {
                                Icon(Icons.Default.Add, contentDescription = "Add to playlist", tint = Color.White)
                            }
                        }
                    )
                }
            }
        } else {
            // Empty query search view
            if (isSearchFieldFocused && recentSearches.isNotEmpty()) {
                // Show Recent Searches Suggestions
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Recent Searches",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(recentSearches) { _, suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSearchQuery(suggestion)
                                        viewModel.performSearch()
                                        focusManager.clearFocus()
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(text = suggestion, color = Color.White, fontSize = 16.sp)
                                }
                                IconButton(onClick = { viewModel.deleteSearchQuery(suggestion) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                            Divider(color = BorderGlass)
                        }
                    }
                }
            } else {
                // Show Trending/Popular feed
                if (isTrendingLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentColor)
                    }
                } else {
                    val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        trendingFeed.forEach { (categoryName, songList) ->
                            if (songList.isNotEmpty()) {
                                item {
                                    TrendingSection(
                                        title = categoryName,
                                        songs = songList,
                                        onSongClick = { song ->
                                            viewModel.playbackManager.playSongImmediate(song)
                                        },
                                        onAddClick = { song ->
                                            showAddToPlaylistDialog = song
                                        },
                                        currentlyLoadingSongId = currentlyLoadingSongId
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add to playlist dialog
    if (showAddToPlaylistDialog != null) {
        val songToAdd = showAddToPlaylistDialog!!
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = null },
            title = { Text("Add to Playlist", color = Color.White) },
            containerColor = BackgroundStart,
            text = {
                val allPlaylistsIncludingLiked = mutableListOf<PlaylistEntity>()
                viewModel.likedPlaylistId.value?.let { likedId ->
                    allPlaylistsIncludingLiked.add(PlaylistEntity(id = likedId, name = "Liked Songs"))
                }
                allPlaylistsIncludingLiked.addAll(playlists)

                if (allPlaylistsIncludingLiked.isEmpty()) {
                    Text("No playlists created yet. Create one in the Library tab first!", color = Color.LightGray)
                } else {
                    LazyColumn {
                        itemsIndexed(allPlaylistsIncludingLiked) { _, playlist ->
                            Text(
                                text = playlist.name,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, songToAdd)
                                        showAddToPlaylistDialog = null
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                            Divider(color = BorderGlass)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = null }) {
                    Text("Cancel", color = AccentColor)
                }
            }
        )
    }
}

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (Long, String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val likedPlaylistId by viewModel.likedPlaylistId.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = {
                    playlistNameInput = ""
                    showCreateDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Playlist", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Liked Songs Auto-Playlist Item
            likedPlaylistId?.let { id ->
                item {
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistClick(id, "Liked Songs") }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red)
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Liked Songs",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-playlist",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Playlists List
            if (playlists.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No custom playlists created yet.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                itemsIndexed(playlists) { _, playlist ->
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistClick(playlist.id, playlist.name) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(AccentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = AccentColor)
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(onClick = {
                                renameInput = playlist.name
                                playlistToRename = playlist
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Color.Gray)
                            }

                            IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Create playlist dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", color = Color.White) },
            containerColor = BackgroundStart,
            text = {
                TextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    placeholder = { Text("Playlist Name", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = SurfaceGlass,
                        unfocusedContainerColor = SurfaceGlass
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            viewModel.createPlaylist(playlistNameInput)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Rename playlist dialog
    if (playlistToRename != null) {
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = { Text("Rename Playlist", color = Color.White) },
            containerColor = BackgroundStart,
            text = {
                TextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = SurfaceGlass,
                        unfocusedContainerColor = SurfaceGlass
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renamePlaylist(playlistToRename!!.id, renameInput)
                            playlistToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailsScreen(
    playlistId: Long,
    playlistName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val songs by viewModel.selectedPlaylistSongs.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.selectPlaylist(playlistId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.selectPlaylist(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = playlistName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (songs.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        viewModel.playbackManager.playPlaylist(songs, 0)
                        viewModel.playbackManager.setShuffleEnabled(false)
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = AccentColor, modifier = Modifier.size(32.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(onClick = {
                        val shuffledList = songs.shuffled()
                        viewModel.playbackManager.playPlaylist(shuffledList, 0)
                        viewModel.playbackManager.setShuffleEnabled(true)
                    }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle Play", tint = AccentColor, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (songs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "This playlist is empty.\nGo to the Search tab and search for songs to add!",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        song = song,
                        onClick = { viewModel.playbackManager.playPlaylist(songs, index) },
                        isLoading = currentlyLoadingSongId == song.id,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.moveSongInPlaylist(playlistId, index, index - 1) },
                                    enabled = index > 0
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Move Up",
                                        tint = if (index > 0) Color.White else Color.DarkGray
                                    )
                                }
                                
                                IconButton(
                                    onClick = { viewModel.moveSongInPlaylist(playlistId, index, index + 1) },
                                    enabled = index < songs.size - 1
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "Move Down",
                                        tint = if (index < songs.size - 1) Color.White else Color.DarkGray
                                    )
                                }

                                IconButton(onClick = { viewModel.removeSongFromPlaylist(playlistId, song.id) }) {
                                    Icon(Icons.Default.RemoveCircle, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val cacheSizeGb by viewModel.cacheSizeLimitGb.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
        )

        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Offline Mode Cache",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Configures maximum disk storage for cached audio streams. Old streams will be automatically evicted (LRU).",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Limit: ${String.format("%.1f GB", cacheSizeGb)}", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                Slider(
                    value = cacheSizeGb,
                    onValueChange = { viewModel.setCacheSizeLimit(it) },
                    valueRange = 0.5f..5.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentColor,
                        activeTrackColor = AccentColor,
                        inactiveTrackColor = Color.DarkGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.clearCache() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Stream Cache", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val crossfadeSeconds by viewModel.crossfadeSeconds.collectAsState()

        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Crossfade Transitions",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Controls the duration of smooth volume transitions between songs.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val displayLabel = if (crossfadeSeconds <= 0f) "Off" else String.format("%.0fs", crossfadeSeconds)
                Text(text = "Crossfade: $displayLabel", color = Color.White, fontWeight = FontWeight.SemiBold)

                Slider(
                    value = crossfadeSeconds,
                    onValueChange = {
                        val snapped = if (it < 1f) 0f else if (it < 2.5f) 2f else if (it < 3.5f) 3f else 4f
                        viewModel.setCrossfadeSeconds(snapped)
                    },
                    valueRange = 0f..4f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentColor,
                        activeTrackColor = AccentColor,
                        inactiveTrackColor = Color.DarkGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val autoplayEnabledSetting by viewModel.playbackManager.autoplayEnabled.collectAsState()

        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Autoplay Similar Songs",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Automatically append similar songs to the queue when you reach the end.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = autoplayEnabledSetting,
                        onCheckedChange = { viewModel.playbackManager.setAutoplayEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentColor,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val updateState by viewModel.updateState.collectAsState()
        val context = LocalContext.current
        val currentVersion = remember(context) {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                "v${packageInfo.versionName ?: "1.0"}"
            } catch (e: Exception) {
                "v1.0"
            }
        }

        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "App Version & Updates",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current Version: $currentVersion",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val statusText = when (val state = updateState) {
                            is UpdateState.Checking -> "Checking for updates..."
                            is UpdateState.Downloading -> "Downloading update..."
                            is UpdateState.UpToDate -> "Your app is up to date!"
                            is UpdateState.Error -> state.message
                            else -> "Check if there's a new version available."
                        }
                        
                        Text(
                            text = statusText,
                            color = if (updateState is UpdateState.Error) Color.Red.copy(alpha = 0.8f) else Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { viewModel.checkForUpdates(isAutoCheck = false) },
                        enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentColor,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.DarkGray,
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Check")
                    }
                }
            }
        }
    }
}

enum class NowPlayingSubView {
    ALBUM_ART,
    LYRICS,
    QUEUE
}

@Composable
fun SmoothScrubber(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit
) {
    var smoothPosition by remember(position) { mutableStateOf(position.toFloat()) }

    LaunchedEffect(isPlaying, position) {
        if (isPlaying) {
            val startTime = System.currentTimeMillis()
            val basePosition = position
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                smoothPosition = (basePosition + elapsed).toFloat().coerceAtMost(duration.toFloat())
                delay(16) // ~60fps
            }
        } else {
            smoothPosition = position.toFloat()
        }
    }

    val progress = if (duration > 0) (smoothPosition / duration).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = { onSeek((it * duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(smoothPosition.toLong()),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatMsCountdown(maxOf(0, duration - smoothPosition.toLong())),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AnimatedPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "PlayPauseScale"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = "Play/Pause",
            tint = Color.Black,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    onCollapse: () -> Unit
) {
    val currentSong by viewModel.playbackManager.currentSong.collectAsState()
    val isPlaying by viewModel.playbackManager.isPlaying.collectAsState()
    val position by viewModel.playbackManager.currentPosition.collectAsState()
    val duration by viewModel.playbackManager.duration.collectAsState()
    val shuffleEnabled by viewModel.playbackManager.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.playbackManager.repeatMode.collectAsState()
    val queue by viewModel.playbackManager.queue.collectAsState()
    val autoplayEnabled by viewModel.playbackManager.autoplayEnabled.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()

    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundEnd),
            contentAlignment = Alignment.Center
        ) {
            Text("No song loaded", color = Color.Gray, fontFamily = InterFontFamily)
        }
        return
    }

    val song = currentSong!!

    val likedSongIds by viewModel.likedSongIds.collectAsState()
    val isLiked = likedSongIds.contains(song.id)

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    val sleepTimerSecs by viewModel.playbackManager.sleepTimerRemainingSeconds.collectAsState()
    val equalizerPreset by viewModel.playbackManager.equalizerPreset.collectAsState()

    var activeSubView by remember { mutableStateOf(NowPlayingSubView.ALBUM_ART) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-bleed blurred album art background (heavy gaussian blur, slightly darkened)
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(60.dp)
                .alpha(0.35f)
        )
        
        // Translucent dark gradient overlay to ensure UI elements stand out
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp) // Apple Music 16dp horizontal padding
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = when (activeSubView) {
                        NowPlayingSubView.ALBUM_ART -> "Now Playing"
                        NowPlayingSubView.LYRICS -> "Lyrics"
                        NowPlayingSubView.QUEUE -> "Playing Next"
                    },
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(36.dp))
            }

            // Center View area (Album Art, Lyrics, Queue)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = activeSubView, label = "CenterView") { view ->
                    when (view) {
                        NowPlayingSubView.ALBUM_ART -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Smooth transition states for Album Art fade & scale on song load
                                var artOpacity by remember { mutableStateOf(0f) }
                                var artScale by remember { mutableStateOf(0.85f) }
                                LaunchedEffect(song.id) {
                                    artOpacity = 0f
                                    artScale = 0.85f
                                    animate(
                                        initialValue = 0f,
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
                                    ) { value, _ ->
                                        artOpacity = value
                                        artScale = 0.85f + (value * 0.15f)
                                    }
                                }

                                // Cover Art Card: Centered, 85% of screen width, 12dp corner radius, soft shadow
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .aspectRatio(1f)
                                        .graphicsLayer(
                                            scaleX = artScale,
                                            scaleY = artScale,
                                            alpha = artOpacity
                                        )
                                        .shadow(elevation = 20.dp, shape = RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = song.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                
                                // Exact vertical gap: 24dp
                                Spacer(modifier = Modifier.height(24.dp))

                                // Song Info Row left-aligned with exact Apple Music fonts
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontSize = 22.sp, // Apple Music Song Title
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.SemiBold, // Semibold weight
                                            letterSpacing = (-0.2).sp, // Tight letter spacing
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = song.channel,
                                            color = Color.White.copy(alpha = 0.7f), // 70% opacity
                                            fontSize = 15.sp, // Apple Music Artist Name
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.toggleLikeSong(song) }) {
                                            Icon(
                                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Like Song",
                                                tint = if (isLiked) Color.Red else Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        IconButton(onClick = { showMenu = true }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreHoriz,
                                                contentDescription = "More Options",
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        NowPlayingSubView.LYRICS -> {
                            LyricsContainer(
                                lyricsState = lyricsState,
                                currentPosition = position,
                                onLineClick = { timestampMs ->
                                    viewModel.playbackManager.seekTo(timestampMs)
                                }
                            )
                        }
                        NowPlayingSubView.QUEUE -> {
                            QueueContainer(
                                queue = queue,
                                currentSong = song,
                                autoplayEnabled = autoplayEnabled,
                                onAutoplayToggle = { viewModel.playbackManager.setAutoplayEnabled(it) },
                                onSongClick = { upcomingSong ->
                                    val player = viewModel.playbackManager.controller
                                    if (player != null) {
                                        for (i in 0 until player.mediaItemCount) {
                                            if (player.getMediaItemAt(i).mediaId == upcomingSong.id) {
                                                player.seekTo(i, 0L)
                                                player.play()
                                                break
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Exact vertical spacing rhythm: 20dp between Title Block & Scrubber
            Spacer(modifier = Modifier.height(20.dp))

            // Scrubber + Playback controls + Bottom row controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                // Smooth Scrubber with coroutine ticker at 60fps
                SmoothScrubber(
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    onSeek = { viewModel.playbackManager.seekTo(it) }
                )

                // Exact vertical spacing rhythm: 32dp between Scrubber & Controls
                Spacer(modifier = Modifier.height(32.dp))

                // Playback controls row: shuffle, prev, play/pause (animated), next, repeat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.playbackManager.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleEnabled) AccentColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.playbackManager.skipToPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp) // Apple Music exact prev icon size
                        )
                    }

                    // Centered Play/Pause Button with 0.92x spring pressed micro-interaction
                    AnimatedPlayPauseButton(
                        isPlaying = isPlaying,
                        onClick = { viewModel.playbackManager.togglePlayPause() }
                    )

                    IconButton(onClick = { viewModel.playbackManager.skipToNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp) // Apple Music exact next icon size
                        )
                    }

                    IconButton(
                        onClick = {
                            val nextMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                                else -> Player.REPEAT_MODE_OFF
                            }
                            viewModel.playbackManager.setRepeatMode(nextMode)
                        }
                    ) {
                        Icon(
                            imageVector = when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) AccentColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bottom Row of icons: Lyrics, Queue, Device Output (cast-style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            activeSubView = if (activeSubView == NowPlayingSubView.LYRICS) {
                                NowPlayingSubView.ALBUM_ART
                            } else {
                                NowPlayingSubView.LYRICS
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = if (activeSubView == NowPlayingSubView.LYRICS) AccentColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            activeSubView = if (activeSubView == NowPlayingSubView.QUEUE) {
                                NowPlayingSubView.ALBUM_ART
                            } else {
                                NowPlayingSubView.QUEUE
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = if (activeSubView == NowPlayingSubView.QUEUE) AccentColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(onClick = { /* Decorative or output cast */ }) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Cast Device",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }

    // More Options Menu Dialog
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("Options", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = BackgroundStart,
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSleepTimerDialog = true
                                showMenu = false
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sleep Timer", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    Divider(color = BorderGlass)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showEqualizerDialog = true
                                showMenu = false
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Equalizer, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Equalizer", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("Close", color = AccentColor)
                }
            }
        )
    }

    // Sleep Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", color = Color.White) },
            containerColor = BackgroundStart,
            text = {
                Column {
                    listOf(
                        "Off" to 0,
                        "5 Minutes" to 5,
                        "15 Minutes" to 15,
                        "30 Minutes" to 30,
                        "45 Minutes" to 45,
                        "60 Minutes" to 60
                    ).forEach { (label, minutes) ->
                        Text(
                            text = label,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playbackManager.startSleepTimer(minutes)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        )
                        Divider(color = BorderGlass)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel", color = AccentColor)
                }
            }
        )
    }

    // Equalizer Preset Dialog
    if (showEqualizerDialog) {
        AlertDialog(
            onDismissRequest = { showEqualizerDialog = false },
            title = { Text("Equalizer Presets", color = Color.White) },
            containerColor = BackgroundStart,
            text = {
                Column {
                    listOf(
                        "NORMAL" to "Normal (Flat)",
                        "BASS_BOOST" to "Bass Boost",
                        "TREBLE_BOOST" to "Treble Boost"
                    ).forEach { (preset, label) ->
                        Text(
                            text = label,
                            color = if (equalizerPreset == preset) AccentColor else Color.White,
                            fontWeight = if (equalizerPreset == preset) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playbackManager.setEqualizerPreset(preset)
                                    showEqualizerDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        )
                        Divider(color = BorderGlass)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEqualizerDialog = false }) {
                    Text("Cancel", color = AccentColor)
                }
            }
        )
    }
}

@Composable
fun LyricsContainer(
    lyricsState: LyricsState,
    currentPosition: Long,
    onLineClick: (Long) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (lyricsState) {
            is LyricsState.Idle, is LyricsState.Loading -> {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
            }
            is LyricsState.NotAvailable -> {
                Text(
                    text = "Lyrics not available for this song",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
            is LyricsState.Success -> {
                val lines = lyricsState.lines
                val isSynced = lyricsState.isSynced
                
                if (isSynced) {
                    val listState = rememberLazyListState()
                    
                    val activeIndex = remember(lines, currentPosition) {
                        var index = -1
                        for (i in lines.indices) {
                            if (currentPosition >= lines[i].timestampMs) {
                                index = i
                            } else {
                                break
                            }
                        }
                        index
                    }
                    
                    LaunchedEffect(activeIndex) {
                        if (activeIndex >= 0) {
                            listState.animateScrollToItem(activeIndex, -180)
                        }
                    }
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(lines.size) { index ->
                            val line = lines[index]
                            val isActive = index == activeIndex
                            
                            val fontSize by animateFloatAsState(
                                targetValue = if (isActive) 24f else 20f,
                                label = "fontSize"
                            )
                            val alpha by animateFloatAsState(
                                targetValue = if (isActive) 1f else 0.4f,
                                label = "alpha"
                            )
                            
                            Text(
                                text = line.text,
                                color = Color.White.copy(alpha = alpha),
                                fontSize = fontSize.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLineClick(line.timestampMs) }
                                    .padding(horizontal = 8.dp),
                                softWrap = true
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(lines.size) { index ->
                            Text(
                                text = lines[index].text,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                softWrap = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QueueContainer(
    queue: List<Song>,
    currentSong: Song,
    autoplayEnabled: Boolean,
    onAutoplayToggle: (Boolean) -> Unit,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Autoplay Similar",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Keep playing related tracks",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = autoplayEnabled,
                onCheckedChange = onAutoplayToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentColor,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Up Next",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        val listState = rememberLazyListState()
        
        val currentSongIndex = remember(queue, currentSong) {
            queue.indexOfFirst { it.id == currentSong.id }
        }
        
        val upcomingSongs = remember(queue, currentSongIndex) {
            if (currentSongIndex >= 0 && currentSongIndex < queue.size) {
                queue.subList(currentSongIndex + 1, queue.size)
            } else {
                queue
            }
        }
        
        if (upcomingSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty. Playback will stop or Autoplay will generate tracks.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(upcomingSongs.size) { index ->
                    val songItem = upcomingSongs[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .clickable { onSongClick(songItem) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = songItem.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = songItem.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = songItem.channel,
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (songItem.isAutoplay) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(AccentColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .border(0.5.dp, AccentColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Autoplayed",
                                            color = AccentColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatMsCountdown(ms: Long): String {
    if (ms <= 0) return "-0:00"
    val totalSeconds = ms / 1000
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("-%d:%02d", mins, secs)
}

fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("%d:%02d", mins, secs)
}

@Composable
fun TrendingSection(
    title: String,
    songs: List<SearchResult>,
    onSongClick: (Song) -> Unit,
    onAddClick: (Song) -> Unit,
    currentlyLoadingSongId: String? = null
) {
    if (songs.isEmpty()) return
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs.size) { index ->
                val item = songs[index]
                val song = Song(
                    id = item.id,
                    title = item.title,
                    channel = item.channel,
                    durationSeconds = item.durationSeconds,
                    thumbnailUrl = item.thumbnailUrl
                )
                TrendingSongCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    onAddClick = { onAddClick(song) },
                    isLoading = currentlyLoadingSongId == song.id
                )
            }
        }
    }
}

@Composable
fun TrendingSongCard(
    song: Song,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    isLoading: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1B2E).copy(alpha = 0.5f))
            .border(1.dp, Color(0x1BFFFFFF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.channel,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AccentColor,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
