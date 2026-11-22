package com.personal.mymusic.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (Long, String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val likedPlaylistId by viewModel.likedPlaylistId.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }

    var playlistMenuTarget by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }

    var selectedFilter by remember { mutableStateOf("All") }
    var librarySearchQuery by remember { mutableStateOf("") }
    var pinnedPlaylistIds by remember { mutableStateOf(setOf<Long>()) }

    FaintCirclesBackground(showArcs = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Bar: 24dp left aligned
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

            Spacer(modifier = Modifier.height(20.dp))

            // Search / Filter Field (height 52dp)
            SketchSearchField(
                value = librarySearchQuery,
                onValueChange = { librarySearchQuery = it },
                placeholder = "Filter playlists...",
                leadingIcon = {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = SketchColors.Ink)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category Filter Tabs
            val filterTabs = listOf("All", "Playlists", "Liked Songs", "Recent", "Favorites")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterTabs.size) { idx ->
                    val label = filterTabs[idx]
                    val isSelected = selectedFilter == label
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) SketchColors.Orange else Color.White)
                            .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(50))
                            .clickable { selectedFilter = label }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
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

            Spacer(modifier = Modifier.height(24.dp))

            // Liked Songs Quick Card
            likedPlaylistId?.let { likedId ->
                SketchCard(
                    modifier = Modifier.fillMaxWidth(),
                    slabColor = SketchColors.HeartRed,
                    rotation = -1.5f,
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

            Spacer(modifier = Modifier.height(28.dp))

            // User Playlists Section
            val userPlaylists = playlists.filter {
                it.id != likedPlaylistId && !it.name.equals("Liked Songs", ignoreCase = true)
            }.filter {
                it.name.contains(librarySearchQuery, ignoreCase = true)
            }

            Text(
                text = "Playlists (${userPlaylists.size})",
                color = SketchColors.Ink,
                fontSize = 18.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (userPlaylists.isEmpty()) {
                SketchCard(
                    modifier = Modifier.fillMaxWidth(),
                    slabColor = SketchColors.Teal,
                    showPin = false
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No custom playlists found.\nTap '+ New' to create your first playlist!",
                            color = SketchColors.MutedText,
                            fontFamily = PoppinsFontFamily,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    userPlaylists.forEachIndexed { index, playlist ->
                        val tilt = if (index % 2 == 0) -1.8f else 1.8f
                        val slab = slabColorForIndex(index)
                        val isPinned = pinnedPlaylistIds.contains(playlist.id)

                        SketchCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onPlaylistClick(playlist.id, playlist.name) },
                                    onLongClick = { playlistMenuTarget = playlist }
                                ),
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
                                    onClick = { playlistMenuTarget = playlist },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = SketchColors.Ink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(130.dp))
        }
    }

    // Playlist Options Menu (Rename & Delete)
    if (playlistMenuTarget != null) {
        val target = playlistMenuTarget!!
        AlertDialog(
            onDismissRequest = { playlistMenuTarget = null },
            title = {
                Text(
                    text = target.name,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SketchColors.Ink
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                renameInput = target.name
                                playlistToRename = target
                                playlistMenuTarget = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = SketchColors.Ink)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Rename Playlist", fontFamily = PoppinsFontFamily, color = SketchColors.Ink, fontWeight = FontWeight.SemiBold)
                    }

                    HorizontalDivider(color = SketchColors.FaintCircle)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playlistToDelete = target
                                playlistMenuTarget = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = SketchColors.HeartRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Delete Playlist", fontFamily = PoppinsFontFamily, color = SketchColors.HeartRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { playlistMenuTarget = null }) {
                    Text("Close", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
        )
    }

    // Delete Playlist Confirmation Dialog
    if (playlistToDelete != null) {
        val target = playlistToDelete!!
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
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
                    "Delete \"${target.name}\"? The songs inside will remain in your library.",
                    color = SketchColors.Ink,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                SketchButton(
                    text = "Delete",
                    onClick = {
                        viewModel.deletePlaylist(target.id)
                        playlistToDelete = null
                    },
                    backgroundColor = SketchColors.HeartRed,
                    contentColor = Color.White
                )
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
        )
    }

    // Rename Dialog
    if (playlistToRename != null) {
        val target = playlistToRename!!
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
            text = {
                Column {
                    SketchSearchField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        placeholder = "Playlist name",
                        leadingIcon = null
                    )
                }
            },
            confirmButton = {
                SketchButton(
                    text = "Rename",
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renamePlaylist(target.id, renameInput)
                            playlistToRename = null
                        }
                    },
                    backgroundColor = SketchColors.Orange
                )
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
        )
    }

    // Create New Playlist Dialog
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
            text = {
                Column {
                    Text(
                        "Enter a name for your custom playlist:",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 13.sp,
                        color = SketchColors.MutedText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SketchSearchField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        placeholder = "e.g. Chill Beats",
                        leadingIcon = {
                            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = SketchColors.Ink)
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
                    backgroundColor = SketchColors.Teal
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
        )
    }
}
