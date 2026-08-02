package com.mirza.beneficiaries.data.datasource

import javax.inject.Inject

interface BeneficiaryRemoteDataSource {

    suspend fun getBeneficiaries(): List<BeneficiaryDto>

    suspend fun addBeneficiary(beneficiary: BeneficiaryDto)

    suspend fun deleteBeneficiary(beneficiaryId: String)
}

class BeneficiaryRemoteDataSourceImpl @Inject constructor() : BeneficiaryRemoteDataSource {

    override suspend fun getBeneficiaries(): List<BeneficiaryDto> {
        // TODO: wire up to the real beneficiaries endpoint once it is available
        return emptyList()
    }

    override suspend fun addBeneficiary(beneficiary: BeneficiaryDto) {
        // TODO: wire up to the real beneficiaries endpoint once it is available
    }

    override suspend fun deleteBeneficiary(beneficiaryId: String) {
        // TODO: wire up to the real beneficiaries endpoint once it is available
    }
}