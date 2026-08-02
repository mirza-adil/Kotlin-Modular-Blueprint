package com.mirza.accounts.data.repository

import com.mirza.accounts.data.datasource.AccountLocalDataSource
import com.mirza.accounts.data.datasource.AccountRemoteDataSource
import com.mirza.accounts.data.mapper.toDomain
import com.mirza.accounts.domain.model.Account
import com.mirza.accounts.domain.repository.AccountRepository
import com.mirza.common.extensions.asResult
import com.mirza.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val remoteDataSource: AccountRemoteDataSource,
    private val localDataSource: AccountLocalDataSource
) : AccountRepository {

    override fun getAccounts(): Flow<Result<List<Account>>> =
        localDataSource.observeAccounts()
            .map { it.toDomain() }
            .asResult()

    override suspend fun getAccountById(accountId: String): Result<Account> {
        return try {
            val dto = localDataSource.getAccountById(accountId)
                ?: remoteDataSource.getAccountById(accountId)
            Result.Success(dto.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshAccounts(): Result<Unit> {
        return try {
            val accounts = remoteDataSource.getAccounts()
            localDataSource.cacheAccounts(accounts)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}