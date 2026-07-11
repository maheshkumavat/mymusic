package com.personal.mymusic.domain.model

data class Song(
    val id: String, // YouTube video ID or unique key
    val title: String,
    val channel: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val streamUrl: String? = null,
    val isAutoplay: Boolean = false
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

sealed interface LyricsState {
    object Idle : LyricsState
    object Loading : LyricsState
    data class Success(val lines: List<LyricLine>, val isSynced: Boolean) : LyricsState
    object NotAvailable : LyricsState
}

data class Playlist(
    val id: Long,
    val name: String,
    val songs: List<Song> = emptyList()
)

data class SearchResult(
    val id: String,
    val title: String,
    val channel: String,
    val durationSeconds: Long,
    val thumbnailUrl: String
)
