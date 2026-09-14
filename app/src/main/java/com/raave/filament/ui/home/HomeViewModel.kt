package com.raave.filament.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raave.filament.data.auth.AuthRepository
import com.raave.filament.data.auth.AuthTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(tokenStore = AuthTokenStore(application))

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSession()
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

    fun onSignOutClick() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update { it.copy(signedOut = true) }
        }
    }
}
