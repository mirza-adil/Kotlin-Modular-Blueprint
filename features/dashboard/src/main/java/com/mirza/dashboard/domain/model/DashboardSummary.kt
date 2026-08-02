package com.mirza.dashboard.domain.model

data class DashboardSummary(
    val totalBalance: Double,
    val currency: String,
    val accountSummaries: List<AccountSummary>,
    val recentTransactions: List<RecentTransaction>
)

data class AccountSummary(
    val accountId: String,
    val accountName: String,
    val maskedAccountNumber: String,
    val balance: Double
)

data class RecentTransaction(
    val id: String,
    val title: String,
    val amount: Double,
    val isCredit: Boolean,
    val date: String
)