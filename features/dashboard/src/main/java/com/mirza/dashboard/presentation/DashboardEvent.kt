package com.mirza.dashboard.presentation

sealed interface DashboardEvent {
    data object LoadSummary : DashboardEvent
    data object Refresh : DashboardEvent
    data object DismissError : DashboardEvent
}