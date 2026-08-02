package com.mirza.transfers.data.mapper

import com.mirza.transfers.data.datasource.TransferDto
import com.mirza.transfers.domain.model.Transfer
import com.mirza.transfers.domain.model.TransferStatus
import com.mirza.transfers.domain.model.TransferType

fun TransferDto.toDomain(): Transfer = Transfer(
    id = id,
    transferType = runCatching { TransferType.valueOf(transferType) }.getOrDefault(TransferType.LOCAL),
    sourceAccountId = sourceAccountId,
    recipientName = recipientName,
    recipientAccountNumber = recipientAccountNumber,
    bankName = bankName,
    amount = amount,
    currency = currency,
    status = runCatching { TransferStatus.valueOf(status) }.getOrDefault(TransferStatus.PENDING),
    note = note,
    createdAt = createdAt
)

fun List<TransferDto>.toDomain(): List<Transfer> = map { it.toDomain() }