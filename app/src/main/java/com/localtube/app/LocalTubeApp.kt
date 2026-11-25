package com.localtube.app

import android.app.Application
import com.localtube.app.di.AppContainer

class LocalTubeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
