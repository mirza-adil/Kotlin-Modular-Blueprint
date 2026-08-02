package com.mirza.dashboard.data.mapper

import com.mirza.dashboard.data.datasource.AccountSummaryDto
import com.mirza.dashboard.data.datasource.DashboardSummaryDto
import com.mirza.dashboard.data.datasource.RecentTransactionDto
import com.mirza.dashboard.domain.model.AccountSummary
import com.mirza.dashboard.domain.model.DashboardSummary
import com.mirza.dashboard.domain.model.RecentTransaction

fun DashboardSummaryDto.toDomain(): DashboardSummary = DashboardSummary(
    totalBalance = totalBalance,
    currency = currency,
    accountSummaries = accountSummaries.map { it.toDomain() },
    recentTransactions = recentTransactions.map { it.toDomain() }
)

fun AccountSummaryDto.toDomain(): AccountSummary = AccountSummary(
    accountId = accountId,
    accountName = accountName,
    maskedAccountNumber = maskedAccountNumber,
    balance = balance
)

fun RecentTransactionDto.toDomain(): RecentTransaction = RecentTransaction(
    id = id,
    title = title,
    amount = amount,
    isCredit = isCredit,
    date = date
)