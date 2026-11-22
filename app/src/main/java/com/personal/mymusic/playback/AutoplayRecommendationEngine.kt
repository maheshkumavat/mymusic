package com.personal.mymusic.playback

import android.content.Context
import com.personal.mymusic.data.database.PlaylistDao
import com.personal.mymusic.data.network.NewPipeService
import com.personal.mymusic.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AutoplayRecommendationEngine(
    private val context: Context,
    private val newPipeService: NewPipeService,
    private val playlistDao: PlaylistDao
) {
    private val prefs = context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)

    @Synchronized
    fun getTasteProfile(): MutableMap<String, Int> {
        val jsonStr = prefs.getString("taste_profile_json", "{}") ?: "{}"
        val map = mutableMapOf<String, Int>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getInt(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    @Synchronized
    fun saveTasteProfile(map: Map<String, Int>) {
        try {
            val json = JSONObject()
            map.forEach { (artist, score) ->
                json.put(artist, score)
            }
            prefs.edit().putString("taste_profile_json", json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun recordLiked(artist: String) {
        val clean = cleanArtistName(artist)
        if (clean.isBlank()) return
        val profile = getTasteProfile()
        profile[clean] = (profile[clean] ?: 0) + 3
        saveTasteProfile(profile)
    }

    fun recordSkipUnder30Seconds(artist: String) {
        val clean = cleanArtistName(artist)
        if (clean.isBlank()) return
        val profile = getTasteProfile()
        profile[clean] = (profile[clean] ?: 0) - 2
        saveTasteProfile(profile)
    }

    fun recordPlayedOver50Percent(artist: String) {
        val clean = cleanArtistName(artist)
        if (clean.isBlank()) return
        val profile = getTasteProfile()
        profile[clean] = (profile[clean] ?: 0) + 1
        saveTasteProfile(profile)
    }

    fun recordPlayedToEnd(artist: String) {
        val clean = cleanArtistName(artist)
        if (clean.isBlank()) return
        val profile = getTasteProfile()
        profile[clean] = (profile[clean] ?: 0) + 2
        saveTasteProfile(profile)
    }

    suspend fun buildRecommendations(
        currentSong: Song?,
        currentQueue: List<Song>
    ): List<Song> = withContext(Dispatchers.IO) {
        try {
            val recentPlays = try {
                playlistDao.getRecentlyPlayed(50).first().map { it.id }.toSet()
            } catch (e: Exception) {
                emptySet()
            }
            val queueIds = currentQueue.map { it.id }.toSet()
            val excludedIds = (recentPlays + queueIds).toMutableSet()
            if (currentSong != null) {
                excludedIds.add(currentSong.id)
            }

            // Seeds: current song + last 5 played + top 3 taste artists
            val seedQueries = mutableListOf<String>()

            if (currentSong != null) {
                val cleanArtist = cleanArtistName(currentSong.channel)
                val cleanTitle = cleanSongTitle(currentSong.title)
                if (cleanArtist.isNotBlank()) {
                    seedQueries.add("$cleanArtist songs")
                    seedQueries.add("$cleanArtist mix")
                }
                if (cleanTitle.isNotBlank()) {
                    seedQueries.add("$cleanTitle similar songs")
                }
            }

            // Last 5 played
            try {
                val last5 = playlistDao.getRecentlyPlayed(5).first()
                for (play in last5) {
                    val a = cleanArtistName(play.channel)
                    if (a.isNotBlank() && seedQueries.none { it.contains(a, ignoreCase = true) }) {
                        seedQueries.add("$a songs")
                    }
                }
            } catch (e: Exception) {}

            // Top 3 artists from taste profile
            val tasteProfile = getTasteProfile()
            val topArtists = tasteProfile.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }
            for (artist in topArtists) {
                if (seedQueries.none { it.contains(artist, ignoreCase = true) }) {
                    seedQueries.add("$artist songs")
                }
            }

            if (seedQueries.isEmpty()) {
                seedQueries.add("trending songs")
                seedQueries.add("top hits music")
            }

            val collectedSongs = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()

            // Query searches
            for (q in seedQueries.take(4)) {
                try {
                    val searchResults = newPipeService.search(q)
                    for (res in searchResults) {
                        if (res.id in excludedIds || res.id in seenIds) continue

                        // Filter out duration > 10 min (600s) or < 60s
                        if (res.durationSeconds > 600 || (res.durationSeconds in 1..59)) continue

                        // Filter non-songs
                        val titleLower = res.title.lowercase()
                        if (titleLower.contains("live stream") ||
                            titleLower.contains("full album") ||
                            titleLower.contains("jukebox") ||
                            titleLower.contains("1 hour") ||
                            titleLower.contains("reaction") ||
                            titleLower.contains("compilation")
                        ) continue

                        seenIds.add(res.id)
                        collectedSongs.add(
                            Song(
                                id = res.id,
                                title = res.title,
                                channel = res.channel,
                                durationSeconds = res.durationSeconds,
                                thumbnailUrl = res.thumbnailUrl,
                                isAutoplay = true
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Silently ignore network failures per spec
                }
            }

            if (collectedSongs.isEmpty()) {
                // Fall back to trending feed
                try {
                    val feed = newPipeService.getTrendingFeed()
                    for ((_, songs) in feed) {
                        for (res in songs) {
                            if (res.id in excludedIds || res.id in seenIds) continue
                            if (res.durationSeconds > 600 || (res.durationSeconds in 1..59)) continue
                            seenIds.add(res.id)
                            collectedSongs.add(
                                Song(
                                    id = res.id,
                                    title = res.title,
                                    channel = res.channel,
                                    durationSeconds = res.durationSeconds,
                                    thumbnailUrl = res.thumbnailUrl,
                                    isAutoplay = true
                                )
                            )
                        }
                    }
                } catch (e: Exception) {}
            }

            // Rank by taste score: 70% familiar, 30% discovery
            val familiar = mutableListOf<Song>()
            val discovery = mutableListOf<Song>()

            for (song in collectedSongs) {
                val artistKey = cleanArtistName(song.channel)
                val score = tasteProfile[artistKey] ?: 0
                if (score > 0) {
                    familiar.add(song)
                } else if (score >= 0) {
                    discovery.add(song)
                }
            }

            // Sort familiar by taste score
            familiar.sortByDescending { tasteProfile[cleanArtistName(it.channel)] ?: 0 }

            val targetCount = 10
            val familiarTarget = (targetCount * 0.7).toInt().coerceAtLeast(1)
            val discoveryTarget = targetCount - familiarTarget

            val resultList = mutableListOf<Song>()
            var fIndex = 0
            var dIndex = 0

            while (resultList.size < targetCount && (fIndex < familiar.size || dIndex < discovery.size)) {
                // Add familiar
                if (fIndex < familiar.size && (resultList.count { it in familiar } < familiarTarget || dIndex >= discovery.size)) {
                    val candidate = familiar[fIndex++]
                    if (canAddArtist(resultList, candidate.channel)) {
                        resultList.add(candidate)
                    }
                } else if (dIndex < discovery.size) {
                    val candidate = discovery[dIndex++]
                    if (canAddArtist(resultList, candidate.channel)) {
                        resultList.add(candidate)
                    }
                } else if (fIndex < familiar.size) {
                    val candidate = familiar[fIndex++]
                    if (canAddArtist(resultList, candidate.channel)) {
                        resultList.add(candidate)
                    }
                } else {
                    break
                }
            }

            resultList.take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun canAddArtist(currentList: List<Song>, channel: String): Boolean {
        if (currentList.size < 2) return true
        val last1 = cleanArtistName(currentList[currentList.size - 1].channel)
        val last2 = cleanArtistName(currentList[currentList.size - 2].channel)
        val current = cleanArtistName(channel)
        return !(last1.equals(current, ignoreCase = true) && last2.equals(current, ignoreCase = true))
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .replace(" - Topic", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .trim()
    }

    private fun cleanSongTitle(title: String): String {
        return title
            .replace(Regex("\\((.*?)\\)"), "")
            .replace(Regex("\\[(.*?)\\]"), "")
            .trim()
    }
}
