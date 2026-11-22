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
import androidx.compose.ui.focus.onFocusChanged
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
    onPlaylistClick: (Long, String) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val recentlyPlayedSongs by viewModel.recentlyPlayed.collectAsState()
    val trendingFeed by viewModel.trendingFeed.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()

    var homeSearchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val homeTabs = listOf("Overview", "Songs", "Album", "Artist")

    var showProfileMenu by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }

    val categories = listOf(
        "Top Hits",
        "Hindi / Bollywood",
        "Punjabi",
        "English Pop",
        "Tamil",
        "Telugu",
        "Hip-Hop",
        "Romantic",
        "Party",
        "Devotional"
    )

    // Category imagery map
    val categoryImages = mapOf(
        "Top Hits" to "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
        "Hindi / Bollywood" to "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
        "Punjabi" to "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
        "English Pop" to "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300",
        "Tamil" to "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300",
        "Telugu" to "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=300",
        "Hip-Hop" to "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=300",
        "Romantic" to "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=300",
        "Party" to "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=300",
        "Devotional" to "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=300"
    )

    FaintCirclesBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // Header: 16dp below status bar
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Top Bar: Teal grid button + "Home" + Bell + Profile Avatar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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

                    val initial = (if (userName.isNotBlank()) userName else "Vibe").take(1).uppercase()
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
                            text = initial,
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
                    text = { Text("Settings", color = SketchColors.Ink, fontFamily = PoppinsFontFamily) },
                    onClick = {
                        showProfileMenu = false
                        viewModel.navigateTo(AppScreen.Settings)
                    }
                )
            }

            // 2. Greeting Title: "Hi, <name>" replacing previous greeting
            Spacer(modifier = Modifier.height(16.dp))

            val displayName = if (userName.isNotBlank()) userName else "Music Lover"
            Text(
                text = "Hi, $displayName",
                color = SketchColors.Ink,
                fontSize = 28.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Explore millions of songs according to your taste",
                color = SketchColors.MutedText,
                fontSize = 14.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )

            // 20dp gap to search field
            Spacer(modifier = Modifier.height(20.dp))

            // 3. Search field (height 52dp)
            SketchSearchField(
                value = homeSearchQuery,
                onValueChange = {
                    homeSearchQuery = it
                    viewModel.setSearchQuery(it)
                },
                placeholder = "Search Music",
                onSearch = {
                    if (homeSearchQuery.isNotBlank()) {
                        viewModel.setSearchQuery(homeSearchQuery)
                        viewModel.performSearch()
                        viewModel.navigateTo(AppScreen.Search)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .onFocusChanged { isSearchFocused = it.isFocused },
                trailingIcon = {
                    if (homeSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                homeSearchQuery = ""
                                viewModel.setSearchQuery("")
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = SketchColors.Ink)
                        }
                    }
                }
            )

            // Live Suggestions Pills when typing
            if (homeSearchQuery.trim().length >= 2 && suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions.size) { idx ->
                        val suggestion = suggestions[idx]
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SketchColors.Orange.copy(alpha = 0.2f))
                                .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(50))
                                .clickable {
                                    homeSearchQuery = suggestion
                                    viewModel.setSearchQuery(suggestion)
                                    viewModel.performSearch()
                                    viewModel.navigateTo(AppScreen.Search)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = suggestion,
                                color = SketchColors.Ink,
                                fontSize = 12.sp,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 20dp gap to tab row
            Spacer(modifier = Modifier.height(20.dp))

            // 4. Tab Row: "Overview", "Songs", "Album", "Artist"
            SketchTabRow(
                tabs = homeTabs,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.fillMaxWidth()
            )

            // 24dp gap between tab row and first section
            Spacer(modifier = Modifier.height(24.dp))

            // 5. Featured Cards Row (tilted with pins)
            val sampleCards = listOf(
                Triple("Alan Olav Walker", "20 Songs", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"),
                Triple("Tim Bergling", "20 Songs", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300"),
                Triple("Marshmello", "15 Songs", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sampleCards.size) { idx ->
                    val card = sampleCards[idx]
                    val tilt = if (idx % 2 == 0) -2.0f else 2.0f
                    val slab = slabColorForIndex(idx)

                    SketchCard(
                        modifier = Modifier
                            .width(160.dp)
                            .padding(4.dp),
                        slabColor = slab,
                        rotation = tilt,
                        showPin = true,
                        onClick = { viewModel.searchAndPlaySong(card.first) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
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

            // 28dp gap between sections
            Spacer(modifier = Modifier.height(28.dp))

            // 6. Wide "Recent Track" Card
            val recentTopSong = recentlyPlayedSongs.firstOrNull() ?: Song(
                id = "marshmello_hate_the_other_side",
                title = "Hate the Other Side",
                channel = "Marshmello",
                durationSeconds = 220,
                thumbnailUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300"
            )

            SketchCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                slabColor = SketchColors.Teal,
                rotation = -1.5f,
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

            // 28dp between sections
            Spacer(modifier = Modifier.height(28.dp))

            // 7. Categories Sections
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Explore Categories",
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
                    modifier = Modifier.clickable { onCategoryClick("Top Hits") }
                )
            }

            // 14dp gap to category cards
            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories.size) { idx ->
                    val catName = categories[idx]
                    val slab = slabColorForIndex(idx)
                    val imgUrl = categoryImages[catName] ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"

                    SketchCard(
                        modifier = Modifier
                            .width(150.dp)
                            .padding(2.dp),
                        slabColor = slab,
                        showPin = false,
                        onClick = { onCategoryClick(catName) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = catName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = catName,
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
                                    text = "25 Songs",
                                    color = SketchColors.MutedText,
                                    fontSize = 11.sp,
                                    fontFamily = PoppinsFontFamily
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = SketchColors.Ink,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 28dp between sections
            Spacer(modifier = Modifier.height(28.dp))

            // 8. Individual Category Highlights
            val highlightCategories = listOf("Top Hits", "Hindi / Bollywood", "Punjabi", "English Pop", "Telugu")
            highlightCategories.forEachIndexed { catIdx, categoryName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoryName,
                        color = SketchColors.Ink,
                        fontSize = 18.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onCategoryClick(categoryName) }
                    )
                    Text(
                        text = "See all",
                        color = SketchColors.Teal,
                        fontSize = 13.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onCategoryClick(categoryName) }
                    )
                }

                // 14dp gap to content
                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(4) { idx ->
                        val slab = slabColorForIndex(idx + catIdx)
                        val sampleTitle = when (idx) {
                            0 -> "$categoryName Anthem"
                            1 -> "Top Hit 2026"
                            2 -> "Trending Track"
                            else -> "Party Mix"
                        }
                        val sampleImg = categoryImages[categoryName] ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"

                        SketchCard(
                            modifier = Modifier
                                .width(140.dp)
                                .padding(2.dp),
                            slabColor = slab,
                            showPin = false,
                            onClick = { onCategoryClick(categoryName) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                AsyncImage(
                                    model = sampleImg,
                                    contentDescription = sampleTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = sampleTitle,
                                    color = SketchColors.Ink,
                                    fontSize = 13.sp,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = categoryName,
                                    color = SketchColors.MutedText,
                                    fontSize = 11.sp,
                                    fontFamily = PoppinsFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 28dp between sections
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Bottom breathing room: mini player height + bottom nav height + 24dp
            Spacer(modifier = Modifier.height(130.dp))
        }
    }

    // Edit Name Dialog (Outlined Sketch style)
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    "Customize Your Name",
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
                        viewModel.completeOnboarding(editNameInput)
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
                        "Version 1.0.21 (Wave Edition)\nDeveloped by Mahesh\nAudio Engine: Media3 ExoPlayer\nSource: NewPipe Extractor",
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
