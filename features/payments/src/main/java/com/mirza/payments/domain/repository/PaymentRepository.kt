package com.mirza.payments.domain.repository

import com.mirza.common.result.Result
import com.mirza.payments.domain.model.Payment
import com.mirza.payments.domain.model.PaymentCategory
import com.mirza.payments.domain.model.ScheduledPayment
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {

    fun getPayments(): Flow<Result<List<Payment>>>

    fun getScheduledPayments(): Flow<Result<List<ScheduledPayment>>>

    suspend fun payBill(
        billerName: String,
        category: PaymentCategory,
        amount: Double,
        currency: String
    ): Result<Payment>

    suspend fun scheduleBillPayment(scheduledPayment: ScheduledPayment): Result<Unit>

    suspend fun cancelScheduledPayment(scheduledPaymentId: String): Result<Unit>

    suspend fun refreshPayments(): Result<Unit>
}