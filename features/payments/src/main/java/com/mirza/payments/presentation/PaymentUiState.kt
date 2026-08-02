package com.mirza.payments.presentation

import com.mirza.payments.domain.model.Payment
import com.mirza.payments.domain.model.ScheduledPayment

data class PaymentUiState(
    val isLoading: Boolean = false,
    val payments: List<Payment> = emptyList(),
    val scheduledPayments: List<ScheduledPayment> = emptyList(),
    val errorMessage: String? = null
)