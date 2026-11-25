package com.localtube.app.di

import android.content.Context
import androidx.room.Room
import com.localtube.app.core.ScreenTimeManager
import com.localtube.app.data.ContentRepository
import com.localtube.app.data.MediaScanner
import com.localtube.app.data.SettingsStore
import com.localtube.app.data.db.LocalTubeDatabase
import com.localtube.app.update.UpdateChecker

/** Lightweight manual dependency container (no Hilt for the initial scaffold). */
class AppContainer(context: Context) {

    private val database: LocalTubeDatabase = Room.databaseBuilder(
        context.applicationContext,
        LocalTubeDatabase::class.java,
        "localtube.db",
    ).fallbackToDestructiveMigration().build()

    val settings: SettingsStore = SettingsStore(context.applicationContext)

    private val scanner = MediaScanner(context.applicationContext)

    val repository: ContentRepository = ContentRepository(
        folderDao = database.folderDao(),
        videoDao = database.videoDao(),
        watchDao = database.watchDao(),
        scanner = scanner,
    )

    val screenTime: ScreenTimeManager = ScreenTimeManager(database.watchDao(), settings)

    val updateChecker: UpdateChecker = UpdateChecker()
}
