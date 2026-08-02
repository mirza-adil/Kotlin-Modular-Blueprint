package com.mirza.payments.domain.usecase

import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import com.mirza.payments.domain.repository.PaymentRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CancelScheduledPaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<String, Unit>(dispatcher) {

    override suspend fun execute(parameters: String) {
        paymentRepository.cancelScheduledPayment(parameters).getOrThrow()
    }
}