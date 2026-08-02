package com.mirza.accounts.data.datasource

import javax.inject.Inject

interface AccountRemoteDataSource {

    suspend fun getAccounts(): List<AccountDto>

    suspend fun getAccountById(accountId: String): AccountDto
}

class AccountRemoteDataSourceImpl @Inject constructor() : AccountRemoteDataSource {

    override suspend fun getAccounts(): List<AccountDto> {
        // TODO: wire up to the real accounts endpoint once it is available
        return emptyList()
    }

    override suspend fun getAccountById(accountId: String): AccountDto {
        throw NoSuchElementException("Account $accountId not found")
    }
}