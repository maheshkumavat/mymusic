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
        playbackManager = PlaybackManager(applicationContext, app.container.newPipeService)

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
            val currentScreen by viewModel.currentScreen.collectAsState()
            val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsState()
            
            BackHandler(enabled = isPlayerExpanded) {
                viewModel.setPlayerExpanded(false)
            }
            BackHandler(enabled = !isPlayerExpanded && currentScreen is AppScreen.PlaylistDetails) {
                viewModel.navigateTo(AppScreen.Library)
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

            if (showBatteryOptDialog) {
                AlertDialog(
                    onDismissRequest = {
                        getSharedPreferences("music_prefs", MODE_PRIVATE).edit().putBoolean("battery_opt_prompted", true).apply()
                        showBatteryOptDialog = false
                    },
                    title = { Text("Background Playback Optimization", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        Text(
                            "Your device may aggressively terminate background apps. To ensure uninterrupted background playback, please configure Battery settings to 'Unrestricted' for MyMusic.",
                            fontFamily = InterFontFamily,
                            color = Color.LightGray
                        )
                    },
                    confirmButton = {
                        Button(
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
                            colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                        ) {
                            Text("Go to Settings", color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                getSharedPreferences("music_prefs", MODE_PRIVATE).edit().putBoolean("battery_opt_prompted", true).apply()
                                showBatteryOptDialog = false
                            }
                        ) {
                            Text("Later", color = Color.White)
                        }
                    },
                    containerColor = BackgroundEnd
                )
            }

            val currentSong by playbackManager.currentSong.collectAsState()
            val isPlaying by playbackManager.isPlaying.collectAsState()
            val position by playbackManager.currentPosition.collectAsState()
            val duration by playbackManager.duration.collectAsState()

            GradientBackground {
                val updateState by viewModel.updateState.collectAsState()

                when (val state = updateState) {
                    is UpdateState.UpdateAvailable -> {
                        AlertDialog(
                            onDismissRequest = { viewModel.resetUpdateState() },
                            title = { Text("Update Available", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = Color.White) },
                            text = {
                                Column {
                                    Text(
                                        "A new version (${state.latestVersion}) is available. Would you like to update now?",
                                        fontFamily = InterFontFamily,
                                        color = Color.LightGray
                                    )
                                    if (state.releaseNotes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Release notes:\n${state.releaseNotes}",
                                            fontFamily = InterFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.startUpdateDownload(
                                            latestVersion = state.latestVersion,
                                            downloadUrl = state.downloadUrl,
                                            onError = { errMsg ->
                                                // Handle error (Toast is shown by UpdateManager)
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                                ) {
                                    Text("Update Now", color = Color.Black)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.resetUpdateState() }) {
                                    Text("Later", color = Color.White)
                                }
                            },
                            containerColor = BackgroundEnd,
                            tonalElevation = 6.dp
                        )
                    }
                    is UpdateState.Downloading -> {
                        AlertDialog(
                            onDismissRequest = { /* Prevent dismiss while downloading */ },
                            title = { Text("Downloading Update", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = Color.White) },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = AccentColor)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Downloading update APK... Once finished, you will be prompted to install it.",
                                        color = Color.LightGray,
                                        fontFamily = InterFontFamily,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            confirmButton = {},
                            containerColor = BackgroundEnd
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
                            // Mini Player
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
                                        onClick = { viewModel.setPlayerExpanded(true) }
                                    )
                                }
                            }

                            // Bottom Navigation Bar
                            NavigationBar(
                                containerColor = SurfaceGlass, // Premium frosted glass look
                                modifier = Modifier
                                    .background(Color.Transparent)
                                    .border(1.dp, BorderGlass, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.Home,
                                    onClick = { viewModel.navigateTo(AppScreen.Home) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentColor,
                                        selectedTextColor = AccentColor,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.Search,
                                    onClick = { viewModel.navigateTo(AppScreen.Search) },
                                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                    label = { Text("Search", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentColor,
                                        selectedTextColor = AccentColor,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.Library || currentScreen is AppScreen.PlaylistDetails,
                                    onClick = { viewModel.navigateTo(AppScreen.Library) },
                                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                                    label = { Text("Library", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentColor,
                                        selectedTextColor = AccentColor,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentScreen is AppScreen.Settings,
                                    onClick = { viewModel.navigateTo(AppScreen.Settings) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentColor,
                                        selectedTextColor = AccentColor,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val screen = currentScreen) {
                            is AppScreen.Home -> HomeScreen(viewModel = viewModel, onPlaylistClick = { id, name ->
                                viewModel.navigateTo(AppScreen.PlaylistDetails(id, name))
                            })
                            is AppScreen.Search -> SearchScreen(viewModel = viewModel, onPlaylistClick = { id, name ->
                                viewModel.navigateTo(AppScreen.PlaylistDetails(id, name))
                            })
                            is AppScreen.Library -> LibraryScreen(viewModel = viewModel, onPlaylistClick = { id, name ->
                                viewModel.navigateTo(AppScreen.PlaylistDetails(id, name))
                            })
                            is AppScreen.PlaylistDetails -> PlaylistDetailsScreen(
                                playlistId = screen.playlistId,
                                playlistName = screen.playlistName,
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(AppScreen.Library) }
                            )
                            is AppScreen.Settings -> SettingsScreen(viewModel = viewModel)
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

@Composable
fun MiniPlayer(
    song: com.personal.mymusic.domain.model.Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp) // Match 16dp padding
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceGlass)
            .clickable(onClick = onClick)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp), // Increase horizontal padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.channel,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                color = AccentColor,
                trackColor = Color(0x11FFFFFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            )
        }
    }
}
