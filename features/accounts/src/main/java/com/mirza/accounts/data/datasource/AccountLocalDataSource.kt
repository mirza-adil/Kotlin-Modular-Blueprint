package com.mirza.accounts.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface AccountLocalDataSource {

    fun observeAccounts(): Flow<List<AccountDto>>

    suspend fun getAccountById(accountId: String): AccountDto?

    suspend fun cacheAccounts(accounts: List<AccountDto>)
}

@Singleton
class AccountLocalDataSourceImpl @Inject constructor() : AccountLocalDataSource {

    private val cachedAccounts = MutableStateFlow<List<AccountDto>>(emptyList())

    override fun observeAccounts(): Flow<List<AccountDto>> = cachedAccounts.asStateFlow()

    override suspend fun getAccountById(accountId: String): AccountDto? =
        cachedAccounts.value.find { it.id == accountId }

    override suspend fun cacheAccounts(accounts: List<AccountDto>) {
        cachedAccounts.value = accounts
    }
}