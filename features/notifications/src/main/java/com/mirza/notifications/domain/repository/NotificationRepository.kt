package com.mirza.notifications.domain.repository

import com.mirza.common.result.Result
import com.mirza.notifications.domain.model.NotificationItem
import com.mirza.notifications.domain.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    fun getNotifications(): Flow<Result<List<NotificationItem>>>

    fun getPreferences(): Flow<Result<NotificationPreferences>>

    suspend fun markAsRead(notificationId: String): Result<Unit>

    suspend fun updatePreferences(preferences: NotificationPreferences): Result<Unit>

    suspend fun refreshNotifications(): Result<Unit>
}