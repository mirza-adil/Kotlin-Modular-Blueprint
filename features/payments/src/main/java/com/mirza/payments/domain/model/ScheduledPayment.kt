package com.mirza.payments.domain.model

data class ScheduledPayment(
    val id: String,
    val billerName: String,
    val category: PaymentCategory,
    val amount: Double,
    val currency: String,
    val frequency: PaymentFrequency,
    val nextRunDate: Long,
    val isActive: Boolean = true
)

enum class PaymentFrequency {
    ONCE,
    WEEKLY,
    MONTHLY,
    YEARLY
}