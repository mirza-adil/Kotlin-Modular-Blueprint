package com.mirza.beneficiaries.domain.usecase

import com.mirza.beneficiaries.domain.model.Beneficiary
import com.mirza.beneficiaries.domain.repository.BeneficiaryRepository
import com.mirza.common.base.UseCase
import com.mirza.common.dispatcher.IoDispatcher
import com.mirza.common.result.getOrThrow
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class AddBeneficiaryUseCase @Inject constructor(
    private val beneficiaryRepository: BeneficiaryRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<Beneficiary, Unit>(dispatcher) {

    override suspend fun execute(parameters: Beneficiary) {
        beneficiaryRepository.addBeneficiary(parameters).getOrThrow()
    }
}