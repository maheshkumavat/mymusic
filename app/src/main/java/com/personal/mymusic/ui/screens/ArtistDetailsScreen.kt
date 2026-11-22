package com.personal.mymusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
fun ArtistDetailsScreen(
    artistId: String,
    artistName: String,
    avatarUrl: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val topSongs = remember {
        listOf(
            Song(id = "${artistId}_a", title = "Midnight Theory", channel = artistName, durationSeconds = 210, thumbnailUrl = avatarUrl),
            Song(id = "${artistId}_b", title = "Golden Hour Pulse", channel = artistName, durationSeconds = 198, thumbnailUrl = avatarUrl),
            Song(id = "${artistId}_c", title = "Shadow Dance", channel = artistName, durationSeconds = 230, thumbnailUrl = avatarUrl)
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
                    text = "Artist Profile",
                    color = SketchColors.Ink,
                    fontSize = 20.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Header Banner in SketchCard
            SketchCard(
                modifier = Modifier.fillMaxWidth(),
                slabColor = SketchColors.Orange,
                rotation = 1.2f,
                showPin = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = artistName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, SketchColors.Ink, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = artistName,
                            color = SketchColors.Ink,
                            fontSize = 22.sp,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = SketchColors.Teal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "4.8M Monthly Listeners",
                        color = SketchColors.MutedText,
                        fontSize = 13.sp,
                        fontFamily = PoppinsFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Popular Songs",
                color = SketchColors.Ink,
                fontSize = 18.sp,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            topSongs.forEachIndexed { index, trk ->
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
                        AsyncImage(
                            model = trk.thumbnailUrl,
                            contentDescription = trk.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
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
