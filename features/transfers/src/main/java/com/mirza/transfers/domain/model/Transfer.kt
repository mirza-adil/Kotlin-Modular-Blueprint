package com.mirza.transfers.domain.model

data class Transfer(
    val id: String,
    val transferType: TransferType,
    val sourceAccountId: String,
    val recipientName: String,
    val recipientAccountNumber: String,
    val bankName: String?,
    val amount: Double,
    val currency: String,
    val status: TransferStatus,
    val note: String?,
    val createdAt: Long
)

enum class TransferType {
    OWN_ACCOUNT,
    LOCAL,
    INTERNATIONAL
}

enum class TransferStatus {
    PENDING,
    COMPLETED,
    FAILED
}