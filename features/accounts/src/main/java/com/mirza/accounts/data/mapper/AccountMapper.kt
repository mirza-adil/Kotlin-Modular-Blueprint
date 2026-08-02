package com.mirza.accounts.data.mapper

import com.mirza.accounts.data.datasource.AccountDto
import com.mirza.accounts.domain.model.Account
import com.mirza.accounts.domain.model.AccountStatus
import com.mirza.accounts.domain.model.AccountType

fun AccountDto.toDomain(): Account = Account(
    id = id,
    accountNumber = accountNumber,
    accountHolderName = accountHolderName,
    accountType = runCatching { AccountType.valueOf(accountType) }.getOrDefault(AccountType.SAVINGS),
    balance = balance,
    currency = currency,
    status = runCatching { AccountStatus.valueOf(status) }.getOrDefault(AccountStatus.ACTIVE)
)

fun List<AccountDto>.toDomain(): List<Account> = map { it.toDomain() }