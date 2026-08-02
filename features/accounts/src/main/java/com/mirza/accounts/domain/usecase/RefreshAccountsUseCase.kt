package com.mirza.accounts.domain.usecase

import com.mirza.accounts.domain.repository.AccountRepository
import com.mirza.common.base.NoParamUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class RefreshAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : NoParamUseCase<Unit>(dispatcher) {

    override suspend fun execute() {
        accountRepository.refreshAccounts().getOrThrow()
    }
}