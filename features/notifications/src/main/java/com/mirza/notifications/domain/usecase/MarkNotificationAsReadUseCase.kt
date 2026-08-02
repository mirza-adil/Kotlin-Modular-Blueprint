package com.mirza.notifications.domain.usecase

import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import com.mirza.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class MarkNotificationAsReadUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<String, Unit>(dispatcher) {

    override suspend fun execute(parameters: String) {
        notificationRepository.markAsRead(parameters).getOrThrow()
    }
}