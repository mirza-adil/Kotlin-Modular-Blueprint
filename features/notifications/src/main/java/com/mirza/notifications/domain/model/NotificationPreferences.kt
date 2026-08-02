package com.mirza.notifications.domain.model

data class NotificationPreferences(
    val pushEnabled: Boolean = true,
    val transactionAlerts: Boolean = true,
    val securityAlerts: Boolean = true,
    val promotions: Boolean = false
)