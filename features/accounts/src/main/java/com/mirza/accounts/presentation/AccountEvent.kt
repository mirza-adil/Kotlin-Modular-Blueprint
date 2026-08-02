package com.mirza.accounts.presentation

sealed interface AccountEvent {
    data object LoadAccounts : AccountEvent
    data class SelectAccount(val accountId: String) : AccountEvent
    data object DismissError : AccountEvent
}