package com.raave.filament.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.raave.filament.ui.chat.ChatScreen
import com.raave.filament.ui.chat.ChatViewModel
import com.raave.filament.ui.home.HomeScreen
import com.raave.filament.ui.login.LoginScreen

/**
 * Navegação do app inteiro numa Activity só. A sessão é a fonte de verdade da raiz: entrar, sair ou
 * receber 401 em qualquer chamada troca a pilha inteira entre [LoginRoute] e [HomeRoute], sem que
 * as telas precisem avisar umas às outras.
 */
@Composable
fun FilamentNavigation(
    isSignedIn: Boolean,
    authCallback: ExternalAuthCallback?,
    onAuthCallbackConsumed: () -> Unit,
) {
    val backStack = rememberNavBackStack(rootFor(isSignedIn))

    LaunchedEffect(isSignedIn) {
        val root = rootFor(isSignedIn)
        if (backStack.firstOrNull() != root) {
            // Numa única transação: a pilha nunca é observada vazia entre as duas operações.
            Snapshot.withMutableSnapshot {
                backStack.clear()
                backStack.add(root)
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            // ViewModel por entrada: cada chat aberto tem o seu, descartado ao sair da tela.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = forwardTransition(),
        popTransitionSpec = backTransition(),
        predictivePopTransitionSpec = predictiveBackTransition(),
        entryProvider = entryProvider {
            // Login e Home entram por fade through quando a sessão troca a raiz da pilha.
            entry<LoginRoute>(metadata = NavDisplay.transitionSpec(FadeThroughTransition)) {
                OpaqueScreen {
                    LoginScreen(authCallback = authCallback, onAuthCallbackConsumed = onAuthCallbackConsumed)
                }
            }
            entry<HomeRoute>(metadata = NavDisplay.transitionSpec(FadeThroughTransition)) {
                OpaqueScreen {
                    HomeScreen(
                        onTicketClick = { ticket -> backStack.add(ChatRoute(ticket.id, ticket.title)) },
                    )
                }
            }
            entry<ChatRoute> { route ->
                val viewModel = hiltViewModel<ChatViewModel, ChatViewModel.Factory>(
                    creationCallback = { factory -> factory.create(route) },
                )
                OpaqueScreen {
                    ChatScreen(viewModel = viewModel, onBackClick = { backStack.removeLastOrNull() })
                }
            }
        },
    )
}

/**
 * As telas não pintam fundo próprio (quem pinta é o Surface do FilamentTheme, atrás da navegação).
 * Sem isso, durante a transição uma aparecia através da outra.
 */
@Composable
private fun OpaqueScreen(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        content()
    }
}

private fun rootFor(isSignedIn: Boolean): NavKey = if (isSignedIn) HomeRoute else LoginRoute
