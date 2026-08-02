package com.mirza.dashboard.domain.usecase

import com.mirza.common.base.FlowUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.Result
import com.mirza.dashboard.domain.model.DashboardSummary
import com.mirza.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, DashboardSummary>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Result<DashboardSummary>> =
        dashboardRepository.getDashboardSummary()
}