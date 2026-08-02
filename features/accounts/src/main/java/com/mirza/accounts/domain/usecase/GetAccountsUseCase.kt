package com.mirza.accounts.domain.usecase

import com.mirza.accounts.domain.model.Account
import com.mirza.accounts.domain.repository.AccountRepository
import com.mirza.common.base.FlowUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<Account>>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Result<List<Account>>> =
        accountRepository.getAccounts()
}