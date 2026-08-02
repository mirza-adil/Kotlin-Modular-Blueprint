package com.mirza.transfers.domain.usecase

import com.mirza.common.base.FlowUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.Result
import com.mirza.transfers.domain.model.Transfer
import com.mirza.transfers.domain.repository.TransferRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<Transfer>>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Result<List<Transfer>>> =
        transferRepository.getTransfers()
}