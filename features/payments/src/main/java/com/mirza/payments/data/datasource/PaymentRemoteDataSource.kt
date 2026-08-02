package com.mirza.payments.data.datasource

import javax.inject.Inject

interface PaymentRemoteDataSource {

    suspend fun getPayments(): List<PaymentDto>

    suspend fun getScheduledPayments(): List<ScheduledPaymentDto>

    suspend fun payBill(billerName: String, category: String, amount: Double, currency: String): PaymentDto

    suspend fun scheduleBillPayment(scheduledPayment: ScheduledPaymentDto)

    suspend fun cancelScheduledPayment(scheduledPaymentId: String)
}

class PaymentRemoteDataSourceImpl @Inject constructor() : PaymentRemoteDataSource {

    override suspend fun getPayments(): List<PaymentDto> {
        // TODO: wire up to the real payments endpoint once it is available
        return emptyList()
    }

    override suspend fun getScheduledPayments(): List<ScheduledPaymentDto> {
        // TODO: wire up to the real scheduled payments endpoint once it is available
        return emptyList()
    }

    override suspend fun payBill(
        billerName: String,
        category: String,
        amount: Double,
        currency: String
    ): PaymentDto {
        // TODO: wire up to the real bill payment endpoint once it is available
        return PaymentDto(
            id = java.util.UUID.randomUUID().toString(),
            billerName = billerName,
            category = category,
            amount = amount,
            currency = currency,
            status = "COMPLETED",
            paidAt = System.currentTimeMillis()
        )
    }

    override suspend fun scheduleBillPayment(scheduledPayment: ScheduledPaymentDto) {
        // TODO: wire up to the real scheduled payments endpoint once it is available
    }

    override suspend fun cancelScheduledPayment(scheduledPaymentId: String) {
        // TODO: wire up to the real scheduled payments endpoint once it is available
    }
}