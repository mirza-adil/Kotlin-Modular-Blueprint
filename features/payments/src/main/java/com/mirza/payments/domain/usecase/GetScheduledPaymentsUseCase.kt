package com.mirza.payments.domain.usecase

import com.mirza.common.base.FlowUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.Result
import com.mirza.payments.domain.model.ScheduledPayment
import com.mirza.payments.domain.repository.PaymentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetScheduledPaymentsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<ScheduledPayment>>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Result<List<ScheduledPayment>>> =
        paymentRepository.getScheduledPayments()
}