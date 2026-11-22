package com.personal.mymusic.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.mymusic.domain.model.Song
import com.personal.mymusic.ui.MainViewModel
import com.personal.mymusic.ui.components.*

@Composable
fun PlaylistDetailsScreen(
    playlistId: Long,
    playlistName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val songs by viewModel.selectedPlaylistSongs.collectAsState()
    val isPlaying by viewModel.playbackManager.isPlaying.collectAsState()
    val currentSong by viewModel.playbackManager.currentSong.collectAsState()
    val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
    val likedPlaylistId by viewModel.likedPlaylistId.collectAsState()

    val suggestedSongs by viewModel.playlistRecommendations.collectAsState()
    val isLoadingSuggestions by viewModel.isLoadingRecommendations.collectAsState()

    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var showRemoveSongDialog by remember { mutableStateOf<Song?>(null) }

    val isLikedPlaylist = playlistId == likedPlaylistId || playlistName.equals("Liked Songs", ignoreCase = true)

    LaunchedEffect(playlistId, playlistName) {
        viewModel.selectPlaylist(playlistId)
        viewModel.fetchPlaylistRecommendations(playlistName, songs)
    }

    LaunchedEffect(songs.size) {
        viewModel.fetchPlaylistRecommendations(playlistName, songs)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.selectPlaylist(null)
        }
    }

    val totalDurationSeconds = remember(songs) {
        songs.sumOf { it.durationSeconds }
    }
    val durationText = remember(totalDurationSeconds) {
        val hrs = totalDurationSeconds / 3600
        val mins = (totalDurationSeconds % 3600) / 60
        if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }

    FaintCirclesBackground(showArcs = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Top Bar: Back Button + Playlist Title + Delete Button + Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SketchIconButton(
                    onClick = onBack,
                    backgroundColor = SketchColors.Orange,
                    size = 42.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = SketchColors.Ink,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = playlistName,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SketchColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    textAlign = TextAlign.Center
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Delete Playlist button (hidden for Liked Songs)
                    if (!isLikedPlaylist) {
                        SketchIconButton(
                            onClick = { showDeletePlaylistDialog = true },
                            backgroundColor = SketchColors.HeartRed.copy(alpha = 0.15f),
                            size = 42.dp,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Playlist",
                                tint = SketchColors.HeartRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${songs.size} tracks • $durationText",
                    color = SketchColors.MutedText,
                    fontSize = 13.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium
                )

                if (songs.isNotEmpty()) {
                    SketchButton(
                        text = "Play All",
                        onClick = { viewModel.playbackManager.playPlaylist(songs) },
                        backgroundColor = SketchColors.Teal,
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SketchColors.Ink, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track list and Suggested songs section in LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                if (songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = SketchColors.MutedText,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "This playlist is empty.",
                                    color = SketchColors.Ink,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Add suggested tracks below to start listening!",
                                    color = SketchColors.MutedText,
                                    fontFamily = PoppinsFontFamily,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                        val tilt = if (index % 2 == 0) -1.8f else 1.8f
                        val isCurrentPlaying = currentSong?.id == song.id
                        val slab = if (isCurrentPlaying) SketchColors.Orange else slabColorForIndex(index)

                        SketchCard(
                            modifier = Modifier.fillMaxWidth(),
                            slabColor = slab,
                            rotation = tilt,
                            showPin = true,
                            onClick = { viewModel.playbackManager.playPlaylist(songs, startIndex = index) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontSize = 14.sp,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = SketchColors.Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.channel} • ${formatDuration(song.durationSeconds)}",
                                        fontSize = 12.sp,
                                        fontFamily = PoppinsFontFamily,
                                        color = SketchColors.MutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (currentlyLoadingSongId == song.id) {
                                    CircularProgressIndicator(
                                        color = SketchColors.Orange,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    SketchIconButton(
                                        onClick = {
                                            if (isCurrentPlaying) {
                                                viewModel.playbackManager.togglePlayPause()
                                            } else {
                                                viewModel.playbackManager.playPlaylist(songs, startIndex = index)
                                            }
                                        },
                                        backgroundColor = if (isCurrentPlaying) SketchColors.Orange else Color.White,
                                        shape = CircleShape,
                                        size = 38.dp,
                                        dropOffset = 2.dp
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isCurrentPlaying && isPlaying) "Pause" else "Play",
                                            tint = SketchColors.Ink,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { showRemoveSongDialog = song },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = SketchColors.MutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ---- Suggested Songs Section ----
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Suggested songs",
                                color = SketchColors.Ink,
                                fontSize = 18.sp,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Based on this playlist",
                                color = SketchColors.MutedText,
                                fontSize = 12.sp,
                                fontFamily = PoppinsFontFamily
                            )
                        }

                        if (isLoadingSuggestions) {
                            CircularProgressIndicator(
                                color = SketchColors.Orange,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = { viewModel.fetchPlaylistRecommendations(playlistName, songs) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SketchColors.Ink)
                            }
                        }
                    }
                }

                if (suggestedSongs.isNotEmpty()) {
                    itemsIndexed(suggestedSongs, key = { _, s -> "sugg_${s.id}" }) { idx, item ->
                        val suggSong = Song(
                            id = item.id,
                            title = item.title,
                            channel = item.channel,
                            durationSeconds = item.durationSeconds,
                            thumbnailUrl = item.thumbnailUrl
                        )
                        val isCurrentPlaying = currentSong?.id == item.id
                        val slab = slabColorForIndex(idx)

                        SketchCard(
                            modifier = Modifier.fillMaxWidth(),
                            slabColor = slab,
                            showPin = false,
                            onClick = { viewModel.playbackManager.playSongImmediate(suggSong) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(10.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = SketchColors.Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.channel} • ${formatDuration(item.durationSeconds)}",
                                        fontSize = 11.sp,
                                        fontFamily = PoppinsFontFamily,
                                        color = SketchColors.MutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Add button ("+")
                                SketchIconButton(
                                    onClick = {
                                        viewModel.addSongToPlaylist(playlistId, suggSong)
                                    },
                                    backgroundColor = SketchColors.Teal,
                                    size = 36.dp,
                                    dropOffset = 2.dp,
                                    shape = CircleShape
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add to playlist",
                                        tint = SketchColors.Ink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Play button
                                SketchIconButton(
                                    onClick = {
                                        if (isCurrentPlaying) {
                                            viewModel.playbackManager.togglePlayPause()
                                        } else {
                                            viewModel.playbackManager.playSongImmediate(suggSong)
                                        }
                                    },
                                    backgroundColor = if (isCurrentPlaying) SketchColors.Orange else Color.White,
                                    shape = CircleShape,
                                    size = 36.dp,
                                    dropOffset = 2.dp
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = SketchColors.Ink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Playlist Confirmation Dialog
    if (showDeletePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePlaylistDialog = false },
            title = {
                Text(
                    "Delete Playlist",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SketchColors.Ink
                )
            },
            text = {
                Text(
                    "Delete \"$playlistName\"? The songs inside will remain in your library.",
                    color = SketchColors.Ink,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 14.sp
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp)),
            confirmButton = {
                SketchButton(
                    text = "Delete",
                    onClick = {
                        viewModel.deletePlaylist(playlistId)
                        showDeletePlaylistDialog = false
                        onBack()
                    },
                    backgroundColor = SketchColors.HeartRed,
                    contentColor = Color.White
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            }
        )
    }

    // Remove Song Confirmation Dialog
    if (showRemoveSongDialog != null) {
        val songToDelete = showRemoveSongDialog!!
        AlertDialog(
            onDismissRequest = { showRemoveSongDialog = null },
            title = {
                Text(
                    "Remove Song",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SketchColors.Ink
                )
            },
            text = {
                Text(
                    "Remove \"${songToDelete.title}\" from $playlistName?",
                    color = SketchColors.Ink,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 14.sp
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp)),
            confirmButton = {
                SketchButton(
                    text = "Remove",
                    onClick = {
                        viewModel.removeSongFromPlaylist(playlistId, songToDelete.id)
                        showRemoveSongDialog = null
                    },
                    backgroundColor = SketchColors.HeartRed,
                    contentColor = Color.White
                )
            },
            dismissButton = {
                TextButton(onClick = { showRemoveSongDialog = null }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            }
        )
    }
}
