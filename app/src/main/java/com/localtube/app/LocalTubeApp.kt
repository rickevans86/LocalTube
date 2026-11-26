package com.localtube.app

import android.app.Application
import android.util.Log
import com.localtube.app.di.AppContainer
import java.io.File

class LocalTubeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        installCrashCatcher()
    }

    /** Writes the last uncaught exception to a file so MainActivity can show it. */
    private fun installCrashCatcher() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                File(filesDir, CRASH_FILE).writeText(
                    "LocalTube ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})\n\n" +
                        Log.getStackTraceString(throwable),
                )
            } catch (_: Exception) {
                // best effort
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        const val CRASH_FILE = "last_crash.txt"
    }
}
