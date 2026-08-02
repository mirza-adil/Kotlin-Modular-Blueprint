package com.mirza.transfers.domain.usecase

import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import com.mirza.transfers.domain.model.Transfer
import com.mirza.transfers.domain.repository.TransferRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetTransferByIdUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<String, Transfer>(dispatcher) {

    override suspend fun execute(parameters: String): Transfer =
        transferRepository.getTransferById(parameters).getOrThrow()
}