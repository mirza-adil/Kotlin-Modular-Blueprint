package com.mirza.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mirza.accounts.domain.usecase.GetAccountByIdUseCase
import com.mirza.accounts.domain.usecase.GetAccountsUseCase
import com.mirza.common.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getAccountByIdUseCase: GetAccountByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        onEvent(AccountEvent.LoadAccounts)
    }

    fun onEvent(event: AccountEvent) {
        when (event) {
            AccountEvent.LoadAccounts -> loadAccounts()
            is AccountEvent.SelectAccount -> selectAccount(event.accountId)
            AccountEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            getAccountsUseCase(Unit).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, accounts = result.data, errorMessage = null)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    private fun selectAccount(accountId: String) {
        viewModelScope.launch {
            when (val result = getAccountByIdUseCase(accountId)) {
                is Result.Success -> _uiState.update { it.copy(selectedAccount = result.data) }
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.message) }
                Result.Loading -> Unit
            }
        }
    }
}