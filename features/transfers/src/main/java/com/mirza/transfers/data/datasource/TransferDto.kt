package com.mirza.transfers.data.datasource

data class TransferDto(
    val id: String,
    val transferType: String,
    val sourceAccountId: String,
    val recipientName: String,
    val recipientAccountNumber: String,
    val bankName: String?,
    val amount: Double,
    val currency: String,
    val status: String,
    val note: String?,
    val createdAt: Long
)