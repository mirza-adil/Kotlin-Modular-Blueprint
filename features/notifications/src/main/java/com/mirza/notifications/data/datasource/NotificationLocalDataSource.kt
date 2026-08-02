package com.mirza.notifications.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationLocalDataSource {

    fun observeNotifications(): Flow<List<NotificationDto>>

    fun observePreferences(): Flow<NotificationPreferencesDto>

    suspend fun cacheNotifications(notifications: List<NotificationDto>)

    suspend fun markAsRead(notificationId: String)

    suspend fun savePreferences(preferences: NotificationPreferencesDto)
}

@Singleton
class NotificationLocalDataSourceImpl @Inject constructor() : NotificationLocalDataSource {

    private val cachedNotifications = MutableStateFlow<List<NotificationDto>>(emptyList())
    private val cachedPreferences = MutableStateFlow(
        NotificationPreferencesDto(
            pushEnabled = true,
            transactionAlerts = true,
            securityAlerts = true,
            promotions = false
        )
    )

    override fun observeNotifications(): Flow<List<NotificationDto>> = cachedNotifications.asStateFlow()

    override fun observePreferences(): Flow<NotificationPreferencesDto> = cachedPreferences.asStateFlow()

    override suspend fun cacheNotifications(notifications: List<NotificationDto>) {
        cachedNotifications.value = notifications
    }

    override suspend fun markAsRead(notificationId: String) {
        cachedNotifications.value = cachedNotifications.value.map { notification ->
            if (notification.id == notificationId) notification.copy(isRead = true) else notification
        }
    }

    override suspend fun savePreferences(preferences: NotificationPreferencesDto) {
        cachedPreferences.value = preferences
    }
}