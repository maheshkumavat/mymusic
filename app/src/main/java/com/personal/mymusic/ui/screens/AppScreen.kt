package com.personal.mymusic.ui.screens

import com.personal.mymusic.domain.model.Song

sealed class AppScreen {
    object Home : AppScreen()
    object Search : AppScreen()
    object Library : AppScreen()
    data class PlaylistDetails(val playlistId: Long, val playlistName: String) : AppScreen()
    object Settings : AppScreen()
    data class AlbumDetails(val albumId: String, val albumName: String, val artistName: String, val coverUrl: String) : AppScreen()
    data class ArtistDetails(val artistId: String, val artistName: String, val avatarUrl: String) : AppScreen()
    data class SongDetails(val song: Song) : AppScreen()
    data class CategoryDetails(val categoryName: String) : AppScreen()
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
