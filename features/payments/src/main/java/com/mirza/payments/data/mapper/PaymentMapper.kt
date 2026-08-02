package com.mirza.payments.data.mapper

import com.mirza.payments.data.datasource.PaymentDto
import com.mirza.payments.data.datasource.ScheduledPaymentDto
import com.mirza.payments.domain.model.Payment
import com.mirza.payments.domain.model.PaymentCategory
import com.mirza.payments.domain.model.PaymentFrequency
import com.mirza.payments.domain.model.PaymentStatus
import com.mirza.payments.domain.model.ScheduledPayment

fun PaymentDto.toDomain(): Payment = Payment(
    id = id,
    billerName = billerName,
    category = runCatching { PaymentCategory.valueOf(category) }.getOrDefault(PaymentCategory.OTHER),
    amount = amount,
    currency = currency,
    status = runCatching { PaymentStatus.valueOf(status) }.getOrDefault(PaymentStatus.PENDING),
    paidAt = paidAt
)

fun List<PaymentDto>.toDomain(): List<Payment> = map { it.toDomain() }

fun ScheduledPaymentDto.toDomain(): ScheduledPayment = ScheduledPayment(
    id = id,
    billerName = billerName,
    category = runCatching { PaymentCategory.valueOf(category) }.getOrDefault(PaymentCategory.OTHER),
    amount = amount,
    currency = currency,
    frequency = runCatching { PaymentFrequency.valueOf(frequency) }.getOrDefault(PaymentFrequency.MONTHLY),
    nextRunDate = nextRunDate,
    isActive = isActive
)

fun List<ScheduledPaymentDto>.toDomainScheduled(): List<ScheduledPayment> = map { it.toDomain() }

fun ScheduledPayment.toDto(): ScheduledPaymentDto = ScheduledPaymentDto(
    id = id,
    billerName = billerName,
    category = category.name,
    amount = amount,
    currency = currency,
    frequency = frequency.name,
    nextRunDate = nextRunDate,
    isActive = isActive
)