package com.mirza.profile.domain.usecase

import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import com.mirza.profile.domain.model.UserProfile
import com.mirza.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<UserProfile, Unit>(dispatcher) {

    override suspend fun execute(parameters: UserProfile) {
        profileRepository.updateUserProfile(parameters).getOrThrow()
    }
}