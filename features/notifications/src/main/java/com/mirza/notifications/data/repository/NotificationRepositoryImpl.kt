package com.mirza.notifications.data.repository

import com.mirza.common.extensions.asResult
import com.mirza.common.result.Result
import com.mirza.notifications.data.datasource.NotificationLocalDataSource
import com.mirza.notifications.data.datasource.NotificationRemoteDataSource
import com.mirza.notifications.data.mapper.toDomain
import com.mirza.notifications.data.mapper.toDto
import com.mirza.notifications.domain.model.NotificationItem
import com.mirza.notifications.domain.model.NotificationPreferences
import com.mirza.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val remoteDataSource: NotificationRemoteDataSource,
    private val localDataSource: NotificationLocalDataSource
) : NotificationRepository {

    override fun getNotifications(): Flow<Result<List<NotificationItem>>> =
        localDataSource.observeNotifications()
            .map { it.toDomain() }
            .asResult()

    override fun getPreferences(): Flow<Result<NotificationPreferences>> =
        localDataSource.observePreferences()
            .map { it.toDomain() }
            .asResult()

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            localDataSource.markAsRead(notificationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updatePreferences(preferences: NotificationPreferences): Result<Unit> {
        return try {
            val dto = preferences.toDto()
            remoteDataSource.updatePreferences(dto)
            localDataSource.savePreferences(dto)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshNotifications(): Result<Unit> {
        return try {
            val notifications = remoteDataSource.getNotifications()
            localDataSource.cacheNotifications(notifications)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}