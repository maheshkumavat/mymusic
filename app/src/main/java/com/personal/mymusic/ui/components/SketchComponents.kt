package com.personal.mymusic.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.mymusic.ui.screens.AppScreen

object SketchColors {
    val Ink = Color(0xFF1C1C28)
    val Background = Color(0xFFFFFFFF)
    val FaintCircle = Color(0xFFEEF1F8)
    val Orange = Color(0xFFF6B96B)
    val Teal = Color(0xFF4DB6C1)
    val Purple = Color(0xFF9B86D6)
    val Green = Color(0xFF7FCF9A)
    val HeartRed = Color(0xFFE8616A)
    val MutedText = Color(0xFF666666)
    val CardBg = Color(0xFFFFFFFF)
    val CardAlt = Color(0xFFF9FAFD)
}

val SlabColors = listOf(
    SketchColors.Orange,
    SketchColors.Teal,
    SketchColors.Purple,
    SketchColors.Green
)

fun slabColorForIndex(index: Int): Color = SlabColors[kotlin.math.abs(index) % SlabColors.size]

val AlternatingTilts = listOf(-2.0f, 2.2f, -1.8f, 2.0f, -2.4f, 1.7f)
fun tiltForIndex(index: Int): Float = AlternatingTilts[kotlin.math.abs(index) % AlternatingTilts.size]

@Composable
fun Pin(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, CircleShape)
            .border(2.dp, SketchColors.Ink, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.3f)
                .background(SketchColors.Ink, CircleShape)
        )
    }
}

@Composable
fun SketchCard(
    modifier: Modifier = Modifier,
    slabColor: Color = SketchColors.Teal,
    slabHeight: Dp = 5.dp,
    shape: CornerBasedShape = RoundedCornerShape(18.dp),
    backgroundColor: Color = SketchColors.CardBg,
    inkBorderColor: Color = SketchColors.Ink,
    borderWidth: Dp = 2.dp,
    rotation: Float = 0f,
    showPin: Boolean = false,
    pinAlignment: Alignment = Alignment.TopStart,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .padding(bottom = slabHeight)
    ) {
        // Underneath colored slab with 2dp ink border
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = slabHeight)
                .background(slabColor, shape)
                .border(borderWidth, inkBorderColor, shape)
        )
        // Main paper card with 2dp ink border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
                .background(backgroundColor, shape)
                .border(borderWidth, inkBorderColor, shape)
                .clip(shape),
            content = content
        )
        if (showPin) {
            Pin(
                modifier = Modifier
                    .align(pinAlignment)
                    .offset(x = 6.dp, y = 6.dp)
            )
        }
    }
}

@Composable
fun SketchButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SketchColors.Orange,
    contentColor: Color = SketchColors.Ink,
    shape: CornerBasedShape = RoundedCornerShape(18.dp),
    borderWidth: Dp = 2.dp,
    dropOffset: Dp = 3.dp,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .padding(bottom = dropOffset, end = dropOffset)
            .bounceClick(onClick = onClick)
    ) {
        // Drop slab layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = dropOffset, y = dropOffset)
                .background(SketchColors.Ink, shape)
        )
        // Main button
        Row(
            modifier = Modifier
                .background(backgroundColor, shape)
                .border(borderWidth, SketchColors.Ink, shape)
                .clip(shape)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SketchIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SketchColors.Orange,
    contentColor: Color = SketchColors.Ink,
    shape: Shape = RoundedCornerShape(12.dp),
    size: Dp = 44.dp,
    dropOffset: Dp = 3.dp,
    borderWidth: Dp = 2.dp,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(bottom = dropOffset, end = dropOffset)
            .size(size)
    ) {
        // Drop shadow / slab
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = dropOffset, y = dropOffset)
                .background(SketchColors.Ink, shape)
        )
        // Main icon button
        Box(
            modifier = Modifier
                .matchParentSize()
                .bounceClick(onClick = onClick)
                .background(backgroundColor, shape)
                .border(borderWidth, SketchColors.Ink, shape)
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
            }
        }
    }
}

@Composable
fun SketchPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    iconSize: Dp = 26.dp,
    backgroundColor: Color = SketchColors.Orange
) {
    SketchIconButton(
        onClick = onClick,
        modifier = modifier,
        backgroundColor = backgroundColor,
        contentColor = SketchColors.Ink,
        shape = CircleShape,
        size = size,
        dropOffset = 3.dp,
        borderWidth = 2.dp
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = SketchColors.Ink,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun SketchTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBackClick != null) {
            SketchIconButton(
                onClick = onBackClick,
                backgroundColor = SketchColors.Orange,
                shape = RoundedCornerShape(12.dp),
                size = 40.dp
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = SketchColors.Ink
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = title,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = SketchColors.Ink,
            modifier = Modifier.weight(1f),
            textAlign = if (onBackClick == null) TextAlign.Start else TextAlign.Center
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            actions()
        }
    }
}

@Composable
fun SketchSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search Music",
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = {
        Icon(Icons.Default.Search, contentDescription = null, tint = SketchColors.Ink)
    },
    trailingIcon: @Composable (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(2.dp, SketchColors.Ink, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(12.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = SketchColors.MutedText,
                        fontFamily = PoppinsFontFamily,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = SketchColors.Ink,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

@Composable
fun SketchTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(index) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    color = if (selected) SketchColors.Ink else SketchColors.MutedText,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (selected) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(4.dp)
                            .background(SketchColors.Orange, RoundedCornerShape(2.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(4.dp)
                            .background(Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
fun SketchBottomNav(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val dropOffset = 4.dp
    val barShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Underneath ink drop slab
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 0.dp, y = dropOffset)
                .background(SketchColors.Ink, barShape)
        )
        // Teal bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SketchColors.Teal, barShape)
                .border(2.dp, SketchColors.Ink, barShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SketchNavItem(
                selected = currentScreen is AppScreen.Home,
                icon = Icons.Default.Home,
                label = "Home",
                onClick = { onNavigate(AppScreen.Home) }
            )
            SketchNavItem(
                selected = currentScreen is AppScreen.Search,
                icon = Icons.Default.Search,
                label = "Search",
                onClick = { onNavigate(AppScreen.Search) }
            )
            SketchNavItem(
                selected = currentScreen is AppScreen.Library || currentScreen is AppScreen.PlaylistDetails,
                icon = Icons.Default.LibraryMusic,
                label = "Library",
                onClick = { onNavigate(AppScreen.Library) }
            )
            SketchNavItem(
                selected = currentScreen is AppScreen.Settings,
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = { onNavigate(AppScreen.Settings) }
            )
        }
    }
}

@Composable
fun SketchNavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    if (selected) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .bounceClick(onClick = onClick)
                .background(SketchColors.Orange, RoundedCornerShape(12.dp))
                .border(2.dp, SketchColors.Ink, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = SketchColors.Ink,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FaintCirclesBackground(
    modifier: Modifier = Modifier,
    showArcs: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SketchColors.Background)
            .drawBehind {
                // Faint decorative circles
                drawCircle(
                    color = SketchColors.FaintCircle,
                    radius = 160.dp.toPx(),
                    center = Offset(-40.dp.toPx(), 180.dp.toPx())
                )
                drawCircle(
                    color = SketchColors.FaintCircle,
                    radius = 120.dp.toPx(),
                    center = Offset(size.width + 30.dp.toPx(), 360.dp.toPx())
                )
                drawCircle(
                    color = SketchColors.FaintCircle,
                    radius = 100.dp.toPx(),
                    center = Offset(size.width * 0.2f, size.height - 80.dp.toPx())
                )

                if (showArcs) {
                    // Large decorative teal and orange arcs behind playlist/library
                    val strokeTeal = Stroke(width = 18.dp.toPx())
                    val strokeOrange = Stroke(width = 18.dp.toPx())

                    // Teal arc
                    drawArc(
                        color = SketchColors.Teal,
                        startAngle = -85f,
                        sweepAngle = 170f,
                        useCenter = false,
                        topLeft = Offset(-130.dp.toPx(), size.height * 0.16f),
                        size = Size(280.dp.toPx(), 440.dp.toPx()),
                        style = strokeTeal
                    )
                    // Orange inner arc
                    drawArc(
                        color = SketchColors.Orange,
                        startAngle = -85f,
                        sweepAngle = 170f,
                        useCenter = false,
                        topLeft = Offset(-110.dp.toPx(), size.height * 0.19f),
                        size = Size(240.dp.toPx(), 390.dp.toPx()),
                        style = strokeOrange
                    )
                }
            },
        content = content
    )
}

@Composable
fun WaveformSeekBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barsCount: Int = 30
) {
    // Waveform heights pattern
    val heights = remember {
        listOf(
            0.3f, 0.5f, 0.8f, 0.4f, 0.7f, 0.9f, 0.6f, 0.35f, 0.75f, 1.0f,
            0.85f, 0.45f, 0.6f, 0.95f, 0.7f, 0.55f, 0.8f, 0.4f, 0.65f, 0.9f,
            0.5f, 0.3f, 0.75f, 0.85f, 0.45f, 0.6f, 0.9f, 0.7f, 0.4f, 0.3f
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onProgressChange(fraction)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onProgressChange(fraction)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalBars = barsCount.coerceAtMost(heights.size)
            for (i in 0 until totalBars) {
                val barFraction = i.toFloat() / totalBars.toFloat()
                val isPlayed = barFraction <= progress
                val heightFactor = heights[i % heights.size]
                val barHeight = 12.dp + (32.dp * heightFactor)

                Box(
                    modifier = Modifier
                        .width(4.5.dp)
                        .height(barHeight)
                        .background(
                            if (isPlayed) SketchColors.Teal else SketchColors.Teal.copy(alpha = 0.3f),
                            RoundedCornerShape(3.dp)
                        )
                        .border(
                            1.dp,
                            if (isPlayed) SketchColors.Ink else SketchColors.Ink.copy(alpha = 0.3f),
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}
