package com.mirza.auth.data.repository

import com.mirza.auth.data.datasource.AuthLocalDataSource
import com.mirza.auth.data.datasource.AuthRemoteDataSource
import com.mirza.auth.data.mapper.toDomain
import com.mirza.auth.domain.model.User
import com.mirza.auth.domain.repository.AuthRepository
import com.mirza.common.result.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val localDataSource: AuthLocalDataSource
) : AuthRepository {

    override fun observeAuthState(): Flow<Boolean> = localDataSource.observeSessionState()

    override fun getCurrentUser(): User? = localDataSource.getCachedUser()?.toDomain()

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = remoteDataSource.login(email, password)
            localDataSource.saveSession(response)
            Result.Success(response.user.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            remoteDataSource.logout()
            localDataSource.clearSession()
            Result.Success(Unit)
        } catch (e: Exception) {
            localDataSource.clearSession()
            Result.Error(e)
        }
    }
}