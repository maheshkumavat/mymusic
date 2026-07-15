@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package com.personal.mymusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.mymusic.MyApplication
import com.personal.mymusic.domain.model.Song

import android.content.Context
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontLoadingStrategy

class AssetFont(
    val path: String,
    override val weight: FontWeight,
    override val style: FontStyle = FontStyle.Normal
) : AndroidFont(
    loadingStrategy = FontLoadingStrategy.Blocking,
    typefaceLoader = AssetTypefaceLoader
) {
}

object AssetTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        val assetFont = font as? AssetFont ?: return null
        return Typeface.createFromAsset(context.assets, assetFont.path)
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
        return loadBlocking(context, font)
    }
}

// Define the Inter Font Family — loaded from assets via custom TypefaceLoader to bypass resource system issues in release builds
val InterFontFamily = FontFamily(
    AssetFont("fonts/inter_regular.ttf", FontWeight.Normal),
    AssetFont("fonts/inter_medium.ttf", FontWeight.Medium),
    AssetFont("fonts/inter_semibold.ttf", FontWeight.SemiBold),
    AssetFont("fonts/inter_bold.ttf", FontWeight.Bold)
)

// Curated Echo Dark/Glassmorphic color palette
val BackgroundStart = Color(0xFF000000) // Strictly OLED Black
val BackgroundEnd = Color(0xFF000000)   // Strictly OLED Black
val AccentColor = Color(0xFFFFB000)     // Brand Accent (Amber)
val AccentGradientStart = Color(0xFFFFB000) // Brand Coral/Amber
val AccentGradientEnd = Color(0xFFFF4D00)   // Warm Coral-Orange
val SurfaceGlass = Color(0xBF121414)    // Frosted glass surface (75% opacity #121414)
val BorderGlass = Color(0x1AFFFFFF)     // Translucent thin border (10% White)

val BrandGradient = Brush.linearGradient(
    colors = listOf(AccentGradientStart, AccentGradientEnd)
)

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black), // Strictly OLED Black Canvas
        content = content
    )
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp), // 24dp for rounded-xl shape
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, BorderGlass, shape)
            .clip(shape),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceGlass
        ),
        content = content
    )
}

@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = "Thumbnail for ${song.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)) // 12dp round corners for thumbnails
                .background(Color.DarkGray)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 15.sp, // Apple Music list title size
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.channel,
                color = Color(0xFFA0A0A0), // Echo Medium Gray
                fontSize = 13.sp, // Apple Music list subtitle size
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        if (isLoading) {
            CircularProgressIndicator(
                color = AccentColor,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = formatDuration(song.durationSeconds),
                color = Color(0xFFA0A0A0), // Echo Medium Gray
                fontSize = 12.sp,
                fontFamily = InterFontFamily
            )
        }
        
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
