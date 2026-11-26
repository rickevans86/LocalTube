package com.localtube.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A parent-chosen source folder. Its [type] decides how its contents are shown. */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Persisted SAF tree URI (takePersistableUriPermission). */
    val treeUri: String,
    /** One of FolderType.name: VIDEOS, MOVIES, CHANNELS. */
    val type: String,
    val visible: Boolean = true,
    val sortOrder: Int = 0,
)

/** One playable video file discovered under a folder. */
@Entity(
    tableName = "videos",
    indices = [Index("folderId"), Index("channelName")],
)
data class VideoEntity(
    /** Stable document URI string — survives rescans. */
    @PrimaryKey val documentUri: String,
    val folderId: Long,
    /** Immediate subfolder name when the folder is a CHANNELS folder, else null. */
    val channelName: String?,
    val title: String,
    val durationMs: Long,
    val isMovie: Boolean,
    val hidden: Boolean = false,
    val addedAt: Long,
)

/** A single playback session, used for history and screen-time accounting. */
@Entity(tableName = "watch_events")
data class WatchEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoUri: String,
    val startedAt: Long,
    val watchedMs: Long,
)

/** Projection: a channel (a show folder) with its episode count. */
data class ChannelRow(
    val folderId: Long,
    val name: String,
    val episodeCount: Int,
)

/**
 * Artwork/metadata mapping for a movie or a series, fetched from Cinemeta/Metahub.
 * itemKey is the movie's documentUri, or "series:<folderId>:<channelName>".
 */
@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val itemKey: String,
    val kind: String,          // "movie" or "series"
    val query: String,
    val imdbId: String?,
    val title: String?,
    val year: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val manual: Boolean = false,
    val updatedAt: Long = 0,
)
