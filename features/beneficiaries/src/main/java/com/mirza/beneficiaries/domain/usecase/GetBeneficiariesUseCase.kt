package com.mirza.beneficiaries.domain.usecase

import com.mirza.beneficiaries.domain.model.Beneficiary
import com.mirza.beneficiaries.domain.repository.BeneficiaryRepository
import com.mirza.common.base.FlowUseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBeneficiariesUseCase @Inject constructor(
    private val beneficiaryRepository: BeneficiaryRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<Beneficiary>>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Result<List<Beneficiary>>> =
        beneficiaryRepository.getBeneficiaries()
}