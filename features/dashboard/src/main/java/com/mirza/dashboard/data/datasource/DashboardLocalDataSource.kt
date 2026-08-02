package com.mirza.dashboard.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface DashboardLocalDataSource {

    fun observeDashboardSummary(): Flow<DashboardSummaryDto?>

    suspend fun cacheDashboardSummary(summary: DashboardSummaryDto)
}

@Singleton
class DashboardLocalDataSourceImpl @Inject constructor() : DashboardLocalDataSource {

    private val cachedSummary = MutableStateFlow<DashboardSummaryDto?>(null)

    override fun observeDashboardSummary(): Flow<DashboardSummaryDto?> = cachedSummary.asStateFlow()

    override suspend fun cacheDashboardSummary(summary: DashboardSummaryDto) {
        cachedSummary.value = summary
    }
}