package com.mirza.dashboard.domain.usecase

import com.mirza.common.base.NoParamUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import com.mirza.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RefreshDashboardSummaryUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : NoParamUseCase<Unit>(dispatcher) {

    override suspend fun execute() {
        dashboardRepository.refreshDashboardSummary().getOrThrow()
    }
}