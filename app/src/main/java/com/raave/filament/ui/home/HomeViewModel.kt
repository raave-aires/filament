package com.raave.filament.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raave.filament.data.auth.AuthRepository
import com.raave.filament.data.auth.AuthTokenStore
import com.raave.filament.data.glpi.GlpiRepository
import com.raave.filament.util.DeviceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStore = AuthTokenStore(application)
    private val repository = AuthRepository(tokenStore = tokenStore)
    private val glpiRepository = GlpiRepository(tokenStore = tokenStore)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSession()
        refreshBlurState()
    }

    private fun loadSession() {
        viewModelScope.launch {
            repository.getCurrentSession().fold(
                onSuccess = { body ->
                    val user = body.optJSONObject("user")
                    if (user == null) {
                        // Token guardado não corresponde mais a uma sessão válida no backend.
                        _uiState.update { it.copy(isLoading = false, signedOut = true) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userName = user.optString("name").takeIf(String::isNotBlank),
                                userEmail = user.optString("email").takeIf(String::isNotBlank),
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, signedOut = true) }
                },
            )
        }
    }

    /**
     * Reavaliado a cada retomada da tela: o modo de economia de bateria pode ser ligado enquanto
     * o app está aberto.
     */
    fun refreshBlurState() {
        val supported = DeviceUtils.isBlurSupported(getApplication())
        _uiState.update { it.copy(isBlurEnabled = supported) }
    }

    fun onTabSelected(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSignOutClick() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update { it.copy(signedOut = true) }
        }
    }

    fun onNewTicketButtonClick() {
        _uiState.update {
            it.copy(isNewTicketDialogOpen = true, newTicketName = "", newTicketContent = "", newTicketError = null)
        }
    }

    fun onNewTicketDismiss() {
        _uiState.update { it.copy(isNewTicketDialogOpen = false, newTicketError = null) }
    }

    fun onNewTicketNameChange(name: String) {
        _uiState.update { it.copy(newTicketName = name, newTicketError = null) }
    }

    fun onNewTicketContentChange(content: String) {
        _uiState.update { it.copy(newTicketContent = content, newTicketError = null) }
    }

    fun onNewTicketSubmit() {
        val state = _uiState.value
        val name = state.newTicketName.trim()
        val content = state.newTicketContent.trim()
        if (name.isEmpty() || content.isEmpty()) {
            _uiState.update { it.copy(newTicketError = "Preencha o título e a descrição") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingTicket = true, newTicketError = null) }
            glpiRepository.createTicket(name, content).fold(
                onSuccess = { ticketId ->
                    _uiState.update {
                        it.copy(
                            isCreatingTicket = false,
                            isNewTicketDialogOpen = false,
                            newTicketName = "",
                            newTicketContent = "",
                            lastCreatedTicketId = ticketId,
                            selectedTab = HomeTab.CHAMADOS,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isCreatingTicket = false, newTicketError = error.message) }
                },
            )
        }
    }
}
