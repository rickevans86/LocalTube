package com.localtube.app.data

import com.localtube.app.data.db.ChannelRow
import com.localtube.app.data.db.FolderDao
import com.localtube.app.data.db.FolderEntity
import com.localtube.app.data.db.VideoDao
import com.localtube.app.data.db.VideoEntity
import com.localtube.app.data.db.WatchDao
import com.localtube.app.data.db.WatchEventEntity
import kotlinx.coroutines.flow.Flow

class ContentRepository(
    private val folderDao: FolderDao,
    private val videoDao: VideoDao,
    private val watchDao: WatchDao,
    private val scanner: MediaScanner,
) {
    val homeFeed: Flow<List<VideoEntity>> = videoDao.observeShuffled(limit = 60)
    val movies: Flow<List<VideoEntity>> = videoDao.observeMovies()
    val channels: Flow<List<ChannelRow>> = videoDao.observeChannels()
    val folders: Flow<List<FolderEntity>> = folderDao.observeAll()

    fun channelEpisodes(folderId: Long, channel: String): Flow<List<VideoEntity>> =
        videoDao.observeChannelEpisodes(folderId, channel)

    suspend fun addFolder(name: String, treeUri: String, type: FolderType): Long {
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

    suspend fun rescanFolder(folder: FolderEntity) {
        val videos = scanner.scan(folder)
        videoDao.deleteByFolder(folder.id)
        videoDao.upsertAll(videos)
    }

    suspend fun setHidden(uri: String, hidden: Boolean) = videoDao.setHidden(uri, hidden)

    suspend fun recordWatch(uri: String, startedAt: Long, watchedMs: Long) =
        watchDao.insert(WatchEventEntity(videoUri = uri, startedAt = startedAt, watchedMs = watchedMs))
}
