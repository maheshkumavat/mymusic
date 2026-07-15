package com.personal.mymusic

import android.app.Application
import com.personal.mymusic.data.network.NewPipeDownloader
import com.personal.mymusic.di.AppContainer
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization

import androidx.work.*
import com.personal.mymusic.worker.CacheCleanupWorker
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    companion object {
        lateinit var instance: MyApplication
            private set
    }

    lateinit var container: AppContainer

    override fun onCreate() {
        instance = this
        super.onCreate()

        // Set global crash handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("MyApplication", "CRASH DETECTED on thread ${thread.name}", throwable)
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post {
                android.widget.Toast.makeText(
                    applicationContext,
                    "An unexpected error occurred: ${throwable.localizedMessage ?: throwable.message ?: "Unknown error"}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            try {
                Thread.sleep(3000)
            } catch (e: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        container = AppContainer(this)
        
        // Initialize NewPipeExtractor with our custom OkHttp downloader
        NewPipe.init(NewPipeDownloader(container.okHttpClient), Localization("EN", "us"))

        // Pre-warm SimpleCache on a background thread.
        // CacheManager.getCache() opens a SQLite database (StandaloneDatabaseProvider) which
        // is slow on first run. Doing it here ensures it's ready before PlaybackService starts,
        // preventing an ANR from main-thread blocking inside PlaybackService.onCreate().
        Thread {
            try {
                com.personal.mymusic.data.cache.CacheManager.getCache(applicationContext)
            } catch (e: Exception) {
                android.util.Log.e("MyApplication", "Cache pre-warm failed", e)
            }
        }.also { it.name = "CacheWarmup"; it.isDaemon = true; it.start() }
        
        // Schedule periodic cache cleanup using WorkManager
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<CacheCleanupWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CacheCleanupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
}
