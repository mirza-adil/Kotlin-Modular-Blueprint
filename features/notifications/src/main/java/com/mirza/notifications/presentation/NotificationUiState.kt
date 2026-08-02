package com.mirza.notifications.presentation

import com.mirza.notifications.domain.model.NotificationItem
import com.mirza.notifications.domain.model.NotificationPreferences

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val preferences: NotificationPreferences = NotificationPreferences(),
    val errorMessage: String? = null
)