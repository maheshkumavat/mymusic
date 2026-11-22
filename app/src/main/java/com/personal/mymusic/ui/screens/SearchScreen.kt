package com.personal.mymusic.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.personal.mymusic.data.database.PlaylistEntity
import com.personal.mymusic.domain.model.Song
import com.personal.mymusic.ui.MainViewModel
import com.personal.mymusic.ui.components.*

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (Long, String) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val error by viewModel.searchError.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var isSearchFieldFocused by remember { mutableStateOf(false) }

    val recentSearches by viewModel.recentSearches.collectAsState()
    var showAddToPlaylistDialog by remember { mutableStateOf<Song?>(null) }

    // Camera Cover Art Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSearchQuery("Album Cover Search")
            viewModel.performSearch()
        }
    }

    // Voice Search
    var isListening by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = sr
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true; voiceError = null }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(err: Int) {
                    isListening = false
                    voiceError = "Voice error ($err). Try again."
                }
                override fun onResults(bundle: Bundle?) {
                    isListening = false
                    val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        viewModel.setSearchQuery(spokenText)
                        viewModel.performSearch()
                        focusManager.clearFocus()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            sr.startListening(intent)
        } else {
            voiceError = "Microphone permission denied."
        }
    }

    fun startVoiceSearch() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = sr
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true; voiceError = null }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(err: Int) {
                    isListening = false
                    voiceError = "Voice error ($err). Try again."
                }
                override fun onResults(bundle: Bundle?) {
                    isListening = false
                    val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        viewModel.setSearchQuery(spokenText)
                        viewModel.performSearch()
                        focusManager.clearFocus()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            sr.startListening(intent)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Title
        Text(
            text = "Search",
            color = SketchColors.Ink,
            fontSize = 28.sp,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Sketch Search Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SketchSearchField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = "Search songs, artists, albums...",
                onSearch = {
                    viewModel.performSearch()
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isSearchFieldFocused = it.isFocused },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.setSearchQuery("")
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = SketchColors.Ink)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Camera Search Icon Button (Sketch Button)
            SketchIconButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                backgroundColor = SketchColors.Teal,
                size = 42.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Camera Search",
                    tint = SketchColors.Ink,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Voice Search Mic Button (Sketch Button)
            SketchIconButton(
                onClick = {
                    if (isListening) speechRecognizer?.stopListening() else startVoiceSearch()
                },
                backgroundColor = if (isListening) SketchColors.HeartRed else SketchColors.Orange,
                size = 42.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = SketchColors.Ink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (voiceError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = voiceError!!,
                color = SketchColors.HeartRed,
                fontSize = 12.sp,
                fontFamily = PoppinsFontFamily,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Outlined Pill Suggestion Chips
        val aiSuggestions = listOf("Top Pop 2026", "Acoustic Chill", "Lo-Fi Beats", "Hindi Hits", "Workout Energy", "Focus Instrumental")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            items(aiSuggestions.size) { idx ->
                val suggestion = aiSuggestions[idx]
                val slab = slabColorForIndex(idx)
                Box(
                    modifier = Modifier
                        .bounceClick {
                            viewModel.setSearchQuery(suggestion)
                            viewModel.performSearch()
                        }
                        .background(Color.White, RoundedCornerShape(50))
                        .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✦ $suggestion",
                        color = SketchColors.Ink,
                        fontSize = 12.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Suggestions Dropdown List
        if (isSearchFieldFocused && suggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(suggestions.size, key = { index -> "suggest_${suggestions[index]}" }) { index ->
                    val suggestion = suggestions[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setSearchQuery(suggestion)
                                viewModel.performSearch()
                                focusManager.clearFocus()
                            }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SketchColors.MutedText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = suggestion,
                            color = SketchColors.Ink,
                            fontSize = 14.sp,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
            }
        } else if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SketchColors.Orange)
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Search failed: $error", color = SketchColors.HeartRed, textAlign = TextAlign.Center, fontFamily = PoppinsFontFamily)
            }
        } else if (query.isNotEmpty() && results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No results found for \"$query\"", color = SketchColors.MutedText, textAlign = TextAlign.Center, fontFamily = PoppinsFontFamily)
            }
        } else if (query.isNotEmpty() || results.isNotEmpty()) {
            val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(results, key = { _, item -> item.id }) { index, item ->
                    val song = Song(
                        id = item.id,
                        title = item.title,
                        channel = item.channel,
                        durationSeconds = item.durationSeconds,
                        thumbnailUrl = item.thumbnailUrl
                    )
                    val slab = slabColorForIndex(index)

                    // Outlined Card per spec: thumbnail + title + channel + duration + play button
                    SketchCard(
                        modifier = Modifier.fillMaxWidth(),
                        slabColor = slab,
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.playbackManager.playSongImmediate(song)
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
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

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
                                    text = song.channel,
                                    fontSize = 12.sp,
                                    fontFamily = PoppinsFontFamily,
                                    color = SketchColors.MutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = formatDuration(song.durationSeconds),
                                fontSize = 12.sp,
                                fontFamily = PoppinsFontFamily,
                                color = SketchColors.MutedText,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            // Play / Add icon buttons
                            IconButton(
                                onClick = { showAddToPlaylistDialog = song },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = SketchColors.Ink)
                            }

                            SketchPlayButton(
                                isPlaying = false,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.playbackManager.playSongImmediate(song)
                                },
                                size = 36.dp,
                                iconSize = 18.dp
                            )
                        }
                    }
                }
            }
        } else {
            // Default Explore & Categories View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Recent Searches
                if (recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Searches", color = SketchColors.Ink, fontSize = 18.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
                        Text(
                            "Clear all",
                            color = SketchColors.Teal,
                            fontSize = 13.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        items(recentSearches.size) { idx ->
                            val item = recentSearches[idx]
                            Box(
                                modifier = Modifier
                                    .bounceClick {
                                        viewModel.setSearchQuery(item)
                                        viewModel.performSearch()
                                    }
                                    .background(Color.White, RoundedCornerShape(50))
                                    .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(50))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = item,
                                    color = SketchColors.Ink,
                                    fontSize = 13.sp,
                                    fontFamily = PoppinsFontFamily
                                )
                            }
                        }
                    }
                }

                // Browse Categories
                Text(
                    text = "Browse Categories",
                    color = SketchColors.Ink,
                    fontSize = 18.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val allCategories = listOf(
                    Triple("Pop", SketchColors.Orange, Icons.Default.GraphicEq),
                    Triple("Hip-Hop", SketchColors.Teal, Icons.Default.Equalizer),
                    Triple("Rock", SketchColors.Purple, Icons.Default.ElectricBolt),
                    Triple("Indie", SketchColors.Green, Icons.Default.MusicNote),
                    Triple("R&B", SketchColors.Orange, Icons.Default.Headphones),
                    Triple("Electronic", SketchColors.Teal, Icons.Default.GraphicEq),
                    Triple("Bollywood", SketchColors.Purple, Icons.Default.MusicVideo),
                    Triple("Classical", SketchColors.Green, Icons.Default.Piano),
                    Triple("Workout", SketchColors.Orange, Icons.Default.FitnessCenter),
                    Triple("Gaming", SketchColors.Teal, Icons.Default.SportsEsports),
                    Triple("Focus", SketchColors.Purple, Icons.Default.Psychology),
                    Triple("Sleep", SketchColors.Green, Icons.Default.Bedtime)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (row in 0 until (allCategories.size / 2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..1) {
                                val cat = allCategories[row * 2 + col]
                                SketchCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(86.dp),
                                    slabColor = cat.second,
                                    onClick = { viewModel.searchAndPlaySong("${cat.first} Music") }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            cat.first,
                                            color = SketchColors.Ink,
                                            fontSize = 16.sp,
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = cat.third,
                                            contentDescription = null,
                                            tint = cat.second,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Add to Playlist Dialog (Sketch Outline Style)
    if (showAddToPlaylistDialog != null) {
        val songToAdd = showAddToPlaylistDialog!!
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = null },
            title = {
                Text(
                    "Add to Playlist",
                    color = SketchColors.Ink,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp)),
            text = {
                val allPlaylistsIncludingLiked = mutableListOf<PlaylistEntity>()
                viewModel.likedPlaylistId.value?.let { likedId ->
                    allPlaylistsIncludingLiked.add(PlaylistEntity(id = likedId, name = "Liked Songs"))
                }
                allPlaylistsIncludingLiked.addAll(playlists)

                if (allPlaylistsIncludingLiked.isEmpty()) {
                    Text("No playlists created yet. Create one in Library first!", color = SketchColors.MutedText, fontFamily = PoppinsFontFamily)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(allPlaylistsIncludingLiked) { _, playlist ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, songToAdd)
                                        showAddToPlaylistDialog = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = playlist.name,
                                    color = SketchColors.Ink,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                            }
                            HorizontalDivider(color = SketchColors.FaintCircle)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = null }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            }
        )
    }
}
