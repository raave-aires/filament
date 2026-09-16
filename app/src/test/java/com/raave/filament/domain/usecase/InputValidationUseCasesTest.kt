package com.raave.filament.domain.usecase

import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.testing.FakeAuthRepository
import com.raave.filament.testing.FakeTicketRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidationUseCasesTest {

    private val authRepository = FakeAuthRepository()
    private val ticketRepository = FakeTicketRepository()

    @Test
    fun `e-mail invalido nao chega ao backend`() = runTest {
        val result = RequestEmailCodeUseCase(authRepository)("usuario@semdominio")

        assertEquals(AppResult.Failure(AppError.InvalidEmail), result)
        assertTrue(authRepository.requestedEmails.isEmpty())
    }

    @Test
    fun `e-mail e enviado sem espacos nas pontas`() = runTest {
        RequestEmailCodeUseCase(authRepository)("  usuario@elinsa.com.br ")

        assertEquals(listOf("usuario@elinsa.com.br"), authRepository.requestedEmails)
    }

    @Test
    fun `codigo precisa ter exatamente seis digitos`() = runTest {
        val useCase = VerifyEmailCodeUseCase(authRepository)

        assertEquals(AppResult.Failure(AppError.InvalidCode), useCase("a@b.co", "12345"))
        assertEquals(AppResult.Failure(AppError.InvalidCode), useCase("a@b.co", "12345a"))
        assertTrue(authRepository.verifiedCodes.isEmpty())

        assertEquals(AppResult.Success(Unit), useCase("a@b.co", "123456"))
    }

    @Test
    fun `chamado sem titulo ou descricao nao e criado`() = runTest {
        val useCase = CreateTicketUseCase(ticketRepository)

        assertEquals(AppResult.Failure(AppError.EmptyTicketFields), useCase("   ", "Descrição"))
        assertEquals(AppResult.Failure(AppError.EmptyTicketFields), useCase("Título", ""))
        assertTrue(ticketRepository.createdTickets.isEmpty())
    }

    @Test
    fun `chamado e criado com texto normalizado`() = runTest {
        ticketRepository.createResult = AppResult.Success(123L)

        val result = CreateTicketUseCase(ticketRepository)(" Impressora ", " Sem tinta\n")

        assertEquals(AppResult.Success(123L), result)
        assertEquals(listOf("Impressora" to "Sem tinta"), ticketRepository.createdTickets)
    }
}
