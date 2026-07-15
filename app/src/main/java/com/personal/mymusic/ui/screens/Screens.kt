package com.personal.mymusic.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
    object Home : AppScreen()
    object Search : AppScreen()
    object Library : AppScreen()
    data class PlaylistDetails(val playlistId: Long, val playlistName: String) : AppScreen()
    object Settings : AppScreen()
}

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (Long, String) -> Unit
) {
    val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
    
    // Determine greeting based on time of day
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top App Bar Greeting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = greeting,
                color = Color.White,
                fontSize = 28.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.ExtraBold
            )
            // Mock Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .border(1.dp, BorderGlass, CircleShape)
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuD-cbXxU4poeME7wmmkzAqWlriPh_98QE_qvxxIfEr8yS4UbGn4fYwjktEdkKWZONDRWXf7NNhld9KXXySw5nHhrdMoL2aNkC9zhUgmLqNplY7PMU2moeMlQqnzBAhHgOspoq1wuIbt7NdxLUz5qzYF_0PDuVGLkMLp5n4nk1NDGDmSZKWUEbDbjy2r0CnNDTJbyyDMkebhZsLaRnFRcqYHy2-rEp8mXTJfLdnDVCFhJ9PChiISSOj69bkOabqrlTfz2SWJLNnQo4jN",
                    contentDescription = "User avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 1: Recently Played
        Text(
            text = "Recently Played",
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        val recentlyPlayed = listOf(
            Pair("Kesariya", "https://lh3.googleusercontent.com/aida-public/AB6AXuBlEp1UWbRilVDh6g6LTeCQ1zZHIhRpx3abgSjoYSV7xHanKxqebFmxwskdt-3pqMowx_rBRaa6yw5lue7Ow5e_I2Ib1ezCRj4FNsAjLShuZg4CFyGE-CDjOz46B0QvmJhNCTTXo5Znd2Werre6eU866baSEONDXVyIZt8NwVWGcF6Pbs6NqDABnZGMd7tvIrluSMol_TCCwQO5akFl-SCrKoBIkYH7QAY5sk3vb06vdIW9SgnnU9n0qtuCnZWnB5Cu7aHsuyZQSNSC"),
            Pair("Anti-Hero", "https://lh3.googleusercontent.com/aida-public/AB6AXuCllqaQMbqRGNSL88VBfuKoBJFoiChW15yq4a4kRj4kxWwYoBUe0252y17CpkjHndOAjCiVA4BSB3LHnIO9UCdCvKX8e2Wrn2pIMnW7RwY4dhLOr-PUDoNYUKb0vlfIHo9TNqCjv19NDEhRoSx48QgUOv0E_bqnCzQZARwTLvkf0IhcWypqZo-94aD67Inl1onLSXl56e08OByeL_5wz99CQMGSmhcDxv6ZuxBjMZ--ZOL1dgkwpewRvVw3r4uuh5S8kg3cvcTkA0KN"),
            Pair("As It Was", "https://lh3.googleusercontent.com/aida-public/AB6AXuCKvvYSa6ICPx3m8pKh4t9frfj4AUuq0RD61fi6ZCR-y6uLmt6TIzJqQLkGnUnJXR83zUzuytV11JS-DP2GN_2RyF2Dadt6WwI8boJH95nhaoLJAxBWOV6bnQ7Z0uAOkZ-nXcEJZRtuAA1SVbx-a4cuS34F-HWB7iw38VS6AwyoUhnDNRIN4nd5I5d_UryPF5XuzhndRs66UxEAI1359jaemWOdpqj1xZJvdnMh4D0luZSyj2wwG2-uQ4q_p5nCTEfv2Ny582Gd9C5o"),
            Pair("Baarishein", "https://lh3.googleusercontent.com/aida-public/AB6AXuAj5RaVmIT1sh9v-PXyIZLmgBSS2pZ6GQ_1FlGsD8TpPVW16Sdd56k10v9NBVcsXiUE8OZijJC4EgyNK8VJvVTjYXoUxYek7Ix2I7PYrwAXYis5Q7i48WDEXynRBA69s83tQiao0XFpAQIcviv5Kbt0luMmvHSEG95GvRX3AIl-GD6fshD4E1Xh_SVvvjn29QnBO8xGBRGQjzSgz3-SXj7CF9pY41i5xraJImiiuRONkCUrICJkl0_J2RnvVYWTrk-dfSH3EAdqp0A-")
        )
        
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(recentlyPlayed.size) { index ->
                val song = recentlyPlayed[index]
                Column(
                    modifier = Modifier
                        .width(130.dp)
                        .clickable { viewModel.searchAndPlaySong(song.first) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(modifier = Modifier.size(130.dp)) {
                        AsyncImage(
                            model = song.second,
                            contentDescription = song.first,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                        )
                        if (currentlyLoadingSongId == song.first) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AccentColor, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = song.first,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section 2: Made For You
        Text(
            text = "Made For You",
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val madeForYou = listOf(
            Triple("Daily Mix 1", "Daily high-energy tracks", Color(0xFF818CF8)),
            Triple("Discover Weekly", "Fresh new discoveries", Color(0xFFA855F7)),
            Triple("Release Radar", "New releases customized for you", Color(0xFFF472B6)),
            Triple("Time Capsule", "Nostalgic classics based on history", Color(0xFF22D3EE))
        )

        // 2x2 Grid of Playlist Cards
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    for (j in 0 until 2) {
                        val mix = madeForYou[i * 2 + j]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(mix.third, mix.third.copy(alpha = 0.4f))
                                    )
                                )
                                .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                                .clickable { viewModel.searchAndPlaySong(mix.first) }
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = mix.first,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mix.second,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section 3: Trending Now
        Text(
            text = "Trending Now",
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val trendingNow = listOf(
            Pair("Diljit Dosanjh", "https://lh3.googleusercontent.com/aida-public/AB6AXuBZVsX_Ct9nZfIQOXTtKdKXRCQM2zd4yfn_p1kV2M2bwtF-bKsQdLDqxefh_Yt2x8aXdgpRK_fvX0yLZdJrCgNv0uSi5fzbz1oFqXGQH2HN9LuInoNS8BO4YaoVVgYrgPVVrpZv32ZwXP1AB4Qg9pdBYTIVZ3oFdBJ401jTk36wtuDv2yvDdK3jLooq_958Z78nv-dxRj_cYFRT3sAqhpx6pH1tSPBVORA_Zl9bgdGHievljDM1wgA8Woje7Lprw2P3bYe7LoK6bOxw"),
            Pair("Traditional Vibes", "https://lh3.googleusercontent.com/aida-public/AB6AXuAj5RaVmIT1sh9v-PXyIZLmgBSS2pZ6GQ_1FlGsD8TpPVW16Sdd56k10v9NBVcsXiUE8OZijJC4EgyNK8VJvVTjYXoUxYek7Ix2I7PYrwAXYis5Q7i48WDEXynRBA69s83tQiao0XFpAQIcviv5Kbt0luMmvHSEG95GvRX3AIl-GD6fshD4E1Xh_SVvvjn29QnBO8xGBRGQjzSgz3-SXj7CF9pY41i5xraJImiiuRONkCUrICJkl0_J2RnvVYWTrk-dfSH3EAdqp0A-")
        )

        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(trendingNow.size) { index ->
                val trend = trendingNow[index]
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                        .clickable { viewModel.searchAndPlaySong(trend.first) }
                ) {
                    AsyncImage(
                        model = trend.second,
                        contentDescription = trend.first,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )
                    // Text in bottom left
                    Text(
                        text = trend.first,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                    if (currentlyLoadingSongId == trend.first) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentColor, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun GenreTile(
    name: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = gradientColors))
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        // Angled visual embellishment
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.TopEnd)
                .offset(x = 12.dp, y = (-12).dp)
                .graphicsLayer(rotationZ = 25f)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun TopResultCard(
    song: Song,
    onPlayClick: () -> Unit,
    isLoading: Boolean = false
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(100.dp)) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                )
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentColor, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.channel,
                    color = Color(0xFFA0A0A0),
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .width(120.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(BrandGradient)
                ) {
                    Text("Play Now", color = Color.White, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow() {
    val chips = listOf("Songs", "Artists", "Playlists", "Albums")
    var selectedChip by remember { mutableStateOf("Songs") }
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        items(chips.size) { index ->
            val chipName = chips[index]
            val isSelected = chipName == selectedChip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(
                        if (isSelected) BrandGradient else Brush.linearGradient(listOf(SurfaceGlass, SurfaceGlass))
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else BorderGlass,
                        RoundedCornerShape(9999.dp)
                    )
                    .clickable { selectedChip = chipName }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = chipName,
                    color = if (isSelected) Color.White else Color(0xFFA0A0A0),
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
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
            text = "Search", // Renamed from Explore Music to Search matching specs
            color = Color.White,
            fontSize = 28.sp,
            fontFamily = InterFontFamily,
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
                shape = RoundedCornerShape(16.dp), // rounded-lg
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                    .onFocusChanged { isSearchFieldFocused = it.isFocused },
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.performSearch()
                        focusManager.clearFocus()
                    }
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = {
                    viewModel.performSearch()
                    focusManager.clearFocus()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                shape = RoundedCornerShape(16.dp), // rounded-lg
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black) // Accent text/icon color is black
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
            
            Column(modifier = Modifier.fillMaxSize()) {
                FilterChipsRow()
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Extract first result for Top Result Card
                    if (results.isNotEmpty()) {
                        val firstItem = results.first()
                        val topSong = Song(
                            id = firstItem.id,
                            title = firstItem.title,
                            channel = firstItem.channel,
                            durationSeconds = firstItem.durationSeconds,
                            thumbnailUrl = firstItem.thumbnailUrl
                        )
                        item {
                            Text(
                                text = "Top Result",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            TopResultCard(
                                song = topSong,
                                onPlayClick = {
                                    focusManager.clearFocus()
                                    viewModel.playbackManager.playSongImmediate(topSong)
                                },
                                isLoading = currentlyLoadingSongId == topSong.id
                            )
                            
                            Text(
                                text = "Songs",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    // Render remaining items (skip the first one since it's in TopResultCard)
                    val remainingResults = if (results.size > 1) results.drop(1) else emptyList()
                    itemsIndexed(remainingResults) { _, item ->
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
                        fontFamily = InterFontFamily,
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
                                    Text(text = suggestion, color = Color.White, fontSize = 16.sp, fontFamily = InterFontFamily)
                                }
                                IconButton(onClick = { viewModel.deleteSearchQuery(suggestion) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                            HorizontalDivider(color = BorderGlass)
                        }
                    }
                }
            } else {
                // Show Browse All genres and Trending Feed below it
                val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Browse All",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    
                    item {
                        val genres = listOf(
                            Pair("Bollywood", listOf(Color(0xFFB91C1C), Color(0xFF450A0A))),
                            Pair("Punjabi", listOf(Color(0xFF581C87), Color(0xFF2E1065))),
                            Pair("Pop", listOf(Color(0xFF0EA5E9), Color(0xFF0C4A6E))),
                            Pair("Folk", listOf(Color(0xFF15803D), Color(0xFF052E16))),
                            Pair("Chill", listOf(Color(0xFFD97706), Color(0xFF451A03))),
                            Pair("Workout", listOf(Color(0xFFEA580C), Color(0xFF431407)))
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (i in 0 until 3) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    for (j in 0 until 2) {
                                        val genre = genres[i * 2 + j]
                                        Box(modifier = Modifier.weight(1f)) {
                                            GenreTile(
                                                name = genre.first,
                                                gradientColors = genre.second,
                                                onClick = {
                                                    viewModel.setSearchQuery(genre.first)
                                                    viewModel.performSearch()
                                                    focusManager.clearFocus()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (isTrendingLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AccentColor)
                            }
                        }
                    } else {
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

    val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
    
    // Calculate total duration
    val totalDurationSeconds = remember(songs) {
        songs.sumOf { it.durationSeconds }
    }
    val durationText = remember(totalDurationSeconds) {
        val hrs = totalDurationSeconds / 3600
        val mins = (totalDurationSeconds % 3600) / 60
        if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Transparent Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = playlistName,
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This playlist is empty.\nGo to the Search tab and search for songs to add!",
                    color = Color(0xFFA0A0A0),
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header item containing artwork and details
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large Artwork Card with ambient glow
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .shadow(elevation = 30.dp, shape = RoundedCornerShape(24.dp), clip = false)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                                .background(BrandGradient)
                        ) {
                            val coverUrl = songs.firstOrNull()?.thumbnailUrl
                            if (coverUrl != null) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .size(80.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Title & Info
                        Text(
                            text = playlistName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${songs.size} songs • $durationText",
                            color = Color(0xFFA0A0A0),
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Controls Row with Floating Solar Play Button (center-aligned)
                        Row(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle Button
                            IconButton(
                                onClick = {
                                    val shuffledList = songs.shuffled()
                                    viewModel.playbackManager.playPlaylist(shuffledList, 0)
                                    viewModel.playbackManager.setShuffleEnabled(true)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SurfaceGlass, CircleShape)
                                    .border(1.dp, BorderGlass, CircleShape)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = Color.White)
                            }

                            // Large Solar Play Button (Brand Gradient)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(BrandGradient)
                                    .shadow(elevation = 16.dp, shape = CircleShape)
                                    .clickable {
                                        viewModel.playbackManager.playPlaylist(songs, 0)
                                        viewModel.playbackManager.setShuffleEnabled(false)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "PlayAll",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Options Button
                            IconButton(
                                onClick = { /* Options */ },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SurfaceGlass, CircleShape)
                                    .border(1.dp, BorderGlass, CircleShape)
                            ) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "Options", tint = Color.White)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Songs list
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
                                        tint = if (index > 0) Color.White else Color.DarkGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = { viewModel.moveSongInPlaylist(playlistId, index, index + 1) },
                                    enabled = index < songs.size - 1
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "Move Down",
                                        tint = if (index < songs.size - 1) Color.White else Color.DarkGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = { viewModel.removeSongFromPlaylist(playlistId, song.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircle,
                                        contentDescription = "Remove",
                                        tint = Color.Red.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
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
            fontFamily = InterFontFamily,
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
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Configures maximum disk storage for cached audio streams. Old streams will be automatically evicted (LRU).",
                    color = Color(0xFFA0A0A0),
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Limit: ${String.format("%.1f GB", cacheSizeGb)}", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                }

                Slider(
                    value = cacheSizeGb,
                    onValueChange = { viewModel.setCacheSizeLimit(it) },
                    valueRange = 0.5f..5.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentColor,
                        activeTrackColor = AccentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.clearCache() },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFF5252).copy(alpha = 0.15f),
                        contentColor = Color(0xFFFF5252)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFFF5252))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Stream Cache", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
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
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Controls the duration of smooth volume transitions between songs.",
                    color = Color(0xFFA0A0A0),
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val displayLabel = if (crossfadeSeconds <= 0f) "Off" else String.format("%.0fs", crossfadeSeconds)
                Text(text = "Crossfade: $displayLabel", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)

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
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
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
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Automatically append similar songs to the queue when you reach the end.",
                            color = Color(0xFFA0A0A0),
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = autoplayEnabledSetting,
                        onCheckedChange = { viewModel.playbackManager.setAutoplayEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AccentColor,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
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
                    fontFamily = InterFontFamily,
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
                            fontFamily = InterFontFamily,
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
                            color = if (updateState is UpdateState.Error) Color.Red.copy(alpha = 0.8f) else Color(0xFFA0A0A0),
                            fontFamily = InterFontFamily,
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
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Check", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
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
                thumbColor = AccentColor,
                activeTrackColor = AccentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
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
            .background(BrandGradient)
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
            tint = Color.White,
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
                                    shape = RoundedCornerShape(24.dp), // 24dp for rounded-xl shape
                                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .aspectRatio(1f)
                                        .graphicsLayer(
                                            scaleX = artScale,
                                            scaleY = artScale,
                                            alpha = artOpacity
                                        )
                                        .shadow(elevation = 30.dp, shape = RoundedCornerShape(24.dp))
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
