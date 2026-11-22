package com.personal.mymusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.mymusic.data.database.PlaylistEntity
import com.personal.mymusic.ui.MainViewModel
import com.personal.mymusic.ui.components.*

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (Long, String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val likedPlaylistId by viewModel.likedPlaylistId.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var selectedFilter by remember { mutableStateOf("All") }
    var librarySearchQuery by remember { mutableStateOf("") }
    var pinnedPlaylistIds by remember { mutableStateOf(setOf<Long>()) }

    FaintCirclesBackground(showArcs = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Library",
                    color = SketchColors.Ink,
                    fontSize = 28.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                )

                SketchButton(
                    text = "+ New",
                    onClick = {
                        playlistNameInput = ""
                        showCreateDialog = true
                    },
                    backgroundColor = SketchColors.Orange
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search / Filter Field
            SketchSearchField(
                value = librarySearchQuery,
                onValueChange = { librarySearchQuery = it },
                placeholder = "Filter playlists...",
                leadingIcon = {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = SketchColors.Ink)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Tabs
            val filterTabs = listOf("All", "Playlists", "Liked Songs", "Recent", "Favorites")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
            ) {
                items(filterTabs.size) { idx ->
                    val label = filterTabs[idx]
                    val isSelected = selectedFilter == label
                    Box(
                        modifier = Modifier
                            .bounceClick { selectedFilter = label }
                            .background(if (isSelected) SketchColors.Orange else Color.White, RoundedCornerShape(50))
                            .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            color = SketchColors.Ink,
                            fontSize = 12.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Liked Songs Quick Card
            likedPlaylistId?.let { likedId ->
                SketchCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    slabColor = SketchColors.HeartRed,
                    rotation = -1.2f,
                    showPin = true,
                    onClick = { onPlaylistClick(likedId, "Liked Songs") }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(SketchColors.HeartRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Liked",
                                tint = SketchColors.HeartRed,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Liked Songs",
                                color = SketchColors.Ink,
                                fontSize = 16.sp,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Auto-synced favorites",
                                color = SketchColors.MutedText,
                                fontSize = 12.sp,
                                fontFamily = PoppinsFontFamily
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = SketchColors.Ink
                        )
                    }
                }
            }

            // User Playlists Section with Staggered Tilted Track Cards with Pins
            val filteredPlaylists = playlists.filter {
                it.name.contains(librarySearchQuery, ignoreCase = true)
            }

            Text(
                text = "Playlists (${filteredPlaylists.size})",
                color = SketchColors.Ink,
                fontSize = 18.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (filteredPlaylists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No playlists found.\nCreate one using the '+ New' button above!",
                        color = SketchColors.MutedText,
                        fontFamily = PoppinsFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    filteredPlaylists.forEachIndexed { index, playlist ->
                        val tilt = tiltForIndex(index)
                        val slab = slabColorForIndex(index)
                        val isPinned = pinnedPlaylistIds.contains(playlist.id)

                        SketchCard(
                            modifier = Modifier.fillMaxWidth(),
                            slabColor = slab,
                            rotation = tilt,
                            showPin = true,
                            onClick = { onPlaylistClick(playlist.id, playlist.name) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(slab, RoundedCornerShape(12.dp))
                                        .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = SketchColors.Ink,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        color = SketchColors.Ink,
                                        fontSize = 15.sp,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isPinned) "Pinned Playlist" else "User Collection",
                                        color = SketchColors.MutedText,
                                        fontSize = 12.sp,
                                        fontFamily = PoppinsFontFamily
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        pinnedPlaylistIds = if (isPinned) pinnedPlaylistIds - playlist.id else pinnedPlaylistIds + playlist.id
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.OutlinedFlag,
                                        contentDescription = "Pin",
                                        tint = if (isPinned) SketchColors.Orange else SketchColors.MutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        renameInput = playlist.name
                                        playlistToRename = playlist
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More",
                                        tint = SketchColors.Ink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Create Playlist Dialog (Sketch Outline Style)
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    "Create New Playlist",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SketchColors.Ink
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp)),
            text = {
                Column {
                    Text(
                        "Give your playlist a memorable name.",
                        color = SketchColors.MutedText,
                        fontSize = 13.sp,
                        fontFamily = PoppinsFontFamily
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SketchSearchField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        placeholder = "e.g. Road Trip Vibes",
                        leadingIcon = {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = SketchColors.Ink)
                        }
                    )
                }
            },
            confirmButton = {
                SketchButton(
                    text = "Create",
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            viewModel.createPlaylist(playlistNameInput)
                            showCreateDialog = false
                        }
                    },
                    backgroundColor = SketchColors.Orange
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            }
        )
    }

    // Rename Playlist Dialog (Sketch Outline Style)
    if (playlistToRename != null) {
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = {
                Text(
                    "Rename Playlist",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SketchColors.Ink
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp)),
            text = {
                Column {
                    SketchSearchField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        placeholder = "New playlist name"
                    )
                }
            },
            confirmButton = {
                SketchButton(
                    text = "Save",
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renamePlaylist(playlistToRename!!.id, renameInput)
                            playlistToRename = null
                        }
                    },
                    backgroundColor = SketchColors.Teal
                )
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            }
        )
    }
}
