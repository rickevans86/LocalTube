package com.localtube.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.localtube.app.data.db.FolderEntity
import com.localtube.app.data.db.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Walks a SAF tree and turns video files into [VideoEntity] rows. */
class MediaScanner(private val context: Context) {

    private val videoExtensions = setOf("mp4", "mkv", "webm", "m4v", "3gp", "mov", "avi", "ts")

    /** Guesses a folder's type from its name/shape (name wins for movies). */
    suspend fun detectType(name: String, treeUri: String): FolderType = withContext(Dispatchers.IO) {
        if (name.lowercase().contains("movie")) return@withContext FolderType.MOVIES
        val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
            ?: return@withContext FolderType.VIDEOS
        val hasSubdirs = root.listFiles().any { it.isDirectory }
        if (hasSubdirs) FolderType.CHANNELS else FolderType.VIDEOS
    }

    suspend fun scan(folder: FolderEntity): List<VideoEntity> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(folder.treeUri))
            ?: return@withContext emptyList()
        val type = FolderType.fromName(folder.type)
        val out = mutableListOf<VideoEntity>()

        when (type) {
            FolderType.CHANNELS -> {
                for (child in root.listFiles()) {
                    if (child.isDirectory) {
                        collect(child, folder.id, channelName = child.name, isMovie = false, out)
                    } else {
                        addIfVideo(child, folder.id, channelName = null, isMovie = false, out)
                    }
                }
            }
            else -> collect(root, folder.id, channelName = null, isMovie = type == FolderType.MOVIES, out)
        }
        out
    }

    private fun collect(
        dir: DocumentFile,
        folderId: Long,
        channelName: String?,
        isMovie: Boolean,
        out: MutableList<VideoEntity>,
    ) {
        for (child in dir.listFiles()) {
            if (child.isDirectory) {
                collect(child, folderId, channelName, isMovie, out)
            } else {
                addIfVideo(child, folderId, channelName, isMovie, out)
            }
        }
    }

    private fun addIfVideo(
        file: DocumentFile,
        folderId: Long,
        channelName: String?,
        isMovie: Boolean,
        out: MutableList<VideoEntity>,
    ) {
        val name = file.name ?: return
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext !in videoExtensions) return
        out += VideoEntity(
            documentUri = file.uri.toString(),
            folderId = folderId,
            channelName = channelName,
            title = prettyTitle(name),
            // Duration is not read during the scan (it was the bottleneck on large
            // libraries). The badge is simply hidden when unknown.
            durationMs = 0L,
            isMovie = isMovie,
            addedAt = System.currentTimeMillis(),
        )
    }

    private fun prettyTitle(fileName: String): String =
        fileName.substringBeforeLast('.')
            .replace('_', ' ')
            .replace('.', ' ')
            .trim()
}
