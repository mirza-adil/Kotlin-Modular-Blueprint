package com.mirza.dashboard.data.datasource

data class DashboardSummaryDto(
    val totalBalance: Double,
    val currency: String,
    val accountSummaries: List<AccountSummaryDto>,
    val recentTransactions: List<RecentTransactionDto>
)

data class AccountSummaryDto(
    val accountId: String,
    val accountName: String,
    val maskedAccountNumber: String,
    val balance: Double
)

data class RecentTransactionDto(
    val id: String,
    val title: String,
    val amount: Double,
    val isCredit: Boolean,
    val date: String
)