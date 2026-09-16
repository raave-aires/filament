package com.raave.filament.ui.navigation

/**
 * Retorno do login feito no navegador (deep link `filament://auth-callback`). Não é `data class` de
 * propósito: cada deep link recebido é um evento distinto, mesmo que o conteúdo se repita.
 *
 * @property oneTimeToken nulo quando o link chegou sem token — a ponte do backend não concluiu o login.
 */
class ExternalAuthCallback(val oneTimeToken: String?)
