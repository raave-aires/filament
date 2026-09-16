package com.raave.filament.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// A interpretação das strings do backend fica na camada de dados (data/glpi/GlpiDates.kt); aqui só
// se formata o instante para o fuso e o idioma do aparelho.

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** "14:05" — horário da mensagem no chat. */
fun Instant.toTimeLabel(): String = atZone(ZoneId.systemDefault()).format(TimeFormat)

/** "15 de set. de 2026" (conforme o idioma do aparelho) — data de abertura do chamado. */
fun Instant.toDateLabel(): String = atZone(ZoneId.systemDefault()).format(DateFormat)
