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
fun CategoryDetailsScreen(
    categoryName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPlaylistClick: (Long, String) -> Unit
) {
    val categoryState by viewModel.currentCategoryState.collectAsState()
    val isPlaying by viewModel.playbackManager.isPlaying.collectAsState()
    val currentSong by viewModel.playbackManager.currentSong.collectAsState()
    val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Latest", "Trending")

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(categoryName) {
        viewModel.loadCategory(categoryName, forceRefresh = false)
    }

    val songs = if (selectedTab == 0) categoryState.latest else categoryState.trending

    FaintCirclesBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Top Bar: Back button + Category Name + Refresh Button
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
                    text = categoryName,
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

                SketchIconButton(
                    onClick = { viewModel.loadCategory(categoryName, forceRefresh = true) },
                    backgroundColor = Color.White,
                    size = 42.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = SketchColors.Ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Tabs: "Latest" and "Trending"
            SketchTabRow(
                tabs = tabs,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Content list / Loading / Error
            if (categoryState.isLoading && songs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SketchColors.Orange)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Loading $categoryName...",
                            color = SketchColors.MutedText,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (categoryState.error != null && songs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Unable to load songs",
                            color = SketchColors.Ink,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = categoryState.error ?: "Please check your network connection.",
                            color = SketchColors.MutedText,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SketchButton(
                            text = "Retry",
                            onClick = { viewModel.loadCategory(categoryName, forceRefresh = true) },
                            backgroundColor = SketchColors.Teal
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                        val isCurrentPlaying = currentSong?.id == song.id
                        val slab = if (isCurrentPlaying) SketchColors.Orange else slabColorForIndex(index)

                        SketchCard(
                            modifier = Modifier.fillMaxWidth(),
                            slabColor = slab,
                            showPin = false,
                            onClick = {
                                viewModel.playbackManager.playPlaylist(songs, startIndex = index)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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

                                Spacer(modifier = Modifier.width(8.dp))

                                // Add to playlist button ("+")
                                IconButton(
                                    onClick = { songToAddToPlaylist = song },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add to playlist",
                                        tint = SketchColors.Ink,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

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
                            }
                        }
                    }
                }
            }
        }
    }

    // Add to Playlist Dialog
    if (songToAddToPlaylist != null) {
        val targetSong = songToAddToPlaylist!!
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            title = {
                Text(
                    text = "Add to Playlist",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SketchColors.Ink
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose playlist for \"${targetSong.title}\":",
                        color = SketchColors.MutedText,
                        fontFamily = PoppinsFontFamily,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    playlists.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addSongToPlaylist(p.id, targetSong)
                                    songToAddToPlaylist = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = SketchColors.Ink)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = p.name,
                                color = SketchColors.Ink,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider(color = SketchColors.FaintCircle)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { songToAddToPlaylist = null }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
        )
    }
}
