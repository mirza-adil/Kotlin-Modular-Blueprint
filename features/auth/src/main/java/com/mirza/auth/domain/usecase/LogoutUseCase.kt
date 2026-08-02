package com.mirza.auth.domain.usecase

import com.mirza.auth.domain.repository.AuthRepository
import com.mirza.common.base.NoParamUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : NoParamUseCase<Unit>(dispatcher) {

    override suspend fun execute() {
        authRepository.logout().getOrThrow()
    }
}