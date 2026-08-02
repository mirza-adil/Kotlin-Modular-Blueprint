package com.mirza.payments.domain.usecase

import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import com.mirza.payments.domain.model.ScheduledPayment
import com.mirza.payments.domain.repository.PaymentRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class ScheduleBillPaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<ScheduledPayment, Unit>(dispatcher) {

    override suspend fun execute(parameters: ScheduledPayment) {
        paymentRepository.scheduleBillPayment(parameters).getOrThrow()
    }
}