package com.personal.mymusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.mymusic.domain.model.Song
import com.personal.mymusic.ui.MainViewModel
import com.personal.mymusic.ui.components.*

@Composable
fun AlbumDetailsScreen(
    albumId: String,
    albumName: String,
    artistName: String,
    coverUrl: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val mockTracks = remember {
        listOf(
            Song(id = "${albumId}_1", title = "Midnight Resonance", channel = artistName, durationSeconds = 214, thumbnailUrl = coverUrl),
            Song(id = "${albumId}_2", title = "Solar Flare", channel = artistName, durationSeconds = 188, thumbnailUrl = coverUrl),
            Song(id = "${albumId}_3", title = "Ethereal Echoes", channel = artistName, durationSeconds = 245, thumbnailUrl = coverUrl),
            Song(id = "${albumId}_4", title = "Neon Cityscape", channel = artistName, durationSeconds = 202, thumbnailUrl = coverUrl),
            Song(id = "${albumId}_5", title = "Velocity Shift", channel = artistName, durationSeconds = 195, thumbnailUrl = coverUrl)
        )
    }

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
                    text = "Album",
                    color = SketchColors.Ink,
                    fontSize = 20.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Album Header Card
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Teal,
                rotation = -1.2f,
                showPin = true
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = albumName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = albumName,
                            color = SketchColors.Ink,
                            fontSize = 18.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = artistName,
                            color = SketchColors.Teal,
                            fontSize = 14.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${mockTracks.size} songs • 2026",
                            color = SketchColors.MutedText,
                            fontSize = 12.sp,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tracks",
                color = SketchColors.Ink,
                fontSize = 18.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            mockTracks.forEachIndexed { index, trk ->
                val slab = slabColorForIndex(index)
                SketchCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    slabColor = slab,
                    onClick = { viewModel.playbackManager.playSongImmediate(trk) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = SketchColors.MutedText,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = trk.title,
                                color = SketchColors.Ink,
                                fontSize = 14.sp,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = trk.channel,
                                color = SketchColors.MutedText,
                                fontSize = 12.sp,
                                fontFamily = PoppinsFontFamily
                            )
                        }
                        Text(
                            text = formatDuration(trk.durationSeconds),
                            color = SketchColors.MutedText,
                            fontSize = 12.sp,
                            fontFamily = PoppinsFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
