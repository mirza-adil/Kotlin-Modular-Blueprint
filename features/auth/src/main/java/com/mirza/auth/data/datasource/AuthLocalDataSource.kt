package com.mirza.auth.data.datasource

import com.mirza.security.di.SecureTokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import model.LoginResponse
import model.UserDto
import javax.inject.Inject
import javax.inject.Singleton

interface AuthLocalDataSource {

    fun observeSessionState(): Flow<Boolean>

    fun getCachedUser(): UserDto?

    fun saveSession(loginResponse: LoginResponse)

    fun clearSession()
}

@Singleton
class AuthLocalDataSourceImpl @Inject constructor(
    private val secureTokenManager: SecureTokenManager
) : AuthLocalDataSource {

    private val sessionState = MutableStateFlow(secureTokenManager.hasValidToken())

    override fun observeSessionState(): Flow<Boolean> = sessionState.asStateFlow()

    override fun getCachedUser(): UserDto? {
        val userId = secureTokenManager.getUserId() ?: return null
        val email = secureTokenManager.getUserEmail() ?: return null
        val name = secureTokenManager.getUserName() ?: return null
        return UserDto(id = userId, email = email, name = name, avatarUrl = null)
    }

    override fun saveSession(loginResponse: LoginResponse) {
        secureTokenManager.saveTokens(
            accessToken = loginResponse.accessToken,
            refreshToken = loginResponse.refreshToken,
            expiresIn = loginResponse.expiresIn
        )
        secureTokenManager.saveUserInfo(
            userId = loginResponse.user.id,
            email = loginResponse.user.email,
            name = loginResponse.user.name
        )
        sessionState.value = true
    }

    override fun clearSession() {
        secureTokenManager.clearAll()
        sessionState.value = false
    }
}