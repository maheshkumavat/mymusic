package com.personal.mymusic.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.personal.mymusic.data.cache.CacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CacheCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // SimpleCache automatically manages LRU eviction, but we can log stats or run additional cleanup here
            val cache = CacheManager.getCache(applicationContext)
            android.util.Log.d("CacheCleanupWorker", "Periodic cache check executed. Total cached keys: ${cache.keys.size}")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("CacheCleanupWorker", "Failed to run periodic cache cleanup", e)
            Result.failure()
        }
    }
}
