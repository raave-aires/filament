package com.raave.filament.di

import javax.inject.Qualifier

/** URL base do backend (`BuildConfig.API_BASE_URL`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

/** Escopo que vive enquanto o processo do app: tarefas que não pertencem a nenhuma tela. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** Dispatcher de I/O — injetado em vez de `Dispatchers.IO` direto pra poder ser trocado em teste. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
