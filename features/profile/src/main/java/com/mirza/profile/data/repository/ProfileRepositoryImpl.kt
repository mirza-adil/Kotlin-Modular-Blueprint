package com.mirza.profile.data.repository

import com.mirza.common.extensions.asResult
import com.mirza.common.result.Result
import com.mirza.profile.data.datasource.ProfileLocalDataSource
import com.mirza.profile.data.datasource.ProfileRemoteDataSource
import com.mirza.profile.data.mapper.toDomain
import com.mirza.profile.data.mapper.toDto
import com.mirza.profile.domain.model.AppPreferences
import com.mirza.profile.domain.model.UserProfile
import com.mirza.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val remoteDataSource: ProfileRemoteDataSource,
    private val localDataSource: ProfileLocalDataSource
) : ProfileRepository {

    override fun getUserProfile(): Flow<Result<UserProfile>> =
        localDataSource.observeUserProfile()
            .filterNotNull()
            .map { it.toDomain() }
            .asResult()

    override fun getPreferences(): Flow<Result<AppPreferences>> =
        localDataSource.observePreferences()
            .map { it.toDomain() }
            .asResult()

    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val dto = profile.toDto()
            remoteDataSource.updateUserProfile(dto)
            localDataSource.cacheUserProfile(dto)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePreferences(preferences: AppPreferences): Result<Unit> {
        return try {
            localDataSource.savePreferences(preferences.toDto())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshUserProfile(): Result<Unit> {
        return try {
            val profile = remoteDataSource.getUserProfile()
            localDataSource.cacheUserProfile(profile)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}