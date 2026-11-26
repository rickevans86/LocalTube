package com.localtube.app.data

import com.localtube.app.data.db.ChannelRow
import com.localtube.app.data.db.FolderDao
import com.localtube.app.data.db.FolderEntity
import com.localtube.app.data.db.MetaDao
import com.localtube.app.data.db.MetaEntity
import com.localtube.app.data.db.VideoDao
import com.localtube.app.data.db.VideoEntity
import com.localtube.app.data.db.WatchDao
import com.localtube.app.data.db.WatchEventEntity
import kotlinx.coroutines.flow.Flow

class ContentRepository(
    private val folderDao: FolderDao,
    private val videoDao: VideoDao,
    private val watchDao: WatchDao,
    private val metaDao: MetaDao,
    private val scanner: MediaScanner,
) {
    val homeFeed: Flow<List<VideoEntity>> = videoDao.observeShuffled(limit = 60)
    val movies: Flow<List<VideoEntity>> = videoDao.observeMovies()
    val channels: Flow<List<ChannelRow>> = videoDao.observeChannels()
    val folders: Flow<List<FolderEntity>> = folderDao.observeAll()
    val recent: Flow<List<VideoEntity>> = videoDao.observeRecent()
    val meta: Flow<List<MetaEntity>> = metaDao.observeAll()

    fun channelEpisodes(folderId: Long, channel: String): Flow<List<VideoEntity>> =
        videoDao.observeChannelEpisodes(folderId, channel)

    fun videosInFolder(folderId: Long): Flow<List<VideoEntity>> =
        videoDao.observeByFolder(folderId)

    fun search(query: String): Flow<List<VideoEntity>> = videoDao.search(query)

    /** Adds a folder, auto-detecting its type from name/shape, then scans it. */
    suspend fun addFolder(name: String, treeUri: String): Long {
        val type = scanner.detectType(name, treeUri)
        val id = folderDao.upsert(FolderEntity(name = name, treeUri = treeUri, type = type.name))
        rescanFolder(FolderEntity(id = id, name = name, treeUri = treeUri, type = type.name))
        return id
    }

    suspend fun setFolderVisible(folder: FolderEntity, visible: Boolean) =
        folderDao.update(folder.copy(visible = visible))

    suspend fun setFolderType(folder: FolderEntity, type: FolderType) =
        folderDao.update(folder.copy(type = type.name))

    suspend fun removeFolder(folderId: Long) {
        videoDao.deleteByFolder(folderId)
        folderDao.delete(folderId)
    }

    suspend fun rescanAll() {
        for (folder in folderDao.getAll()) rescanFolder(folder)
    }

    /**
     * Full refresh: re-walk every folder (adds new files, drops missing ones —
     * no files on disk are touched) and fill in artwork for anything new.
     */
    suspend fun refreshLibrary() {
        rescanAll()
        matchAll(onlyMissing = true)
    }

    suspend fun rescanFolder(folder: FolderEntity) {
        val videos = scanner.scan(folder)
        if (videos.isEmpty()) {
            videoDao.deleteByFolder(folder.id)
            return
        }
        // Drop only files that vanished, then upsert the rest — this avoids
        // wiping the folder (which made the home screen flash on every refresh).
        videoDao.deleteMissing(folder.id, videos.map { it.documentUri })
        videoDao.upsertAll(videos)
    }

    suspend fun setHidden(uri: String, hidden: Boolean) = videoDao.setHidden(uri, hidden)

    suspend fun recordWatch(uri: String, startedAt: Long, watchedMs: Long) =
        watchDao.insert(WatchEventEntity(videoUri = uri, startedAt = startedAt, watchedMs = watchedMs))

    suspend fun saveMeta(meta: MetaEntity) = metaDao.upsert(meta)

    /**
     * Matches movies and series to artwork; always keeps manual fixes.
     * When [onlyMissing] is true, items that already have artwork are skipped
     * (used by the automatic refresh so it doesn't re-hit the network for
     * everything); when false, every non-manual item is re-matched.
     */
    suspend fun matchAll(onlyMissing: Boolean = false) {
        for (movie in videoDao.getMovies()) {
            val key = movie.documentUri
            val existing = metaDao.get(key)
            if (existing?.manual == true) continue
            if (onlyMissing && existing != null) continue
            val query = NameParser.cleanTitle(movie.title)
            val hint = NameParser.imdbId(movie.title)
            ArtworkClient.match("movie", key, query, hint)?.let { metaDao.upsert(it) }
        }
        for (channel in videoDao.getChannelsOnce()) {
            val key = seriesKey(channel.folderId, channel.name)
            val existing = metaDao.get(key)
            if (existing?.manual == true) continue
            if (onlyMissing && existing != null) continue
            val query = NameParser.cleanTitle(channel.name)
            val hint = NameParser.imdbId(channel.name)
            ArtworkClient.match("series", key, query, hint)?.let { metaDao.upsert(it) }
        }
    }

    companion object {
        fun seriesKey(folderId: Long, channelName: String) = "series:$folderId:$channelName"
    }
}
