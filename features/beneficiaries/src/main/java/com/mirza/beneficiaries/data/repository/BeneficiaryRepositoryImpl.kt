package com.mirza.beneficiaries.data.repository

import com.mirza.beneficiaries.data.datasource.BeneficiaryLocalDataSource
import com.mirza.beneficiaries.data.datasource.BeneficiaryRemoteDataSource
import com.mirza.beneficiaries.data.mapper.toDomain
import com.mirza.beneficiaries.data.mapper.toDto
import com.mirza.beneficiaries.domain.model.Beneficiary
import com.mirza.beneficiaries.domain.repository.BeneficiaryRepository
import com.mirza.common.extensions.asResult
import com.mirza.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeneficiaryRepositoryImpl @Inject constructor(
    private val remoteDataSource: BeneficiaryRemoteDataSource,
    private val localDataSource: BeneficiaryLocalDataSource
) : BeneficiaryRepository {

    override fun getBeneficiaries(): Flow<Result<List<Beneficiary>>> =
        localDataSource.observeBeneficiaries()
            .map { it.toDomain() }
            .asResult()

    override suspend fun getBeneficiaryById(beneficiaryId: String): Result<Beneficiary> {
        return try {
            val dto = localDataSource.getBeneficiaryById(beneficiaryId)
                ?: throw NoSuchElementException("Beneficiary $beneficiaryId not found")
            Result.Success(dto.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addBeneficiary(beneficiary: Beneficiary): Result<Unit> {
        return try {
            val dto = beneficiary.toDto()
            remoteDataSource.addBeneficiary(dto)
            localDataSource.upsertBeneficiary(dto)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteBeneficiary(beneficiaryId: String): Result<Unit> {
        return try {
            remoteDataSource.deleteBeneficiary(beneficiaryId)
            localDataSource.removeBeneficiary(beneficiaryId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshBeneficiaries(): Result<Unit> {
        return try {
            val beneficiaries = remoteDataSource.getBeneficiaries()
            localDataSource.cacheBeneficiaries(beneficiaries)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}