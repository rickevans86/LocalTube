package com.localtube.app.data

/** How a main folder's contents are presented to kids. */
enum class FolderType {
    /** Flat list of videos with landscape thumbnails. */
    VIDEOS,

    /** Movies shown as portrait posters. */
    MOVIES,

    /** Each immediate subfolder is treated as a channel (a show). */
    CHANNELS;

    companion object {
        fun fromName(name: String): FolderType =
            entries.firstOrNull { it.name == name } ?: VIDEOS
    }
}
