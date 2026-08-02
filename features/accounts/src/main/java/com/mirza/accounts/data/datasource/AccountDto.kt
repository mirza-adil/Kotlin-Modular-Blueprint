package com.mirza.accounts.data.datasource

data class AccountDto(
    val id: String,
    val accountNumber: String,
    val accountHolderName: String,
    val accountType: String,
    val balance: Double,
    val currency: String,
    val status: String
)