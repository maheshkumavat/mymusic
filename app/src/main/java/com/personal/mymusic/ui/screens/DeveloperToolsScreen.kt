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
fun DeveloperToolsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val debugLogs by viewModel.debugLogs.collectAsState()
    val fakePremium by viewModel.fakePremiumEnabled.collectAsState()
    val uiInspector by viewModel.uiInspectorEnabled.collectAsState()
    val fpsMonitor by viewModel.fpsMonitorEnabled.collectAsState()

    var showConfirmResetDialog by remember { mutableStateOf(false) }

    FaintCirclesBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SketchIconButton(
                    onClick = onBack,
                    backgroundColor = SketchColors.Orange,
                    size = 42.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SketchColors.Ink)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Developer Tools",
                    color = SketchColors.Ink,
                    fontSize = 20.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Flags Card
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Orange
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Development Flags",
                        color = SketchColors.Ink,
                        fontSize = 17.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fake Premium Mode", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Unlocks all bitrate settings", color = SketchColors.MutedText, fontSize = 11.sp, fontFamily = PoppinsFontFamily)
                        }
                        Switch(
                            checked = fakePremium,
                            onCheckedChange = { viewModel.setFakePremium(it) },
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
                            Text("UI Inspector Grid", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Highlights layout boundaries", color = SketchColors.MutedText, fontSize = 11.sp, fontFamily = PoppinsFontFamily)
                        }
                        Switch(
                            checked = uiInspector,
                            onCheckedChange = { viewModel.setUiInspector(it) },
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
                            Text("FPS & Frame Monitor", color = SketchColors.Ink, fontSize = 14.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Shows live render performance", color = SketchColors.MutedText, fontSize = 11.sp, fontFamily = PoppinsFontFamily)
                        }
                        Switch(
                            checked = fpsMonitor,
                            onCheckedChange = { viewModel.setFpsMonitor(it) },
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

            // Database Utilities Card
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Teal
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Database & Storage Utilities",
                        color = SketchColors.Ink,
                        fontSize = 17.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SketchButton(
                            text = "Clear Image Cache",
                            onClick = { viewModel.clearCoilImageCache() },
                            backgroundColor = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        SketchButton(
                            text = "Seed Demo Data",
                            onClick = { viewModel.seedDemoData() },
                            backgroundColor = SketchColors.Teal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SketchButton(
                        text = "Reset Database",
                        onClick = { showConfirmResetDialog = true },
                        backgroundColor = SketchColors.HeartRed,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Debug Logs Console
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Purple
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "System Debug Logs",
                            color = SketchColors.Ink,
                            fontSize = 17.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clear Logs",
                            color = SketchColors.Purple,
                            fontSize = 12.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearDebugLogs() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(SketchColors.CardAlt, RoundedCornerShape(12.dp))
                            .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (debugLogs.isEmpty()) {
                            Text("No debug logs recorded.", color = SketchColors.MutedText, fontSize = 12.sp, fontFamily = PoppinsFontFamily)
                        } else {
                            Text(
                                text = debugLogs.takeLast(20).joinToString("\n"),
                                color = SketchColors.Ink,
                                fontSize = 11.sp,
                                fontFamily = PoppinsFontFamily
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            title = {
                Text("Confirm Database Reset", color = SketchColors.Ink, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This will wipe all local playlists and playback history. Continue?", color = SketchColors.MutedText, fontFamily = PoppinsFontFamily)
            },
            confirmButton = {
                SketchButton(
                    text = "Reset Now",
                    onClick = {
                        viewModel.resetDatabase()
                        showConfirmResetDialog = false
                    },
                    backgroundColor = SketchColors.HeartRed,
                    contentColor = Color.White
                )
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetDialog = false }) {
                    Text("Cancel", color = SketchColors.Ink, fontFamily = PoppinsFontFamily)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
        )
    }
}
