package com.raave.filament.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * Cache local dos dados do backend. Tudo aqui é reconstruível pela rede, por isso o builder usa
 * migração destrutiva: mudar o esquema só custa uma nova sincronização, nunca dado do usuário.
 */
@Database(entities = [TicketEntity::class, MessageEntity::class], version = 2, exportSchema = true)
abstract class FilamentDatabase : RoomDatabase() {
    abstract fun ticketDao(): TicketDao
    abstract fun messageDao(): MessageDao

    companion object {
        const val NAME = "filament.db"
    }
}
