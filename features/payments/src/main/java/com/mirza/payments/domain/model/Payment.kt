package com.mirza.payments.domain.model

data class Payment(
    val id: String,
    val billerName: String,
    val category: PaymentCategory,
    val amount: Double,
    val currency: String,
    val status: PaymentStatus,
    val paidAt: Long?
)

enum class PaymentCategory {
    UTILITY,
    TELECOM,
    INSURANCE,
    CREDIT_CARD,
    OTHER
}

enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED
}