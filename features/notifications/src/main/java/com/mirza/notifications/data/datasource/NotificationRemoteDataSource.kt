package com.mirza.notifications.data.datasource

import javax.inject.Inject

interface NotificationRemoteDataSource {

    suspend fun getNotifications(): List<NotificationDto>

    suspend fun updatePreferences(preferences: NotificationPreferencesDto)
}

class NotificationRemoteDataSourceImpl @Inject constructor() : NotificationRemoteDataSource {

    override suspend fun getNotifications(): List<NotificationDto> {
        // TODO: wire up to the real notifications endpoint once it is available
        return emptyList()
    }

    override suspend fun updatePreferences(preferences: NotificationPreferencesDto) {
        // TODO: wire up to the real notification preferences endpoint once it is available
    }
}