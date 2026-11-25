package com.localtube.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FolderEntity::class, VideoEntity::class, WatchEventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LocalTubeDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun videoDao(): VideoDao
    abstract fun watchDao(): WatchDao
}
