package com.mirza.payments.domain.usecase

import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import com.mirza.payments.domain.model.Payment
import com.mirza.payments.domain.model.PaymentCategory
import com.mirza.payments.domain.repository.PaymentRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class PayBillUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<PayBillUseCase.Params, Payment>(dispatcher) {

    data class Params(
        val billerName: String,
        val category: PaymentCategory,
        val amount: Double,
        val currency: String
    )

    override suspend fun execute(parameters: Params): Payment =
        paymentRepository.payBill(
            billerName = parameters.billerName,
            category = parameters.category,
            amount = parameters.amount,
            currency = parameters.currency
        ).getOrThrow()
}