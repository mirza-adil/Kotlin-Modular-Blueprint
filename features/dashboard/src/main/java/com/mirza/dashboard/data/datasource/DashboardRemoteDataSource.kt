package com.mirza.dashboard.data.datasource

import javax.inject.Inject

interface DashboardRemoteDataSource {

    suspend fun getDashboardSummary(): DashboardSummaryDto
}

class DashboardRemoteDataSourceImpl @Inject constructor() : DashboardRemoteDataSource {

    override suspend fun getDashboardSummary(): DashboardSummaryDto {
        // TODO: wire up to the real dashboard/aggregation endpoint once it is available
        return DashboardSummaryDto(
            totalBalance = 0.0,
            currency = "USD",
            accountSummaries = emptyList(),
            recentTransactions = emptyList()
        )
    }
}