package com.raave.filament.di

import com.raave.filament.data.attachment.ContentResolverAttachmentRepository
import com.raave.filament.data.auth.BetterAuthRepository
import com.raave.filament.data.glpi.GlpiTicketRepository
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
    abstract fun bindTicketRepository(impl: GlpiTicketRepository): TicketRepository

    @Binds
    abstract fun bindAttachmentRepository(impl: ContentResolverAttachmentRepository): AttachmentRepository
}
