package com.localtube.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY sortOrder, name")
    suspend fun getAll(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun delete(folderId: Long)
}

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(videos: List<VideoEntity>)

    @Query("DELETE FROM videos WHERE folderId = :folderId")
    suspend fun deleteByFolder(folderId: Long)

    /** Shuffled home feed across every visible folder. */
    @Query(
        "SELECT v.* FROM videos v JOIN folders f ON v.folderId = f.id " +
            "WHERE v.hidden = 0 AND f.visible = 1 ORDER BY RANDOM() LIMIT :limit"
    )
    fun observeShuffled(limit: Int): Flow<List<VideoEntity>>

    @Query(
        "SELECT v.* FROM videos v JOIN folders f ON v.folderId = f.id " +
            "WHERE v.hidden = 0 AND f.visible = 1 AND v.isMovie = 1 ORDER BY v.title"
    )
    fun observeMovies(): Flow<List<VideoEntity>>

    @Query(
        "SELECT v.channelName AS name, v.folderId AS folderId, COUNT(*) AS episodeCount " +
            "FROM videos v JOIN folders f ON v.folderId = f.id " +
            "WHERE v.hidden = 0 AND f.visible = 1 AND v.channelName IS NOT NULL " +
            "GROUP BY v.folderId, v.channelName ORDER BY v.channelName"
    )
    fun observeChannels(): Flow<List<ChannelRow>>

    @Query(
        "SELECT * FROM videos WHERE folderId = :folderId AND channelName = :channel " +
            "AND hidden = 0 ORDER BY title"
    )
    fun observeChannelEpisodes(folderId: Long, channel: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderId = :folderId AND hidden = 0 ORDER BY title")
    fun observeByFolder(folderId: Long): Flow<List<VideoEntity>>

    @Query("UPDATE videos SET hidden = :hidden WHERE documentUri = :uri")
    suspend fun setHidden(uri: String, hidden: Boolean)
}

@Dao
interface WatchDao {
    @Insert
    suspend fun insert(event: WatchEventEntity)

    @Query("SELECT COALESCE(SUM(watchedMs), 0) FROM watch_events WHERE startedAt >= :since")
    suspend fun watchedMsSince(since: Long): Long
}
