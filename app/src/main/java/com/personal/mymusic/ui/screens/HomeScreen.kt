package com.personal.mymusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.mymusic.domain.model.SearchResult
import com.personal.mymusic.domain.model.Song
import com.personal.mymusic.ui.MainViewModel
import com.personal.mymusic.ui.components.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (Long, String) -> Unit
) {
    val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val greetingState by viewModel.greetingState.collectAsState()

    val recentlyPlayedSongs by viewModel.recentlyPlayed.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val trendingFeed by viewModel.trendingFeed.collectAsState()

    val displaySongs = recentlyPlayedSongs
    val displayArtists = if (mostPlayedArtists.isNotEmpty()) mostPlayedArtists else listOf("Pritam", "Taylor Swift", "Harry Styles", "Anuv Jain")

    var selectedTab by remember { mutableIntStateOf(0) }
    val homeTabs = listOf("Overview", "Songs", "Album", "Artist")

    var showProfileMenu by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var selectedAiVibe by remember { mutableStateOf("Lo-Fi Chill") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // 1. Sketch Top Bar: Teal grid button + "Home" + Bell with red dot + Avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Teal rounded square button with grid icon (88 style)
            SketchIconButton(
                onClick = { showProfileMenu = true },
                backgroundColor = SketchColors.Teal,
                size = 42.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Menu",
                    tint = SketchColors.Ink,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Home",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = SketchColors.Ink
            )

            // Right: Notification Bell + Avatar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { showAppInfoDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = SketchColors.Ink,
                        modifier = Modifier.size(22.dp)
                    )
                    // Faint notification red dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .background(SketchColors.HeartRed, CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SketchColors.Orange, CircleShape)
                        .border(2.dp, SketchColors.Ink, CircleShape)
                        .clickable {
                            editNameInput = userName
                            showEditNameDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        color = SketchColors.Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PoppinsFontFamily
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showProfileMenu,
            onDismissRequest = { showProfileMenu = false },
            modifier = Modifier
                .background(Color.White)
                .border(2.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                text = { Text("App Info", color = SketchColors.Ink, fontFamily = PoppinsFontFamily) },
                onClick = {
                    showProfileMenu = false
                    showAppInfoDialog = true
                }
            )
            DropdownMenuItem(
                text = { Text("Settings & Privacy", color = SketchColors.Ink, fontFamily = PoppinsFontFamily) },
                onClick = {
                    showProfileMenu = false
                    viewModel.navigateTo(AppScreen.Settings)
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Greeting & Title per reference image:
        // "Listening Everyday"
        // "Explore millions of music according to your taste"
        Text(
            text = "Listening Everyday",
            color = SketchColors.Ink,
            fontSize = 28.sp,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Explore millions of music according to your taste",
            color = SketchColors.MutedText,
            fontSize = 14.sp,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        // 3. Search Bar
        SketchSearchField(
            value = "",
            onValueChange = {},
            placeholder = "Search Music",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = SketchColors.Ink,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(AppScreen.Search) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Tab Row: "Overview", "Songs", "Album", "Artist" with orange highlighter underline
        SketchTabRow(
            tabs = homeTabs,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Featured Cards Row (Tilted with pins, per reference image)
        // Two prominent tilted cards: Alan Olav Walker & Tim Bergling or trending songs
        val sampleCards = listOf(
            Triple("Alan Olav Walker", "20 Song", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"),
            Triple("Tim Bergling", "20 Song", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300"),
            Triple("Marshmello", "15 Song", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300")
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(sampleCards.size) { idx ->
                val card = sampleCards[idx]
                val tilt = if (idx % 2 == 0) -2.2f else 2.2f
                val slab = slabColorForIndex(idx)

                SketchCard(
                    modifier = Modifier.width(160.dp),
                    slabColor = slab,
                    rotation = tilt,
                    showPin = true,
                    onClick = { viewModel.searchAndPlaySong(card.first) }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = card.third,
                                contentDescription = card.first,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
                            )
                            // Heart icon at top right
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = SketchColors.HeartRed,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = card.second,
                                    fontSize = 11.sp,
                                    color = SketchColors.MutedText,
                                    fontFamily = PoppinsFontFamily
                                )
                                Text(
                                    text = card.first,
                                    fontSize = 13.sp,
                                    color = SketchColors.Ink,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Small circular play button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White, CircleShape)
                                    .border(1.5.dp, SketchColors.Ink, CircleShape)
                                    .clickable { viewModel.searchAndPlaySong(card.first) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
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

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Wide "Recent Track" Card (tilted with pin, per reference image)
        val recentTopSong = displaySongs.firstOrNull() ?: Song(
            id = "marshmello_hate_the_other_side",
            title = "Hate the Other Side",
            channel = "Marshmello",
            durationSeconds = 220,
            thumbnailUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300"
        )

        SketchCard(
            modifier = Modifier.fillMaxWidth(),
            slabColor = SketchColors.Teal,
            rotation = -1.8f,
            showPin = true,
            onClick = { viewModel.playbackManager.playSongImmediate(recentTopSong) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = recentTopSong.thumbnailUrl,
                    contentDescription = recentTopSong.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recentTopSong.channel,
                        fontSize = 12.sp,
                        color = SketchColors.MutedText,
                        fontFamily = PoppinsFontFamily
                    )
                    Text(
                        text = recentTopSong.title,
                        fontSize = 15.sp,
                        color = SketchColors.Ink,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatDuration(recentTopSong.durationSeconds),
                    fontSize = 13.sp,
                    color = SketchColors.Ink,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 7. Smart AI Recommendations Section (styled in sketch outline)
        SketchCard(
            modifier = Modifier.fillMaxWidth(),
            slabColor = SketchColors.Purple
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Recommendations",
                        tint = SketchColors.Purple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart AI Recommendations",
                        color = SketchColors.Ink,
                        fontSize = 17.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pick a vibe and let AI synthesize an adaptive stream for your moment.",
                    color = SketchColors.MutedText,
                    fontSize = 13.sp,
                    fontFamily = PoppinsFontFamily
                )
                Spacer(modifier = Modifier.height(14.dp))

                val aiVibes = listOf("Lo-Fi Chill", "Focus House", "Retro Synth", "Acoustic Pop", "Workout Hype", "Sleep Drift")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(aiVibes.size) { idx ->
                        val vibe = aiVibes[idx]
                        val isSelected = selectedAiVibe == vibe
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) SketchColors.Orange else Color.White)
                                .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(50))
                                .clickable {
                                    selectedAiVibe = vibe
                                    viewModel.searchAndPlaySong("$vibe Music")
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = vibe,
                                color = SketchColors.Ink,
                                fontSize = 12.sp,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 8. Recently Played Section
        if (displaySongs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Played",
                    color = SketchColors.Ink,
                    fontSize = 18.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See all",
                    color = SketchColors.Teal,
                    fontSize = 13.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.Library) }
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                items(displaySongs.size) { idx ->
                    val song = displaySongs[idx]
                    val tilt = tiltForIndex(idx)
                    RecentSongCard(
                        song = song,
                        isLoading = currentlyLoadingSongId == song.id,
                        tilt = tilt,
                        slabColor = slabColorForIndex(idx),
                        onClick = { viewModel.playbackManager.playSongImmediate(song) }
                    )
                }
            }
        }

        // 9. Recommended Stations
        Text(
            text = "Recommended Stations",
            color = SketchColors.Ink,
            fontSize = 18.sp,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            items(displayArtists.size) { idx ->
                val artist = displayArtists[idx]
                RecommendedStationCard(
                    artist = artist,
                    images = getImagesForArtist(artist, displaySongs),
                    slabColor = slabColorForIndex(idx),
                    onClick = { viewModel.searchAndPlaySong("$artist radio") }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Edit Name Dialog (Outlined Sketch style)
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    "Customize Your Profile",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SketchColors.Ink
                )
            },
            text = {
                Column {
                    Text(
                        "Set your name so MyMusic can personalize your greeting and experience.",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 13.sp,
                        color = SketchColors.MutedText
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SketchSearchField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        placeholder = "Enter your name",
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = SketchColors.Ink)
                        }
                    )
                }
            },
            confirmButton = {
                SketchButton(
                    text = "Save",
                    onClick = {
                        viewModel.updateUserName(editNameInput)
                        showEditNameDialog = false
                    },
                    backgroundColor = SketchColors.Orange
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
        )
    }

    // App Info Dialog (Outlined Sketch style)
    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = {
                Text(
                    "About MyMusic",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SketchColors.Ink
                )
            },
            text = {
                Column {
                    Text(
                        "MyMusic is an open, private music streaming app designed with high fidelity audio and an expressive sketch design system.",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 13.sp,
                        color = SketchColors.Ink
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Version 1.0.19 (Sketch Edition)\nAudio Engine: Media3 ExoPlayer\nSource: NewPipe Extractor",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 12.sp,
                        color = SketchColors.MutedText
                    )
                }
            },
            confirmButton = {
                SketchButton(
                    text = "Got it",
                    onClick = { showAppInfoDialog = false },
                    backgroundColor = SketchColors.Teal
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
        )
    }
}

@Composable
fun RecentSongCard(
    song: Song,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    tilt: Float = 0f,
    slabColor: Color = SketchColors.Teal
) {
    SketchCard(
        modifier = Modifier.width(140.dp),
        slabColor = slabColor,
        rotation = tilt,
        showPin = true,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                color = SketchColors.Ink,
                fontSize = 13.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.channel,
                    color = SketchColors.MutedText,
                    fontSize = 11.sp,
                    fontFamily = PoppinsFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        color = SketchColors.Orange,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendedStationCard(
    artist: String,
    images: List<String>,
    slabColor: Color = SketchColors.Orange,
    onClick: () -> Unit
) {
    SketchCard(
        modifier = Modifier.width(150.dp),
        slabColor = slabColor,
        showPin = false,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = images.firstOrNull() ?: "",
                contentDescription = artist,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$artist Radio",
                color = SketchColors.Ink,
                fontSize = 13.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Artist Station",
                color = SketchColors.MutedText,
                fontSize = 11.sp,
                fontFamily = PoppinsFontFamily
            )
        }
    }
}

val artistStationImages = mapOf(
    "Pritam" to listOf(
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300"
    ),
    "Taylor Swift" to listOf(
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
        "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300"
    ),
    "Harry Styles" to listOf(
        "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300",
        "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=300"
    ),
    "Anuv Jain" to listOf(
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300"
    )
)

fun getImagesForArtist(artist: String, history: List<Song>): List<String> {
    val artistSongs = history.filter { it.channel.contains(artist, ignoreCase = true) }
    val images = artistSongs.map { it.thumbnailUrl }.distinct().take(3).toMutableList()
    val fallbackList = artistStationImages[artist] ?: listOf(
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"
    )
    if (images.isEmpty()) {
        images.addAll(fallbackList)
    }
    return images
}
