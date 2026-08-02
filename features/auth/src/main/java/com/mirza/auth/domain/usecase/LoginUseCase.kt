package com.mirza.auth.domain.usecase

import com.mirza.auth.domain.model.User
import com.mirza.auth.domain.repository.AuthRepository
import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<LoginUseCase.Params, User>(dispatcher) {

    data class Params(val email: String, val password: String)

    override suspend fun execute(parameters: Params): User =
        authRepository.login(parameters.email, parameters.password).getOrThrow()
}