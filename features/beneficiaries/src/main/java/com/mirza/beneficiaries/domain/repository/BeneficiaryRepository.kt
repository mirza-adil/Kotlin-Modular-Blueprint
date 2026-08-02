package com.mirza.beneficiaries.domain.repository

import com.mirza.beneficiaries.domain.model.Beneficiary
import com.mirza.common.result.Result
import kotlinx.coroutines.flow.Flow

interface BeneficiaryRepository {

    fun getBeneficiaries(): Flow<Result<List<Beneficiary>>>

    suspend fun getBeneficiaryById(beneficiaryId: String): Result<Beneficiary>

    suspend fun addBeneficiary(beneficiary: Beneficiary): Result<Unit>

    suspend fun deleteBeneficiary(beneficiaryId: String): Result<Unit>

    suspend fun refreshBeneficiaries(): Result<Unit>
}