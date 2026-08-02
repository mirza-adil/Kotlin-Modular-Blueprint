package com.mirza.profile.domain.usecase

import com.mirza.common.base.FlowUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.Result
import com.mirza.profile.domain.model.AppPreferences
import com.mirza.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPreferencesUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, AppPreferences>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Result<AppPreferences>> =
        profileRepository.getPreferences()
}