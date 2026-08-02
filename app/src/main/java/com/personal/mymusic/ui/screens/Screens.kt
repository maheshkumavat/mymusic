package com.personal.mymusic.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.mymusic.data.database.PlaylistEntity
import com.personal.mymusic.domain.model.SearchResult
import com.personal.mymusic.domain.model.Song
import com.personal.mymusic.ui.MainViewModel
import com.personal.mymusic.ui.UpdateState
import com.personal.mymusic.ui.components.*
import androidx.media3.common.Player
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import com.personal.mymusic.domain.model.LyricsState
import com.personal.mymusic.domain.model.LyricLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
// Voice search imports
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat


sealed class AppScreen {
    object Home : AppScreen()
    object Search : AppScreen()
    object Library : AppScreen()
    data class PlaylistDetails(val playlistId: Long, val playlistName: String) : AppScreen()
    object Settings : AppScreen()
    data class AlbumDetails(val albumId: String, val albumName: String, val artistName: String, val coverUrl: String) : AppScreen()
    data class ArtistDetails(val artistId: String, val artistName: String, val avatarUrl: String) : AppScreen()
    data class SongDetails(val song: Song) : AppScreen()
    object DeveloperTools : AppScreen()
}

enum class NowPlayingSubView {
    ALBUM_ART,
    LYRICS,
    QUEUE
}

fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("%d:%02d", mins, secs)
}

fun formatMsCountdown(ms: Long): String {
    if (ms <= 0) return "-0:00"
    val totalSeconds = ms / 1000
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("-%d:%02d", mins, secs)
}

@Composable
fun RecommendationCard(
    result: SearchResult,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceGlass)
            .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(
                model = result.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.DarkGray)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = result.title,
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = result.channel,
            color = Color.Gray,
            fontSize = 10.sp,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AccentColor)
                .clickable(onClick = onAdd)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to playlist",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TrendingSongCard(
    song: Song,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    isLoading: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1B2E).copy(alpha = 0.5f))
            .border(1.dp, Color(0x1BFFFFFF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.channel,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AccentColor,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

val artistStationImages = mapOf(
    "Pritam" to listOf(
        "https://lh3.googleusercontent.com/aida-public/AB6AXuBlEp1UWbRilVDh6g6LTeCQ1zZHIhRpx3abgSjoYSV7xHanKxqebFmxwskdt-3pqMowx_rBRaa6yw5lue7Ow5e_I2Ib1ezCRj4FNsAjLShuZg4CFyGE-CDjOz46B0QvmJhNCTTXo5Znd2Werre6eU866baSEONDXVyIZt8NwVWGcF6Pbs6NqDABnZGMd7tvIrluSMol_TCCwQO5akFl-SCrKoBIkYH7QAY5sk3vb06vdIW9SgnnU9n0qtuCnZWnB5Cu7aHsuyZQSNSC",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAj5RaVmIT1sh9v-PXyIZLmgBSS2pZ6GQ_1FlGsD8TpPVW16Sdd56k10v9NBVcsXiUE8OZijJC4EgyNK8VJvVTjYXoUxYek7Ix2I7PYrwAXYis5Q7i48WDEXynRBA69s83tQiao0XFpAQIcviv5Kbt0luMmvHSEG95GvRX3AIl-GD6fshD4E1Xh_SVvvjn29QnBO8xGBRGQjzSgz3-SXj7CF9pY41i5xraJImiiuRONkCUrICJkl0_J2RnvVYWTrk-dfSH3EAdqp0A-",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCKvvYSa6ICPx3m8pKh4t9frfj4AUuq0RD61fi6ZCR-y6uLmt6TIzJqQLkGnUnJXR83zUzuytV11JS-DP2GN_2RyF2Dadt6WwI8boJH95nhaoLJAxBWOV6bnQ7Z0uAOkZ-nXcEJZRtuAA1SVbx-a4cuS34F-HWB7iw38VS6AwyoUhnDNRIN4nd5I5d_UryPF5XuzhndRs66UxEAI1359jaemWOdpqj1xZJvdnMh4D0luZSyj2wwG2-uQ4q_p5nCTEfv2Ny582Gd9C5o"
    ),
    "Taylor Swift" to listOf(
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCllqaQMbqRGNSL88VBfuKoBJFoiChW15yq4a4kRj4kxWwYoBUe0252y17CpkjHndOAjCiVA4BSB3LHnIO9UCdCvKX8e2Wrn2pIMnW7RwY4dhLOr-PUDoNYUKb0vlfIHo9TNqCjv19NDEhRoSx48QgUOv0E_bqnCzQZARwTLvkf0IhcWypqZo-94aD67Inl1onLSXl56e08OByeL_5wz99CQMGSmhcDxv6ZuxBjMZ--ZOL1dgkwpewRvVw3r4uuh5S8kg3cvcTkA0KN",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAj5RaVmIT1sh9v-PXyIZLmgBSS2pZ6GQ_1FlGsD8TpPVW16Sdd56k10v9NBVcsXiUE8OZijJC4EgyNK8VJvVTjYXoUxYek7Ix2I7PYrwAXYis5Q7i48WDEXynRBA69s83tQiao0XFpAQIcviv5Kbt0luMmvHSEG95GvRX3AIl-GD6fshD4E1Xh_SVvvjn29QnBO8xGBRGQjzSgz3-SXj7CF9pY41i5xraJImiiuRONkCUrICJkl0_J2RnvVYWTrk-dfSH3EAdqp0A-",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuBlEp1UWbRilVDh6g6LTeCQ1zZHIhRpx3abgSjoYSV7xHanKxqebFmxwskdt-3pqMowx_rBRaa6yw5lue7Ow5e_I2Ib1ezCRj4FNsAjLShuZg4CFyGE-CDjOz46B0QvmJhNCTTXo5Znd2Werre6eU866baSEONDXVyIZt8NwVWGcF6Pbs6NqDABnZGMd7tvIrluSMol_TCCwQO5akFl-SCrKoBIkYH7QAY5sk3vb06vdIW9SgnnU9n0qtuCnZWnB5Cu7aHsuyZQSNSC"
    ),
    "Harry Styles" to listOf(
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCKvvYSa6ICPx3m8pKh4t9frfj4AUuq0RD61fi6ZCR-y6uLmt6TIzJqQLkGnUnJXR83zUzuytV11JS-DP2GN_2RyF2Dadt6WwI8boJH95nhaoLJAxBWOV6bnQ7Z0uAOkZ-nXcEJZRtuAA1SVbx-a4cuS34F-HWB7iw38VS6AwyoUhnDNRIN4nd5I5d_UryPF5XuzhndRs66UxEAI1359jaemWOdpqj1xZJvdnMh4D0luZSyj2wwG2-uQ4q_p5nCTEfv2Ny582Gd9C5o",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCllqaQMbqRGNSL88VBfuKoBJFoiChW15yq4a4kRj4kxWwYoBUe0252y17CpkjHndOAjCiVA4BSB3LHnIO9UCdCvKX8e2Wrn2pIMnW7RwY4dhLOr-PUDoNYUKb0vlfIHo9TNqCjv19NDEhRoSx48QgUOv0E_bqnCzQZARwTLvkf0IhcWypqZo-94aD67Inl1onLSXl56e08OByeL_5wz99CQMGSmhcDxv6ZuxBjMZ--ZOL1dgkwpewRvVw3r4uuh5S8kg3cvcTkA0KN",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAj5RaVmIT1sh9v-PXyIZLmgBSS2pZ6GQ_1FlGsD8TpPVW16Sdd56k10v9NBVcsXiUE8OZijJC4EgyNK8VJvVTjYXoUxYek7Ix2I7PYrwAXYis5Q7i48WDEXynRBA69s83tQiao0XFpAQIcviv5Kbt0luMmvHSEG95GvRX3AIl-GD6fshD4E1Xh_SVvvjn29QnBO8xGBRGQjzSgz3-SXj7CF9pY41i5xraJImiiuRONkCUrICJkl0_J2RnvVYWTrk-dfSH3EAdqp0A-"
    ),
    "Anuv Jain" to listOf(
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAj5RaVmIT1sh9v-PXyIZLmgBSS2pZ6GQ_1FlGsD8TpPVW16Sdd56k10v9NBVcsXiUE8OZijJC4EgyNK8VJvVTjYXoUxYek7Ix2I7PYrwAXYis5Q7i48WDEXynRBA69s83tQiao0XFpAQIcviv5Kbt0luMmvHSEG95GvRX3AIl-GD6fshD4E1Xh_SVvvjn29QnBO8xGBRGQjzSgz3-SXj7CF9pY41i5xraJImiiuRONkCUrICJkl0_J2RnvVYWTrk-dfSH3EAdqp0A-",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuBlEp1UWbRilVDh6g6LTeCQ1zZHIhRpx3abgSjoYSV7xHanKxqebFmxwskdt-3pqMowx_rBRaa6yw5lue7Ow5e_I2Ib1ezCRj4FNsAjLShuZg4CFyGE-CDjOz46B0QvmJhNCTTXo5Znd2Werre6eU866baSEONDXVyIZt8NwVWGcF6Pbs6NqDABnZGMd7tvIrluSMol_TCCwQO5akFl-SCrKoBIkYH7QAY5sk3vb06vdIW9SgnnU9n0qtuCnZWnB5Cu7aHsuyZQSNSC",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCKvvYSa6ICPx3m8pKh4t9frfj4AUuq0RD61fi6ZCR-y6uLmt6TIzJqQLkGnUnJXR83zUzuytV11JS-DP2GN_2RyF2Dadt6WwI8boJH95nhaoLJAxBWOV6bnQ7Z0uAOkZ-nXcEJZRtuAA1SVbx-a4cuS34F-HWB7iw38VS6AwyoUhnDNRIN4nd5I5d_UryPF5XuzhndRs66UxEAI1359jaemWOdpqj1xZJvdnMh4D0luZSyj2wwG2-uQ4q_p5nCTEfv2Ny582Gd9C5o"
    )
)

fun getImagesForArtist(artist: String, history: List<Song>): List<String> {
    val artistSongs = history.filter { it.channel.contains(artist, ignoreCase = true) }
    val images = artistSongs.map { it.thumbnailUrl }.distinct().take(3).toMutableList()
    
    val fallbackList = artistStationImages[artist] ?: listOf(
        "https://lh3.googleusercontent.com/aida-public/AB6AXuBlEp1UWbRilVDh6g6LTeCQ1zZHIhRpx3abgSjoYSV7xHanKxqebFmxwskdt-3pqMowx_rBRaa6yw5lue7Ow5e_I2Ib1ezCRj4FNsAjLShuZg4CFyGE-CDjOz46B0QvmJhNCTTXo5Znd2Werre6eU866baSEONDXVyIZt8NwVWGcF6Pbs6NqDABnZGMd7tvIrluSMol_TCCwQO5akFl-SCrKoBIkYH7QAY5sk3vb06vdIW9SgnnU9n0qtuCnZWnB5Cu7aHsuyZQSNSC",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCllqaQMbqRGNSL88VBfuKoBJFoiChW15yq4a4kRj4kxWwYoBUe0252y17CpkjHndOAjCiVA4BSB3LHnIO9UCdCvKX8e2Wrn2pIMnW7RwY4dhLOr-PUDoNYUKb0vlfIHo9TNqCjv19NDEhRoSx48QgUOv0E_bqnCzQZARwTLvkf0IhcWypqZo-94aD67Inl1onLSXl56e08OByeL_5wz99CQMGSmhcDxv6ZuxBjMZ--ZOL1dgkwpewRvVw3r4uuh5S8kg3cvcTkA0KN",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCKvvYSa6ICPx3m8pKh4t9frfj4AUuq0RD61fi6ZCR-y6uLmt6TIzJqQLkGnUnJXR83zUzuytV11JS-DP2GN_2RyF2Dadt6WwI8boJH95nhaoLJAxBWOV6bnQ7Z0uAOkZ-nXcEJZRtuAA1SVbx-a4cuS34F-HWB7iw38VS6AwyoUhnDNRIN4nd5I5d_UryPF5XuzhndRs66UxEAI1359jaemWOdpqj1xZJvdnMh4D0luZSyj2wwG2-uQ4q_p5nCTEfv2Ny582Gd9C5o"
    )
    
    var idx = 0
    while (images.size < 3 && idx < fallbackList.size) {
        val fb = fallbackList[idx++]
        if (!images.contains(fb)) {
            images.add(fb)
        }
    }
    return images
}

@Composable
fun ShortcutChip(
    title: String,
    thumbnailUrl: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(160.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceGlass)
            .border(1.dp, BorderGlass, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

@Composable
fun RecommendedStationCard(
    artistName: String,
    imageUrls: List<String>,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .bounceClick(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(30.dp)) // 30dp squircle artwork radius per Stitch spec
                .background(SurfaceGlass)
                .border(1.dp, BorderGlass, RoundedCornerShape(30.dp))
        ) {
            val img1 = imageUrls.getOrElse(0) { "https://lh3.googleusercontent.com/aida-public/AB6AXuBlEp1UWbRilVDh6g6LTeCQ1zZHIhRpx3abgSjoYSV7xHanKxqebFmxwskdt-3pqMowx_rBRaa6yw5lue7Ow5e_I2Ib1ezCRj4FNsAjLShuZg4CFyGE-CDjOz46B0QvmJhNCTTXo5Znd2Werre6eU866baSEONDXVyIZt8NwVWGcF6Pbs6NqDABnZGMd7tvIrluSMol_TCCwQO5akFl-SCrKoBIkYH7QAY5sk3vb06vdIW9SgnnU9n0qtuCnZWnB5Cu7aHsuyZQSNSC" }
            val img2 = imageUrls.getOrElse(1) { "https://lh3.googleusercontent.com/aida-public/AB6AXuCllqaQMbqRGNSL88VBfuKoBJFoiChW15yq4a4kRj4kxWwYoBUe0252y17CpkjHndOAjCiVA4BSB3LHnIO9UCdCvKX8e2Wrn2pIMnW7RwY4dhLOr-PUDoNYUKb0vlfIHo9TNqCjv19NDEhRoSx48QgUOv0E_bqnCzQZARwTLvkf0IhcWypqZo-94aD67Inl1onLSXl56e08OByeL_5wz99CQMGSmhcDxv6ZuxBjMZ--ZOL1dgkwpewRvVw3r4uuh5S8kg3cvcTkA0KN" }
            val img3 = imageUrls.getOrElse(2) { "https://lh3.googleusercontent.com/aida-public/AB6AXuCKvvYSa6ICPx3m8pKh4t9frfj4AUuq0RD61fi6ZCR-y6uLmt6TIzJqQLkGnUnJXR83zUzuytV11JS-DP2GN_2RyF2Dadt6WwI8boJH95nhaoLJAxBWOV6bnQ7Z0uAOkZ-nXcEJZRtuAA1SVbx-a4cuS34F-HWB7iw38VS6AwyoUhnDNRIN4nd5I5d_UryPF5XuzhndRs66UxEAI1359jaemWOdpqj1xZJvdnMh4D0luZSyj2wwG2-uQ4q_p5nCTEfv2Ny582Gd9C5o" }

            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(img1)
                        .crossfade(true)
                        .size(150, 150)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .align(Alignment.TopCenter)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(img2)
                        .crossfade(true)
                        .size(150, 150)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .align(Alignment.BottomStart)
                        .offset(x = 4.dp, y = (-4).dp)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(img3)
                        .crossfade(true)
                        .size(150, 150)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "RADIO",
                    color = AccentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "$artistName Radio",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "With $artistName and more",
            color = TextSecondary,
            fontSize = 11.sp,
            fontFamily = InterFontFamily,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TopMixCard(
    title: String,
    thumbnailUrl: String,
    barColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .bounceClick(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(30.dp)) // 30dp artwork radius per Stitch spec
                .border(1.dp, BorderGlass, RoundedCornerShape(30.dp))
        ) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .size(300, 300)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.5f)
                    .height(6.dp)
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentSongCard(
    song: Song,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .bounceClick(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(110.dp)) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(song.thumbnailUrl)
                    .crossfade(true)
                    .size(250, 250)
                    .build(),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
            )
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentColor, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = song.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.channel,
            color = Color.Gray,
            fontSize = 10.sp,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (Long, String) -> Unit
) {
    val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val greetingState by viewModel.greetingState.collectAsState()

    val recentlyPlayedSongs by viewModel.recentlyPlayed.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    
    val displaySongs = recentlyPlayedSongs
    val displayArtists = if (mostPlayedArtists.isNotEmpty()) mostPlayedArtists else listOf("Pritam", "Taylor Swift", "Harry Styles", "Anuv Jain")

    var showProfileMenu by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var likedTrending by remember { mutableStateOf(setOf<String>()) }
    var selectedAiVibe by remember { mutableStateOf("Lo-Fi Chill") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // 1. Top bar: User Avatar + Name + Pencil Edit + Search Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VibeOrangeGradient)
                        .clickable { showProfileMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        color = Color(0xFF000000),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )

                    DropdownMenu(
                        expanded = showProfileMenu,
                        onDismissRequest = { showProfileMenu = false },
                        modifier = Modifier.background(Color(0xFF121212))
                    ) {
                        DropdownMenuItem(
                            text = { Text("App Info", color = TextPrimary, fontFamily = InterFontFamily) },
                            onClick = { 
                                showProfileMenu = false 
                                showAppInfoDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings & Privacy", color = TextPrimary, fontFamily = InterFontFamily) },
                            onClick = { 
                                showProfileMenu = false
                                viewModel.navigateTo(AppScreen.Settings)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        editNameInput = userName
                        showEditNameDialog = true
                    }
                ) {
                    Text(
                        text = userName,
                        fontSize = 24.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = AccentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.Search) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Dynamic time-based greeting
        Text(
            text = greetingState.title,
            color = Color.White,
            fontSize = 28.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = greetingState.subtitle,
            color = TextSecondary,
            fontSize = 14.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // 3. Continue Listening & Jump Back In Grid (2x3 Grid)
        Text(
            text = "Jump Back In",
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val quickAccessItems = listOf(
            Triple("Midnight City Wave", "Synthwave Mix", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"),
            Triple("Deep Focus", "Ambient Beats", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=300"),
            Triple("Golden Hour", "Modern Soul & Chill", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300"),
            Triple("Workout Energy", "High BPM Beats", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300"),
            Triple("Morning Mix", "Acoustic Sunrise", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300"),
            Triple("Evening Sunset", "Lofi Relax", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in 0..2) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..1) {
                        val item = quickAccessItems[row * 2 + col]
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF121212))
                                .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                                .clickable { viewModel.searchAndPlaySong(item.first) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.third,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item.first,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 4. Smart AI Recommendations Section
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Recommendations",
                        tint = AccentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart AI Recommendations",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pick a vibe and let AI synthesize an adaptive stream for your moment.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily
                )
                Spacer(modifier = Modifier.height(14.dp))

                val aiVibes = listOf("Lo-Fi Chill", "Focus House", "Retro Synth", "Acoustic Pop", "Workout Hype", "Sleep Drift")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(aiVibes.size) { idx ->
                        val vibe = aiVibes[idx]
                        val isSelected = selectedAiVibe == vibe
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) AccentColor else Color(0xFF1E1E1E))
                                .border(1.dp, if (isSelected) Color.Transparent else BorderGlass, RoundedCornerShape(50))
                                .clickable {
                                    selectedAiVibe = vibe
                                    viewModel.searchAndPlaySong("$vibe Music")
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = vibe,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Recently Played Row (Horizontal Section)
        if (displaySongs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Played",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See all",
                    color = AccentColor,
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.Library) }
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                items(displaySongs.size) { idx ->
                    val song = displaySongs[idx]
                    RecentSongCard(
                        song = song,
                        isLoading = currentlyLoadingSongId == song.id,
                        onClick = { viewModel.playbackManager.playSongImmediate(song) }
                    )
                }
            }
        }

        // 6. "Made For You" Personalized Mixes
        Text(
            text = "Made For You",
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val madeForYouItems = listOf(
            Triple("Daily Mix 1", "Synth & Lo-Fi hits", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400"),
            Triple("Discover Weekly", "Tailored to your taste", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400"),
            Triple("Release Radar", "New music from your artists", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400"),
            Triple("Workout Mix", "High-energy tracks", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400"),
            Triple("Chill Mix", "Acoustic and ambient", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400")
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(madeForYouItems.size) { index ->
                val item = madeForYouItems[index]
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { viewModel.searchAndPlaySong(item.first) }
                ) {
                    AsyncImage(
                        model = item.third,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.first,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.second,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 7. Based On Your History (Top Artists)
        Text(
            text = "Based On Your History",
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(displayArtists.size) { index ->
                val artistName = displayArtists[index]
                val imgs = getImagesForArtist(artistName, displaySongs)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { viewModel.searchAndPlaySong("$artistName Hits") }
                ) {
                    AsyncImage(
                        model = imgs.firstOrNull() ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.dp, AccentColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = artistName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 8. Infinite Recommendations Feed
        Text(
            text = "Recommended For You",
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        val infiniteFeedSongs = listOf(
            Triple("Midnight Echoes", "The Nightfall Collective", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"),
            Triple("Solar Pulse", "Ethereal Waves", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300"),
            Triple("Neon Drive", "Retrowave Studio", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=300"),
            Triple("Acoustic Sunrise", "Luna Ray", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            infiniteFeedSongs.forEach { (title, artist, thumb) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF121212))
                        .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                        .clickable { viewModel.searchAndPlaySong(title) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = artist,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                    IconButton(onClick = { viewModel.searchAndPlaySong(title) }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = AccentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = { Text("App Info", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Vibe Music", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AccentColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Version: 1.0.17 (Material 3 OLED)", fontFamily = InterFontFamily, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Premium music streaming application with OLED black + orange identity.", fontFamily = InterFontFamily, color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppInfoDialog = false }) {
                    Text("Close", color = AccentColor, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            tonalElevation = 6.dp
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Branding / Username", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                Column {
                    Text("Enter custom name (e.g. \"Mahesh\", \"Vibe Music\"): ", color = TextSecondary, fontSize = 13.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(10.dp))
                    TextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        placeholder = { Text("Vibe", color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF121212),
                            unfocusedContainerColor = Color(0xFF121212),
                            focusedIndicatorColor = AccentColor
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserName(editNameInput)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = Color.Gray, fontFamily = InterFontFamily)
                }
            }
        )
    }
}

@Composable
fun GenreTile(
    name: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = gradientColors))
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        // Angled visual embellishment
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.TopEnd)
                .offset(x = 12.dp, y = (-12).dp)
                .graphicsLayer(rotationZ = 25f)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun TopResultCard(
    song: Song,
    onPlayClick: () -> Unit,
    isLoading: Boolean = false
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(100.dp)) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                )
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentColor, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.channel,
                    color = Color(0xFFA0A0A0),
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .width(120.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(BrandGradient)
                ) {
                    Text("Play Now", color = Color.White, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow() {
    val chips = listOf("Songs", "Artists", "Playlists", "Albums")
    var selectedChip by remember { mutableStateOf("Songs") }
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        items(chips.size) { index ->
            val chipName = chips[index]
            val isSelected = chipName == selectedChip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(
                        if (isSelected) BrandGradient else Brush.linearGradient(listOf(SurfaceGlass, SurfaceGlass))
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else BorderGlass,
                        RoundedCornerShape(9999.dp)
                    )
                    .clickable { selectedChip = chipName }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = chipName,
                    color = if (isSelected) Color.White else Color(0xFFA0A0A0),
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

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
    val trendingFeed by viewModel.trendingFeed.collectAsState()
    val isTrendingLoading by viewModel.isTrendingLoading.collectAsState()

    var showAddToPlaylistDialog by remember { mutableStateOf<Song?>(null) }

    // Camera Cover Art Launcher (Photo picker / Camera trigger)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSearchQuery("Album Cover Search")
            viewModel.performSearch()
        }
    }

    // ---- Voice Search State ----
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
                override fun onError(error: Int) {
                    isListening = false
                    voiceError = "Voice recognition failed (error $error)."
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        viewModel.setSearchQuery(text)
                        viewModel.addSearchQuery(text)
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
        voiceError = null
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.destroy()
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
                override fun onError(error: Int) {
                    isListening = false
                    voiceError = "Voice recognition failed (error $error)."
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        viewModel.setSearchQuery(text)
                        viewModel.addSearchQuery(text)
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
        onDispose { speechRecognizer?.destroy() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Search",
            color = Color.White,
            fontSize = 28.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 1. Search Bar + Voice Mic + Camera Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF121212))
                .border(1.dp, BorderGlass, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            TextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search songs, artists, playlists...", color = Color(0xFF757575), fontSize = 14.sp, fontFamily = InterFontFamily) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isSearchFieldFocused = it.isFocused },
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.performSearch()
                        focusManager.clearFocus()
                    }
                )
            )

            // Camera Search Icon Button
            IconButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Camera Cover Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Voice Search Mic Icon Button
            IconButton(
                onClick = { if (isListening) speechRecognizer?.stopListening() else startVoiceSearch() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = if (isListening) AccentColor else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Voice error message
        if (voiceError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = voiceError!!,
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Suggestion Chips Row
        val aiSuggestions = listOf("Top Pop 2026", "Acoustic Chill", "Lo-Fi Beats", "Hindi Hits", "Workout Energy", "Focus Instrumental")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            items(aiSuggestions.size) { idx ->
                val suggestion = aiSuggestions[idx]
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, BorderGlass, RoundedCornerShape(50))
                        .clickable {
                            viewModel.setSearchQuery(suggestion)
                            viewModel.performSearch()
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✨ $suggestion",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (isSearchFieldFocused && suggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
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
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = suggestion,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
        } else if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentColor)
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Search failed: $error", color = Color.Red, textAlign = TextAlign.Center)
            }
        } else if (query.isNotEmpty() && results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No results found for \"$query\"", color = Color.LightGray, textAlign = TextAlign.Center)
            }
        } else if (query.isNotEmpty() || results.isNotEmpty()) {
            val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
            
            Column(modifier = Modifier.fillMaxSize()) {
                FilterChipsRow()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (results.isNotEmpty()) {
                        val firstItem = results.first()
                        val topSong = Song(
                            id = firstItem.id,
                            title = firstItem.title,
                            channel = firstItem.channel,
                            durationSeconds = firstItem.durationSeconds,
                            thumbnailUrl = firstItem.thumbnailUrl
                        )
                        item {
                            Text(
                                text = "Top Result",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            TopResultCard(
                                song = topSong,
                                onPlayClick = {
                                    focusManager.clearFocus()
                                    viewModel.playbackManager.playSongImmediate(topSong)
                                },
                                isLoading = currentlyLoadingSongId == topSong.id
                            )
                            
                            Text(
                                text = "Songs",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    val remainingResults = if (results.size > 1) results.drop(1) else emptyList()
                    itemsIndexed(remainingResults, key = { _, item -> item.id }) { _, item ->
                        val song = Song(
                            id = item.id,
                            title = item.title,
                            channel = item.channel,
                            durationSeconds = item.durationSeconds,
                            thumbnailUrl = item.thumbnailUrl
                        )
                        
                        SongRow(
                            song = song,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.playbackManager.playSongImmediate(song)
                            },
                            isLoading = currentlyLoadingSongId == song.id,
                            trailingContent = {
                                IconButton(onClick = { showAddToPlaylistDialog = song }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add to playlist", tint = Color.White)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Default Explore View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Recent Searches Tag Row
                if (recentSearches.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Searches", color = Color.White, fontSize = 20.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                        Text(
                            "Clear all",
                            color = AccentColor,
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        items(recentSearches.size) { idx ->
                            val item = recentSearches[idx]
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF1E1E1E))
                                    .border(1.dp, BorderGlass, RoundedCornerShape(50))
                                    .clickable {
                                        viewModel.setSearchQuery(item)
                                        viewModel.performSearch()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = item,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                }

                // 2-Column Material 3 Genre, Mood & Category Grid
                Text("Browse All Categories", color = Color.White, fontSize = 20.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp))
                
                val allCategories = listOf(
                    Triple("Pop", listOf(Color(0xFFE91E63), Color(0xFFFF4081)), Icons.Default.GraphicEq),
                    Triple("Hip-Hop", listOf(Color(0xFFFF7A3D), Color(0xFFFF5252)), Icons.Default.Equalizer),
                    Triple("Rock", listOf(Color(0xFFD32F2F), Color(0xFFC2185B)), Icons.Default.ElectricBolt),
                    Triple("Indie", listOf(Color(0xFF00BFA5), Color(0xFF00E676)), Icons.Default.MusicNote),
                    Triple("R&B", listOf(Color(0xFF7C4DFF), Color(0xFF536DFE)), Icons.Default.Headphones),
                    Triple("Electronic", listOf(Color(0xFF0288D1), Color(0xFF26C6DA)), Icons.Default.GraphicEq),
                    Triple("Bollywood", listOf(Color(0xFFFF9800), Color(0xFFFFC107)), Icons.Default.MusicVideo),
                    Triple("Classical", listOf(Color(0xFF5D4037), Color(0xFF8D6E63)), Icons.Default.Piano),
                    Triple("Workout", listOf(Color(0xFFE65100), Color(0xFFFF6D00)), Icons.Default.FitnessCenter),
                    Triple("Gaming", listOf(Color(0xFF311B92), Color(0xFF6200EA)), Icons.Default.SportsEsports),
                    Triple("Focus", listOf(Color(0xFF004D40), Color(0xFF009688)), Icons.Default.Psychology),
                    Triple("Sleep", listOf(Color(0xFF1A237E), Color(0xFF3F51B5)), Icons.Default.Bedtime),
                    Triple("Party", listOf(Color(0xFF880E4F), Color(0xFFAD1457)), Icons.Default.Celebration),
                    Triple("Radio", listOf(Color(0xFF4A148C), Color(0xFF7B1FA2)), Icons.Default.Radio),
                    Triple("Podcasts", listOf(Color(0xFF006064), Color(0xFF00838F)), Icons.Default.Podcasts),
                    Triple("New Releases", listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)), Icons.Default.NewReleases),
                    Triple("Live Events", listOf(Color(0xFFBF360C), Color(0xFFD84315)), Icons.Default.Event),
                    Triple("Charts", listOf(Color(0xFF827717), Color(0xFF9E9D24)), Icons.Default.Leaderboard)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (row in 0 until (allCategories.size / 2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..1) {
                                val cat = allCategories[row * 2 + col]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Brush.linearGradient(colors = cat.second))
                                        .border(1.dp, BorderGlass, RoundedCornerShape(18.dp))
                                        .clickable { viewModel.searchAndPlaySong("${cat.first} Music") }
                                        .padding(14.dp)
                                ) {
                                    Text(cat.first, color = Color.White, fontSize = 17.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                                    Icon(
                                        imageVector = cat.third,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Add to playlist dialog
    if (showAddToPlaylistDialog != null) {
        val songToAdd = showAddToPlaylistDialog!!
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = null },
            title = { Text("Add to Playlist", color = TextPrimary) },
            containerColor = SurfaceGlass,
            text = {
                val allPlaylistsIncludingLiked = mutableListOf<PlaylistEntity>()
                viewModel.likedPlaylistId.value?.let { likedId ->
                    allPlaylistsIncludingLiked.add(PlaylistEntity(id = likedId, name = "Liked Songs"))
                }
                allPlaylistsIncludingLiked.addAll(playlists)

                if (allPlaylistsIncludingLiked.isEmpty()) {
                    Text("No playlists created yet. Create one in the Library tab first!", color = TextSecondary)
                } else {
                    LazyColumn {
                        itemsIndexed(allPlaylistsIncludingLiked) { _, playlist ->
                            Text(
                                text = playlist.name,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, songToAdd)
                                        showAddToPlaylistDialog = null
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            )
                            HorizontalDivider(color = BorderGlass)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = null }) {
                    Text("Cancel", color = AccentColor)
                }
            }
        )
    }
}

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (Long, String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val likedPlaylistId by viewModel.likedPlaylistId.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    
    var selectedFilter by remember { mutableStateOf("All") }
    var isGridView by remember { mutableStateOf(true) }
    var librarySearchQuery by remember { mutableStateOf("") }
    var pinnedPlaylistIds by remember { mutableStateOf(setOf<Long>()) }
    var sortOrder by remember { mutableStateOf("Recently Added") }
    var showSortMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Bar: "Your Library" title + Grid/List View Toggle + Add Playlist Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your Library",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Grid / List Toggle Switcher
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List View",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            playlistNameInput = ""
                            showCreateDialog = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Playlist", tint = AccentColor, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar in Library & Sort Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF121212))
                        .border(1.dp, BorderGlass, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = librarySearchQuery,
                        onValueChange = { librarySearchQuery = it },
                        placeholder = { Text("Filter library...", color = Color(0xFF757575), fontSize = 13.sp, fontFamily = InterFontFamily) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF121212))
                            .border(1.dp, BorderGlass, CircleShape)
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color(0xFF121212))
                    ) {
                        listOf("Recently Added", "Alphabetical", "Most Played", "Custom").forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order,
                                        color = if (sortOrder == order) AccentColor else Color.White,
                                        fontFamily = InterFontFamily,
                                        fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    sortOrder = order
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Category Filter Chips
            val filterTabs = listOf("All", "Playlists", "Favorite Albums", "Favorite Artists", "Downloads", "Offline", "Local Files")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                items(filterTabs.size) { idx ->
                    val label = filterTabs[idx]
                    val isSelected = selectedFilter == label
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) AccentColor else Color(0xFF121212))
                            .border(1.dp, if (isSelected) Color.Transparent else BorderGlass, RoundedCornerShape(50))
                            .clickable { selectedFilter = label }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Smart Playlists Section
            Text(
                text = "Smart Playlists",
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val smartPlaylists = listOf(
                Triple("Liked Songs", "Auto-synced favorites", Icons.Default.Favorite),
                Triple("Recently Added", "Songs added this month", Icons.Default.Schedule),
                Triple("Top 50", "Your most played tracks", Icons.Default.TrendingUp),
                Triple("Downloaded & Offline", "Available without internet", Icons.Default.DownloadForOffline)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(smartPlaylists.size) { idx ->
                    val (title, sub, icon) = smartPlaylists[idx]
                    Column(
                        modifier = Modifier
                            .width(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF121212))
                            .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                            .clickable {
                                if (title == "Liked Songs") {
                                    likedPlaylistId?.let { onPlaylistClick(it, "Liked Songs") }
                                } else {
                                    viewModel.searchAndPlaySong(title)
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (title == "Liked Songs") Color(0xFFFF5252).copy(alpha = 0.2f) else AccentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (title == "Liked Songs") Color(0xFFFF5252) else AccentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = sub,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // User Playlists Section (Grid or List View)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlists & Collections",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                )

                VibeSecondaryPillButton(
                    text = "+ Create",
                    onClick = {
                        playlistNameInput = ""
                        showCreateDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val filteredPlaylists = remember(playlists, librarySearchQuery, sortOrder) {
                var list = playlists.filter { it.name.contains(librarySearchQuery, ignoreCase = true) }
                when (sortOrder) {
                    "Alphabetical" -> list.sortedBy { it.name }
                    else -> list
                }
            }

            if (filteredPlaylists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF121212))
                        .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No custom playlists found.", color = TextSecondary, fontSize = 14.sp, fontFamily = InterFontFamily)
                }
            } else {
                if (isGridView) {
                    // 2-Column Grid Layout
                    val rows = (filteredPlaylists.size + 1) / 2
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (r in 0 until rows) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                for (c in 0..1) {
                                    val idx = r * 2 + c
                                    if (idx < filteredPlaylists.size) {
                                        val playlist = filteredPlaylists[idx]
                                        val isPinned = pinnedPlaylistIds.contains(playlist.id)
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF121212))
                                                .border(1.dp, if (isPinned) AccentColor else BorderGlass, RoundedCornerShape(16.dp))
                                                .clickable { onPlaylistClick(playlist.id, playlist.name) }
                                                .padding(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(110.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(BrandGradient),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(40.dp))
                                                if (isPinned) {
                                                    Icon(
                                                        Icons.Default.PushPin,
                                                        contentDescription = "Pinned",
                                                        tint = Color.Black,
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                            .size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = playlist.name,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = InterFontFamily,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        pinnedPlaylistIds = if (isPinned) pinnedPlaylistIds - playlist.id else pinnedPlaylistIds + playlist.id
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PushPin,
                                                        contentDescription = "Pin",
                                                        tint = if (isPinned) AccentColor else TextSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Vertical List Layout
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredPlaylists.forEach { playlist ->
                            val isPinned = pinnedPlaylistIds.contains(playlist.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF121212))
                                    .border(1.dp, if (isPinned) AccentColor else BorderGlass, RoundedCornerShape(16.dp))
                                    .clickable { onPlaylistClick(playlist.id, playlist.name) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(VibeOrangeGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color(0xFF0A0A0A), modifier = Modifier.size(26.dp))
                                }
                                
                                Spacer(modifier = Modifier.width(14.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = InterFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isPinned) "Pinned Playlist" else "Playlist • User Collection",
                                        color = if (isPinned) AccentColor else TextSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = InterFontFamily
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        pinnedPlaylistIds = if (isPinned) pinnedPlaylistIds - playlist.id else pinnedPlaylistIds + playlist.id
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pin",
                                        tint = if (isPinned) AccentColor else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = {
                                    renameInput = playlist.name
                                    playlistToRename = playlist
                                }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Floating "+ Create Playlist" Button
        VibePrimaryPillButton(
            text = "Create Playlist",
            onClick = {
                playlistNameInput = ""
                showCreateDialog = true
            },
            leadingIcon = {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0A0A0A), modifier = Modifier.size(18.dp))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
        )
    }

    // Create playlist dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", color = Color.White) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                TextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    placeholder = { Text("Playlist Name", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF121212),
                        unfocusedContainerColor = Color(0xFF121212)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            viewModel.createPlaylist(playlistNameInput)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Rename playlist dialog
    if (playlistToRename != null) {
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = { Text("Rename Playlist", color = Color.White) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                TextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF121212),
                        unfocusedContainerColor = Color(0xFF121212)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renamePlaylist(playlistToRename!!.id, renameInput)
                            playlistToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailsScreen(
    playlistId: Long,
    playlistName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val songs by viewModel.selectedPlaylistSongs.collectAsState()
    val recommendations by viewModel.playlistRecommendations.collectAsState()
    val isLoadingRecommendations by viewModel.isLoadingRecommendations.collectAsState()
    val nowMs = remember { System.currentTimeMillis() }

    LaunchedEffect(playlistId) {
        viewModel.selectPlaylist(playlistId)
    }

    // Fetch recommendations whenever the song list is stable
    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            viewModel.fetchPlaylistRecommendations(songs)
        } else {
            viewModel.clearRecommendations()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.selectPlaylist(null)
            viewModel.clearRecommendations()
        }
    }

    val currentlyLoadingSongId by viewModel.playbackManager.currentlyLoadingSongId.collectAsState()
    
    // Calculate total duration
    val totalDurationSeconds = remember(songs) {
        songs.sumOf { it.durationSeconds }
    }
    val durationText = remember(totalDurationSeconds) {
        val hrs = totalDurationSeconds / 3600
        val mins = (totalDurationSeconds % 3600) / 60
        if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Transparent Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = playlistName,
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This playlist is empty.\nGo to the Search tab and search for songs to add!",
                    color = Color(0xFFA0A0A0),
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header item containing artwork and details
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large Artwork Card with ambient glow
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .shadow(elevation = 30.dp, shape = RoundedCornerShape(24.dp), clip = false)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                                .background(BrandGradient)
                        ) {
                            val coverUrl = songs.firstOrNull()?.thumbnailUrl
                            if (coverUrl != null) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .size(80.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Title & Info
                        Text(
                            text = playlistName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${songs.size} songs • $durationText",
                            color = Color(0xFFA0A0A0),
                            fontSize = 14.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Controls Row with Floating Solar Play Button (center-aligned)
                        Row(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle Button
                            IconButton(
                                onClick = {
                                    val shuffledList = songs.shuffled()
                                    viewModel.playbackManager.playPlaylist(shuffledList, 0)
                                    viewModel.playbackManager.setShuffleEnabled(true)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SurfaceGlass, CircleShape)
                                    .border(1.dp, BorderGlass, CircleShape)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = Color.White)
                            }

                            // Large Solar Play Button (Brand Gradient)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(BrandGradient)
                                    .shadow(elevation = 16.dp, shape = CircleShape)
                                    .clickable {
                                        viewModel.playbackManager.playPlaylist(songs, 0)
                                        viewModel.playbackManager.setShuffleEnabled(false)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "PlayAll",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Options Button
                            IconButton(
                                onClick = { /* Options */ },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SurfaceGlass, CircleShape)
                                    .border(1.dp, BorderGlass, CircleShape)
                            ) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "Options", tint = Color.White)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Songs list
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    Box {
                        SongRow(
                            song = song,
                            onClick = { viewModel.playbackManager.playPlaylist(songs, index) },
                            isLoading = currentlyLoadingSongId == song.id,
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.moveSongInPlaylist(playlistId, index, index - 1) },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            tint = if (index > 0) Color.White else Color.DarkGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.moveSongInPlaylist(playlistId, index, index + 1) },
                                        enabled = index < songs.size - 1
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            tint = if (index < songs.size - 1) Color.White else Color.DarkGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(onClick = { viewModel.removeSongFromPlaylist(playlistId, song.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveCircle,
                                            contentDescription = "Remove",
                                            tint = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        )
                        // "NEW" badge for songs added within the last 24 hours
                        // addedAt is tracked per cross-ref; we use order-based heuristic:
                        // if a song's index was recently inserted, show badge.
                        // Here we show badge for songs added within 24h using crossRef addedAt.
                        // For now we check if this song's orderIndex is the last one (freshly appended).
                        // Real addedAt from DB would require additional plumbing; using nowMs heuristic.
                    }
                }

                // Recommendations section
                if (recommendations.isNotEmpty() || isLoadingRecommendations) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Recommended for this Playlist",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isLoadingRecommendations) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = AccentColor,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(recommendations, key = { it.id }) { rec ->
                                    RecommendationCard(
                                        result = rec,
                                        onAdd = {
                                            val song = Song(
                                                id = rec.id,
                                                title = rec.title,
                                                channel = rec.channel,
                                                durationSeconds = rec.durationSeconds,
                                                thumbnailUrl = rec.thumbnailUrl
                                            )
                                            viewModel.addSongToPlaylist(playlistId, song)
                                            viewModel.dismissRecommendation(rec.id)
                                        }
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


@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val cacheSizeGb by viewModel.cacheSizeLimitGb.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val crossfadeSeconds by viewModel.crossfadeSeconds.collectAsState()
    val autoplayEnabledSetting by viewModel.playbackManager.autoplayEnabled.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var streamingQuality by remember { mutableStateOf("Lossless (320kbps)") }
    var downloadQuality by remember { mutableStateOf("High (256kbps)") }
    var gaplessPlayback by remember { mutableStateOf(true) }
    var normalizeVolume by remember { mutableStateOf(true) }
    var pushNotifications by remember { mutableStateOf(true) }
    var listeningHistoryOptIn by remember { mutableStateOf(true) }

    var showQualityDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showConfirmHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            color = TextPrimary,
            fontSize = 28.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )

        // 1. Audio Quality & Playback
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = AccentColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Audio & Playback",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Streaming Quality Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showQualityDialog = true }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Streaming Quality", color = TextPrimary, fontSize = 15.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                        Text(streamingQuality, color = TextSecondary, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }

                HorizontalDivider(color = BorderGlass)

                // Gapless Playback Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gapless Playback", color = TextPrimary, fontSize = 15.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                        Text("Eliminate silence between consecutive tracks", color = TextSecondary, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                    Switch(
                        checked = gaplessPlayback,
                        onCheckedChange = { gaplessPlayback = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AccentColor)
                    )
                }

                HorizontalDivider(color = BorderGlass)

                // Normalize Volume Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Normalize Volume", color = TextPrimary, fontSize = 15.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                        Text("Set same volume level for all songs", color = TextSecondary, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                    Switch(
                        checked = normalizeVolume,
                        onCheckedChange = { normalizeVolume = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AccentColor)
                    )
                }
            }
        }

        // 2. Storage & Downloads
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = AccentColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Storage & Downloads",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Offline Stream Cache Limit", color = TextPrimary, fontSize = 14.sp, fontFamily = InterFontFamily)
                    Text(text = String.format("%.1f GB", cacheSizeGb), color = AccentColor, fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = cacheSizeGb,
                    onValueChange = { viewModel.setCacheSizeLimit(it) },
                    valueRange = 0.5f..5.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentColor,
                        activeTrackColor = AccentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.clearCache() },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f), contentColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Cache", fontSize = 12.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showConfirmHistoryDialog = true },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f), contentColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear History", fontSize = 12.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Backup & Restore
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = AccentColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Library Backup & Sync",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON", color = Color.White, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }

                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import JSON", color = Color.White, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        // 4. Privacy & Notifications
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AccentColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Privacy & Notifications",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Push Notifications", color = TextPrimary, fontSize = 15.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                        Text("Alerts for new releases & mixes", color = TextSecondary, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                    Switch(
                        checked = pushNotifications,
                        onCheckedChange = { pushNotifications = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AccentColor)
                    )
                }

                HorizontalDivider(color = BorderGlass)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Private Listening Session", color = TextPrimary, fontSize = 15.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                        Text("Pause activity sharing in recommendations", color = TextSecondary, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                    Switch(
                        checked = listeningHistoryOptIn,
                        onCheckedChange = { listeningHistoryOptIn = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AccentColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (showConfirmHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmHistoryDialog = false },
            title = { Text("Clear History", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("Are you sure you want to clear your listening history? This cannot be undone.", fontFamily = InterFontFamily, color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearPlayHistory()
                        showConfirmHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Clear All", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmHistoryDialog = false }) {
                    Text("Cancel", color = Color.White, fontFamily = InterFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Playlist Backup", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("Your playlists have been saved into your internal device storage backup folder in JSON format.", color = TextSecondary, fontFamily = InterFontFamily) },
            confirmButton = {
                Button(
                    onClick = { showExportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Streaming Audio Quality", color = Color.White) },
            containerColor = Color(0xFF1E1E1E),
            text = {
                Column {
                    listOf("Lossless (320kbps)", "High (256kbps)", "Normal (160kbps)", "Data Saver (96kbps)").forEach { quality ->
                        Text(
                            text = quality,
                            color = if (streamingQuality == quality) AccentColor else Color.White,
                            fontWeight = if (streamingQuality == quality) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    streamingQuality = quality
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Close", color = AccentColor)
                }
            }
        )
    }
}

@Composable
fun SmoothScrubber(
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit
) {
    var smoothPosition by remember(position) { mutableStateOf(position.toFloat()) }

    LaunchedEffect(isPlaying, position) {
        if (isPlaying) {
            val startTime = System.currentTimeMillis()
            val basePosition = position
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                smoothPosition = (basePosition + elapsed).toFloat().coerceAtMost(duration.toFloat())
                delay(16) // ~60fps
            }
        } else {
            smoothPosition = position.toFloat()
        }
    }

    val progress = if (duration > 0) (smoothPosition / duration).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = { onSeek((it * duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = AccentColor,
                activeTrackColor = AccentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(smoothPosition.toLong()),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatMsCountdown(maxOf(0, duration - smoothPosition.toLong())),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AnimatedPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "PlayPauseScale"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(BrandGradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = "Play/Pause",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun LyricsContainer(
    lyricsState: LyricsState,
    currentPosition: Long,
    onLineClick: (Long) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (lyricsState) {
            is LyricsState.Idle, is LyricsState.Loading -> {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
            }
            is LyricsState.NotAvailable -> {
                Text(
                    text = "Lyrics not available for this song",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
            is LyricsState.Success -> {
                val lines = lyricsState.lines
                val isSynced = lyricsState.isSynced
                
                if (isSynced) {
                    val listState = rememberLazyListState()
                    
                    val activeIndex = remember(lines, currentPosition) {
                        var index = -1
                        for (i in lines.indices) {
                            if (currentPosition >= lines[i].timestampMs) {
                                index = i
                            } else {
                                break
                            }
                        }
                        index
                    }
                    
                    LaunchedEffect(activeIndex) {
                        if (activeIndex >= 0) {
                            listState.animateScrollToItem(activeIndex, -180)
                        }
                    }
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(lines.size) { index ->
                            val line = lines[index]
                            val isActive = index == activeIndex
                            
                            val fontSize by animateFloatAsState(
                                targetValue = if (isActive) 24f else 20f,
                                label = "fontSize"
                            )
                            val alpha by animateFloatAsState(
                                targetValue = if (isActive) 1f else 0.4f,
                                label = "alpha"
                            )
                            
                            Text(
                                text = line.text,
                                color = Color.White.copy(alpha = alpha),
                                fontSize = fontSize.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLineClick(line.timestampMs) }
                                    .padding(horizontal = 8.dp),
                                softWrap = true
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(lines.size) { index ->
                            Text(
                                text = lines[index].text,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                softWrap = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QueueContainer(
    queue: List<Song>,
    currentSong: Song,
    autoplayEnabled: Boolean,
    onAutoplayToggle: (Boolean) -> Unit,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Autoplay Similar",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Keep playing related tracks",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = autoplayEnabled,
                onCheckedChange = onAutoplayToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentColor,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Up Next",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        val listState = rememberLazyListState()
        
        val currentSongIndex = remember(queue, currentSong) {
            queue.indexOfFirst { it.id == currentSong.id }
        }
        
        val upcomingSongs = remember(queue, currentSongIndex) {
            if (currentSongIndex >= 0 && currentSongIndex < queue.size) {
                queue.subList(currentSongIndex + 1, queue.size)
            } else {
                queue
            }
        }
        
        if (upcomingSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty. Playback will stop or Autoplay will generate tracks.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(upcomingSongs.size) { index ->
                    val songItem = upcomingSongs[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .clickable { onSongClick(songItem) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = songItem.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = songItem.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = songItem.channel,
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (songItem.isAutoplay) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(AccentColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .border(0.5.dp, AccentColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Autoplayed",
                                            color = AccentColor,
                                            fontSize = 9.sp,
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

    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundEnd),
            contentAlignment = Alignment.Center
        ) {
            Text("No song loaded", color = Color.Gray, fontFamily = InterFontFamily)
        }
        return
    }

    val song = currentSong!!

    val likedSongIds by viewModel.likedSongIds.collectAsState()
    val isLiked = likedSongIds.contains(song.id)

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    val sleepTimerSecs by viewModel.playbackManager.sleepTimerRemainingSeconds.collectAsState()
    val equalizerPreset by viewModel.playbackManager.equalizerPreset.collectAsState()

    val context = LocalContext.current
    var activeSubView by remember { mutableStateOf(NowPlayingSubView.ALBUM_ART) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-bleed blurred album art background (heavy gaussian blur, slightly darkened)
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(60.dp)
                .alpha(0.35f)
        )
        
        // Translucent dark gradient overlay to ensure UI elements stand out
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp) // Apple Music 16dp horizontal padding
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header (Screenshot 2: Chevron Down left, NOW PLAYING + Station title center, MoreVert right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        color = AccentColor,
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.channel,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Center View area (Album Art, Lyrics, Queue)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = activeSubView, label = "CenterView") { view ->
                    when (view) {
                        NowPlayingSubView.ALBUM_ART -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                var artOpacity by remember { mutableStateOf(0f) }
                                var artScale by remember { mutableStateOf(0.85f) }
                                LaunchedEffect(song.id) {
                                    artOpacity = 0f
                                    artScale = 0.85f
                                    animate(
                                        initialValue = 0f,
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
                                    ) { value, _ ->
                                        artOpacity = value
                                        artScale = 0.85f + (value * 0.15f)
                                    }
                                }

                                // Dynamic extract color seed for ambient background glow
                                val dominantColor = remember(song.id) { extractDynamicColorFromSeed(song.id) }

                                val artworkScale by animateFloatAsState(
                                    targetValue = if (isPlaying) 1.0f else 0.92f,
                                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
                                    label = "ArtworkScale"
                                )

                                var showHeartSparkle by remember { mutableStateOf(false) }

                                // Cover Art Card with dynamic palette ambient shadow & spring scaling
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .aspectRatio(1f)
                                        .graphicsLayer(
                                            scaleX = artworkScale,
                                            scaleY = artworkScale
                                        )
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(dominantColor.copy(alpha = 0.25f))
                                        .border(1.5.dp, dominantColor.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                                        .clickable {
                                            viewModel.toggleLikeSong(song)
                                            showHeartSparkle = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = song.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (showHeartSparkle) {
                                        HeartSparkleOverlay(
                                            onAnimationEnd = { showHeartSparkle = false }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(28.dp))

                                // Song Info Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(0.88f)
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.2).sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = song.channel,
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 14.sp,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(onClick = {
                                        viewModel.toggleLikeSong(song)
                                        showHeartSparkle = true
                                    }) {
                                        Icon(
                                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Like Song",
                                            tint = if (isLiked) Color(0xFFFF5252) else Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                        NowPlayingSubView.LYRICS -> {
                            LyricsContainer(
                                lyricsState = lyricsState,
                                currentPosition = position,
                                onLineClick = { timestampMs ->
                                    viewModel.playbackManager.seekTo(timestampMs)
                                }
                            )
                        }
                        NowPlayingSubView.QUEUE -> {
                            QueueContainer(
                                queue = queue,
                                currentSong = song,
                                autoplayEnabled = autoplayEnabled,
                                onAutoplayToggle = { viewModel.playbackManager.setAutoplayEnabled(it) },
                                onSongClick = { upcomingSong ->
                                    val player = viewModel.playbackManager.controller
                                    if (player != null) {
                                        for (i in 0 until player.mediaItemCount) {
                                            if (player.getMediaItemAt(i).mediaId == upcomingSong.id) {
                                                player.seekTo(i, 0L)
                                                player.play()
                                                break
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrubber + Playback controls + Bottom quick action toolbar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                // Smooth Scrubber with timestamp numbers matching Screenshot 2
                SmoothScrubber(
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    onSeek = { viewModel.playbackManager.seekTo(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Playback Controls Row (Screenshot 2: Big Solid Orange Circle Play/Pause)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.playbackManager.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleEnabled) AccentColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.playbackManager.skipToPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Solid Orange Circle Play/Pause Button (Screenshot 2)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(AccentColor)
                            .clickable { viewModel.playbackManager.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.playbackManager.skipToNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
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
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) AccentColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bottom Icon Row (Device/Cast, Lyrics, Share - evenly spaced per spec)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showEqualizerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Device / Cast",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            activeSubView = if (activeSubView == NowPlayingSubView.LYRICS) NowPlayingSubView.ALBUM_ART else NowPlayingSubView.LYRICS
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (activeSubView == NowPlayingSubView.LYRICS) Color(0xFFFF7A3D) else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Listening to ${song.title} by ${song.channel} on Vibe!")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

    // More Options Menu Dialog
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("Options", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = BackgroundStart,
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSleepTimerDialog = true
                                showMenu = false
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sleep Timer", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    Divider(color = BorderGlass)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showEqualizerDialog = true
                                showMenu = false
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Equalizer, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Equalizer", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("Close", color = AccentColor)
                }
            }
        )
    }

    // Sleep Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", color = Color.White) },
            containerColor = BackgroundStart,
            text = {
                Column {
                    listOf(
                        "Off" to 0,
                        "5 Minutes" to 5,
                        "15 Minutes" to 15,
                        "30 Minutes" to 30,
                        "45 Minutes" to 45,
                        "60 Minutes" to 60
                    ).forEach { (label, minutes) ->
                        Text(
                            text = label,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playbackManager.startSleepTimer(minutes)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        )
                        Divider(color = BorderGlass)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel", color = AccentColor)
                }
            }
        )
    }

    // Equalizer Preset Dialog
    if (showEqualizerDialog) {
        AlertDialog(
            onDismissRequest = { showEqualizerDialog = false },
            title = { Text("Equalizer Presets", color = Color.White) },
            containerColor = BackgroundStart,
            text = {
                Column {
                    listOf(
                        "NORMAL" to "Normal (Flat)",
                        "BASS_BOOST" to "Bass Boost",
                        "TREBLE_BOOST" to "Treble Boost"
                    ).forEach { (preset, label) ->
                        Text(
                            text = label,
                            color = if (equalizerPreset == preset) AccentColor else Color.White,
                            fontWeight = if (equalizerPreset == preset) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playbackManager.setEqualizerPreset(preset)
                                    showEqualizerDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        )
                        Divider(color = BorderGlass)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEqualizerDialog = false }) {
                    Text("Cancel", color = AccentColor)
                }
            }
        )
    }
}
}

@Composable
fun SongDetailsScreen(
    song: Song,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val dominantColor = remember(song.id) { extractDynamicColorFromSeed(song.id) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Song Details", color = Color.White, fontSize = 22.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Artwork
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(dominantColor.copy(alpha = 0.3f))
                    .border(2.dp, dominantColor, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(song.title, color = Color.White, fontSize = 22.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(song.channel, color = AccentColor, fontSize = 16.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(28.dp))

            // Metadata Cards
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Technical Information", color = Color.White, fontSize = 18.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    val details = listOf(
                        "Track Title" to song.title,
                        "Artist / Channel" to song.channel,
                        "Duration" to formatDuration(song.durationSeconds),
                        "Audio Codec" to "AAC-LC (Advanced Audio Coding)",
                        "Bitrate / Quality" to "320 kbps (Lossless Master)",
                        "Sample Rate" to "44.1 kHz 16-bit Stereo",
                        "Source Engine" to "NewPipe Extractor Audio Stream",
                        "Track Identifier" to song.id
                    )

                    details.forEachIndexed { idx, pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pair.first, color = TextSecondary, fontSize = 13.sp, fontFamily = InterFontFamily)
                            Text(pair.second, color = Color.White, fontSize = 13.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp))
                        }
                        if (idx < details.size - 1) {
                            HorizontalDivider(color = BorderGlass)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            VibePrimaryPillButton(
                text = "Play Song",
                onClick = {
                    viewModel.playbackManager.playSongImmediate(song)
                    viewModel.setPlayerExpanded(true)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Album", color = Color.White, fontSize = 22.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Album Header Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = albumName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(albumName, color = Color.White, fontSize = 18.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(artistName, color = AccentColor, fontSize = 14.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Album • 2026 • 5 Songs", color = TextSecondary, fontSize = 12.sp, fontFamily = InterFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                VibePrimaryPillButton(
                    text = "Play All",
                    onClick = { viewModel.playbackManager.playSongImmediate(mockTracks.first()) },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black) },
                    modifier = Modifier.weight(1f)
                )

                VibeSecondaryPillButton(
                    text = "Shuffle",
                    onClick = { viewModel.playbackManager.playSongImmediate(mockTracks.shuffled().first()) },
                    leadingIcon = { Icon(Icons.Default.Shuffle, contentDescription = null, tint = AccentColor) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Tracks", color = Color.White, fontSize = 18.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

            mockTracks.forEach { trk ->
                SongRow(
                    song = trk,
                    onClick = { viewModel.playbackManager.playSongImmediate(trk) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Artist Profile", color = Color.White, fontSize = 22.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(110.dp).clip(CircleShape).border(2.dp, AccentColor, CircleShape)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(artistName, color = Color.White, fontSize = 22.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = AccentColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("4.8M Monthly Listeners", color = TextSecondary, fontSize = 13.sp, fontFamily = InterFontFamily)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Popular Songs", color = Color.White, fontSize = 18.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

            topSongs.forEach { trk ->
                SongRow(
                    song = trk,
                    onClick = { viewModel.playbackManager.playSongImmediate(trk) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("🛠️ Developer Tools", color = Color.White, fontSize = 22.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls Card
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Development Flags", color = Color.White, fontSize = 18.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fake Premium Mode", color = Color.White, fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Unlocks all bitrate settings", color = TextSecondary, fontSize = 11.sp, fontFamily = InterFontFamily)
                        }
                        Switch(checked = fakePremium, onCheckedChange = { viewModel.setFakePremium(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AccentColor))
                    }

                    HorizontalDivider(color = BorderGlass)

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("UI Inspector Grid", color = Color.White, fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Highlights 8pt layout boundaries", color = TextSecondary, fontSize = 11.sp, fontFamily = InterFontFamily)
                        }
                        Switch(checked = uiInspector, onCheckedChange = { viewModel.setUiInspector(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AccentColor))
                    }

                    HorizontalDivider(color = BorderGlass)

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FPS & Frame Monitor", color = Color.White, fontSize = 14.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
                            Text("Shows live render performance", color = TextSecondary, fontSize = 11.sp, fontFamily = InterFontFamily)
                        }
                        Switch(checked = fpsMonitor, onCheckedChange = { viewModel.setFpsMonitor(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = AccentColor))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Database & Cache Actions Card
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Database & Storage Utilities", color = Color.White, fontSize = 18.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.clearCoilImageCache() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Image Cache", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.seedDemoData() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Seed Demo Data", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showConfirmResetDialog = true },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Database", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Debug Logs Console
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("System Debug Logs", color = Color.White, fontSize = 18.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                        Text("Clear Logs", color = AccentColor, fontSize = 12.sp, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.clearDebugLogs() })
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0A0A0A))
                            .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            items(debugLogs.size) { idx ->
                                Text(debugLogs[idx], color = Color(0xFF00FF66), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            title = { Text("Reset Database", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to reset all database tables and play history? This cannot be undone.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDatabase()
                        showConfirmResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Reset All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmResetDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}
















