package com.raave.filament.data.glpi

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val GlpiDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/**
 * Datas do backend chegam como texto em formatos diferentes: ISO-8601 com offset (fuso de onde o GLPI
 * está), ISO sem offset ou o formato nativo do GLPI (`yyyy-MM-dd HH:mm:ss`). Sem offset, assume o
 * fuso [zone] — o GLPI e os usuários ficam no mesmo fuso. Texto irreconhecível vira `null`.
 */
internal fun parseGlpiInstant(value: String?, zone: ZoneId = ZoneId.systemDefault()): Instant? {
    val text = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return runCatching { OffsetDateTime.parse(text).toInstant() }
        .recoverCatching { LocalDateTime.parse(text).atZone(zone).toInstant() }
        .recoverCatching { LocalDateTime.parse(text, GlpiDateTimeFormat).atZone(zone).toInstant() }
        .getOrNull()
}
