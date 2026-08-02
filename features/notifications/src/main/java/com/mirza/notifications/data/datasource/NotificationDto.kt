package com.mirza.notifications.data.datasource

data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    val category: String,
    val timestamp: Long,
    val isRead: Boolean
)

data class NotificationPreferencesDto(
    val pushEnabled: Boolean,
    val transactionAlerts: Boolean,
    val securityAlerts: Boolean,
    val promotions: Boolean
)