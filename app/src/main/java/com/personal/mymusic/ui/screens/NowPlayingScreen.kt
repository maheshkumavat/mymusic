package com.personal.mymusic.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.personal.mymusic.domain.model.LyricLine
import com.personal.mymusic.domain.model.LyricsState
import com.personal.mymusic.domain.model.Song
import com.personal.mymusic.ui.MainViewModel
import com.personal.mymusic.ui.components.*

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

    val defaultSong = remember {
        Song(
            id = "hate_the_other_side",
            title = "Hate the Other Side",
            channel = "Marshmello",
            durationSeconds = 220,
            thumbnailUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600"
        )
    }
    val song = currentSong ?: defaultSong
    val displayPosition = if (currentSong != null) position else 73000L
    val displayDuration = if (currentSong != null && duration > 0) duration else 220000L

    val likedSongIds by viewModel.likedSongIds.collectAsState()
    val isLiked = likedSongIds.contains(song.id)

    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var currentVolumePercent by remember {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        mutableIntStateOf(if (max > 0) (current * 100 / max) else 40)
    }

    var activeSubView by remember { mutableStateOf(NowPlayingSubView.ALBUM_ART) }
    var showMenu by remember { mutableStateOf(false) }

    val progressFraction = remember(displayPosition, displayDuration) {
        if (displayDuration > 0) (displayPosition.toFloat() / displayDuration.toFloat()).coerceIn(0f, 1f) else 0.33f
    }

    FaintCirclesBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // 1. Top Bar: Orange rounded square back button + "Now Playing" + Share/Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button: Orange rounded square (12dp radius, ink outline, 3dp drop)
                SketchIconButton(
                    onClick = onCollapse,
                    backgroundColor = SketchColors.Orange,
                    size = 42.dp,
                    shape = RoundedCornerShape(12.dp),
                    dropOffset = 3.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Collapse",
                        tint = SketchColors.Ink,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Now Playing",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SketchColors.Ink
                )

                // Subview switcher / Options menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SketchIconButton(
                        onClick = {
                            activeSubView = when (activeSubView) {
                                NowPlayingSubView.ALBUM_ART -> NowPlayingSubView.LYRICS
                                NowPlayingSubView.LYRICS -> NowPlayingSubView.QUEUE
                                NowPlayingSubView.QUEUE -> NowPlayingSubView.ALBUM_ART
                            }
                        },
                        backgroundColor = if (activeSubView != NowPlayingSubView.ALBUM_ART) SketchColors.Teal else Color.White,
                        size = 42.dp,
                        shape = RoundedCornerShape(12.dp),
                        dropOffset = 3.dp
                    ) {
                        Icon(
                            imageVector = when (activeSubView) {
                                NowPlayingSubView.ALBUM_ART -> Icons.Default.Lyrics
                                NowPlayingSubView.LYRICS -> Icons.Default.QueueMusic
                                NowPlayingSubView.QUEUE -> Icons.Default.Image
                            },
                            contentDescription = "Switch View",
                            tint = SketchColors.Ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (activeSubView) {
                NowPlayingSubView.ALBUM_ART -> {
                    // Title + Channel + Heart Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = SketchColors.Ink,
                                fontSize = 22.sp,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = song.channel,
                                color = SketchColors.MutedText,
                                fontSize = 14.sp,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleLikeSong(song) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like Song",
                                tint = if (isLiked) SketchColors.HeartRed else SketchColors.Ink,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Widescreen Art Card with Teal Slab
                    SketchCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        slabColor = SketchColors.Teal,
                        slabHeight = 6.dp,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Controller Card (Tilted white card with teal slab + pins)
                    SketchCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        slabColor = SketchColors.Teal,
                        rotation = 0f,
                        showPin = true,
                        pinAlignment = Alignment.TopStart
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Time labels & Waveform-style seek bar (Teal)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatMs(position),
                                    fontSize = 12.sp,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SketchColors.Ink
                                )
                                Text(
                                    text = formatMs(duration),
                                    fontSize = 12.sp,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SketchColors.Ink
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            WaveformSeekBar(
                                progress = progressFraction,
                                onProgressChange = { fraction ->
                                    val targetMs = (fraction * duration).toLong()
                                    viewModel.playbackManager.seekTo(targetMs)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Controls: shuffle / prev / orange circular play button (3dp drop) / next / repeat
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.playbackManager.toggleShuffle() }) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = if (shuffleEnabled) SketchColors.Orange else SketchColors.Ink,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(onClick = { viewModel.playbackManager.skipToPrevious() }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = SketchColors.Ink,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Orange Circular Play/Pause Button with 3dp drop
                                SketchPlayButton(
                                    isPlaying = isPlaying,
                                    onClick = { viewModel.playbackManager.togglePlayPause() },
                                    size = 58.dp,
                                    iconSize = 30.dp,
                                    backgroundColor = SketchColors.Orange
                                )

                                IconButton(onClick = { viewModel.playbackManager.skipToNext() }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next",
                                        tint = SketchColors.Ink,
                                        modifier = Modifier.size(28.dp)
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
                                            Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                                            else -> Icons.Default.Repeat
                                        },
                                        contentDescription = "Repeat",
                                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) SketchColors.Orange else SketchColors.Ink,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Volume Row: Speaker icon, dot indicator, volume percentage
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Speaker",
                                    fontSize = 12.sp,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = SketchColors.MutedText
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .height(6.dp)
                                        .background(SketchColors.FaintCircle, RoundedCornerShape(3.dp))
                                        .border(1.dp, SketchColors.Ink, RoundedCornerShape(3.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((currentVolumePercent / 100f).coerceIn(0f, 1f))
                                            .background(SketchColors.Teal, RoundedCornerShape(3.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "$currentVolumePercent%",
                                    fontSize = 12.sp,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SketchColors.Ink
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Autoplay Toggle Chip in Now Playing Screen
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (autoplayEnabled) SketchColors.Orange.copy(alpha = 0.2f) else Color.White)
                                    .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(50))
                                    .clickable { viewModel.playbackManager.setAutoplayEnabled(!autoplayEnabled) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AllInclusive,
                                        contentDescription = "Autoplay",
                                        tint = if (autoplayEnabled) SketchColors.Ink else SketchColors.MutedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (autoplayEnabled) "Autoplay ON" else "Autoplay OFF",
                                        fontSize = 11.sp,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (autoplayEnabled) SketchColors.Ink else SketchColors.MutedText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                NowPlayingSubView.LYRICS -> {
                    // Mini song card on top per spec
                    SketchCard(
                        modifier = Modifier.fillMaxWidth(),
                        slabColor = SketchColors.Teal,
                        rotation = -1.2f
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = SketchColors.Ink,
                                    fontSize = 14.sp,
                                    fontFamily = PoppinsFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.channel,
                                    color = SketchColors.MutedText,
                                    fontSize = 12.sp,
                                    fontFamily = PoppinsFontFamily
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Lyrics Container with highlighted orange-slab active line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val sampleLyrics: List<LyricLine> = remember {
                            listOf(
                                LyricLine(0L, "I'm still holding on to everything we had"),
                                LyricLine(30000L, "Trying not to think about the times we lost"),
                                LyricLine(60000L, "Hate the other side of being alone"),
                                LyricLine(90000L, "Counting all the memories that turn to dust"),
                                LyricLine(120000L, "Wish that I could run away with you tonight")
                            )
                        }

                        val lines = when (lyricsState) {
                            is LyricsState.Success -> (lyricsState as LyricsState.Success).lines
                            else -> sampleLyrics
                        }

                        val listState = rememberLazyListState()

                        val activeIndex = remember(lines, displayPosition) {
                            var idx = 2
                            for (i in lines.indices) {
                                if (displayPosition >= lines[i].timestampMs) {
                                    idx = i
                                } else {
                                    break
                                }
                            }
                            idx
                        }

                        LaunchedEffect(activeIndex) {
                            if (activeIndex >= 0) {
                                listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 24.dp)
                        ) {
                            itemsIndexed(lines) { index, line ->
                                val isActive = index == activeIndex

                                if (isActive) {
                                    // Current line in an orange-slab highlighted card per spec
                                    SketchCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        slabColor = SketchColors.Orange,
                                        rotation = 1.0f,
                                        onClick = { viewModel.playbackManager.seekTo(line.timestampMs) }
                                    ) {
                                        Text(
                                            text = line.text,
                                            color = SketchColors.Ink,
                                            fontSize = 18.sp,
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        )
                                    }
                                } else {
                                    // Other lines in plain grey/muted text
                                    Text(
                                        text = line.text,
                                        color = SketchColors.MutedText.copy(alpha = 0.7f),
                                        fontSize = 15.sp,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.playbackManager.seekTo(line.timestampMs) }
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Mini playback bar at bottom of lyrics
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.playbackManager.skipToPrevious() }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = SketchColors.Ink, modifier = Modifier.size(28.dp))
                        }
                        SketchPlayButton(
                            isPlaying = isPlaying,
                            onClick = { viewModel.playbackManager.togglePlayPause() },
                            size = 50.dp,
                            iconSize = 24.dp
                        )
                        IconButton(onClick = { viewModel.playbackManager.skipToNext() }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = SketchColors.Ink, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                NowPlayingSubView.QUEUE -> {
                    // Queue Subview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Up Next (${queue.size})",
                            color = SketchColors.Ink,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Autoplay",
                                color = SketchColors.MutedText,
                                fontFamily = PoppinsFontFamily,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = autoplayEnabled,
                                onCheckedChange = { viewModel.playbackManager.setAutoplayEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SketchColors.Orange,
                                    checkedTrackColor = SketchColors.Ink,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = SketchColors.FaintCircle
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val (regularQueue, autoplayQueue) = remember(queue) {
                        queue.partition { !it.isAutoplay }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(regularQueue) { idx, qSong ->
                            val isCurrent = qSong.id == song.id
                            SketchCard(
                                modifier = Modifier.fillMaxWidth(),
                                slabColor = if (isCurrent) SketchColors.Orange else slabColorForIndex(idx),
                                rotation = if (isCurrent) -1.2f else 0f,
                                onClick = {
                                    val player = viewModel.playbackManager.controller
                                    if (player != null) {
                                        for (i in 0 until player.mediaItemCount) {
                                            if (player.getMediaItemAt(i).mediaId == qSong.id) {
                                                player.seekTo(i, 0L)
                                                player.play()
                                                break
                                            }
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = qSong.thumbnailUrl,
                                        contentDescription = qSong.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, SketchColors.Ink, RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = qSong.title,
                                            color = SketchColors.Ink,
                                            fontSize = 13.sp,
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = qSong.channel,
                                            color = SketchColors.MutedText,
                                            fontSize = 11.sp,
                                            fontFamily = PoppinsFontFamily
                                        )
                                    }
                                    if (isCurrent) {
                                        Text(
                                            text = "Playing",
                                            color = SketchColors.Orange,
                                            fontSize = 11.sp,
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Autoplay · Up next Section
                        if (autoplayQueue.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Autoplay · Up next",
                                        color = SketchColors.Ink,
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(SketchColors.Teal.copy(alpha = 0.2f))
                                            .border(1.dp, SketchColors.Ink, RoundedCornerShape(50))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Autoplay",
                                            color = SketchColors.Ink,
                                            fontSize = 10.sp,
                                            fontFamily = PoppinsFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            itemsIndexed(autoplayQueue) { idx, qSong ->
                                val isCurrent = qSong.id == song.id
                                SketchCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    slabColor = if (isCurrent) SketchColors.Orange else SketchColors.Teal,
                                    rotation = 0f,
                                    onClick = {
                                        val player = viewModel.playbackManager.controller
                                        if (player != null) {
                                            for (i in 0 until player.mediaItemCount) {
                                                if (player.getMediaItemAt(i).mediaId == qSong.id) {
                                                    player.seekTo(i, 0L)
                                                    player.play()
                                                    break
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = qSong.thumbnailUrl,
                                            contentDescription = qSong.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, SketchColors.Ink, RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = qSong.title,
                                                color = SketchColors.Ink,
                                                fontSize = 13.sp,
                                                fontFamily = PoppinsFontFamily,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = qSong.channel,
                                                    color = SketchColors.MutedText,
                                                    fontSize = 11.sp,
                                                    fontFamily = PoppinsFontFamily
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "• Autoplay",
                                                    color = SketchColors.Teal,
                                                    fontSize = 10.sp,
                                                    fontFamily = PoppinsFontFamily,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        if (isCurrent) {
                                            Text(
                                                text = "Playing",
                                                color = SketchColors.Orange,
                                                fontSize = 11.sp,
                                                fontFamily = PoppinsFontFamily,
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
}
