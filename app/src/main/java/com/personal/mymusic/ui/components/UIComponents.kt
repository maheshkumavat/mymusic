@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package com.personal.mymusic.ui.components

import android.content.Context
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.personal.mymusic.R
import com.personal.mymusic.domain.model.Song

fun Modifier.bounceClick(onClick: () -> Unit) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        awaitRelease()
                    } finally {
                        isPressed = false
                    }
                },
                onTap = { onClick() }
            )
        }
}

class AssetFont(
    val path: String,
    override val weight: FontWeight,
    override val style: FontStyle = FontStyle.Normal
) : AndroidFont(
    loadingStrategy = FontLoadingStrategy.Blocking,
    typefaceLoader = AssetTypefaceLoader
)

object AssetTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        val assetFont = font as? AssetFont ?: return null
        return try {
            Typeface.createFromAsset(context.assets, assetFont.path)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
        return loadBlocking(context, font)
    }
}

// Poppins Font Family - Primary font for Sketch design system
val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

// Legacy alias to Poppins
val InterFontFamily = PoppinsFontFamily

// Sketch Palette
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val surfaceLow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentSecondary: Color,
    val border: Color,
    val isDark: Boolean
)

val SketchLightPalette = AppColors(
    background = SketchColors.Background,
    surface = Color.White,
    surfaceGlass = Color.White,
    surfaceLow = SketchColors.CardAlt,
    textPrimary = SketchColors.Ink,
    textSecondary = SketchColors.MutedText,
    accent = SketchColors.Orange,
    accentSecondary = SketchColors.Teal,
    border = SketchColors.Ink,
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { SketchLightPalette }

@Composable
fun MyMusicTheme(
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppColors provides SketchLightPalette) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = SketchColors.Orange,
                secondary = SketchColors.Teal,
                background = SketchColors.Background,
                surface = Color.White,
                onPrimary = SketchColors.Ink,
                onBackground = SketchColors.Ink,
                onSurface = SketchColors.Ink
            )
        ) {
            content()
        }
    }
}

val BackgroundStart: Color @Composable get() = LocalAppColors.current.background
val BackgroundEnd: Color @Composable get() = LocalAppColors.current.surfaceLow
val AccentColor: Color @Composable get() = LocalAppColors.current.accent
val AccentGradientStart: Color @Composable get() = LocalAppColors.current.accent
val AccentGradientEnd: Color @Composable get() = LocalAppColors.current.accentSecondary

val TextPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalAppColors.current.textSecondary
val SurfaceGlass: Color @Composable get() = LocalAppColors.current.surfaceGlass
val SurfaceContainerLow: Color @Composable get() = LocalAppColors.current.surfaceLow
val BorderGlass: Color @Composable get() = LocalAppColors.current.border

val BrandGradient: Brush @Composable get() = Brush.linearGradient(
    colors = listOf(SketchColors.Orange, SketchColors.Teal)
)

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    FaintCirclesBackground(modifier = modifier, content = content)
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    slabColor: Color = SketchColors.Teal,
    content: @Composable ColumnScope.() -> Unit
) {
    SketchCard(
        modifier = modifier,
        slabColor = slabColor,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    slabColor: Color = SketchColors.Teal,
    rotation: Float = 0f,
    showPin: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null
) {
    SketchCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        slabColor = slabColor,
        rotation = rotation,
        showPin = showPin,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with ink border
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(song.thumbnailUrl)
                    .crossfade(true)
                    .size(150, 150)
                    .build(),
                contentDescription = "Thumbnail for ${song.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
                    .background(SketchColors.FaintCircle)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    color = SketchColors.Ink,
                    fontSize = 15.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.channel,
                    color = SketchColors.MutedText,
                    fontSize = 13.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = SketchColors.Orange,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = formatDuration(song.durationSeconds),
                    color = SketchColors.MutedText,
                    fontSize = 12.sp,
                    fontFamily = PoppinsFontFamily
                )
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(6.dp))
                trailingContent()
            }
        }
    }
}

@Composable
fun SongOverflowMenu(
    song: Song,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSongDetails: () -> Unit,
    onViewAlbum: () -> Unit,
    onViewArtist: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Song options for ${song.title}",
                tint = SketchColors.Ink
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White)
                .border(2.dp, SketchColors.Ink, RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                text = { Text("Play Now", color = SketchColors.Ink, fontFamily = PoppinsFontFamily) },
                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SketchColors.Orange) },
                onClick = { expanded = false; onPlay() }
            )
            DropdownMenuItem(
                text = { Text("Add to Playlist", color = SketchColors.Ink, fontFamily = PoppinsFontFamily) },
                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = SketchColors.Teal) },
                onClick = { expanded = false; onAddToPlaylist() }
            )
            DropdownMenuItem(
                text = { Text("Song Details", color = SketchColors.Ink, fontFamily = PoppinsFontFamily) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = SketchColors.Purple) },
                onClick = { expanded = false; onSongDetails() }
            )
            DropdownMenuItem(
                text = { Text("View Album", color = SketchColors.Ink, fontFamily = PoppinsFontFamily) },
                leadingIcon = { Icon(Icons.Default.Album, contentDescription = null, tint = SketchColors.Teal) },
                onClick = { expanded = false; onViewAlbum() }
            )
            DropdownMenuItem(
                text = { Text("View Artist", color = SketchColors.Ink, fontFamily = PoppinsFontFamily) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SketchColors.Green) },
                onClick = { expanded = false; onViewArtist() }
            )
        }
    }
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0:00"
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}

val VibeOrangeGradient = Brush.horizontalGradient(
    colors = listOf(SketchColors.Orange, Color(0xFFFFD399))
)

@Composable
fun VibePrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    SketchButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        backgroundColor = SketchColors.Orange,
        leadingIcon = leadingIcon
    )
}

@Composable
fun VibeSecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    SketchButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        backgroundColor = Color.White,
        leadingIcon = leadingIcon
    )
}

@Composable
fun VibeOrangePlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    isPlaying: Boolean = false
) {
    SketchPlayButton(
        isPlaying = isPlaying,
        onClick = onClick,
        modifier = modifier,
        size = size,
        iconSize = iconSize
    )
}

@Composable
fun ShimmerBrush(
    targetValue: Float = 1000f,
    showShimmer: Boolean = true
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color(0xFFEEEEEE),
            Color(0xFFDDDDDD),
            Color(0xFFEEEEEE)
        )
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset(translateAnimation, translateAnimation),
            end = androidx.compose.ui.geometry.Offset(translateAnimation + 300f, translateAnimation + 300f)
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }
}

@Composable
fun SkeletonSongRow(modifier: Modifier = Modifier) {
    val shimmer = ShimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmer)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
            }
        }
    }
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    val shimmer = ShimmerBrush()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(shimmer)
            .border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
    )
}

fun extractDynamicColorFromSeed(seedStr: String): Color {
    val hash = seedStr.hashCode()
    val hue = (kotlin.math.abs(hash) % 360).toFloat()
    return Color.hsv(hue = hue, saturation = 0.55f, value = 0.85f)
}

@Composable
fun HeartSparkleOverlay(
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        androidx.compose.animation.core.animate(
            initialValue = 0.4f,
            targetValue = 1.3f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
        ) { value, _ -> scale = value }

        androidx.compose.animation.core.animate(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        ) { value, _ -> alpha = value }

        onAnimationEnd()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = SketchColors.HeartRed,
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                )
        )
    }
}
