package com.raave.filament.domain.usecase

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.repository.TicketRepository
import javax.inject.Inject

class CreateTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
) {
    /** Devolve o número do chamado criado. */
    suspend operator fun invoke(title: String, description: String): AppResult<Long> {
        val normalizedTitle = title.trim()
        val normalizedDescription = description.trim()
        if (normalizedTitle.isEmpty() || normalizedDescription.isEmpty()) {
            return AppResult.Failure(AppError.EmptyTicketFields)
        }
        return ticketRepository.createTicket(normalizedTitle, normalizedDescription)
    }
}
