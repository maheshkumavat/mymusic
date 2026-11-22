package com.personal.mymusic.data.category

import android.content.Context
import com.personal.mymusic.data.network.NewPipeService
import com.personal.mymusic.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

data class CategorySongsState(
    val latest: List<Song> = emptyList(),
    val trending: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CategoryRepository(
    private val context: Context,
    private val newPipeService: NewPipeService
) {
    private val prefs = context.getSharedPreferences("music_category_cache", Context.MODE_PRIVATE)
    private val memoryCache = ConcurrentHashMap<String, Pair<Long, CategorySongsState>>()
    private val TTL_MS = 6 * 60 * 60 * 1000L // 6 hours

    suspend fun getCategorySongs(categoryName: String, forceRefresh: Boolean = false): CategorySongsState = withContext(Dispatchers.IO) {
        val cacheKey = categoryName.lowercase().trim()
        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            // Check memory cache
            val mem = memoryCache[cacheKey]
            if (mem != null && (now - mem.first) < TTL_MS) {
                return@withContext mem.second
            }

            // Check disk cache in SharedPreferences
            val diskJson = prefs.getString(cacheKey, null)
            val diskTime = prefs.getLong("${cacheKey}_time", 0L)
            if (diskJson != null && (now - diskTime) < TTL_MS) {
                try {
                    val parsed = parseCategoryJson(diskJson)
                    memoryCache[cacheKey] = Pair(diskTime, parsed)
                    return@withContext parsed
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Fetch fresh data in parallel
        try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            coroutineScope {
                val latestDeferred = async(Dispatchers.IO) {
                    fetchTabSongs(
                        queries = listOf(
                            "new $categoryName songs $currentYear",
                            "$categoryName new releases",
                            "latest $categoryName songs"
                        )
                    )
                }

                val trendingDeferred = async(Dispatchers.IO) {
                    fetchTabSongs(
                        queries = listOf(
                            "trending $categoryName songs",
                            "$categoryName top hits",
                            "$categoryName viral songs"
                        )
                    )
                }

                val latestList = latestDeferred.await()
                val trendingList = trendingDeferred.await()

                if (latestList.isEmpty() && trendingList.isEmpty()) {
                    throw IllegalStateException("No songs found for $categoryName")
                }

                val resultState = CategorySongsState(
                    latest = latestList,
                    trending = trendingList,
                    isLoading = false,
                    error = null
                )

                // Save to caches
                memoryCache[cacheKey] = Pair(now, resultState)
                try {
                    val jsonStr = serializeCategoryJson(resultState)
                    prefs.edit()
                        .putString(cacheKey, jsonStr)
                        .putLong("${cacheKey}_time", now)
                        .apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                resultState
            }
        } catch (e: Exception) {
            // Return cached data if available on network error
            val diskJson = prefs.getString(cacheKey, null)
            if (diskJson != null) {
                try {
                    val parsed = parseCategoryJson(diskJson)
                    return@withContext parsed
                } catch (_: Exception) {}
            }
            CategorySongsState(
                isLoading = false,
                error = e.localizedMessage ?: "Failed to load $categoryName"
            )
        }
    }

    private suspend fun fetchTabSongs(queries: List<String>): List<Song> {
        val collected = mutableListOf<Song>()
        val seenIds = mutableSetOf<String>()

        for (q in queries) {
            try {
                val results = newPipeService.search(q)
                for (res in results) {
                    if (res.id in seenIds) continue

                    // Duration filter: 60s to 600s
                    if (res.durationSeconds > 600 || (res.durationSeconds in 1..59)) continue

                    // Non-songs filter
                    val t = res.title.lowercase()
                    if (t.contains("live stream") ||
                        t.contains("full album") ||
                        t.contains("jukebox") ||
                        t.contains("1 hour") ||
                        t.contains("reaction") ||
                        t.contains("compilation")
                    ) continue

                    seenIds.add(res.id)
                    collected.add(
                        Song(
                            id = res.id,
                            title = res.title,
                            channel = res.channel,
                            durationSeconds = res.durationSeconds,
                            thumbnailUrl = res.thumbnailUrl
                        )
                    )
                    if (collected.size >= 25) break
                }
            } catch (e: Exception) {
                // Silently try next query
            }
            if (collected.size >= 25) break
        }

        return collected.take(25)
    }

    private fun serializeCategoryJson(state: CategorySongsState): String {
        val root = JSONObject()
        val latestArr = JSONArray()
        state.latest.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("channel", s.channel)
                put("durationSeconds", s.durationSeconds)
                put("thumbnailUrl", s.thumbnailUrl)
            }
            latestArr.put(obj)
        }
        val trendingArr = JSONArray()
        state.trending.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("channel", s.channel)
                put("durationSeconds", s.durationSeconds)
                put("thumbnailUrl", s.thumbnailUrl)
            }
            trendingArr.put(obj)
        }
        root.put("latest", latestArr)
        root.put("trending", trendingArr)
        return root.toString()
    }

    private fun parseCategoryJson(jsonStr: String): CategorySongsState {
        val root = JSONObject(jsonStr)
        val latestList = mutableListOf<Song>()
        val trendingList = mutableListOf<Song>()

        val latestArr = root.optJSONArray("latest")
        if (latestArr != null) {
            for (i in 0 until latestArr.length()) {
                val obj = latestArr.getJSONObject(i)
                latestList.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        channel = obj.getString("channel"),
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        thumbnailUrl = obj.optString("thumbnailUrl", "")
                    )
                )
            }
        }

        val trendingArr = root.optJSONArray("trending")
        if (trendingArr != null) {
            for (i in 0 until trendingArr.length()) {
                val obj = trendingArr.getJSONObject(i)
                trendingList.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        channel = obj.getString("channel"),
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        thumbnailUrl = obj.optString("thumbnailUrl", "")
                    )
                )
            }
        }

        return CategorySongsState(
            latest = latestList,
            trending = trendingList,
            isLoading = false,
            error = null
        )
    }
}
