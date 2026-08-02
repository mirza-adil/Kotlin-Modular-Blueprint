package com.mirza.transfers.domain.repository

import com.mirza.common.result.Result
import com.mirza.transfers.domain.model.Transfer
import com.mirza.transfers.domain.model.TransferType
import kotlinx.coroutines.flow.Flow

interface TransferRepository {

    fun getTransfers(): Flow<Result<List<Transfer>>>

    suspend fun getTransferById(transferId: String): Result<Transfer>

    suspend fun createTransfer(
        transferType: TransferType,
        sourceAccountId: String,
        recipientName: String,
        recipientAccountNumber: String,
        bankName: String?,
        amount: Double,
        currency: String,
        note: String?
    ): Result<Transfer>

    suspend fun refreshTransfers(): Result<Unit>
}