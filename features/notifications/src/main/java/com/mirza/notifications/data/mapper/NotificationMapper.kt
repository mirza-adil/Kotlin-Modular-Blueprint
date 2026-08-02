package com.mirza.notifications.data.mapper

import com.mirza.notifications.data.datasource.NotificationDto
import com.mirza.notifications.data.datasource.NotificationPreferencesDto
import com.mirza.notifications.domain.model.NotificationCategory
import com.mirza.notifications.domain.model.NotificationItem
import com.mirza.notifications.domain.model.NotificationPreferences

fun NotificationDto.toDomain(): NotificationItem = NotificationItem(
    id = id,
    title = title,
    message = message,
    category = runCatching { NotificationCategory.valueOf(category) }
        .getOrDefault(NotificationCategory.GENERAL),
    timestamp = timestamp,
    isRead = isRead
)

fun List<NotificationDto>.toDomain(): List<NotificationItem> = map { it.toDomain() }

fun NotificationPreferencesDto.toDomain(): NotificationPreferences = NotificationPreferences(
    pushEnabled = pushEnabled,
    transactionAlerts = transactionAlerts,
    securityAlerts = securityAlerts,
    promotions = promotions
)

fun NotificationPreferences.toDto(): NotificationPreferencesDto = NotificationPreferencesDto(
    pushEnabled = pushEnabled,
    transactionAlerts = transactionAlerts,
    securityAlerts = securityAlerts,
    promotions = promotions
)