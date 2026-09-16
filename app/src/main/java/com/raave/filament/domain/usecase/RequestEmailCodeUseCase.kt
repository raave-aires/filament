package com.raave.filament.domain.usecase

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.repository.AuthRepository
import javax.inject.Inject

class RequestEmailCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): AppResult<Unit> {
        val normalized = email.trim()
        if (!EmailPattern.matches(normalized)) return AppResult.Failure(AppError.InvalidEmail)
        return authRepository.requestEmailCode(normalized)
    }

    private companion object {
        // Só barra erro de digitação óbvio; quem valida de verdade é o envio do código. Regex
        // própria em vez de android.util.Patterns pra manter o domínio testável sem Android.
        val EmailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
    }
}
