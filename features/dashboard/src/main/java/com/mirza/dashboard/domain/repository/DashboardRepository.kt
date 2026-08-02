package com.mirza.dashboard.domain.repository

import com.mirza.common.result.Result
import com.mirza.dashboard.domain.model.DashboardSummary
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {

    fun getDashboardSummary(): Flow<Result<DashboardSummary>>

    suspend fun refreshDashboardSummary(): Result<Unit>
}