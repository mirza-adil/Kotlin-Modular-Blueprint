package com.mirza.notifications.domain.usecase

import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import com.mirza.notifications.domain.model.NotificationPreferences
import com.mirza.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class UpdateNotificationPreferencesUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<NotificationPreferences, Unit>(dispatcher) {

    override suspend fun execute(parameters: NotificationPreferences) {
        notificationRepository.updatePreferences(parameters).getOrThrow()
    }
}