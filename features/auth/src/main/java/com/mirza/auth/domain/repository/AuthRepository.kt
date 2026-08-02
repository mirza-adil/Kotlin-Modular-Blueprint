package com.mirza.auth.domain.repository

import com.mirza.auth.domain.model.User
import com.mirza.common.result.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    fun observeAuthState(): Flow<Boolean>

    fun getCurrentUser(): User?

    suspend fun login(email: String, password: String): Result<User>

    suspend fun logout(): Result<Unit>
}