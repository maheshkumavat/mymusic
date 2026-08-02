package com.personal.mymusic.data.network

import com.personal.mymusic.domain.model.SearchResult
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class NewPipeService {
    private val youtubeService: YoutubeService
        get() = ServiceList.YouTube as YoutubeService

    @Throws(Exception::class)
    private suspend fun searchRaw(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val searchExtractor = youtubeService.getSearchExtractor(query) as SearchExtractor
            searchExtractor.fetchPage()
            
            searchExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .map { item ->
                    val videoId = when {
                        item.url.contains("v=") -> item.url.substringAfter("v=").substringBefore("&")
                        item.url.contains("youtu.be/") -> item.url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                        else -> item.url.substringAfterLast("/")
                    }
                    SearchResult(
                        id = videoId,
                        title = item.name ?: "Unknown Title",
                        channel = item.uploaderName ?: "Unknown Channel",
                        durationSeconds = item.duration,
                        thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: ""
                    )
                }
        } catch (e: Exception) {
            android.util.Log.e("NewPipeService", "Error during searchRaw for: $query", e)
            throw e
        }
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return@withContext emptyList()
        try {
            val suggestionExtractor = youtubeService.suggestionExtractor
            suggestionExtractor.suggestionList(trimmedQuery)
        } catch (e: Exception) {
            android.util.Log.e("NewPipeService", "Error fetching search suggestions for: $trimmedQuery", e)
            emptyList()
        }
    }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return@withContext emptyList()

        var results = searchRaw(trimmedQuery)
        
        // If results are empty or very few, try fallback query expansions and spelling retry
        if (results.size < 3) {
            val cleanQuery = trimmedQuery.replace(Regex("[^a-zA-Z0-9\\s]"), "").replace(Regex("\\s+"), " ").trim()
            val variations = mutableListOf<String>()
            
            if (cleanQuery != trimmedQuery && cleanQuery.isNotEmpty()) {
                variations.add(cleanQuery)
            }
            variations.add("$trimmedQuery song")
            if (cleanQuery.isNotEmpty() && cleanQuery != trimmedQuery) {
                variations.add("$cleanQuery song")
            }

            android.util.Log.d("NewPipeService", "Initial search returned ${results.size} results. Retrying with variations: $variations")

            coroutineScope {
                val deferredResults = variations.map { variation ->
                    async { searchRaw(variation) }
                }
                val variationResultsList = deferredResults.map { it.await() }
                results = (results + variationResultsList.flatten()).distinctBy { it.id }
            }
        }

        results.take(20)
    }

    suspend fun getTrendingFeed(): Map<String, List<SearchResult>> = coroutineScope {
        val categories = mapOf(
            "Top Hits" to "top hits",
            "Hindi Trends" to "trending hindi songs",
            "Punjabi Trends" to "trending punjabi songs",
            "English Pop" to "trending english songs",
            "Tamil Melodies" to "trending tamil songs"
        )
        
        categories.mapValues { (_, query) ->
            async(Dispatchers.IO) {
                try {
                    search(query).take(10)
                } catch (e: Exception) {
                    android.util.Log.e("NewPipeService", "Error loading trending category: $query", e)
                    emptyList<SearchResult>()
                }
            }
        }.mapValues { it.value.await() }
    }

    @Throws(IOException::class)
    suspend fun getAudioStreamUrl(videoId: String): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("NewPipeService", "PLAYBACK_TIMING: getAudioStreamUrl start for videoId=$videoId at t=$startTime")
        val videoUrl = "https://www.youtube.com/watch?v=$videoId"
        val streamExtractor = youtubeService.getStreamExtractor(videoUrl)
        streamExtractor.fetchPage()
        
        val fetchDuration = System.currentTimeMillis() - startTime
        android.util.Log.d("NewPipeService", "PLAYBACK_TIMING: fetchPage completed in ${fetchDuration}ms for videoId=$videoId")
        
        val audioStreams = streamExtractor.audioStreams
        if (audioStreams.isNullOrEmpty()) {
            throw IOException("No audio streams found for video $videoId. Total video: ${streamExtractor.videoStreams?.size}, videoOnly: ${streamExtractor.videoOnlyStreams?.size}")
        }
        
        // Prefer M4A / AAC audio streams for fastest ExoPlayer start & hardware decoding
        val bestStream = audioStreams
            .filter { (it.format?.name ?: "").contains("m4a", ignoreCase = true) || (it.format?.name ?: "").contains("mp4", ignoreCase = true) }
            .maxByOrNull { it.bitrate }
            ?: audioStreams.maxByOrNull { it.bitrate }
            ?: audioStreams.first()
            
        val url = bestStream.url ?: throw IOException("Audio stream URL is null")
        val totalDuration = System.currentTimeMillis() - startTime
        android.util.Log.d("NewPipeService", "PLAYBACK_TIMING: getAudioStreamUrl resolved in ${totalDuration}ms for videoId=$videoId (Format=${bestStream.format?.name}, Bitrate=${bestStream.bitrate})")
        url
    }
}

