package com.mirza.notifications.domain.usecase

import com.mirza.common.base.FlowUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.Result
import com.mirza.notifications.domain.model.NotificationItem
import com.mirza.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<NotificationItem>>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Result<List<NotificationItem>>> =
        notificationRepository.getNotifications()
}