package com.mirza.dashboard.presentation

import com.mirza.dashboard.domain.model.DashboardSummary

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummary? = null,
    val errorMessage: String? = null
)