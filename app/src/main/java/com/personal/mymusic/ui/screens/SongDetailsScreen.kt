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
fun SongDetailsScreen(
    song: Song,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    FaintCirclesBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
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
                    text = "Song Details",
                    color = SketchColors.Ink,
                    fontSize = 20.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Artwork in SketchCard
            SketchCard(
                modifier = Modifier.size(220.dp),
                slabColor = SketchColors.Teal,
                rotation = -1.5f,
                showPin = true
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details Card
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Orange
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = song.title,
                        color = SketchColors.Ink,
                        fontSize = 20.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.channel,
                        color = SketchColors.MutedText,
                        fontSize = 15.sp,
                        fontFamily = PoppinsFontFamily
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SketchColors.FaintCircle)
                    Spacer(modifier = Modifier.height(14.dp))

                    DetailItem(label = "Duration", value = formatDuration(song.durationSeconds))
                    DetailItem(label = "Identifier", value = song.id)
                    DetailItem(label = "Audio Codec", value = "Opus 48kHz (High Resolution)")
                    DetailItem(label = "Source", value = "YouTube Music / NewPipe")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SketchButton(
                text = "Play Track Now",
                onClick = { viewModel.playbackManager.playSongImmediate(song) },
                backgroundColor = SketchColors.Orange,
                leadingIcon = {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SketchColors.Ink)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = SketchColors.MutedText, fontSize = 13.sp, fontFamily = PoppinsFontFamily)
        Text(text = value, color = SketchColors.Ink, fontSize = 13.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
    }
}
