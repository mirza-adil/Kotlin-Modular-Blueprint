package com.mirza.accounts.presentation

import com.mirza.accounts.domain.model.Account

data class AccountUiState(
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null,
    val errorMessage: String? = null
)