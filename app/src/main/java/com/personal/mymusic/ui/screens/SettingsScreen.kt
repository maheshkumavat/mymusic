package com.personal.mymusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.mymusic.ui.MainViewModel
import com.personal.mymusic.ui.components.*

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val cacheSizeGb by viewModel.cacheSizeLimitGb.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val crossfadeSeconds by viewModel.crossfadeSeconds.collectAsState()
    val autoplayEnabledSetting by viewModel.playbackManager.autoplayEnabled.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var streamingQuality by remember { mutableStateOf("Lossless (320kbps)") }
    var gaplessPlayback by remember { mutableStateOf(true) }
    var normalizeVolume by remember { mutableStateOf(true) }
    var pushNotifications by remember { mutableStateOf(true) }
    var listeningHistoryOptIn by remember { mutableStateOf(true) }

    var showQualityDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showConfirmHistoryDialog by remember { mutableStateOf(false) }

    FaintCirclesBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Settings",
                color = SketchColors.Ink,
                fontSize = 28.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 1. Audio Quality & Playback Card
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Orange
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SketchColors.Orange, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Audio & Playback",
                            color = SketchColors.Ink,
                            fontSize = 17.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Streaming Quality Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQualityDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Streaming Quality", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            Text(streamingQuality, color = SketchColors.MutedText, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SketchColors.Ink)
                    }

                    HorizontalDivider(color = SketchColors.FaintCircle)

                    // Gapless Playback
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gapless Playback", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Eliminate silence between tracks", color = SketchColors.MutedText, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                        }
                        Switch(
                            checked = gaplessPlayback,
                            onCheckedChange = { gaplessPlayback = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SketchColors.Orange,
                                checkedTrackColor = SketchColors.Ink,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = SketchColors.FaintCircle
                            )
                        )
                    }

                    HorizontalDivider(color = SketchColors.FaintCircle)

                    // Normalize Volume
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Normalize Volume", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Consistent volume across all tracks", color = SketchColors.MutedText, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                        }
                        Switch(
                            checked = normalizeVolume,
                            onCheckedChange = { normalizeVolume = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SketchColors.Orange,
                                checkedTrackColor = SketchColors.Ink,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = SketchColors.FaintCircle
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Storage & Cache Card
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Teal
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = SketchColors.Teal, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Storage & Cache",
                            color = SketchColors.Ink,
                            fontSize = 17.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Current limit: ${"%.1f".format(cacheSizeGb)} GB",
                        color = SketchColors.MutedText,
                        fontSize = 13.sp,
                        fontFamily = PoppinsFontFamily
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SketchButton(
                            text = "Clear Cache",
                            onClick = { viewModel.clearCache() },
                            backgroundColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        SketchButton(
                            text = "Clear History",
                            onClick = { showConfirmHistoryDialog = true },
                            backgroundColor = SketchColors.HeartRed,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Backup & Sync Card
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Purple
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = SketchColors.Purple, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Backup & Playlists",
                            color = SketchColors.Ink,
                            fontSize = 17.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SketchButton(
                            text = "Export JSON",
                            onClick = { showExportDialog = true },
                            backgroundColor = SketchColors.Purple,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        SketchButton(
                            text = "Import JSON",
                            onClick = { showExportDialog = true },
                            backgroundColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Privacy & Notifications Card
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Green
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SketchColors.Green, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacy & Notifications",
                            color = SketchColors.Ink,
                            fontSize = 17.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Push Notifications", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Alerts for updates & releases", color = SketchColors.MutedText, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                        }
                        Switch(
                            checked = pushNotifications,
                            onCheckedChange = { pushNotifications = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SketchColors.Orange,
                                checkedTrackColor = SketchColors.Ink,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = SketchColors.FaintCircle
                            )
                        )
                    }

                    HorizontalDivider(color = SketchColors.FaintCircle)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Private Session", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Pause playback history tracking", color = SketchColors.MutedText, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                        }
                        Switch(
                            checked = listeningHistoryOptIn,
                            onCheckedChange = { listeningHistoryOptIn = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SketchColors.Orange,
                                checkedTrackColor = SketchColors.Ink,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = SketchColors.FaintCircle
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Updates & Developer Tools
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Orange
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.checkForUpdates() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Check for Updates", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
                            Text("Current version: 1.0.19", color = SketchColors.MutedText, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                        }
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SketchColors.Orange)
                    }

                    HorizontalDivider(color = SketchColors.FaintCircle)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(AppScreen.DeveloperTools) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Developer Tools", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
                            Text("Diagnostics, logs and engine inspector", color = SketchColors.MutedText, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                        }
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = SketchColors.Ink)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Clear History Dialog (Sketch Outline Style)
    if (showConfirmHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmHistoryDialog = false },
            title = {
                Text(
                    "Clear Listening History",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SketchColors.Ink
                )
            },
            text = {
                Text(
                    "Are you sure you want to clear your listening history? This cannot be undone.",
                    fontFamily = PoppinsFontFamily,
                    color = SketchColors.MutedText
                )
            },
            confirmButton = {
                SketchButton(
                    text = "Clear All",
                    onClick = {
                        viewModel.clearPlayHistory()
                        showConfirmHistoryDialog = false
                    },
                    backgroundColor = SketchColors.HeartRed,
                    contentColor = Color.White
                )
            },
            dismissButton = {
                TextButton(onClick = { showConfirmHistoryDialog = false }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
        )
    }

    // Backup Dialog (Sketch Outline Style)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    "Playlist Backup",
                    color = SketchColors.Ink,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Your playlists have been saved into your internal device storage backup folder in JSON format.",
                    color = SketchColors.MutedText,
                    fontFamily = PoppinsFontFamily
                )
            },
            confirmButton = {
                SketchButton(
                    text = "Done",
                    onClick = { showExportDialog = false },
                    backgroundColor = SketchColors.Teal
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
        )
    }

    // Audio Quality Dialog (Sketch Outline Style)
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = {
                Text(
                    "Streaming Audio Quality",
                    color = SketchColors.Ink,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp)),
            text = {
                Column {
                    listOf("Lossless (320kbps)", "High (256kbps)", "Normal (160kbps)", "Data Saver (96kbps)").forEach { quality ->
                        val isSelected = streamingQuality == quality
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    streamingQuality = quality
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = quality,
                                color = if (isSelected) SketchColors.Orange else SketchColors.Ink,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = PoppinsFontFamily
                            )
                        }
                        HorizontalDivider(color = SketchColors.FaintCircle)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Close", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            }
        )
    }
}
