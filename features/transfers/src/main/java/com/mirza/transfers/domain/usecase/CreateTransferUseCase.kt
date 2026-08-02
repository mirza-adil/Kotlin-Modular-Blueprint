package com.mirza.transfers.domain.usecase

import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import com.mirza.transfers.domain.model.Transfer
import com.mirza.transfers.domain.model.TransferType
import com.mirza.transfers.domain.repository.TransferRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CreateTransferUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<CreateTransferUseCase.Params, Transfer>(dispatcher) {

    data class Params(
        val transferType: TransferType,
        val sourceAccountId: String,
        val recipientName: String,
        val recipientAccountNumber: String,
        val bankName: String?,
        val amount: Double,
        val currency: String,
        val note: String?
    )

    override suspend fun execute(parameters: Params): Transfer =
        transferRepository.createTransfer(
            transferType = parameters.transferType,
            sourceAccountId = parameters.sourceAccountId,
            recipientName = parameters.recipientName,
            recipientAccountNumber = parameters.recipientAccountNumber,
            bankName = parameters.bankName,
            amount = parameters.amount,
            currency = parameters.currency,
            note = parameters.note
        ).getOrThrow()
}