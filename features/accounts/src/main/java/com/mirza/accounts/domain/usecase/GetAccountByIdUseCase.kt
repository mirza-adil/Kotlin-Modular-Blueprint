package com.mirza.accounts.domain.usecase

import com.mirza.accounts.domain.model.Account
import com.mirza.accounts.domain.repository.AccountRepository
import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetAccountByIdUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<String, Account>(dispatcher) {

    override suspend fun execute(parameters: String): Account =
        accountRepository.getAccountById(parameters).getOrThrow()
}