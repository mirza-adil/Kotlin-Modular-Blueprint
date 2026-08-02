package com.mirza.payments.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface PaymentLocalDataSource {

    fun observePayments(): Flow<List<PaymentDto>>

    fun observeScheduledPayments(): Flow<List<ScheduledPaymentDto>>

    suspend fun cachePayments(payments: List<PaymentDto>)

    suspend fun cacheScheduledPayments(scheduledPayments: List<ScheduledPaymentDto>)

    suspend fun addPayment(payment: PaymentDto)

    suspend fun upsertScheduledPayment(scheduledPayment: ScheduledPaymentDto)

    suspend fun removeScheduledPayment(scheduledPaymentId: String)
}

@Singleton
class PaymentLocalDataSourceImpl @Inject constructor() : PaymentLocalDataSource {

    private val cachedPayments = MutableStateFlow<List<PaymentDto>>(emptyList())
    private val cachedScheduledPayments = MutableStateFlow<List<ScheduledPaymentDto>>(emptyList())

    override fun observePayments(): Flow<List<PaymentDto>> = cachedPayments.asStateFlow()

    override fun observeScheduledPayments(): Flow<List<ScheduledPaymentDto>> =
        cachedScheduledPayments.asStateFlow()

    override suspend fun cachePayments(payments: List<PaymentDto>) {
        cachedPayments.value = payments
    }

    override suspend fun cacheScheduledPayments(scheduledPayments: List<ScheduledPaymentDto>) {
        cachedScheduledPayments.value = scheduledPayments
    }

    override suspend fun addPayment(payment: PaymentDto) {
        cachedPayments.value = listOf(payment) + cachedPayments.value
    }

    override suspend fun upsertScheduledPayment(scheduledPayment: ScheduledPaymentDto) {
        val current = cachedScheduledPayments.value.filterNot { it.id == scheduledPayment.id }
        cachedScheduledPayments.value = current + scheduledPayment
    }

    override suspend fun removeScheduledPayment(scheduledPaymentId: String) {
        cachedScheduledPayments.value =
            cachedScheduledPayments.value.filterNot { it.id == scheduledPaymentId }
    }
}