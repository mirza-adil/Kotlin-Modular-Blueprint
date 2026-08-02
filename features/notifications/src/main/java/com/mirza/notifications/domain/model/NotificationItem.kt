package com.mirza.notifications.domain.model

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val category: NotificationCategory,
    val timestamp: Long,
    val isRead: Boolean
)

enum class NotificationCategory {
    TRANSACTION,
    SECURITY,
    PROMOTION,
    GENERAL
}