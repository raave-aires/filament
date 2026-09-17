package com.raave.filament.data.ticket

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.raave.filament.data.glpi.TicketRemoteDataSource
import com.raave.filament.data.local.FilamentDatabase
import com.raave.filament.data.local.toEntity
import com.raave.filament.domain.model.AppError
import com.raave.filament.domain.model.AppResult
import com.raave.filament.domain.model.Attachment
import com.raave.filament.domain.model.Message
import com.raave.filament.domain.model.Ticket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Room real (em memória) + backend falso: valida as regras de gravação que a UI assume. */
@RunWith(AndroidJUnit4::class)
class OfflineFirstTicketRepositoryTest {

    private lateinit var database: FilamentDatabase
    private lateinit var remote: FakeRemote
    private lateinit var repository: OfflineFirstTicketRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<FilamentDatabase>(ApplicationProvider.getApplicationContext<Context>())
            .setDriver(AndroidSQLiteDriver())
            .build()
        remote = FakeRemote()
        repository = OfflineFirstTicketRepository(remote, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun atualizarListaPreservaDescricaoEOrdemDoBackend() = runTest {
        remote.tickets = listOf(ticket(1), ticket(2))
        remote.details[2] = ticket(2, description = "Abertura")
        repository.refreshTickets()
        repository.syncConversation(2, full = false)

        remote.tickets = listOf(ticket(2, title = "Renomeado"), ticket(1))
        repository.refreshTickets()

        val stored = repository.observeTickets().first()
        assertEquals(listOf(2L, 1L), stored.map { it.id })
        assertEquals("Renomeado", stored.first().title)
        assertEquals("Abertura", stored.first().description)
    }

    @Test
    fun chamadoQueSaiDaListaLevaAsMensagensJunto() = runTest {
        remote.tickets = listOf(ticket(1), ticket(2))
        remote.messages[1] = listOf(message(10))
        repository.refreshTickets()
        repository.syncConversation(1, full = false)

        remote.tickets = listOf(ticket(2))
        repository.refreshTickets()

        assertEquals(listOf(2L), repository.observeTickets().first().map { it.id })
        assertTrue(repository.observeMessages(1).first().isEmpty())
    }

    @Test
    fun sincronizacaoIncrementalPedeSoDepoisDaUltimaGuardada() = runTest {
        remote.messages[1] = listOf(message(10), message(11))
        repository.syncConversation(1, full = false)
        assertNull(remote.lastAfterId)

        remote.messages[1] = listOf(message(10), message(11), message(15))
        repository.syncConversation(1, full = false)

        assertEquals(11L, remote.lastAfterId)
        assertEquals(listOf(10L, 11L, 15L), repository.observeMessages(1).first().map { it.id })
    }

    @Test
    fun detalheSoEBuscadoUmaVezForaDoModoCompleto() = runTest {
        remote.details[1] = ticket(1, description = "Abertura")
        repository.syncConversation(1, full = false)
        repository.syncConversation(1, full = false)
        assertEquals(1, remote.detailCalls)

        repository.syncConversation(1, full = true)
        assertEquals(2, remote.detailCalls)
    }

    @Test
    fun releituraCompletaApagaExcluidasMasMantemMensagensMaisNovasQueEla() = runTest {
        remote.messages[1] = listOf(message(10), message(11), message(12))
        repository.syncConversation(1, full = false)

        // 11 foi excluída no GLPI; enquanto a releitura estava em andamento, o usuário enviou a 20.
        remote.messages[1] = listOf(message(10), message(12))
        remote.beforeReturningMessages = {
            database.messageDao().upsert(listOf(message(20, isMine = true).toEntity(ticketId = 1)))
        }
        repository.syncConversation(1, full = true)

        assertEquals(listOf(10L, 12L, 20L), repository.observeMessages(1).first().map { it.id })
    }

    @Test
    fun mensagemEnviadaEGuardadaUmaVezSoMesmoVoltandoNaSincronizacao() = runTest {
        remote.sendResult = message(30, text = "Olá", isMine = true)
        repository.sendMessage(1, "Olá", emptyList())

        remote.messages[1] = listOf(message(30, text = "Olá", isMine = true))
        repository.syncConversation(1, full = true)

        val stored = repository.observeMessages(1).first()
        assertEquals(listOf(30L), stored.map { it.id })
    }

    @Test
    fun falhaDeRedeNaoMexeNoQueEstaGuardado() = runTest {
        remote.messages[1] = listOf(message(10))
        repository.syncConversation(1, full = false)

        remote.error = AppError.Network
        val result = repository.syncConversation(1, full = true)

        assertEquals(AppResult.Failure(AppError.Network), result)
        assertEquals(listOf(10L), repository.observeMessages(1).first().map { it.id })
    }

    @Test
    fun marcadorDeLeituraSoAvancaESobreviveAAtualizacaoDaLista() = runTest {
        remote.tickets = listOf(ticket(1))
        repository.refreshTickets()

        repository.markRead(1, 20)
        repository.markRead(1, 15)
        repository.refreshTickets()

        assertEquals(20L, repository.observeTicket(1).first()?.lastReadMessageId)
    }

    @Test
    fun limparCacheApagaTudo() = runTest {
        remote.tickets = listOf(ticket(1))
        remote.messages[1] = listOf(message(10))
        repository.refreshTickets()
        repository.syncConversation(1, full = false)

        database.clearAllTables()

        assertTrue(repository.observeTickets().first().isEmpty())
        assertTrue(repository.observeMessages(1).first().isEmpty())
    }

    private fun ticket(id: Long, title: String = "Chamado $id", description: String? = null) =
        Ticket(id = id, title = title, status = "Novo", openedAt = null, description = description)

    private fun message(id: Long, text: String = "mensagem $id", isMine: Boolean = false) =
        Message(id = id, text = text, sentAt = null, authorName = "Suporte", isMine = isMine)

    private class FakeRemote : TicketRemoteDataSource {
        var tickets: List<Ticket> = emptyList()
        val details = mutableMapOf<Long, Ticket>()
        val messages = mutableMapOf<Long, List<Message>>()
        var error: AppError? = null
        var sendResult: Message? = null
        var lastAfterId: Long? = null
        var detailCalls = 0
        var beforeReturningMessages: suspend () -> Unit = {}

        override suspend fun getTickets(): AppResult<List<Ticket>> =
            error?.let { AppResult.Failure(it) } ?: AppResult.Success(tickets)

        override suspend fun getTicket(id: Long): AppResult<Ticket> {
            detailCalls++
            error?.let { return AppResult.Failure(it) }
            return AppResult.Success(details[id] ?: Ticket(id = id, title = "Chamado $id", status = "Novo", openedAt = null))
        }

        override suspend fun getMessages(ticketId: Long, afterId: Long?): AppResult<List<Message>> {
            lastAfterId = afterId
            error?.let { return AppResult.Failure(it) }
            val result = messages[ticketId].orEmpty().filter { afterId == null || it.id > afterId }
            beforeReturningMessages()
            return AppResult.Success(result)
        }

        override suspend fun createTicket(title: String, description: String): AppResult<Long> = AppResult.Success(1)

        override suspend fun sendMessage(ticketId: Long, text: String, attachments: List<Attachment>): AppResult<Message> =
            sendResult?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Network)
    }
}
