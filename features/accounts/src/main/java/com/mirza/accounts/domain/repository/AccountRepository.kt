package com.mirza.accounts.domain.repository

import com.mirza.accounts.domain.model.Account
import com.mirza.common.result.Result
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    fun getAccounts(): Flow<Result<List<Account>>>

    suspend fun getAccountById(accountId: String): Result<Account>

    suspend fun refreshAccounts(): Result<Unit>
}