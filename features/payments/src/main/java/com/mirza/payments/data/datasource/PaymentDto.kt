package com.mirza.payments.data.datasource

data class PaymentDto(
    val id: String,
    val billerName: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val paidAt: Long?
)

data class ScheduledPaymentDto(
    val id: String,
    val billerName: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val frequency: String,
    val nextRunDate: Long,
    val isActive: Boolean
)