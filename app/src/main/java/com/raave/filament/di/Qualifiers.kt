package com.raave.filament.di

import javax.inject.Qualifier

/** URL base do backend (`BuildConfig.API_BASE_URL`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

/** Dispatcher de I/O — injetado em vez de `Dispatchers.IO` direto pra poder ser trocado em teste. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
