package com.personal.mymusic.data.cache

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object CacheManager {
    private var cacheInstance: SimpleCache? = null
    private var evictorInstance: LeastRecentlyUsedCacheEvictor? = null
    
    // Default size is 1GB
    private var maxCacheSizeBytes: Long = 1024L * 1024 * 1024

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (cacheInstance == null) {
            val cacheDir = File(context.cacheDir, "media_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val databaseProvider = StandaloneDatabaseProvider(context)
            val evictor = LeastRecentlyUsedCacheEvictor(maxCacheSizeBytes)
            evictorInstance = evictor
            cacheInstance = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cacheInstance!!
    }

    @Synchronized
    fun updateCacheSize(context: Context, newMaxSizeBytes: Long) {
        if (newMaxSizeBytes == maxCacheSizeBytes) return
        maxCacheSizeBytes = newMaxSizeBytes
        
        // Re-initialize SimpleCache with new size limit
        releaseCache()
        getCache(context)
    }

    @Synchronized
    fun clearCache() {
        cacheInstance?.let { cache ->
            try {
                cache.keys.forEach { key ->
                    cache.removeResource(key)
                }
            } catch (e: Exception) {
                android.util.Log.e("CacheManager", "Error clearing cache", e)
            }
        }
    }

    @Synchronized
    fun releaseCache() {
        try {
            cacheInstance?.release()
        } catch (e: Exception) {
            android.util.Log.e("CacheManager", "Error releasing cache", e)
        }
        cacheInstance = null
        evictorInstance = null
    }
}
