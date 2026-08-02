@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package com.personal.mymusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import coil.compose.AsyncImage
import com.personal.mymusic.domain.model.Song
import android.content.Context
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.composed
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

fun Modifier.bounceClick(onClick: () -> Unit) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
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

// Vibe Design System Palette (Pure Dark Near-Black Theme)
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

val DarkVibeColors = AppColors(
    background = Color(0xFF000000), // OLED Pure Black
    surface = Color(0xFF121212),
    surfaceGlass = Color(0xCC1A1A1A),
    surfaceLow = Color(0xFF0D0D0D),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB3B3B3),
    accent = Color(0xFFFF7A3D),
    accentSecondary = Color(0xFFFF9E66),
    border = Color(0x2AFFFFFF),
    isDark = true
)

val LightColors = DarkVibeColors // Vibe design system is strictly true dark theme

val DarkOledColors = DarkVibeColors

val LocalAppColors = staticCompositionLocalOf { LightColors }

@Composable
fun MyMusicTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val targetColors = DarkOledColors
    
    val animDuration = 400
    val bgAnim by animateColorAsState(targetColors.background, tween(animDuration), label = "bg")
    val surfaceAnim by animateColorAsState(targetColors.surface, tween(animDuration), label = "surface")
    val glassAnim by animateColorAsState(targetColors.surfaceGlass, tween(animDuration), label = "glass")
    val surfaceLowAnim by animateColorAsState(targetColors.surfaceLow, tween(animDuration), label = "surfaceLow")
    val textPrimaryAnim by animateColorAsState(targetColors.textPrimary, tween(animDuration), label = "textPrimary")
    val textSecondaryAnim by animateColorAsState(targetColors.textSecondary, tween(animDuration), label = "textSecondary")
    val accentAnim by animateColorAsState(targetColors.accent, tween(animDuration), label = "accent")
    val accentSecondaryAnim by animateColorAsState(targetColors.accentSecondary, tween(animDuration), label = "accentSecondary")
    val borderAnim by animateColorAsState(targetColors.border, tween(animDuration), label = "border")

    val animatedAppColors = AppColors(
        background = bgAnim,
        surface = surfaceAnim,
        surfaceGlass = glassAnim,
        surfaceLow = surfaceLowAnim,
        textPrimary = textPrimaryAnim,
        textSecondary = textSecondaryAnim,
        accent = accentAnim,
        accentSecondary = accentSecondaryAnim,
        border = borderAnim,
        isDark = true
    )

    CompositionLocalProvider(LocalAppColors provides animatedAppColors) {
        content()
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
            .background(LocalAppColors.current.background),
        content = content
    )
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp), // 24dp container radius per Stitch spec
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, BorderGlass, shape)
            .clip(shape),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceGlass
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(song.thumbnailUrl)
                .crossfade(true)
                .size(150, 150)
                .build(),
            contentDescription = "Thumbnail for ${song.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp)) // 16dp rounded corners per Stitch component spec
                .background(SurfaceContainerLow)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.channel,
                color = TextSecondary,
                fontSize = 13.sp,
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
                color = TextSecondary,
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
                tint = TextSecondary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1E1E1E))
        ) {
            DropdownMenuItem(
                text = { Text("Play Now", color = Color.White, fontFamily = InterFontFamily) },
                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentColor) },
                onClick = { expanded = false; onPlay() }
            )
            DropdownMenuItem(
                text = { Text("Add to Playlist", color = Color.White, fontFamily = InterFontFamily) },
                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = AccentColor) },
                onClick = { expanded = false; onAddToPlaylist() }
            )
            DropdownMenuItem(
                text = { Text("Song Details", color = Color.White, fontFamily = InterFontFamily) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = AccentColor) },
                onClick = { expanded = false; onSongDetails() }
            )
            DropdownMenuItem(
                text = { Text("View Album", color = Color.White, fontFamily = InterFontFamily) },
                leadingIcon = { Icon(Icons.Default.Album, contentDescription = null, tint = AccentColor) },
                onClick = { expanded = false; onViewAlbum() }
            )
            DropdownMenuItem(
                text = { Text("View Artist", color = Color.White, fontFamily = InterFontFamily) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AccentColor) },
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
    colors = listOf(Color(0xFFFF7A3D), Color(0xFFFFB380))
)

@Composable
fun VibePrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .bounceClick(onClick = onClick)
            .clip(RoundedCornerShape(50))
            .background(VibeOrangeGradient)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = Color(0xFF0A0A0A),
                fontSize = 14.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VibeSecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .bounceClick(onClick = onClick)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun VibeOrangePlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    isPlaying: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .bounceClick(onClick = onClick)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(VibeOrangeGradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color(0xFF0A0A0A),
            modifier = Modifier.size(iconSize)
        )
    }
}

// Modern Shimmer Loader for Skeleton State
@Composable
fun ShimmerBrush(
    targetValue: Float = 1000f,
    showShimmer: Boolean = true
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color(0xFF161616),
            Color(0xFF262626),
            Color(0xFF161616)
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shimmer)
        )
        Spacer(modifier = Modifier.width(16.dp))
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

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    val shimmer = ShimmerBrush()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(shimmer)
    )
}

// Extract Vibrant/Dominant Dynamic Gradient for Now Playing & Home Cards
fun extractDynamicColorFromSeed(seedStr: String): Color {
    val hash = seedStr.hashCode()
    val hue = (kotlin.math.abs(hash) % 360).toFloat()
    return Color.hsv(hue = hue, saturation = 0.65f, value = 0.45f)
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
            tint = Color(0xFFFF5252),
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


