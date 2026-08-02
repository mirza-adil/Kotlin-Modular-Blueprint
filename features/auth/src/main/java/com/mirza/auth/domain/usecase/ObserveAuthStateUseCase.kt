package com.mirza.auth.domain.usecase

import com.mirza.auth.domain.repository.AuthRepository
import com.mirza.common.base.NoParamFlowUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.extensions.asResult
import com.mirza.common.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : NoParamFlowUseCase<Boolean>(dispatcher) {

    override fun execute(): Flow<Result<Boolean>> =
        authRepository.observeAuthState().asResult()
}