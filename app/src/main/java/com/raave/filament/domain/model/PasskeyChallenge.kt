package com.raave.filament.domain.model

/**
 * Desafio WebAuthn. [optionsJson] vai pro Credential Manager do Android; [handle] é opaco pra quem
 * chama e precisa voltar junto na verificação (na implementação atual, o cookie de challenge).
 */
data class PasskeyChallenge(
    val optionsJson: String,
    val handle: String?,
)
