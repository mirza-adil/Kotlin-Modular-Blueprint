package com.mirza.transfers.data.datasource

import java.util.UUID
import javax.inject.Inject

interface TransferRemoteDataSource {

    suspend fun getTransfers(): List<TransferDto>

    suspend fun createTransfer(
        transferType: String,
        sourceAccountId: String,
        recipientName: String,
        recipientAccountNumber: String,
        bankName: String?,
        amount: Double,
        currency: String,
        note: String?
    ): TransferDto
}

class TransferRemoteDataSourceImpl @Inject constructor() : TransferRemoteDataSource {

    override suspend fun getTransfers(): List<TransferDto> {
        // TODO: wire up to the real transfers endpoint once it is available
        return emptyList()
    }

    override suspend fun createTransfer(
        transferType: String,
        sourceAccountId: String,
        recipientName: String,
        recipientAccountNumber: String,
        bankName: String?,
        amount: Double,
        currency: String,
        note: String?
    ): TransferDto {
        // TODO: wire up to the real transfer creation endpoint once it is available
        return TransferDto(
            id = UUID.randomUUID().toString(),
            transferType = transferType,
            sourceAccountId = sourceAccountId,
            recipientName = recipientName,
            recipientAccountNumber = recipientAccountNumber,
            bankName = bankName,
            amount = amount,
            currency = currency,
            status = "COMPLETED",
            note = note,
            createdAt = System.currentTimeMillis()
        )
    }
}