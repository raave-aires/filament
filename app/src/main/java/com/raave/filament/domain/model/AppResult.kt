package com.raave.filament.domain.model

/**
 * Resultado de uma operação de domínio com erro tipado. Substitui o `kotlin.Result` usado antes,
 * cujo `Throwable` obrigava a UI a exibir `error.message` cru — em inglês quando vinha do backend,
 * sem distinguir "sem internet" de "sessão expirada".
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.value
