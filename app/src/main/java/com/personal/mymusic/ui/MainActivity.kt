package com.personal.mymusic.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.personal.mymusic.MyApplication
import com.personal.mymusic.playback.PlaybackManager
import com.personal.mymusic.ui.components.*
import com.personal.mymusic.ui.screens.*

class MainActivity : ComponentActivity() {

    private lateinit var playbackManager: PlaybackManager
    private lateinit var viewModel: MainViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS permission dynamically on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val app = application as MyApplication
        playbackManager = app.container.playbackManager

        val factory = MainViewModelFactory(
            application = app,
            playlistDao = app.container.playlistDao,
            newPipeService = app.container.newPipeService,
            playbackManager = playbackManager
        )
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        val openNowPlaying = intent?.getBooleanExtra("open_now_playing", false) ?: false
        if (openNowPlaying) {
            viewModel.setPlayerExpanded(true)
        }

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            MyMusicTheme(isDarkTheme = isDarkTheme) {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsState()

                // Unified back handler
                val backStack by viewModel.backStack.collectAsState()
                BackHandler(enabled = isPlayerExpanded || currentScreen !is AppScreen.Home || backStack.isNotEmpty()) {
                    viewModel.popBackStack()
                }

                var showBatteryOptDialog by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    val prefs = getSharedPreferences("music_prefs", MODE_PRIVATE)
                    val alreadyPrompted = prefs.getBoolean("battery_opt_prompted", false)
                    if (!alreadyPrompted) {
                        val pm = getSystemService(POWER_SERVICE) as PowerManager
                        val isIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            pm.isIgnoringBatteryOptimizations(packageName)
                        } else {
                            true
                        }
                        if (!isIgnoring) {
                            val brand = Build.BRAND.lowercase()
                            val manufacturer = Build.MANUFACTURER.lowercase()
                            val isRestrictive = listOf("xiaomi", "oppo", "vivo", "samsung", "oneplus", "huawei", "realme", "redmi", "poco").any {
                                brand.contains(it) || manufacturer.contains(it)
                            }
                            if (isRestrictive) {
                                showBatteryOptDialog = true
                            }
                        }
                    }
                }

                // Battery Optimization Dialog in Sketch Outline Style
                if (showBatteryOptDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            getSharedPreferences("music_prefs", MODE_PRIVATE).edit().putBoolean("battery_opt_prompted", true).apply()
                            showBatteryOptDialog = false
                        },
                        title = {
                            Text(
                                "Background Playback Optimization",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = SketchColors.Ink
                            )
                        },
                        text = {
                            Text(
                                "Your device may aggressively terminate background apps. To ensure uninterrupted background playback, please configure Battery settings to 'Unrestricted' for MyMusic.",
                                fontFamily = PoppinsFontFamily,
                                color = SketchColors.MutedText,
                                fontSize = 13.sp
                            )
                        },
                        confirmButton = {
                            SketchButton(
                                text = "Go to Settings",
                                onClick = {
                                    getSharedPreferences("music_prefs", MODE_PRIVATE).edit().putBoolean("battery_opt_prompted", true).apply()
                                    showBatteryOptDialog = false
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", packageName, null)
                                        }
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback
                                    }
                                },
                                backgroundColor = SketchColors.Orange
                            )
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    getSharedPreferences("music_prefs", MODE_PRIVATE).edit().putBoolean("battery_opt_prompted", true).apply()
                                    showBatteryOptDialog = false
                                }
                            ) {
                                Text("Later", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
                    )
                }

                val currentSong by playbackManager.currentSong.collectAsState()
                val isPlaying by playbackManager.isPlaying.collectAsState()
                val position by playbackManager.currentPosition.collectAsState()
                val duration by playbackManager.duration.collectAsState()

                FaintCirclesBackground {
                    val updateState by viewModel.updateState.collectAsState()

                    when (val state = updateState) {
                        is UpdateState.UpdateAvailable -> {
                            AlertDialog(
                                onDismissRequest = { viewModel.resetUpdateState() },
                                title = {
                                    Text(
                                        "Update Available",
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = SketchColors.Ink
                                    )
                                },
                                text = {
                                    Column {
                                        Text(
                                            "A new version (${state.latestVersion}) is available. Would you like to update now?",
                                            fontFamily = PoppinsFontFamily,
                                            color = SketchColors.MutedText,
                                            fontSize = 13.sp
                                        )
                                        if (state.releaseNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Release notes:\n${state.releaseNotes}",
                                                fontFamily = PoppinsFontFamily,
                                                fontSize = 12.sp,
                                                color = SketchColors.MutedText
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    SketchButton(
                                        text = "Update Now",
                                        onClick = {
                                            viewModel.startUpdateDownload(
                                                latestVersion = state.latestVersion,
                                                downloadUrl = state.downloadUrl,
                                                onError = {}
                                            )
                                        },
                                        backgroundColor = SketchColors.Orange
                                    )
                                },
                                dismissButton = {
                                    TextButton(onClick = { viewModel.resetUpdateState() }) {
                                        Text("Later", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                                    }
                                },
                                containerColor = Color.White,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
                            )
                        }
                        is UpdateState.Downloading -> {
                            AlertDialog(
                                onDismissRequest = {},
                                title = {
                                    Text(
                                        "Downloading Update",
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = SketchColors.Ink
                                    )
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = SketchColors.Orange)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Downloading update APK... Once finished, you will be prompted to install it.",
                                            color = SketchColors.MutedText,
                                            fontFamily = PoppinsFontFamily,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                confirmButton = {},
                                containerColor = Color.White,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
                            )
                        }
                        else -> {}
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            Column(
                                modifier = Modifier.background(Color.Transparent)
                            ) {
                                // Mini Player in Outlined Slab Capsule style
                                AnimatedVisibility(
                                    visible = currentSong != null && !isPlayerExpanded,
                                    enter = slideInVertically(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) { it },
                                    exit = slideOutVertically(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) { it }
                                ) {
                                    currentSong?.let { song ->
                                        MiniPlayer(
                                            song = song,
                                            isPlaying = isPlaying,
                                            progress = if (duration > 0) position.toFloat() / duration else 0f,
                                            onPlayPauseClick = { playbackManager.togglePlayPause() },
                                            onCloseClick = { playbackManager.stop() },
                                            onClick = { viewModel.setPlayerExpanded(true) }
                                        )
                                    }
                                }

                                // Sketch Bottom Navigation Bar
                                SketchBottomNav(
                                    currentScreen = currentScreen,
                                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (val screen = currentScreen) {
                                is AppScreen.Home -> HomeScreen(
                                    viewModel = viewModel,
                                    onPlaylistClick = { id, name ->
                                        viewModel.navigateTo(AppScreen.PlaylistDetails(id, name))
                                    }
                                )
                                is AppScreen.Search -> SearchScreen(
                                    viewModel = viewModel,
                                    onPlaylistClick = { id, name ->
                                        viewModel.navigateTo(AppScreen.PlaylistDetails(id, name))
                                    }
                                )
                                is AppScreen.Library -> LibraryScreen(
                                    viewModel = viewModel,
                                    onPlaylistClick = { id, name ->
                                        viewModel.navigateTo(AppScreen.PlaylistDetails(id, name))
                                    }
                                )
                                is AppScreen.PlaylistDetails -> PlaylistDetailsScreen(
                                    playlistId = screen.playlistId,
                                    playlistName = screen.playlistName,
                                    viewModel = viewModel,
                                    onBack = { viewModel.popBackStack() }
                                )
                                is AppScreen.Settings -> SettingsScreen(viewModel = viewModel)
                                is AppScreen.AlbumDetails -> AlbumDetailsScreen(
                                    albumId = screen.albumId,
                                    albumName = screen.albumName,
                                    artistName = screen.artistName,
                                    coverUrl = screen.coverUrl,
                                    viewModel = viewModel,
                                    onBack = { viewModel.popBackStack() }
                                )
                                is AppScreen.ArtistDetails -> ArtistDetailsScreen(
                                    artistId = screen.artistId,
                                    artistName = screen.artistName,
                                    avatarUrl = screen.avatarUrl,
                                    viewModel = viewModel,
                                    onBack = { viewModel.popBackStack() }
                                )
                                is AppScreen.SongDetails -> SongDetailsScreen(
                                    song = screen.song,
                                    viewModel = viewModel,
                                    onBack = { viewModel.popBackStack() }
                                )
                                is AppScreen.DeveloperTools -> DeveloperToolsScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.popBackStack() }
                                )
                            }
                        }
                    }

                    // Full Now Playing View Overlay
                    AnimatedVisibility(
                        visible = isPlayerExpanded,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) { it },
                        exit = slideOutVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { it }
                    ) {
                        NowPlayingScreen(
                            viewModel = viewModel,
                            onCollapse = { viewModel.setPlayerExpanded(false) }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val openNowPlaying = intent?.getBooleanExtra("open_now_playing", false) ?: false
        if (openNowPlaying) {
            viewModel.setPlayerExpanded(true)
        }
    }

    override fun onDestroy() {
        playbackManager.release()
        super.onDestroy()
    }
}

// Mini player: outlined slab capsule above bottom nav per design spec
@Composable
fun MiniPlayer(
    song: com.personal.mymusic.domain.model.Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onCloseClick: () -> Unit,
    onClick: () -> Unit
) {
    SketchCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        slabColor = SketchColors.Teal,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = SketchColors.Ink,
                        fontFamily = PoppinsFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.channel,
                        color = SketchColors.MutedText,
                        fontFamily = PoppinsFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Orange circular play button with 2dp ink border and drop
                SketchIconButton(
                    onClick = onPlayPauseClick,
                    backgroundColor = SketchColors.Orange,
                    shape = CircleShape,
                    size = 36.dp,
                    dropOffset = 2.dp
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = SketchColors.Ink,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Player",
                        tint = SketchColors.MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Thin teal progress indicator
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                color = SketchColors.Teal,
                trackColor = SketchColors.FaintCircle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
        }
    }
}
