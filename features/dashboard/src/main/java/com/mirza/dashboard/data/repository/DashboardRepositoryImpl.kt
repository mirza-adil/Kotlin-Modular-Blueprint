package com.mirza.dashboard.data.repository

import com.mirza.common.extensions.asResult
import com.mirza.common.result.Result
import com.mirza.dashboard.data.datasource.DashboardLocalDataSource
import com.mirza.dashboard.data.datasource.DashboardRemoteDataSource
import com.mirza.dashboard.data.mapper.toDomain
import com.mirza.dashboard.domain.model.DashboardSummary
import com.mirza.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val remoteDataSource: DashboardRemoteDataSource,
    private val localDataSource: DashboardLocalDataSource
) : DashboardRepository {

    override fun getDashboardSummary(): Flow<Result<DashboardSummary>> =
        localDataSource.observeDashboardSummary()
            .onStart { refreshDashboardSummary() }
            .filterNotNull()
            .map { it.toDomain() }
            .asResult()

    override suspend fun refreshDashboardSummary(): Result<Unit> {
        return try {
            val summary = remoteDataSource.getDashboardSummary()
            localDataSource.cacheDashboardSummary(summary)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}