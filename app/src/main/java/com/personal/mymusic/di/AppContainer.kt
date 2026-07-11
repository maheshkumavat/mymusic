package com.personal.mymusic.di

import android.content.Context
import com.personal.mymusic.data.database.MusicDatabase
import com.personal.mymusic.data.database.PlaylistDao
import com.personal.mymusic.data.network.LyricsService
import com.personal.mymusic.data.network.NewPipeService
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val database: MusicDatabase by lazy {
        MusicDatabase.getDatabase(context)
    }

    val playlistDao: PlaylistDao by lazy {
        database.playlistDao()
    }

    val newPipeService: NewPipeService by lazy {
        NewPipeService()
    }

    val lyricsService: LyricsService by lazy {
        LyricsService(okHttpClient)
    }

    val updateManager: com.personal.mymusic.data.network.UpdateManager by lazy {
        com.personal.mymusic.data.network.UpdateManager(context, okHttpClient)
    }
}
