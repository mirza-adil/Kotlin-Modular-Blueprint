package com.mirza.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirza.auth.domain.usecase.LoginUseCase
import com.mirza.auth.domain.usecase.LogoutUseCase
import com.mirza.auth.domain.usecase.ObserveAuthStateUseCase
import com.mirza.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(isLoggedIn = result.data) }
                }
            }
        }
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> _uiState.update { it.copy(email = event.email) }
            is AuthEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password) }
            AuthEvent.Login -> login()
            AuthEvent.Logout -> logout()
            AuthEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun login() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loginUseCase(LoginUseCase.Params(state.email, state.password))) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, isLoggedIn = true, user = result.data)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            when (val result = logoutUseCase()) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoggedIn = false, user = null, email = "", password = "")
                }
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }
}