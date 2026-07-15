package com.personal.mymusic.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String, // YouTube Video ID
    val title: String,
    val channel: String,
    val durationSeconds: Long,
    val thumbnailUrl: String
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["songId"])
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String,
    val orderIndex: Int
)

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deletePlaylistSongCrossRef(playlistId: Long, songId: String)

    @Query("SELECT MAX(orderIndex) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?

    @Transaction
    suspend fun addSongToPlaylist(playlistId: Long, song: SongEntity) {
        insertSong(song)
        val maxIndex = getMaxOrderIndex(playlistId) ?: -1
        insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId, song.id, maxIndex + 1))
    }

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_song_cross_ref r ON s.id = r.songId
        WHERE r.playlistId = :playlistId
        ORDER BY r.orderIndex ASC
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query("UPDATE playlist_song_cross_ref SET orderIndex = :orderIndex WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongOrder(playlistId: Long, songId: String, orderIndex: Int)

    @Transaction
    suspend fun reorderSongs(playlistId: Long, songIds: List<String>) {
        songIds.forEachIndexed { index, songId ->
            updateSongOrder(playlistId, songId, index)
        }
    }

    // Search History Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(searchQuery: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    // Play History Operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayHistoryIgnore(playHistory: PlayHistoryEntity): Long

    @Query("UPDATE play_history SET playCount = playCount + 1, timestamp = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: String, timestamp: Long)

    @Transaction
    suspend fun logPlay(song: SongEntity) {
        val timestamp = System.currentTimeMillis()
        val playHistory = PlayHistoryEntity(
            id = song.id,
            title = song.title,
            channel = song.channel,
            durationSeconds = song.durationSeconds,
            thumbnailUrl = song.thumbnailUrl,
            timestamp = timestamp,
            playCount = 1
        )
        val result = insertPlayHistoryIgnore(playHistory)
        if (result == -1L) {
            incrementPlayCount(song.id, timestamp)
        }
    }

    @Query("SELECT * FROM play_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 10): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedSongs(limit: Int = 10): Flow<List<PlayHistoryEntity>>

    @Query("SELECT channel FROM play_history GROUP BY channel ORDER BY SUM(playCount) DESC LIMIT :limit")
    fun getMostPlayedArtists(limit: Int = 10): Flow<List<String>>

    @Query("DELETE FROM play_history WHERE id = :songId")
    suspend fun deletePlayHistorySong(songId: String)
}

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey val id: String, // YouTube Video ID
    val title: String,
    val channel: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)

@Database(entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class, SearchHistoryEntity::class, PlayHistoryEntity::class], version = 3, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
