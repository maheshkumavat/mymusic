package com.personal.mymusic.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LyricsService(private val client: OkHttpClient) {

    data class LyricsResponse(
        val plainLyrics: String?,
        val syncedLyrics: String?
    )

    suspend fun fetchLyrics(title: String, artist: String, durationSeconds: Long): LyricsResponse? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTitle(title)
        val cleanArtist = cleanArtist(artist)
        
        android.util.Log.d("LyricsService", "Fetching lyrics for original='$title'/'$artist' -> clean='$cleanTitle'/'$cleanArtist'")

        // 1. Try search with track_name and artist_name
        var url = "https://lrclib.net/api/search?track_name=${URLEncoder.encode(cleanTitle, "UTF-8")}&artist_name=${URLEncoder.encode(cleanArtist, "UTF-8")}"
        var lyrics = parseSearchResponse(url, durationSeconds)
        if (lyrics != null) return@withContext lyrics
        
        // 2. Try search with query (q)
        url = "https://lrclib.net/api/search?q=${URLEncoder.encode("$cleanTitle $cleanArtist", "UTF-8")}"
        lyrics = parseSearchResponse(url, durationSeconds)
        if (lyrics != null) return@withContext lyrics

        // 3. Fallback: try raw title/artist just in case cleaning was too aggressive
        url = "https://lrclib.net/api/search?q=${URLEncoder.encode("$title $artist", "UTF-8")}"
        lyrics = parseSearchResponse(url, durationSeconds)
        
        return@withContext lyrics
    }

    private fun parseSearchResponse(url: String, targetDuration: Long): LyricsResponse? {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MyMusicApp/1.0 (https://github.com/personal/mymusic)")
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null
                val jsonArray = JSONArray(bodyString)
                if (jsonArray.length() == 0) return null
                
                // Find best result: closest duration
                var bestIndex = -1
                var minDiff = Long.MAX_VALUE
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val duration = obj.optDouble("duration", 0.0).toLong()
                    val diff = Math.abs(duration - targetDuration)
                    if (diff < minDiff) {
                        minDiff = diff
                        bestIndex = i
                    }
                }
                
                if (bestIndex != -1) {
                    val bestObj = jsonArray.getJSONObject(bestIndex)
                    val plainLyrics = bestObj.optString("plainLyrics", "").takeIf { it.isNotEmpty() }
                    val syncedLyrics = bestObj.optString("syncedLyrics", "").takeIf { it.isNotEmpty() }
                    
                    if (plainLyrics != null || syncedLyrics != null) {
                        android.util.Log.d("LyricsService", "Found lyrics (synced=${syncedLyrics != null}) with duration diff of $minDiff seconds")
                        return LyricsResponse(plainLyrics, syncedLyrics)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LyricsService", "Error calling lrclib: ${e.message}", e)
        }
        return null
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\(official video\\)"), "")
            .replace(Regex("(?i)\\[official music video\\]"), "")
            .replace(Regex("(?i)\\(lyrics\\)"), "")
            .replace(Regex("(?i)\\(lyric video\\)"), "")
            .replace(Regex("(?i)official audio"), "")
            .replace(Regex("(?i)\\(official audio\\)"), "")
            .replace(Regex("(?i)\\[hd\\]"), "")
            .replace(Regex("(?i)\\|.*"), "") // remove anything after pipe
            .replace(Regex("(?i)-.*"), "") // remove anything after dash
            .trim()
    }

    private fun cleanArtist(artist: String): String {
        return artist
            .replace(" - Topic", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .replace("official", "", ignoreCase = true)
            .trim()
    }
}
