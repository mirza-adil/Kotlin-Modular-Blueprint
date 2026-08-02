package com.mirza.transfers.presentation

import com.mirza.transfers.domain.model.TransferType

sealed interface TransferEvent {
    data object LoadTransfers : TransferEvent
    data class CreateTransfer(
        val transferType: TransferType,
        val sourceAccountId: String,
        val recipientName: String,
        val recipientAccountNumber: String,
        val bankName: String?,
        val amount: Double,
        val currency: String,
        val note: String?
    ) : TransferEvent
    data object DismissError : TransferEvent
}