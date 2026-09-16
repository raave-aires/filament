package com.raave.filament.domain.usecase

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyEmailCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, code: String): AppResult<Unit> {
        val normalizedCode = code.trim()
        if (normalizedCode.length != CODE_LENGTH || !normalizedCode.all(Char::isDigit)) {
            return AppResult.Failure(AppError.InvalidCode)
        }
        return authRepository.verifyEmailCode(email.trim(), normalizedCode)
    }

    companion object {
        const val CODE_LENGTH = 6
    }
}
