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

    var showDeleteConfirmDialog by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(playlistId) {
        viewModel.selectPlaylist(playlistId)
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar: Back Button (orange rounded square) + "My Playlist" + Share Button
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
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                SketchIconButton(
                    onClick = { /* Share playlist */ },
                    backgroundColor = Color.White,
                    size = 42.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = SketchColors.Ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                        onClick = { viewModel.playbackManager.playSongImmediate(songs.first()) },
                        backgroundColor = SketchColors.Teal,
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SketchColors.Ink, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val sampleSongs = remember {
                listOf(
                    Song("happier", "Happier", "Marshmello", 278, "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400"),
                    Song("sad_song", "Sad Song", "We The Kings", 204, "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400"),
                    Song("put_your_hands", "Put Your Hands", "Forest Blakk", 250, "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400"),
                    Song("hate_the_other_side", "Hate the Other Side", "Marshmello", 220, "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400")
                )
            }
            val displaySongs = if (songs.isNotEmpty()) songs else sampleSongs

            // Staggered Tilted Track Cards with Pins (matching design image Screen 3)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(displaySongs, key = { _, s -> s.id }) { index, song ->
                        val tilt = if (index % 2 == 0) -2.0f else 2.2f
                        val isCurrentPlaying = currentSong?.id == song.id
                        val slab = if (isCurrentPlaying) SketchColors.Orange else slabColorForIndex(index)

                        SketchCard(
                            modifier = Modifier.fillMaxWidth(),
                            slabColor = slab,
                            rotation = tilt,
                            showPin = true,
                            onClick = { viewModel.playbackManager.playSongImmediate(song) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail
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
                                        text = formatDuration(song.durationSeconds),
                                        fontSize = 12.sp,
                                        fontFamily = PoppinsFontFamily,
                                        color = SketchColors.MutedText
                                    )
                                }

                                if (currentlyLoadingSongId == song.id) {
                                    CircularProgressIndicator(
                                        color = SketchColors.Orange,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    // Circular play/pause button
                                    SketchIconButton(
                                        onClick = {
                                            if (isCurrentPlaying) {
                                                viewModel.playbackManager.togglePlayPause()
                                            } else {
                                                viewModel.playbackManager.playSongImmediate(song)
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

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { showDeleteConfirmDialog = song },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove",
                                        tint = SketchColors.MutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Controls Row per Screen 3 in reference image
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Repeat toggle */ }) {
                        Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = SketchColors.Ink)
                    }
                    IconButton(onClick = { viewModel.playbackManager.skipToPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = SketchColors.Ink, modifier = Modifier.size(28.dp))
                    }
                    // Orange Circular Play/Pause button with 3dp drop
                    SketchPlayButton(
                        isPlaying = isPlaying,
                        onClick = { viewModel.playbackManager.togglePlayPause() },
                        size = 56.dp,
                        iconSize = 28.dp,
                        backgroundColor = SketchColors.Orange
                    )
                    IconButton(onClick = { viewModel.playbackManager.skipToNext() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = SketchColors.Ink, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { /* Shuffle toggle */ }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = SketchColors.Ink)
                    }
                }
            }
        }

    // Remove Song Confirmation Dialog (Sketch Outline Style)
    if (showDeleteConfirmDialog != null) {
        val songToDelete = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
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
                        showDeleteConfirmDialog = null
                    },
                    backgroundColor = SketchColors.HeartRed,
                    contentColor = Color.White
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            }
        )
    }
}
