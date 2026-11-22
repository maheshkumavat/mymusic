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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.mymusic.ui.MainViewModel
import com.personal.mymusic.ui.components.*

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val userName by viewModel.userName.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val autoplayEnabled by viewModel.playbackManager.autoplayEnabled.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var streamingQuality by remember { mutableStateOf("Lossless (320kbps)") }
    var gaplessPlayback by remember { mutableStateOf(true) }
    var normalizeVolume by remember { mutableStateOf(true) }

    var showQualityDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }

    FaintCirclesBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Settings",
                color = SketchColors.Ink,
                fontSize = 28.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Profile / Account Card (Editable Name)
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Teal
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = SketchColors.Teal, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Profile",
                            color = SketchColors.Ink,
                            fontSize = 17.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editNameInput = userName
                                showEditNameDialog = true
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Name", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (userName.isNotBlank()) userName else "Tap to set name",
                                color = SketchColors.MutedText,
                                fontSize = 12.sp,
                                fontFamily = PoppinsFontFamily
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = SketchColors.Ink, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Audio & Playback Card (With Autoplay Switch)
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

                    // Autoplay Switch (Default ON)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Autoplay", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Keep music playing with similar songs", color = SketchColors.MutedText, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                        }
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

                    HorizontalDivider(color = SketchColors.FaintCircle)

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

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Updates & Info Card
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Purple
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
                            Text("Current version: 1.0.21", color = SketchColors.MutedText, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                        }
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SketchColors.Purple)
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

            Spacer(modifier = Modifier.height(36.dp))

            // 4. Footer: "Developed by Mahesh" in small centered sketch title
            Text(
                text = "Developed by Mahesh",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = SketchColors.MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    // Edit Name Dialog (Outlined Sketch style)
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(
                    "Edit Name",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = SketchColors.Ink
                )
            },
            text = {
                Column {
                    Text(
                        "Update the name displayed on your Home screen greeting.",
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
                        if (editNameInput.isNotBlank()) {
                            viewModel.completeOnboarding(editNameInput)
                        }
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

    // Audio Quality Dialog
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
