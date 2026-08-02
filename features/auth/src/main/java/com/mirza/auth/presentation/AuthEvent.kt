package com.mirza.auth.presentation

sealed interface AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent
    data class PasswordChanged(val password: String) : AuthEvent
    data object Login : AuthEvent
    data object Logout : AuthEvent
    data object DismissError : AuthEvent
}