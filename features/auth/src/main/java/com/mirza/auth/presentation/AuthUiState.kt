package com.mirza.auth.presentation

import com.mirza.auth.domain.model.User

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null
)