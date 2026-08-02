package com.mirza.accounts.domain.model

data class Account(
    val id: String,
    val accountNumber: String,
    val accountHolderName: String,
    val accountType: AccountType,
    val balance: Double,
    val currency: String,
    val status: AccountStatus
)

enum class AccountType {
    SAVINGS,
    CURRENT,
    FIXED_DEPOSIT
}

enum class AccountStatus {
    ACTIVE,
    INACTIVE,
    FROZEN
}