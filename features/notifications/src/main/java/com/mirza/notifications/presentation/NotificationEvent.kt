package com.mirza.notifications.presentation

sealed interface NotificationEvent {
    data object LoadNotifications : NotificationEvent
    data class MarkAsRead(val notificationId: String) : NotificationEvent
    data class TogglePushEnabled(val enabled: Boolean) : NotificationEvent
    data class ToggleTransactionAlerts(val enabled: Boolean) : NotificationEvent
    data class ToggleSecurityAlerts(val enabled: Boolean) : NotificationEvent
    data class TogglePromotions(val enabled: Boolean) : NotificationEvent
    data object DismissError : NotificationEvent
}