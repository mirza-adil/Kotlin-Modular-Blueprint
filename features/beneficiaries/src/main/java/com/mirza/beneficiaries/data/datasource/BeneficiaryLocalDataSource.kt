package com.mirza.beneficiaries.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface BeneficiaryLocalDataSource {

    fun observeBeneficiaries(): Flow<List<BeneficiaryDto>>

    suspend fun getBeneficiaryById(beneficiaryId: String): BeneficiaryDto?

    suspend fun cacheBeneficiaries(beneficiaries: List<BeneficiaryDto>)

    suspend fun upsertBeneficiary(beneficiary: BeneficiaryDto)

    suspend fun removeBeneficiary(beneficiaryId: String)
}

@Singleton
class BeneficiaryLocalDataSourceImpl @Inject constructor() : BeneficiaryLocalDataSource {

    private val cachedBeneficiaries = MutableStateFlow<List<BeneficiaryDto>>(emptyList())

    override fun observeBeneficiaries(): Flow<List<BeneficiaryDto>> = cachedBeneficiaries.asStateFlow()

    override suspend fun getBeneficiaryById(beneficiaryId: String): BeneficiaryDto? =
        cachedBeneficiaries.value.find { it.id == beneficiaryId }

    override suspend fun cacheBeneficiaries(beneficiaries: List<BeneficiaryDto>) {
        cachedBeneficiaries.value = beneficiaries
    }

    override suspend fun upsertBeneficiary(beneficiary: BeneficiaryDto) {
        val current = cachedBeneficiaries.value.filterNot { it.id == beneficiary.id }
        cachedBeneficiaries.value = current + beneficiary
    }

    override suspend fun removeBeneficiary(beneficiaryId: String) {
        cachedBeneficiaries.value = cachedBeneficiaries.value.filterNot { it.id == beneficiaryId }
    }
}