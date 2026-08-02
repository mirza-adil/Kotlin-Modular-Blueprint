package com.mirza.payments.presentation

import com.mirza.payments.domain.model.PaymentCategory
import com.mirza.payments.domain.model.ScheduledPayment

sealed interface PaymentEvent {
    data object LoadPayments : PaymentEvent
    data class PayBill(
        val billerName: String,
        val category: PaymentCategory,
        val amount: Double,
        val currency: String
    ) : PaymentEvent
    data class ScheduleBillPayment(val scheduledPayment: ScheduledPayment) : PaymentEvent
    data class CancelScheduledPayment(val scheduledPaymentId: String) : PaymentEvent
    data object DismissError : PaymentEvent
}