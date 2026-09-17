package com.raave.filament.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.raave.filament.data.local.FilamentDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FilamentDatabase =
        Room.databaseBuilder<FilamentDatabase>(context, FilamentDatabase.NAME)
            // SQLite do próprio Android: sem biblioteca nativa extra no APK.
            .setDriver(AndroidSQLiteDriver())
            // Cache reconstruível pela rede (ver FilamentDatabase): mudar o esquema só força nova sincronização.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
}
