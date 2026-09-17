package com.raave.filament.di

import com.raave.filament.data.attachment.ContentResolverAttachmentRepository
import com.raave.filament.data.auth.BetterAuthRepository
import com.raave.filament.data.glpi.GlpiTicketRemoteDataSource
import com.raave.filament.data.glpi.TicketRemoteDataSource
import com.raave.filament.data.ticket.OfflineFirstTicketRepository
import com.raave.filament.domain.repository.AttachmentRepository
import com.raave.filament.domain.repository.AuthRepository
import com.raave.filament.domain.repository.TicketRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: BetterAuthRepository): AuthRepository

    @Binds
    abstract fun bindTicketRepository(impl: OfflineFirstTicketRepository): TicketRepository

    @Binds
    abstract fun bindTicketRemoteDataSource(impl: GlpiTicketRemoteDataSource): TicketRemoteDataSource

    @Binds
    abstract fun bindAttachmentRepository(impl: ContentResolverAttachmentRepository): AttachmentRepository
}
