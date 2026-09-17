package com.raave.filament

import android.app.Application
import com.raave.filament.data.local.SessionCacheCleaner
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FilamentApplication : Application() {

    @Inject
    lateinit var sessionCacheCleaner: SessionCacheCleaner

    override fun onCreate() {
        super.onCreate()
        sessionCacheCleaner.start()
    }
}
